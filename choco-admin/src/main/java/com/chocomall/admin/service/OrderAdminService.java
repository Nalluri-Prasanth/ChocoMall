package com.chocomall.admin.service;

import com.chocomall.admin.dto.SalesDtos.ManualPaymentParam;
import com.chocomall.admin.dto.SalesDtos.OrderDetail;
import com.chocomall.admin.dto.SalesDtos.OrderQuery;
import com.chocomall.admin.dto.SalesDtos.ReceiverParam;
import com.chocomall.admin.dto.SalesDtos.ShipParam;
import com.chocomall.admin.security.CurrentAdmin;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.PageParams;
import com.chocomall.core.order.OrderLifecycleService;
import com.chocomall.core.payment.PaymentService;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.FulfillmentType;
import com.chocomall.model.enums.OrderStatus;
import com.chocomall.model.repository.CouponHistoryRepository;
import com.chocomall.model.repository.OrderItemRepository;
import com.chocomall.model.repository.OrderOperateHistoryRepository;
import com.chocomall.model.repository.PaymentRepository;
import com.chocomall.model.repository.ShopOrderRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Order handling for staff: search, preparation workflow, shipping, manual payment confirmation and cancellation. */
@Service
@RequiredArgsConstructor
public class OrderAdminService {

    private final ShopOrderRepository orders;
    private final OrderItemRepository items;
    private final OrderOperateHistoryRepository history;
    private final PaymentRepository payments;
    private final CouponHistoryRepository couponHistories;
    private final OrderLifecycleService lifecycle;
    private final PaymentService paymentService;

    @Transactional(readOnly = true)
    public CommonPage<ShopOrder> list(OrderQuery q) {
        Specification<ShopOrder> spec = (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            all.add(cb.isFalse(root.get("deleted")));
            if (q.orderSn() != null && !q.orderSn().isBlank()) {
                all.add(cb.equal(root.get("orderSn"), q.orderSn().trim()));
            }
            if (q.receiver() != null && !q.receiver().isBlank()) {
                String like = "%" + q.receiver().trim().toLowerCase(Locale.ROOT) + "%";
                all.add(cb.or(cb.like(cb.lower(cb.coalesce(root.<String>get("receiverName"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("memberUsername"), "")), like),
                        cb.like(cb.coalesce(root.<String>get("receiverPhone"), ""), "%" + q.receiver().trim() + "%")));
            }
            if (q.status() != null) {
                all.add(cb.equal(root.get("status"), q.status()));
            }
            if (q.fulfillmentType() != null) {
                all.add(cb.equal(root.get("fulfillmentType"), q.fulfillmentType()));
            }
            if (q.preorder() != null) {
                all.add(cb.equal(root.get("preorder"), q.preorder()));
            }
            if (q.createdFrom() != null) {
                all.add(cb.greaterThanOrEqualTo(root.<java.time.LocalDateTime>get("createTime"), q.createdFrom()));
            }
            if (q.createdTo() != null) {
                all.add(cb.lessThan(root.<java.time.LocalDateTime>get("createTime"), q.createdTo()));
            }
            return cb.and(all.toArray(Predicate[]::new));
        };
        return CommonPage.of(orders.findAll(spec, PageParams.of(q.pageNum(), q.pageSize(), Sort.by(Sort.Order.desc("id")))));
    }

    @Transactional(readOnly = true)
    public OrderDetail detail(Long id) {
        ShopOrder o = load(id);
        return new OrderDetail(o, items.findByOrderId(id), history.findByOrderIdOrderByIdAsc(id),
                payments.findByOrderIdOrderByIdDesc(id), couponHistories.findByOrderId(id));
    }

    /** Pickup orders: the order is ready on the counter. */
    @Transactional
    public void markReady(Set<Long> ids) {
        ids.forEach(id -> lifecycle.advance(id, OrderStatus.READY_FOR_PICKUP, CurrentAdmin.username(), "Ready for pickup"));
    }

    @Transactional
    public void ship(List<ShipParam> params) {
        params.forEach(p -> lifecycle.ship(p.orderId(), p.company(), p.trackingNumber(), CurrentAdmin.username()));
    }

    @Transactional
    public void complete(Set<Long> ids, String note) {
        ids.forEach(id -> lifecycle.advance(id, OrderStatus.COMPLETED, CurrentAdmin.username(),
                note == null || note.isBlank() ? "Collected / delivered" : note));
    }

    /** Cancels orders; paid orders are refunded and restocked. */
    @Transactional
    public void close(Set<Long> ids, String reason) {
        String why = reason == null || reason.isBlank() ? "Closed by staff" : reason;
        ids.forEach(id -> lifecycle.cancel(id, why, CurrentAdmin.username()));
    }

    @Transactional
    public void invalidate(Set<Long> ids) {
        ids.forEach(id -> lifecycle.advance(id, OrderStatus.INVALID, CurrentAdmin.username(), "Marked invalid"));
    }

    /** Cash at the counter or a bank transfer that staff have verified. */
    @Transactional
    public void confirmPayment(ManualPaymentParam p) {
        paymentService.confirmManually(p.orderId(), p.method(), CurrentAdmin.username());
    }

    @Transactional
    public ShopOrder updateReceiver(Long id, ReceiverParam p) {
        ShopOrder o = load(id);
        Asserts.state(o.getFulfillmentType() == FulfillmentType.DELIVERY, "Only delivery orders have a receiver address");
        Asserts.state(o.getStatus() == OrderStatus.PENDING_PAYMENT || o.getStatus() == OrderStatus.PREPARING,
                "The address can no longer be changed once the order is on its way");
        o.setReceiverName(p.name());
        o.setReceiverPhone(p.phone());
        o.setReceiverPostCode(p.postCode());
        o.setReceiverProvince(p.province());
        o.setReceiverCity(p.city());
        o.setReceiverRegion(p.region());
        o.setReceiverDetailAddress(p.detailAddress());
        return orders.save(o);
    }

    @Transactional
    public ShopOrder updateNote(Long id, String note) {
        ShopOrder o = load(id);
        o.setNote(note);
        return orders.save(o);
    }

    /** Hides finished orders from the list; the records themselves are kept for accounting. */
    @Transactional
    public void delete(Set<Long> ids) {
        for (ShopOrder o : orders.findAllById(ids)) {
            Asserts.state(o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.CANCELLED
                    || o.getStatus() == OrderStatus.INVALID, "Only completed, cancelled or invalid orders can be removed from the list");
            o.setDeleted(true);
        }
    }

    private ShopOrder load(Long id) {
        return Asserts.found(orders.findById(id), "Order");
    }
}
