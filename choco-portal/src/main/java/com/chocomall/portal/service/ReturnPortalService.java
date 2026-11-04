package com.chocomall.portal.service;

import com.chocomall.common.exception.Asserts;
import com.chocomall.model.entity.OrderItem;
import com.chocomall.model.entity.ReturnApply;
import com.chocomall.model.entity.ReturnReason;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.OrderStatus;
import com.chocomall.model.enums.ReturnStatus;
import com.chocomall.model.repository.OrderItemRepository;
import com.chocomall.model.repository.ReturnApplyRepository;
import com.chocomall.model.repository.ReturnReasonRepository;
import com.chocomall.model.repository.ShopOrderRepository;
import com.chocomall.core.order.OrderLifecycleService;
import com.chocomall.portal.dto.PortalDtos.ReturnParam;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Customers ask for a return or refund of a collected order line within the return window. */
@Service
@RequiredArgsConstructor
@Transactional
public class ReturnPortalService {

    private final ShopOrderRepository orders;
    private final OrderItemRepository items;
    private final ReturnApplyRepository applies;
    private final ReturnReasonRepository reasons;
    private final OrderLifecycleService lifecycle;

    public ReturnApply create(Long memberId, ReturnParam p) {
        ShopOrder order = Asserts.found(orders.findByIdAndMemberId(p.orderId(), memberId), "Order");
        Asserts.state(order.getStatus() == OrderStatus.COMPLETED, "Returns can be requested once the order is completed");
        int windowDays = lifecycle.setting().getReturnWindowDays();
        LocalDateTime received = order.getReceiveTime() == null ? order.getUpdateTime() : order.getReceiveTime();
        Asserts.state(received != null && received.plusDays(windowDays).isAfter(LocalDateTime.now()),
                "The " + windowDays + "-day return window for this order has ended");

        OrderItem line = items.findByOrderId(order.getId()).stream().filter(i -> i.getProductId().equals(p.productId())).findFirst()
                .orElseThrow(() -> Asserts.notFound("That product in this order"));
        Asserts.isTrue(p.quantity() <= line.getProductQuantity(), "You only ordered " + line.getProductQuantity());
        Asserts.state(!applies.existsByOrderIdAndProductIdAndStatusIn(order.getId(), p.productId(),
                EnumSet.of(ReturnStatus.PENDING, ReturnStatus.ACCEPTED)), "A return request for this item is already open");

        BigDecimal amount = line.getRealAmount().multiply(BigDecimal.valueOf(p.quantity()))
                .divide(BigDecimal.valueOf(line.getProductQuantity()), 2, RoundingMode.HALF_UP);
        ReturnApply r = new ReturnApply();
        r.setOrderId(order.getId());
        r.setOrderSn(order.getOrderSn());
        r.setMemberId(memberId);
        r.setMemberUsername(order.getMemberUsername());
        r.setProductId(p.productId());
        r.setProductName(line.getProductName());
        r.setProductQuantity(p.quantity());
        r.setReturnAmount(amount);
        r.setReturnName(order.getReceiverName());
        r.setReturnPhone(order.getReceiverPhone());
        r.setReason(p.reason());
        r.setDescription(p.description());
        r.setProofPics(p.proofPics());
        return applies.save(r);
    }

    @Transactional(readOnly = true)
    public List<ReturnApply> mine(Long memberId) {
        return applies.findByMemberIdOrderByIdDesc(memberId);
    }

    @Transactional(readOnly = true)
    public List<ReturnReason> reasons() {
        return reasons.findByStatusTrueOrderBySortDesc();
    }
}
