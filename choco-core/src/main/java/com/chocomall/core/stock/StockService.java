package com.chocomall.core.stock;

import com.chocomall.common.api.ResultCode;
import com.chocomall.common.exception.ApiException;
import com.chocomall.common.exception.Asserts;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.StockMovement;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.model.enums.StockReason;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only place that changes stock. Quantities are in the product's base unit (a piece, a bar, a cup...), so a
 * "box of 12" SKU consumes 12 units of the same product. Every change is an atomic conditional UPDATE (no
 * read-modify-write), which makes overselling impossible under concurrent checkouts, and is recorded in the
 * stock ledger.
 *
 * <p>Only items that sit on a shelf are tracked. Made-to-order drinks, experiences, personalised items and
 * preorder lines do not draw down stock (preorders consume the capacity of a preorder window instead).
 */
@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository products;
    private final StockMovementRepository movements;

    public static boolean tracksStock(Product p) {
        if (p.isPreorderOnly() || p.isBuildABox()) {
            return false;
        }
        return p.getKind() == ProductKind.MADE_IN_SHOP || p.getKind() == ProductKind.MADE_FRESH
                || p.getKind() == ProductKind.SEALED_RETAIL;
    }

    /** Reserves units for an unpaid order. Fails with 409 if fewer units are available. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(Product p, int units, String refSn) {
        if (!tracksStock(p) || units <= 0) {
            return;
        }
        if (products.lockStock(p.getId(), units) == 0) {
            throw new ApiException(ResultCode.CONFLICT, "Not enough stock for " + p.getName());
        }
        record(p.getId(), 0, StockReason.ORDER_LOCK, refSn, "system");
    }

    /** Returns reserved units, e.g. when an unpaid order is cancelled or times out. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Product p, int units, String refSn) {
        if (!tracksStock(p) || units <= 0) {
            return;
        }
        products.releaseStock(p.getId(), units);
        record(p.getId(), 0, StockReason.ORDER_RELEASE, refSn, "system");
    }

    /** Turns a reservation into a sale once the order is paid. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void confirmSale(Product p, int units, int soldCount, String refSn) {
        if (!tracksStock(p) || units <= 0) {
            return;
        }
        if (products.confirmSale(p.getId(), units, soldCount) == 0) {
            throw new ApiException(ResultCode.CONFLICT, "Stock reservation is missing for " + p.getName());
        }
        record(p.getId(), -units, StockReason.ORDER_SALE, refSn, "system");
    }

    /** Puts units back on the shelf after a paid order is cancelled or a return is received. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void restock(Product p, int units, StockReason reason, String refSn, String operator) {
        if (!tracksStock(p) || units <= 0) {
            return;
        }
        products.addStock(p.getId(), units);
        record(p.getId(), units, reason, refSn, operator);
    }

    /** Production releases and waste adjustments. Positive adds, negative removes (never below reserved units). */
    @Transactional
    public int adjust(Long productId, int deltaUnits, StockReason reason, String refSn, String operator) {
        Product p = Asserts.found(products.findById(productId), "Product");
        if (deltaUnits >= 0) {
            products.addStock(productId, deltaUnits);
        } else {
            int available = p.getStock() - p.getLockStock();
            Asserts.state(available + deltaUnits >= 0, "Cannot remove " + (-deltaUnits) + " units: only " + available + " are not reserved");
            products.addStock(productId, deltaUnits);
        }
        record(productId, deltaUnits, reason, refSn, operator);
        return products.stockOf(productId);
    }

    /** Sets the counted quantity (stock-take). */
    @Transactional
    public int setCount(Long productId, int counted, String operator) {
        Product p = Asserts.found(products.findById(productId), "Product");
        Asserts.isTrue(counted >= p.getLockStock(), "Counted stock cannot be below the " + p.getLockStock() + " reserved units");
        int delta = counted - p.getStock();
        products.setStock(productId, counted);
        record(productId, delta, StockReason.ADJUSTMENT, "stock-take", operator);
        return counted;
    }

    private void record(Long productId, int change, StockReason reason, String refSn, String operator) {
        StockMovement m = new StockMovement();
        m.setProductId(productId);
        m.setChangeUnits(change);
        Integer balance = products.stockOf(productId);
        m.setBalanceAfter(balance == null ? 0 : balance);
        m.setReason(reason);
        m.setRefSn(refSn);
        m.setOperator(operator);
        movements.save(m);
    }
}
