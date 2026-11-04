package com.chocomall.portal.service;

import com.chocomall.common.api.CommonPage;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.PageParams;
import com.chocomall.core.order.OrderLifecycleService;
import com.chocomall.model.entity.OrderItem;
import com.chocomall.model.entity.OrderOperateHistory;
import com.chocomall.model.entity.Payment;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.OrderStatus;
import com.chocomall.model.repository.OrderItemRepository;
import com.chocomall.model.repository.OrderOperateHistoryRepository;
import com.chocomall.model.repository.PaymentRepository;
import com.chocomall.model.repository.ShopOrderRepository;
import com.chocomall.portal.dto.PortalDtos.OrderView;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A customer's own orders: history, detail, cancelling unpaid orders and confirming collection or delivery. */
@Service
@RequiredArgsConstructor
@Transactional
public class PortalOrderService {

    public record OrderDetailView(ShopOrder order, List<OrderItem> items, List<OrderOperateHistory> timeline, List<Payment> payments) {
    }

    private final ShopOrderRepository orders;
    private final OrderItemRepository items;
    private final OrderOperateHistoryRepository history;
    private final PaymentRepository payments;
    private final OrderLifecycleService lifecycle;

    @Transactional(readOnly = true)
    public CommonPage<OrderView> list(Long memberId, OrderStatus status, Integer pageNum, Integer pageSize) {
        var pageable = PageParams.of(pageNum, pageSize, Sort.by(Sort.Order.desc("id")));
        Page<ShopOrder> page = status == null ? orders.findByMemberIdAndDeletedFalse(memberId, pageable)
                : orders.findByMemberIdAndStatusAndDeletedFalse(memberId, status, pageable);
        Map<Long, List<OrderItem>> byOrder = items.findByOrderIdIn(page.getContent().stream().map(ShopOrder::getId).toList()).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        return CommonPage.of(page, o -> new OrderView(o, byOrder.getOrDefault(o.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public OrderDetailView detail(Long memberId, Long id) {
        ShopOrder o = owned(memberId, id);
        return new OrderDetailView(o, items.findByOrderId(id), history.findByOrderIdOrderByIdAsc(id), payments.findByOrderIdOrderByIdDesc(id));
    }

    /** Customers cancel unpaid orders themselves; once paid, the shop decides (ingredients may be prepared). */
    public ShopOrder cancel(Long memberId, Long id, String reason) {
        ShopOrder o = owned(memberId, id);
        Asserts.state(o.getStatus() == OrderStatus.PENDING_PAYMENT || o.getStatus() == OrderStatus.CANCELLED,
                "This order is already paid. Contact the shop to change or cancel it.");
        return lifecycle.cancel(id, reason == null || reason.isBlank() ? "Cancelled by customer" : reason, "customer");
    }

    public ShopOrder confirmReceipt(Long memberId, Long id) {
        owned(memberId, id);
        return lifecycle.advance(id, OrderStatus.COMPLETED, "customer", "Confirmed by customer");
    }

    public void delete(Long memberId, Long id) {
        ShopOrder o = owned(memberId, id);
        Asserts.state(o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.INVALID,
                "Only finished orders can be removed from your list");
        o.setDeleted(true);
    }

    private ShopOrder owned(Long memberId, Long id) {
        return Asserts.found(orders.findByIdAndMemberId(id, memberId).filter(o -> !o.isDeleted()), "Order");
    }
}
