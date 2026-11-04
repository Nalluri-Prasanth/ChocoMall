package com.chocomall.portal.service;

import com.chocomall.common.exception.Asserts;
import com.chocomall.core.order.BoxLine;
import com.chocomall.core.order.OrderItemUnits;
import com.chocomall.core.stock.StockService;
import com.chocomall.model.entity.CartItem;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ProductSku;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.model.repository.CartItemRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ProductSkuRepository;
import com.chocomall.portal.dto.PortalDtos.BoxLineParam;
import com.chocomall.portal.dto.PortalDtos.BoxLineView;
import com.chocomall.portal.dto.PortalDtos.CartAddParam;
import com.chocomall.portal.dto.PortalDtos.CartItemView;
import com.chocomall.portal.dto.PortalDtos.CartView;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The shopping basket. Prices are snapshotted for display but always re-read from the SKU at checkout. A
 * build-a-box line stores the chosen pieces, which must fill the box exactly and come from bonbons that are in stock.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    public static final int MAX_LINE_QUANTITY = 50;

    private final CartItemRepository cart;
    private final ProductRepository products;
    private final ProductSkuRepository skus;
    private final CatalogViewService catalog;
    private final OrderItemUnits boxJson;

    public CartView add(Long memberId, CartAddParam p) {
        Product product = Asserts.found(products.findById(p.productId()).filter(Product::isPublished), "Product");
        Asserts.state(product.getKind() != ProductKind.PERSONALIZED && product.getKind() != ProductKind.EXPERIENCE,
                product.getKind() == ProductKind.EXPERIENCE ? "Experiences are booked from the sessions page" : "Personalised items are requested, not added to the basket");
        ProductSku sku = resolveSku(product, p.skuId());
        Asserts.isTrue(p.quantity() >= 1 && p.quantity() <= MAX_LINE_QUANTITY, "Quantity must be between 1 and " + MAX_LINE_QUANTITY);

        String box = null;
        if (product.isBuildABox()) {
            box = boxJson.write(normalizeBox(sku, p.box()));
        } else {
            Asserts.isTrue(p.box() == null || p.box().isEmpty(), "Only a build-a-box product takes a selection of pieces");
        }
        if (StockService.tracksStock(product)) {
            int available = product.getStock() - product.getLockStock();
            Asserts.state(available >= p.quantity() * sku.getUnits(), "Only " + available + " " + product.getUnit() + "(s) of " + product.getName() + " are available");
        }

        final String boxSignature = box;
        CartItem line = cart.findByMemberIdAndProductIdAndProductSkuId(memberId, product.getId(), sku.getId()).stream()
                .filter(c -> Objects.equals(c.getBoxSelection(), boxSignature)).findFirst().orElse(null);
        if (line == null) {
            line = new CartItem();
            line.setMemberId(memberId);
            line.setProductId(product.getId());
            line.setProductSkuId(sku.getId());
            line.setQuantity(0);
            line.setProductName(product.getName());
            line.setProductSkuLabel(sku.getLabel());
            line.setProductPic(product.getPic());
            line.setBoxSelection(box);
        }
        int quantity = line.getQuantity() + p.quantity();
        Asserts.isTrue(quantity <= MAX_LINE_QUANTITY, "A basket line holds at most " + MAX_LINE_QUANTITY + " of the same item");
        line.setQuantity(quantity);
        line.setPrice(sku.getPrice());
        cart.save(line);
        return view(memberId);
    }

    @Transactional(readOnly = true)
    public CartView view(Long memberId) {
        List<CartItem> lines = cart.findByMemberIdOrderByIdDesc(memberId);
        return toView(lines);
    }

    public CartView updateQuantity(Long memberId, Long id, int quantity) {
        CartItem line = Asserts.found(cart.findByIdAndMemberId(id, memberId), "Basket item");
        Asserts.isTrue(quantity >= 1 && quantity <= MAX_LINE_QUANTITY, "Quantity must be between 1 and " + MAX_LINE_QUANTITY);
        line.setQuantity(quantity);
        return view(memberId);
    }

    public CartView remove(Long memberId, Set<Long> ids) {
        cart.deleteByMemberIdAndIdIn(memberId, ids);
        return view(memberId);
    }

    public void clear(Long memberId) {
        cart.deleteByMemberId(memberId);
    }

    /** Builds the display model for the given basket lines (also used by checkout). */
    @Transactional(readOnly = true)
    public CartView toView(List<CartItem> lines) {
        Map<Long, Product> byProduct = products.findByIdIn(lines.stream().map(CartItem::getProductId).toList()).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, ProductSku> bySku = new HashMap<>();
        skus.findAllById(lines.stream().map(CartItem::getProductSkuId).filter(Objects::nonNull).toList()).forEach(s -> bySku.put(s.getId(), s));
        Map<Long, String> boxNames = new HashMap<>();

        List<CartItemView> views = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int count = 0;
        for (CartItem l : lines) {
            Product p = byProduct.get(l.getProductId());
            ProductSku s = l.getProductSkuId() == null ? null : bySku.get(l.getProductSkuId());
            boolean live = p != null && p.isPublished() && s != null && s.isActive();
            BigDecimal unit = s == null ? l.getPrice() : s.getPrice();
            boolean available = live;
            if (live && StockService.tracksStock(p)) {
                available = p.getStock() - p.getLockStock() >= l.getQuantity() * s.getUnits();
            }
            List<BoxLineView> box = null;
            if (l.getBoxSelection() != null) {
                List<BoxLine> lines2 = boxJson.parse(l.getBoxSelection());
                products.findByIdIn(lines2.stream().map(BoxLine::productId).toList()).forEach(bp -> boxNames.put(bp.getId(), bp.getName()));
                box = lines2.stream().map(b -> new BoxLineView(b.productId(), boxNames.getOrDefault(b.productId(), "Unavailable"), b.quantity())).toList();
                if (live) {
                    for (BoxLine b : lines2) {
                        Product bp = products.findById(b.productId()).orElse(null);
                        if (bp == null || !bp.isPublished() || bp.getStock() - bp.getLockStock() < b.quantity() * l.getQuantity()) {
                            available = false;
                        }
                    }
                }
            }
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(l.getQuantity()));
            views.add(new CartItemView(l.getId(), l.getProductId(), l.getProductSkuId(), p == null ? l.getProductName() : p.getName(),
                    s == null ? l.getProductSkuLabel() : s.getLabel(), l.getProductPic(), l.getQuantity(), unit, lineTotal,
                    l.getPrice() != null && unit.compareTo(l.getPrice()) != 0, available, p != null && p.isRequiresColdChain(),
                    p != null && p.isPreorderOnly(), box));
            if (available) {
                subtotal = subtotal.add(lineTotal);
            }
            count += l.getQuantity();
        }
        return new CartView(views, subtotal, count);
    }

    private ProductSku resolveSku(Product product, Long skuId) {
        List<ProductSku> active = skus.findByProductIdOrderBySortAscIdAsc(product.getId()).stream().filter(ProductSku::isActive).toList();
        Asserts.state(!active.isEmpty(), product.getName() + " is not available right now");
        if (skuId == null) {
            Asserts.isTrue(active.size() == 1, "Choose a size for " + product.getName());
            return active.get(0);
        }
        return active.stream().filter(s -> s.getId().equals(skuId)).findFirst()
                .orElseThrow(() -> Asserts.notFound("Size " + skuId + " of " + product.getName()));
    }

    /** The chosen pieces must fill the box exactly and be bonbons that are on the shelf. */
    private List<BoxLine> normalizeBox(ProductSku sku, List<BoxLineParam> chosen) {
        Asserts.isTrue(chosen != null && !chosen.isEmpty(), "Choose the pieces for your box");
        Map<Long, Integer> merged = new TreeMap<>();
        for (BoxLineParam b : chosen) {
            merged.merge(b.productId(), b.quantity(), Integer::sum);
        }
        int total = merged.values().stream().mapToInt(Integer::intValue).sum();
        Asserts.isTrue(total == sku.getUnits(), "This box holds exactly " + sku.getUnits() + " pieces; you chose " + total);
        Map<Long, Product> allowed = catalog.boxCandidates().stream().collect(Collectors.toMap(Product::getId, p -> p));
        List<BoxLine> out = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : merged.entrySet()) {
            Product p = allowed.get(e.getKey());
            Asserts.isTrue(p != null, "Product " + e.getKey() + " cannot go in a box (only bonbons that are in stock)");
            Asserts.state(p.getStock() - p.getLockStock() >= e.getValue(), "Only " + (p.getStock() - p.getLockStock()) + " of " + p.getName() + " are left");
            out.add(new BoxLine(e.getKey(), e.getValue()));
        }
        out.sort(Comparator.comparing(BoxLine::productId));
        return out;
    }
}
