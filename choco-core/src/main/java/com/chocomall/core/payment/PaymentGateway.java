package com.chocomall.core.payment;

import com.chocomall.model.enums.PayMethod;
import java.math.BigDecimal;

/** Port to the payment provider. The bundled {@link MockPaymentGateway} stands in for a real PSP. */
public interface PaymentGateway {

    ChargeResult charge(BigDecimal amount, PayMethod method, String reference, String token);

    record ChargeResult(boolean success, String transactionRef, String failureReason) {

        public static ChargeResult ok(String ref) {
            return new ChargeResult(true, ref, null);
        }

        public static ChargeResult declined(String reason) {
            return new ChargeResult(false, null, reason);
        }
    }
}
