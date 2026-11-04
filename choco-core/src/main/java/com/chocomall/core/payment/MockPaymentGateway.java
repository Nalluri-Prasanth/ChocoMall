package com.chocomall.core.payment;

import com.chocomall.model.enums.PayMethod;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Development gateway. Card payments succeed unless the token is {@code tok_decline}, which lets tests and demos
 * exercise the failure path. Replace with an adapter for a real provider by defining another
 * {@link PaymentGateway} bean.
 */
@Component
@ConditionalOnMissingBean(value = PaymentGateway.class, ignored = MockPaymentGateway.class)
public class MockPaymentGateway implements PaymentGateway {

    public static final String DECLINE_TOKEN = "tok_decline";

    @Override
    public ChargeResult charge(BigDecimal amount, PayMethod method, String reference, String token) {
        if (amount == null || amount.signum() <= 0) {
            return ChargeResult.declined("Amount must be positive");
        }
        if (method == PayMethod.CARD && DECLINE_TOKEN.equals(token)) {
            return ChargeResult.declined("Card declined");
        }
        return ChargeResult.ok("MOCK-" + UUID.randomUUID());
    }
}
