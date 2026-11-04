package com.chocomall.core.order;

import com.chocomall.common.exception.Asserts;
import com.chocomall.core.loyalty.LoyaltyService;
import com.chocomall.core.stock.StockService;
import com.chocomall.model.entity.CouponHistory;
import com.chocomall.model.entity.OrderItem;
import com.chocomall.model.entity.OrderOperateHistory;
import com.chocomall.model.entity.OrderSetting;
import com.chocomall.model.entity.Payment;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.CouponUseStatus;
import com.chocomall.model.enums.OrderStatus;
import com.chocomall.model.enums.PayMethod;
import com.chocomall.model.enums.PaymentStatus;
import com.chocomall.model.enums.StockReason;
import com.chocomall.model.repository.CouponHistoryRepository;
import com.chocomall.model.repository.CouponRepository;
import com.chocomall.model.repository.OrderItemRepository;
import com.chocomall.model.repository.OrderOperateHistoryRepository;
import com.chocomall.model.repository.OrderSettingRepository;
import com.chocomall.model.repository.PaymentRepository;
import com.chocomall.model.repository.PreorderWindowRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ShopOrderRepository;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every order status change goes through here, so stock, preorder capacity, coupons, loyalty points and the
 * audit trail always move together. Methods are idempotent where retries are likely (payment callbacks,
 * the timeout job and a customer cancelling at the same moment).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderLifecycleService {

    private final ShopOrderRepository orders;
    private final OrderItemRepository items;
    private final OrderOperateHistoryRepository history;
    private final ProductRepository products;
    private final OrderSettingRepository settings;
    private final PreorderWindowRepository windows;
    private final CouponHistoryRepository couponHistories;
    private final CouponRepository coupons;
    private final PaymentRepository payments;
    private final StockService stock;
    private final LoyaltyService loyalty;
    private final OrderItemUnits itemUnits;

    public OrderSetting setting() {
        return settings.findAll().stream().findFirst().orElseGet(OrderSetting::new);
    }

    @Transactional
    public ShopOrder markPaid(Long orderId, PayMethod method, String operator) {
        ShopOrder order = load(orderId);
        if (OrderStateMachine.isPaid(order.getStatus())) {
            return order; // payment callbacks can arrive twice
        }
        Asserts.state(order.getStatus() == OrderStatus.PENDING_PAYMENT,
                "Order " + order.getOrderSn() + " is " + order.getStatus() + " and can no longer be paid");

        Map<Long, Product> byId = productsOf(order);
        unitsOf(order).forEach((productId, units) -> {
            Product p = byId.get(productId);
            if (p != null) { // a product deleted after the order was placed has nothing left to track
                stock.confirmSale(p, units, units, order.getOrderSn());
            }
        });

        order.setStatus(OrderStatus.PREPARING);
        order.setPaymentTime(LocalDateTime.now());
        order.setPayMethod(method);
        loyalty.earn(order, setting());
        record(order, operator, "Payment received (" + method + ")");
        return orders.save(order);
    }

    /**
     * Cancels an unpaid order (releasing reservations) or refunds a paid one that is still being prepared
     * (restocking, returning coupon and points, and marking the payment refunded).
     */
    @Transactional
    public ShopOrder cancel(Long orderId, String reason, String operator) {
        ShopOrder order = load(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }
        Asserts.state(order.getStatus() == OrderStatus.PENDING_PAYMENT || order.getStatus() == OrderStatus.PREPARING,
                "Order " + order.getOrderSn() + " is " + order.getStatus() + " and can no longer be cancelled");

        boolean paid = order.getStatus() == OrderStatus.PREPARING;
        Map<Long, Product> byId = productsOf(order);
        unitsOf(order).forEach((productId, units) -> {
            Product p = byId.get(productId);
            if (p == null) {
                return;
            }
            if (paid) {
                stock.restock(p, units, StockReason.RETURN, order.getOrderSn(), operator);
            } else {
                stock.release(p, units, order.getOrderSn());
            }
        });
        if (order.isPreorder() && order.getPreorderWindowId() != null && order.getPreorderUnits() != null) {
            windows.release(order.getPreorderWindowId(), order.getPreorderUnits());
        }
        releaseCoupon(order);
        loyalty.returnRedeemed(order.getMemberId(), order.getUseIntegration(), order.getOrderSn());
        if (paid) {
            loyalty.reverseEarned(order);
            for (Payment p : payments.findByOrderIdOrderByIdDesc(order.getId())) {
                if (p.getStatus() == PaymentStatus.SUCCEEDED) {
                    p.setStatus(PaymentStatus.REFUNDED);
                }
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        record(order, operator, "Cancelled: " + reason);
        return orders.save(order);
    }

    /** Moves a paid order forward: ready for pickup, shipped or completed. */
    @Transactional
    public ShopOrder advance(Long orderId, OrderStatus target, String operator, String note) {
        ShopOrder order = load(orderId);
        if (order.getStatus() == target) {
            return order;
        }
        Asserts.state(OrderStateMachine.canMove(order.getStatus(), target, order.getFulfillmentType()),
                "An order that is " + order.getStatus() + " cannot become " + target);
        LocalDateTime now = LocalDateTime.now();
        switch (target) {
            case SHIPPED, READY_FOR_PICKUP -> order.setDeliveryTime(now);
            case COMPLETED -> order.setReceiveTime(now);
            case INVALID -> {
                if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                    return invalidateUnpaid(order, operator);
                }
            }
            default -> { }
        }
        order.setStatus(target);
        record(order, operator, note == null || note.isBlank() ? "Status changed to " + target : note);
        return orders.save(order);
    }

    @Transactional
    public ShopOrder ship(Long orderId, String company, String trackingNumber, String operator) {
        ShopOrder order = advance(orderId, OrderStatus.SHIPPED, operator, "Handed to " + company + " (" + trackingNumber + ")");
        order.setDeliveryCompany(company);
        order.setDeliverySn(trackingNumber);
        return orders.save(order);
    }

    /** Cancels every unpaid order older than the payment window. Safe to run repeatedly. */
    @Transactional
    public int cancelExpired() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(setting().getNormalOrderTimeoutMinutes());
        List<ShopOrder> expired = orders.findByStatusAndCreateTimeBefore(OrderStatus.PENDING_PAYMENT, cutoff);
        expired.forEach(o -> cancel(o.getId(), "Payment window expired", "system"));
        return expired.size();
    }

    /** Cancels a single order if it is still unpaid (target of the delayed message). */
    @Transactional
    public void cancelIfUnpaid(Long orderId) {
        orders.findById(orderId).filter(o -> o.getStatus() == OrderStatus.PENDING_PAYMENT)
                .ifPresent(o -> cancel(o.getId(), "Payment window expired", "system"));
    }

    /** Completes shipped and ready orders that were never confirmed by the customer. */
    @Transactional
    public int autoComplete() {
        int days = setting().getAutoConfirmDays();
        List<ShopOrder> due = orders.findByStatusInAndDeliveryTimeBefore(
                EnumSet.of(OrderStatus.SHIPPED, OrderStatus.READY_FOR_PICKUP), LocalDateTime.now().minusDays(days));
        due.forEach(o -> advance(o.getId(), OrderStatus.COMPLETED, "system", "Automatically completed after " + days + " days"));
        return due.size();
    }

    // ---------------------------------------------------------------------------- helpers

    private ShopOrder invalidateUnpaid(ShopOrder order, String operator) {
        ShopOrder cancelled = cancel(order.getId(), "Marked invalid", operator);
        cancelled.setStatus(OrderStatus.INVALID);
        record(cancelled, operator, "Marked invalid");
        return orders.save(cancelled);
    }

    private void releaseCoupon(ShopOrder order) {
        for (CouponHistory h : couponHistories.findByOrderId(order.getId())) {
            if (h.getUseStatus() == CouponUseStatus.USED) {
                h.setUseStatus(CouponUseStatus.UNUSED);
                h.setUseTime(null);
                h.setOrderId(null);
                h.setOrderSn(null);
                coupons.addUsed(h.getCouponId(), -1);
            }
        }
    }

    private ShopOrder load(Long id) {
        return Asserts.found(orders.findById(id), "Order");
    }

    private Map<Long, Integer> unitsOf(ShopOrder order) {
        return itemUnits.unitsByProduct(items.findByOrderId(order.getId()));
    }

    private Map<Long, Product> productsOf(ShopOrder order) {
        List<OrderItem> lines = items.findByOrderId(order.getId());
        List<Long> ids = itemUnits.unitsByProduct(lines).keySet().stream().toList();
        return products.findByIdIn(ids).stream().collect(Collectors.toMap(Product::getId, p -> p));
    }

    private void record(ShopOrder order, String operator, String note) {
        OrderOperateHistory h = new OrderOperateHistory();
        h.setOrderId(order.getId());
        h.setOperator(operator);
        h.setOrderStatus(order.getStatus());
        h.setNote(note);
        history.save(h);
    }
}
