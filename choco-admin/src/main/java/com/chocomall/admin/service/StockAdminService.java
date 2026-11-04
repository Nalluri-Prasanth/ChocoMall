package com.chocomall.admin.service;

import com.chocomall.admin.dto.OpsDtos.StockAdjustParam;
import com.chocomall.admin.dto.OpsDtos.StockCountParam;
import com.chocomall.admin.dto.OpsDtos.StockRow;
import com.chocomall.admin.security.CurrentAdmin;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.PageParams;
import com.chocomall.core.stock.StockService;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.StockMovement;
import com.chocomall.model.enums.StockReason;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.StockMovementRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inventory view, manual adjustments, stock-takes and the stock ledger. */
@Service
@RequiredArgsConstructor
public class StockAdminService {

    private final ProductRepository products;
    private final StockMovementRepository movements;
    private final StockService stock;

    @Transactional(readOnly = true)
    public CommonPage<StockRow> inventory(String keyword, boolean lowOnly, Integer pageNum, Integer pageSize) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            all.add(cb.isFalse(root.get("preorderOnly")));
            all.add(cb.isFalse(root.get("buildABox")));
            all.add(root.get("kind").in(com.chocomall.model.enums.ProductKind.MADE_IN_SHOP,
                    com.chocomall.model.enums.ProductKind.MADE_FRESH, com.chocomall.model.enums.ProductKind.SEALED_RETAIL));
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                all.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("productSn")), like)));
            }
            if (lowOnly) {
                all.add(cb.le(cb.diff(root.<Integer>get("stock"), root.<Integer>get("lockStock")), root.<Integer>get("lowStock")));
            }
            return cb.and(all.toArray(Predicate[]::new));
        };
        var page = products.findAll(spec, PageParams.of(pageNum, pageSize, Sort.by("name")));
        return CommonPage.of(page, StockAdminService::row);
    }

    @Transactional
    public int adjust(StockAdjustParam p) {
        Product product = Asserts.found(products.findById(p.productId()), "Product");
        Asserts.state(StockService.tracksStock(product), product.getName() + " is not tracked in stock");
        Asserts.isTrue(p.delta() != 0, "The change cannot be zero");
        switch (p.reason()) {
            case WASTE -> Asserts.isTrue(p.delta() < 0, "Waste removes units, use a negative number");
            case PRODUCTION -> Asserts.isTrue(p.delta() > 0, "Production adds units, use a positive number");
            case ADJUSTMENT -> { }
            default -> Asserts.isTrue(false, "Use WASTE, PRODUCTION or ADJUSTMENT for manual changes");
        }
        return stock.adjust(p.productId(), p.delta(), p.reason(), p.reference(), CurrentAdmin.username());
    }

    @Transactional
    public int count(StockCountParam p) {
        Product product = Asserts.found(products.findById(p.productId()), "Product");
        Asserts.state(StockService.tracksStock(product), product.getName() + " is not tracked in stock");
        return stock.setCount(p.productId(), p.counted(), CurrentAdmin.username());
    }

    @Transactional(readOnly = true)
    public CommonPage<StockMovement> ledger(Long productId, Integer pageNum, Integer pageSize) {
        return CommonPage.of(movements.findByProductIdOrderByIdDesc(productId, PageRequest.of(Math.max(pageNum, 1) - 1, pageSize)));
    }

    @Transactional(readOnly = true)
    public List<StockRow> lowStock() {
        return products.findLowStock().stream().filter(StockService::tracksStock).map(StockAdminService::row).toList();
    }

    private static StockRow row(Product p) {
        int available = p.getStock() - p.getLockStock();
        return new StockRow(p.getId(), p.getProductSn(), p.getName(), p.getUnit(), p.getStock(), p.getLockStock(),
                available, p.getLowStock(), available <= p.getLowStock());
    }

    /** Reasons that make sense for a hand-entered movement. */
    public static List<StockReason> manualReasons() {
        return List.of(StockReason.WASTE, StockReason.PRODUCTION, StockReason.ADJUSTMENT);
    }
}
