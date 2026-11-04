package com.chocomall.core.payment;

import com.chocomall.common.api.ResultCode;
import com.chocomall.common.exception.ApiException;
import com.chocomall.common.exception.Asserts;
import com.chocomall.core.order.OrderLifecycleService;
import com.chocomall.model.entity.Payment;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.OrderStatus;
import com.chocomall.model.enums.PayMethod;
import com.chocomall.model.enums.PaymentStatus;
import com.chocomall.model.repository.PaymentRepository;
import com.chocomall.model.repository.ShopOrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment ledger on top of a {@link PaymentGateway}. Card payments are charged immediately; counter cash and bank
 * transfers stay pending until staff confirm them in the back office.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository payments;
    private final ShopOrderRepository orders;
    private final PaymentGateway gateway;
    private final OrderLifecycleService lifecycle;

    /** Pays an order. Returns the existing payment when the order was already paid (safe to retry). */
    @Transactional(noRollbackFor = ApiException.class)
    public Payment payOrder(Long orderId, Long memberId, PayMethod method, String token) {
        ShopOrder order = orders.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> Asserts.notFound("Order"));
        var existing = payments.findFirstByOrderIdAndStatus(orderId, PaymentStatus.SUCCEEDED);
        if (existing.isPresent()) {
            return existing.get();
        }
        Asserts.state(order.getStatus() == OrderStatus.PENDING_PAYMENT,
                "Order " + order.getOrderSn() + " is " + order.getStatus() + " and cannot be paid");

        Payment p = draft(order.getPayAmount(), method);
        p.setOrderId(orderId);
        p.setOrderSn(order.getOrderSn());
        if (method != PayMethod.CARD) {
            return payments.save(p); // waits for staff confirmation
        }
        Payment charged = charge(p, token);
        if (charged.getStatus() == PaymentStatus.SUCCEEDED) {
            lifecycle.markPaid(orderId, method, "customer");
        }
        return charged;
    }

    /** Staff confirmation of cash at the counter or a bank transfer. */
    @Transactional
    public Payment confirmManually(Long orderId, PayMethod method, String operator) {
        Asserts.isTrue(method != PayMethod.CARD, "Card payments are confirmed by the payment gateway");
        ShopOrder order = Asserts.found(orders.findById(orderId), "Order");
        Payment p = payments.findByOrderIdOrderByIdDesc(orderId).stream()
                .filter(x -> x.getStatus() == PaymentStatus.PENDING).findFirst().orElseGet(() -> {
                    Payment fresh = draft(order.getPayAmount(), method);
                    fresh.setOrderId(orderId);
                    fresh.setOrderSn(order.getOrderSn());
                    return fresh;
                });
        p.setMethod(method);
        p.setStatus(PaymentStatus.SUCCEEDED);
        p.setPaidTime(LocalDateTime.now());
        p.setTransactionRef("MANUAL-" + order.getOrderSn());
        payments.save(p);
        lifecycle.markPaid(orderId, method, operator);
        return p;
    }

    /** Saves a draft payment and charges it. Failure is recorded and reported as a 402-style error by the caller. */
    @Transactional(noRollbackFor = ApiException.class)
    public Payment charge(Payment draft, String token) {
        Payment saved = payments.save(draft);
        var result = gateway.charge(saved.getAmount(), saved.getMethod(), String.valueOf(saved.getId()), token);
        if (result.success()) {
            saved.setStatus(PaymentStatus.SUCCEEDED);
            saved.setTransactionRef(result.transactionRef());
            saved.setPaidTime(LocalDateTime.now());
        } else {
            saved.setStatus(PaymentStatus.FAILED);
            throw new ApiException(ResultCode.PAYMENT_FAILED, "Payment failed: " + result.failureReason());
        }
        return payments.save(saved);
    }

    public Payment draft(BigDecimal amount, PayMethod method) {
        Payment p = new Payment();
        p.setAmount(amount);
        p.setMethod(method);
        p.setStatus(PaymentStatus.PENDING);
        return p;
    }
}
