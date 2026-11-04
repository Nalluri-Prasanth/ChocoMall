package com.chocomall.admin.service;

import com.chocomall.admin.dto.SalesDtos.ReasonParam;
import com.chocomall.admin.dto.SalesDtos.ReturnHandleParam;
import com.chocomall.admin.dto.SalesDtos.ReturnQuery;
import com.chocomall.admin.security.CurrentAdmin;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.PageParams;
import com.chocomall.core.stock.StockService;
import com.chocomall.model.entity.OrderItem;
import com.chocomall.model.entity.Payment;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ReturnApply;
import com.chocomall.model.entity.ReturnReason;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.PaymentStatus;
import com.chocomall.model.enums.ReturnStatus;
import com.chocomall.model.enums.StockReason;
import com.chocomall.model.repository.OrderItemRepository;
import com.chocomall.model.repository.PaymentRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ReturnApplyRepository;
import com.chocomall.model.repository.ReturnReasonRepository;
import com.chocomall.model.repository.ShopOrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Return and refund handling. A request is accepted (with the refund amount), the goods are received, and the
 * request is completed: the refund is written to the payment ledger and, if the goods are fit for sale, restocked.
 * The total refunded for an order can never exceed what the customer paid.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReturnAdminService {

    private final ReturnApplyRepository applies;
    private final ReturnReasonRepository reasons;
    private final ShopOrderRepository orders;
    private final OrderItemRepository items;
    private final PaymentRepository payments;
    private final ProductRepository products;
    private final StockService stock;

    @Transactional(readOnly = true)
    public CommonPage<ReturnApply> list(ReturnQuery q) {
        return CommonPage.of(applies.search(q.status(), blank(q.orderSn()), blank(q.member()),
                PageParams.of(q.pageNum(), q.pageSize(), Sort.by(Sort.Order.desc("id")))));
    }

    @Transactional(readOnly = true)
    public ReturnApply get(Long id) {
        return Asserts.found(applies.findById(id), "Return request");
    }

    public ReturnApply handle(Long id, ReturnHandleParam p) {
        ReturnApply r = get(id);
        ShopOrder order = Asserts.found(orders.findById(r.getOrderId()), "Order");
        String operator = CurrentAdmin.username();
        switch (p.status()) {
            case ACCEPTED -> {
                Asserts.state(r.getStatus() == ReturnStatus.PENDING, "Only a pending request can be accepted");
                BigDecimal amount = p.returnAmount() == null ? r.getReturnAmount() : p.returnAmount();
                Asserts.isTrue(amount != null && amount.signum() > 0, "Enter the refund amount");
                BigDecimal refundedBefore = refunded(order.getId());
                Asserts.isTrue(refundedBefore.add(amount).compareTo(order.getPayAmount()) <= 0,
                        "Refunds would exceed the " + order.getPayAmount() + " paid for this order");
                r.setReturnAmount(amount);
                r.setHandleNote(p.handleNote());
            }
            case REJECTED -> {
                Asserts.state(r.getStatus() == ReturnStatus.PENDING, "Only a pending request can be rejected");
                Asserts.isTrue(p.handleNote() != null && !p.handleNote().isBlank(), "Explain why the request is rejected");
                r.setHandleNote(p.handleNote());
            }
            case COMPLETED -> {
                Asserts.state(r.getStatus() == ReturnStatus.ACCEPTED, "Accept the request before completing it");
                r.setReceiveMan(p.receiveMan() == null ? operator : p.receiveMan());
                r.setReceiveTime(LocalDateTime.now());
                r.setReceiveNote(p.receiveNote());
                refund(r, order);
                if (p.restock()) {
                    restock(r, operator);
                }
            }
            default -> Asserts.isTrue(false, "A request cannot be moved back to " + p.status());
        }
        r.setStatus(p.status());
        r.setHandleMan(operator);
        r.setHandleTime(LocalDateTime.now());
        return r;
    }

    private void refund(ReturnApply r, ShopOrder order) {
        Payment original = payments.findByOrderIdOrderByIdDesc(order.getId()).stream()
                .filter(x -> x.getStatus() == PaymentStatus.SUCCEEDED || x.getStatus() == PaymentStatus.REFUNDED)
                .findFirst().orElse(null);
        Payment refund = new Payment();
        refund.setOrderId(order.getId());
        refund.setOrderSn(order.getOrderSn());
        refund.setAmount(r.getReturnAmount());
        refund.setMethod(original == null ? order.getPayMethod() : original.getMethod());
        refund.setStatus(PaymentStatus.REFUNDED);
        refund.setTransactionRef("REFUND-" + r.getId() + "-" + System.currentTimeMillis());
        refund.setPaidTime(LocalDateTime.now());
        payments.save(refund);
    }

    private void restock(ReturnApply r, String operator) {
        if (r.getProductId() == null || r.getProductQuantity() == null) {
            return;
        }
        OrderItem line = items.findByOrderId(r.getOrderId()).stream()
                .filter(i -> i.getProductId().equals(r.getProductId())).findFirst().orElse(null);
        Product product = products.findById(r.getProductId()).orElse(null);
        if (line != null && product != null) {
            stock.restock(product, r.getProductQuantity() * line.getUnitsPerSku(), StockReason.RETURN, r.getOrderSn(), operator);
        }
    }

    private BigDecimal refunded(Long orderId) {
        return payments.findByOrderIdOrderByIdDesc(orderId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.REFUNDED && p.getTransactionRef() != null
                        && p.getTransactionRef().startsWith("REFUND-"))
                .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ---------------------------------------------------------------------------- reasons

    @Transactional(readOnly = true)
    public List<ReturnReason> reasons() {
        return reasons.findAll(Sort.by(Sort.Order.desc("sort")));
    }

    public ReturnReason createReason(ReasonParam p) {
        ReturnReason r = new ReturnReason();
        applyReason(r, p);
        return reasons.save(r);
    }

    public ReturnReason updateReason(Long id, ReasonParam p) {
        ReturnReason r = Asserts.found(reasons.findById(id), "Reason");
        applyReason(r, p);
        return r;
    }

    public void deleteReasons(Set<Long> ids) {
        reasons.deleteAllById(ids);
    }

    private void applyReason(ReturnReason r, ReasonParam p) {
        r.setName(p.name());
        r.setSort(p.sort() == null ? 0 : p.sort());
        r.setStatus(p.status() == null || p.status());
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
