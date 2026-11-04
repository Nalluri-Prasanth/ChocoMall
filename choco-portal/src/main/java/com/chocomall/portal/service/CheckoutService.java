package com.chocomall.portal.service;

import com.chocomall.common.api.ResultCode;
import com.chocomall.common.exception.ApiException;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.SnGenerator;
import com.chocomall.core.loyalty.LoyaltyService;
import com.chocomall.core.messaging.OrderTimeoutPublisher;
import com.chocomall.core.order.BoxLine;
import com.chocomall.core.order.OrderItemUnits;
import com.chocomall.core.order.OrderLifecycleService;
import com.chocomall.core.stock.StockService;
import com.chocomall.core.tx.AfterCommit;
import com.chocomall.model.entity.Bundle;
import com.chocomall.model.entity.BundleItem;
import com.chocomall.model.entity.CartItem;
import com.chocomall.model.entity.Coupon;
import com.chocomall.model.entity.CouponHistory;
import com.chocomall.model.entity.Member;
import com.chocomall.model.entity.MemberAddress;
import com.chocomall.model.entity.MemberLevel;
import com.chocomall.model.entity.OrderItem;
import com.chocomall.model.entity.OrderOperateHistory;
import com.chocomall.model.entity.OrderSetting;
import com.chocomall.model.entity.PreorderWindow;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ProductSku;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.CouponType;
import com.chocomall.model.enums.CouponUseStatus;
import com.chocomall.model.enums.FulfillmentType;
import com.chocomall.model.enums.OrderStatus;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.model.repository.BundleItemRepository;
import com.chocomall.model.repository.BundleRepository;
import com.chocomall.model.repository.CartItemRepository;
import com.chocomall.model.repository.CouponHistoryRepository;
import com.chocomall.model.repository.CouponRepository;
import com.chocomall.model.repository.MemberAddressRepository;
import com.chocomall.model.repository.MemberLevelRepository;
import com.chocomall.model.repository.MemberRepository;
import com.chocomall.model.repository.OrderItemRepository;
import com.chocomall.model.repository.OrderOperateHistoryRepository;
import com.chocomall.model.repository.PreorderWindowRepository;
import com.chocomall.model.repository.ProductCategoryRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ProductSkuRepository;
import com.chocomall.model.repository.ShopOrderRepository;
import com.chocomall.portal.dto.PortalDtos.CartItemView;
import com.chocomall.portal.dto.PortalDtos.CheckoutParam;
import com.chocomall.portal.dto.PortalDtos.CheckoutPreview;
import com.chocomall.portal.dto.PortalDtos.CouponOption;
import com.chocomall.portal.dto.PortalDtos.OrderView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns basket lines into an order. {@link #preview} prices a basket without side effects; {@link #generate}
 * repeats the same calculation and then, in a single transaction, reserves preorder capacity, locks stock with
 * atomic updates, redeems points, marks the coupon used, writes the order and empties the basket lines.
 *
 * <p>Rules enforced here: prices come from the SKU (never from the client); a preorder cannot be mixed with items
 * available now; chilled items are pickup only; delivery needs the setting enabled and an address; pickup time
 * must respect the preparation lead time; points are capped at a share of the order; discounts are allocated
 * to lines so refunds can be computed exactly.
 */
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final int MAX_DAYS_AHEAD = 14;

    private final CartItemRepository cart;
    private final CartService cartService;
    private final ProductRepository products;
    private final ProductSkuRepository skus;
    private final ProductCategoryRepository categories;
    private final MemberRepository members;
    private final MemberLevelRepository levels;
    private final MemberAddressRepository addresses;
    private final CouponHistoryRepository couponHistories;
    private final CouponRepository coupons;
    private final BundleRepository bundles;
    private final BundleItemRepository bundleItems;
    private final PreorderWindowRepository windows;
    private final ShopOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderOperateHistoryRepository history;
    private final OrderLifecycleService lifecycle;
    private final StockService stock;
    private final LoyaltyService loyalty;
    private final OrderTimeoutPublisher timeoutPublisher;
    private final OrderItemUnits boxJson;

    /** A basket line resolved against the current catalog. */
    private record Line(CartItem item, Product product, ProductSku sku, int quantity, BigDecimal unitPrice, BigDecimal total,
                        List<BoxLine> box) {
    }

    /** The result of pricing a basket. */
    private static final class Calc {
        final List<Line> lines = new ArrayList<>();
        final List<String> problems = new ArrayList<>();
        final List<String> notices = new ArrayList<>();
        final List<CouponOption> couponOptions = new ArrayList<>();
        OrderSetting setting;
        Member member;
        MemberLevel level;
        MemberAddress address;
        PreorderWindow window;
        CouponHistory couponHistory;
        Coupon coupon;
        boolean preorder;
        LocalDateTime slot;
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal bundleDiscount = BigDecimal.ZERO;
        BigDecimal couponDiscount = BigDecimal.ZERO;
        BigDecimal pointsDiscount = BigDecimal.ZERO;
        BigDecimal deliveryFee = BigDecimal.ZERO;
        BigDecimal giftFee = BigDecimal.ZERO;
        BigDecimal pay = BigDecimal.ZERO;
        int pointsUsed;
        int maxPoints;
        int pointsToEarn;
        boolean deliveryAvailable;
        List<String> bundleNames = new ArrayList<>();
    }

    // ------------------------------------------------------------------------------------------- preview

    @Transactional(readOnly = true)
    public CheckoutPreview preview(Long memberId, CheckoutParam p) {
        Calc c = calculate(memberId, p);
        List<CartItemView> views = cartService.toView(c.lines.stream().map(Line::item).toList()).items();
        return new CheckoutPreview(views, c.subtotal, c.bundleDiscount, c.couponDiscount, c.pointsDiscount, c.deliveryFee, c.giftFee,
                c.pay, c.pointsUsed, c.maxPoints, c.pointsToEarn, c.preorder, c.deliveryAvailable, c.couponOptions, c.problems, c.notices);
    }

    // ------------------------------------------------------------------------------------------- generate

    @Transactional
    public OrderView generate(Long memberId, CheckoutParam p) {
        Calc c = calculate(memberId, p);
        if (!c.problems.isEmpty()) {
            throw new ApiException(ResultCode.VALIDATE_FAILED, String.join("; ", c.problems));
        }
        String orderSn = SnGenerator.next("ORD");
        int preorderUnits = c.lines.stream().mapToInt(l -> l.quantity() * l.sku().getUnits()).sum();

        if (c.preorder && windows.reserve(c.window.getId(), preorderUnits, LocalDateTime.now()) == 0) {
            throw new ApiException(ResultCode.CONFLICT, "The preorder window is full or its cut-off has passed");
        }
        lockStock(c, orderSn);

        ShopOrder order = new ShopOrder();
        order.setMemberId(memberId);
        order.setMemberUsername(c.member.getUsername());
        order.setOrderSn(orderSn);
        order.setTotalAmount(c.subtotal);
        order.setPromotionAmount(c.bundleDiscount);
        order.setCouponAmount(c.couponDiscount);
        order.setIntegrationAmount(c.pointsDiscount);
        order.setUseIntegration(c.pointsUsed);
        order.setFreightAmount(c.deliveryFee);
        order.setGiftPackagingAmount(c.giftFee);
        order.setPayAmount(c.pay);
        order.setFulfillmentType(p.fulfillmentType());
        order.setPreorder(c.preorder);
        order.setPreorderWindowId(c.window == null ? null : c.window.getId());
        order.setPreorderUnits(c.preorder ? preorderUnits : null);
        order.setPickupSlotStart(c.slot);
        order.setPickupSlotEnd(c.slot == null ? null : c.slot.plusMinutes(30));
        order.setAutoConfirmDays(c.setting.getAutoConfirmDays());
        order.setNote(p.note());
        order.setGiftMessage(p.giftMessage());
        order.setGiftSleeve(p.giftSleeve());
        if (c.address != null) {
            order.setReceiverName(c.address.getName());
            order.setReceiverPhone(c.address.getPhone());
            order.setReceiverPostCode(c.address.getPostCode());
            order.setReceiverProvince(c.address.getProvince());
            order.setReceiverCity(c.address.getCity());
            order.setReceiverRegion(c.address.getRegion());
            order.setReceiverDetailAddress(c.address.getDetailAddress());
        }
        if (c.couponHistory != null) {
            order.setCouponHistoryId(c.couponHistory.getId());
        }
        order = orders.saveAndFlush(order);

        saveItems(order, c);
        if (c.couponHistory != null) {
            c.couponHistory.setUseStatus(CouponUseStatus.USED);
            c.couponHistory.setUseTime(LocalDateTime.now());
            c.couponHistory.setOrderId(order.getId());
            c.couponHistory.setOrderSn(orderSn);
            coupons.addUsed(c.coupon.getId(), 1);
        }
        loyalty.redeem(memberId, c.pointsUsed, orderSn);
        cart.deleteByMemberIdAndIdIn(memberId, p.cartItemIds());
        record(order, c.member.getUsername(), "Order placed");

        if (c.pay.signum() == 0) {
            order = lifecycle.markPaid(order.getId(), null, "system");
        } else {
            Long id = order.getId();
            long delay = Duration.ofMinutes(c.setting.getNormalOrderTimeoutMinutes()).toMillis();
            AfterCommit.run(() -> timeoutPublisher.schedule(id, delay));
        }
        return new OrderView(order, orderItems.findByOrderId(order.getId()));
    }

    // ------------------------------------------------------------------------------------------- calculation

    private Calc calculate(Long memberId, CheckoutParam p) {
        Calc c = new Calc();
        c.setting = lifecycle.setting();
        c.member = Asserts.found(members.findById(memberId), "Member");
        c.level = c.member.getLevelId() == null ? null : levels.findById(c.member.getLevelId()).orElse(null);

        resolveLines(memberId, p, c);
        if (c.lines.isEmpty()) {
            c.problems.add("Your basket is empty");
            return c;
        }
        c.subtotal = c.lines.stream().map(Line::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        applyFulfillment(p, c);
        applyBundles(c);
        applyCoupons(memberId, p, c);
        applyPoints(memberId, p, c);
        applyFees(p, c);

        BigDecimal pay = c.subtotal.subtract(c.bundleDiscount).subtract(c.couponDiscount).subtract(c.pointsDiscount)
                .add(c.deliveryFee).add(c.giftFee);
        c.pay = pay.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal multiplier = c.level == null ? BigDecimal.ONE : c.level.getPointsMultiplier();
        c.pointsToEarn = c.pay.multiply(BigDecimal.valueOf(c.setting.getPointsPerCurrencySpent())).multiply(multiplier)
                .setScale(0, RoundingMode.DOWN).intValue();
        return c;
    }

    private void resolveLines(Long memberId, CheckoutParam p, Calc c) {
        List<CartItem> items = cart.findByMemberIdAndIdIn(memberId, p.cartItemIds());
        if (items.size() != p.cartItemIds().size()) {
            throw Asserts.notFound("One of the basket items");
        }
        Map<Long, Product> byProduct = products.findByIdIn(items.stream().map(CartItem::getProductId).toList()).stream()
                .collect(Collectors.toMap(Product::getId, x -> x));
        Map<Long, ProductSku> bySku = new HashMap<>();
        skus.findAllById(items.stream().map(CartItem::getProductSkuId).filter(java.util.Objects::nonNull).toList())
                .forEach(s -> bySku.put(s.getId(), s));
        for (CartItem item : items) {
            Product product = byProduct.get(item.getProductId());
            ProductSku sku = item.getProductSkuId() == null ? null : bySku.get(item.getProductSkuId());
            String name = item.getProductName();
            if (product == null || !product.isPublished() || sku == null || !sku.isActive()) {
                c.problems.add(name + " is no longer available. Remove it from your basket.");
                continue;
            }
            if (product.getKind() == ProductKind.PERSONALIZED || product.getKind() == ProductKind.EXPERIENCE) {
                c.problems.add(name + " cannot be ordered from the basket");
                continue;
            }
            List<BoxLine> box = item.getBoxSelection() == null ? null : boxJson.parse(item.getBoxSelection());
            BigDecimal total = sku.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            c.lines.add(new Line(item, product, sku, item.getQuantity(), sku.getPrice(), total, box));
        }
    }

    private void applyFulfillment(CheckoutParam p, Calc c) {
        boolean anyPreorder = c.lines.stream().anyMatch(l -> l.product().isPreorderOnly());
        boolean allPreorder = c.lines.stream().allMatch(l -> l.product().isPreorderOnly());
        c.preorder = anyPreorder;
        if (anyPreorder && !allPreorder) {
            c.problems.add("Preorder items must be ordered separately from items available now");
        }
        boolean cold = c.lines.stream().anyMatch(l -> l.product().isRequiresColdChain());
        c.deliveryAvailable = c.setting.isDeliveryEnabled() && !cold;
        LocalDateTime now = LocalDateTime.now();

        if (p.fulfillmentType() == FulfillmentType.DELIVERY) {
            if (cold) {
                c.problems.add("Chilled items (fresh fruit, mousse, frozen desserts) are available for pickup only");
            } else if (!c.setting.isDeliveryEnabled()) {
                c.problems.add("Delivery is not available right now. Choose pickup.");
            }
            if (p.addressId() == null) {
                c.problems.add("Choose a delivery address");
            } else {
                c.address = addresses.findByIdAndMemberId(p.addressId(), c.member.getId()).orElse(null);
                if (c.address == null) {
                    c.problems.add("The delivery address was not found");
                }
            }
        }
        if (c.preorder) {
            if (p.preorderWindowId() == null) {
                c.problems.add("Choose a preorder window");
            } else {
                c.window = windows.findById(p.preorderWindowId()).orElse(null);
                int units = c.lines.stream().mapToInt(l -> l.quantity() * l.sku().getUnits()).sum();
                if (c.window == null) {
                    c.problems.add("The preorder window was not found");
                } else if (c.window.getStatus() != com.chocomall.model.enums.WindowStatus.OPEN || !c.window.getOrderCutoff().isAfter(now)) {
                    c.problems.add("The preorder window \"" + c.window.getName() + "\" has closed");
                } else if (c.window.getCapacityUnits() - c.window.getReservedUnits() < units) {
                    c.problems.add("Only " + (c.window.getCapacityUnits() - c.window.getReservedUnits()) + " units are left in \"" + c.window.getName() + "\"");
                }
            }
            if (p.pickupSlot() == null) {
                c.problems.add(p.fulfillmentType() == FulfillmentType.PICKUP ? "Choose a collection date" : "Choose a delivery date");
            } else if (c.window != null && (p.pickupSlot().toLocalDate().isBefore(c.window.getFulfilFrom())
                    || p.pickupSlot().toLocalDate().isAfter(c.window.getFulfilTo()))) {
                c.problems.add("Choose a date between " + c.window.getFulfilFrom() + " and " + c.window.getFulfilTo());
            }
            c.slot = p.pickupSlot();
        } else if (p.fulfillmentType() == FulfillmentType.PICKUP) {
            LocalDateTime earliest = now.plusMinutes(c.setting.getPickupLeadMinutes());
            LocalDateTime slot = p.pickupSlot() == null ? earliest : p.pickupSlot();
            if (slot.isBefore(earliest.minusMinutes(1))) {
                c.problems.add("Orders need " + c.setting.getPickupLeadMinutes() + " minutes of preparation. Choose a later pickup time.");
            } else if (slot.isAfter(now.plusDays(MAX_DAYS_AHEAD))) {
                c.problems.add("Pickup can be booked up to " + MAX_DAYS_AHEAD + " days ahead");
            }
            c.slot = slot;
        }
    }

    /** A bundle applies once for every complete set found in the basket (e.g. one drink plus one brownie). */
    private void applyBundles(Calc c) {
        LocalDateTime now = LocalDateTime.now();
        Map<Integer, Integer> remaining = new TreeMap<>();
        for (int i = 0; i < c.lines.size(); i++) {
            remaining.put(i, c.lines.get(i).quantity());
        }
        BigDecimal discount = BigDecimal.ZERO;
        for (Bundle b : bundles.findByStatusTrueOrderBySortDesc()) {
            if ((b.getStartTime() != null && b.getStartTime().isAfter(now)) || (b.getEndTime() != null && b.getEndTime().isBefore(now))) {
                continue;
            }
            List<BundleItem> items = bundleItems.findByBundleId(b.getId());
            if (items.isEmpty()) {
                continue;
            }
            Map<BundleItem, Integer> lineOf = new HashMap<>();
            int sets = Integer.MAX_VALUE;
            for (BundleItem item : items) {
                int found = -1;
                for (int i = 0; i < c.lines.size(); i++) {
                    Line l = c.lines.get(i);
                    if (l.product().getId().equals(item.getProductId()) && (item.getSkuId() == null || item.getSkuId().equals(l.sku().getId()))) {
                        found = i;
                        break;
                    }
                }
                if (found < 0) {
                    sets = 0;
                    break;
                }
                lineOf.put(item, found);
                sets = Math.min(sets, remaining.get(found) / item.getQuantity());
            }
            if (sets <= 0 || sets == Integer.MAX_VALUE) {
                continue;
            }
            BigDecimal regular = BigDecimal.ZERO;
            for (BundleItem item : items) {
                Line l = c.lines.get(lineOf.get(item));
                regular = regular.add(l.unitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                remaining.merge(lineOf.get(item), -sets * item.getQuantity(), Integer::sum);
            }
            BigDecimal perSet = regular.subtract(b.getPrice());
            if (perSet.signum() > 0) {
                discount = discount.add(perSet.multiply(BigDecimal.valueOf(sets)));
                c.bundleNames.add(b.getName() + (sets > 1 ? " x" + sets : ""));
            }
        }
        c.bundleDiscount = discount.setScale(2, RoundingMode.HALF_UP);
        if (!c.bundleNames.isEmpty()) {
            c.notices.add("Bundle price applied: " + String.join(", ", c.bundleNames));
        }
    }

    private void applyCoupons(Long memberId, CheckoutParam p, Calc c) {
        LocalDateTime now = LocalDateTime.now();
        for (CouponHistory h : couponHistories.findByMemberIdAndUseStatus(memberId, CouponUseStatus.UNUSED)) {
            Coupon coupon = coupons.findById(h.getCouponId()).orElse(null);
            if (coupon == null) {
                continue;
            }
            Evaluation e = evaluate(coupon, c, now);
            c.couponOptions.add(new CouponOption(h.getId(), coupon.getName(), e.discount, e.reason == null, e.reason));
            if (p.couponHistoryId() != null && p.couponHistoryId().equals(h.getId())) {
                if (e.reason != null) {
                    c.problems.add("Coupon \"" + coupon.getName() + "\" cannot be used: " + e.reason);
                } else {
                    c.couponHistory = h;
                    c.coupon = coupon;
                    c.couponDiscount = e.discount;
                }
            }
        }
        if (p.couponHistoryId() != null && c.couponHistory == null && c.problems.stream().noneMatch(s -> s.startsWith("Coupon"))) {
            c.problems.add("The coupon was not found or was already used");
        }
    }

    private record Evaluation(BigDecimal discount, String reason) {
    }

    private Evaluation evaluate(Coupon coupon, Calc c, LocalDateTime now) {
        if (!coupon.isStatus() || coupon.getStartTime().isAfter(now) || coupon.getEndTime().isBefore(now)) {
            return new Evaluation(BigDecimal.ZERO, "it is not valid right now");
        }
        if (coupon.getMemberLevelId() != null) {
            MemberLevel required = levels.findById(coupon.getMemberLevelId()).orElse(null);
            int growth = required == null ? 0 : required.getGrowthPoint();
            if (c.level == null || c.level.getGrowthPoint() < growth) {
                return new Evaluation(BigDecimal.ZERO, "it is reserved for " + (required == null ? "a higher" : required.getName()) + " members");
            }
        }
        Set<Long> categoryIds = new HashSet<>(coupon.getCategoryIds());
        coupon.getCategoryIds().forEach(id -> categories.findByParentIdOrderBySortDesc(id).forEach(ch -> categoryIds.add(ch.getId())));
        BigDecimal eligible = BigDecimal.ZERO;
        for (Line l : c.lines) {
            boolean ok = switch (coupon.getCouponType()) {
                case ALL -> true;
                case CATEGORY -> categoryIds.contains(l.product().getCategoryId());
                case PRODUCT -> coupon.getProductIds().contains(l.product().getId());
            };
            if (ok) {
                eligible = eligible.add(l.total());
            }
        }
        if (eligible.signum() == 0) {
            return new Evaluation(BigDecimal.ZERO, "nothing in your basket qualifies");
        }
        if (eligible.compareTo(coupon.getMinSpend()) < 0) {
            return new Evaluation(BigDecimal.ZERO, "spend at least " + coupon.getMinSpend() + " on qualifying items");
        }
        return new Evaluation(coupon.getAmount().min(eligible).setScale(2, RoundingMode.HALF_UP), null);
    }

    private void applyPoints(Long memberId, CheckoutParam p, Calc c) {
        BigDecimal base = c.subtotal.subtract(c.bundleDiscount).subtract(c.couponDiscount).max(BigDecimal.ZERO);
        c.maxPoints = loyalty.maxRedeemablePoints(memberId, base, c.setting);
        int want = p.useIntegration() == null ? 0 : p.useIntegration();
        if (want > c.maxPoints) {
            c.problems.add("You can use at most " + c.maxPoints + " points on this order");
            want = c.maxPoints;
        }
        c.pointsUsed = want;
        c.pointsDiscount = loyalty.pointsValue(want, c.setting);
    }

    private void applyFees(CheckoutParam p, Calc c) {
        BigDecimal afterDiscounts = c.subtotal.subtract(c.bundleDiscount).subtract(c.couponDiscount);
        if (p.fulfillmentType() == FulfillmentType.DELIVERY) {
            BigDecimal threshold = c.level != null && c.level.getFreeDeliveryThreshold() != null
                    ? c.level.getFreeDeliveryThreshold() : c.setting.getFreeDeliveryThreshold();
            c.deliveryFee = afterDiscounts.compareTo(threshold) >= 0 ? BigDecimal.ZERO : c.setting.getDeliveryFee();
            if (c.deliveryFee.signum() > 0) {
                c.notices.add("Spend " + threshold.subtract(afterDiscounts).setScale(2, RoundingMode.HALF_UP) + " more for free delivery");
            }
        }
        c.giftFee = p.giftSleeve() ? c.setting.getGiftPackagingFee() : BigDecimal.ZERO;
    }

    // ------------------------------------------------------------------------------------------- persistence

    /** Locks stock for every product in a fixed order so concurrent checkouts cannot deadlock. */
    private void lockStock(Calc c, String orderSn) {
        List<OrderItem> temp = new ArrayList<>();
        for (Line l : c.lines) {
            OrderItem item = new OrderItem();
            item.setProductId(l.product().getId());
            item.setProductQuantity(l.quantity());
            item.setUnitsPerSku(l.sku().getUnits());
            item.setBoxSelection(l.item().getBoxSelection());
            temp.add(item);
        }
        Map<Long, Integer> units = new TreeMap<>(boxJson.unitsByProduct(temp));
        Map<Long, Product> byId = products.findByIdIn(units.keySet()).stream().collect(Collectors.toMap(Product::getId, x -> x));
        units.forEach((productId, qty) -> stock.lock(byId.get(productId), qty, orderSn));
    }

    private void saveItems(ShopOrder order, Calc c) {
        List<BigDecimal> weights = c.lines.stream().map(Line::total).toList();
        List<BigDecimal> promo = allocate(c.bundleDiscount, weights);
        List<BigDecimal> coupon = allocate(c.couponDiscount, weights);
        List<BigDecimal> points = allocate(c.pointsDiscount, weights);
        for (int i = 0; i < c.lines.size(); i++) {
            Line l = c.lines.get(i);
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setOrderSn(order.getOrderSn());
            item.setProductId(l.product().getId());
            item.setProductPic(l.product().getPic());
            item.setProductName(l.product().getName());
            item.setProductSn(l.product().getProductSn());
            item.setProductSkuId(l.sku().getId());
            item.setProductSkuCode(l.sku().getSkuCode());
            item.setSkuLabel(l.sku().getLabel());
            item.setUnitsPerSku(l.sku().getUnits());
            item.setProductPrice(l.unitPrice());
            item.setProductQuantity(l.quantity());
            item.setPromotionAmount(promo.get(i));
            item.setCouponAmount(coupon.get(i));
            item.setIntegrationAmount(points.get(i));
            item.setRealAmount(l.total().subtract(promo.get(i)).subtract(coupon.get(i)).subtract(points.get(i)).max(BigDecimal.ZERO));
            item.setBoxSelection(l.item().getBoxSelection());
            orderItems.save(item);
        }
    }

    /** Splits an amount over the lines in proportion to their value; the last line absorbs the rounding remainder. */
    static List<BigDecimal> allocate(BigDecimal amount, List<BigDecimal> weights) {
        List<BigDecimal> out = new ArrayList<>();
        BigDecimal totalWeight = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal left = amount;
        for (int i = 0; i < weights.size(); i++) {
            BigDecimal part;
            if (amount.signum() == 0 || totalWeight.signum() == 0) {
                part = BigDecimal.ZERO;
            } else if (i == weights.size() - 1) {
                part = left;
            } else {
                part = amount.multiply(weights.get(i)).divide(totalWeight, 2, RoundingMode.HALF_UP).min(left);
            }
            out.add(part.setScale(2, RoundingMode.HALF_UP));
            left = left.subtract(part);
        }
        return out;
    }

    private void record(ShopOrder order, String operator, String note) {
        OrderOperateHistory h = new OrderOperateHistory();
        h.setOrderId(order.getId());
        h.setOperator(operator);
        h.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        h.setNote(note);
        history.save(h);
    }
}
