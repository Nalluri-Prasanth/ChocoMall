package com.chocomall.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chocomall.common.exception.ApiException;
import com.chocomall.core.loyalty.LoyaltyService;
import com.chocomall.core.order.OrderLifecycleService;
import com.chocomall.core.payment.MockPaymentGateway;
import com.chocomall.core.payment.PaymentService;
import com.chocomall.model.entity.MemberLevel;
import com.chocomall.model.entity.Member;
import com.chocomall.model.entity.OrderSetting;
import com.chocomall.model.entity.Payment;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.OrderStatus;
import com.chocomall.model.enums.PayMethod;
import com.chocomall.model.enums.PaymentStatus;
import com.chocomall.model.repository.MemberLevelRepository;
import com.chocomall.model.repository.MemberRepository;
import com.chocomall.model.repository.PaymentRepository;
import com.chocomall.model.repository.ShopOrderRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = CoreTestApplication.class)
class LoyaltyAndPaymentTest {

    @Autowired LoyaltyService loyalty;
    @Autowired PaymentService paymentService;
    @Autowired OrderLifecycleService lifecycle;
    @Autowired Fixtures fixtures;
    @Autowired TransactionTemplate tx;
    @Autowired MemberRepository members;
    @Autowired MemberLevelRepository levels;
    @Autowired ShopOrderRepository orders;
    @Autowired PaymentRepository payments;

    // ------------------------------------------------------------------------------- loyalty

    @Test
    void pointsCannotBeRedeemedBeyondTheBalance() {
        Member m = fixtures.member();
        tx.executeWithoutResult(s -> loyalty.adjust(m.getId(), 50, "welcome"));

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> loyalty.redeem(m.getId(), 80, "ORD-X")))
                .isInstanceOf(ApiException.class).hasMessageContaining("Not enough loyalty points");
        assertThat(members.integrationOf(m.getId())).isEqualTo(50);
    }

    @Test
    void redemptionIsCappedAtAShareOfTheOrder() {
        Member m = fixtures.member();
        tx.executeWithoutResult(s -> loyalty.adjust(m.getId(), 10_000, "grant"));
        OrderSetting setting = lifecycle.setting(); // 100 points = 1.00, max 20% of the order

        int max = loyalty.maxRedeemablePoints(m.getId(), new BigDecimal("50.00"), setting);

        assertThat(max).isEqualTo(1000);                       // 20% of 50.00 = 10.00 = 1000 points
        assertThat(loyalty.pointsValue(1000, setting)).isEqualByComparingTo("10.00");
        assertThat(loyalty.maxRedeemablePoints(m.getId(), new BigDecimal("5.00"), setting)).isEqualTo(100);
    }

    @Test
    void growthPromotesTheMemberToHigherLevelsAndHigherMultipliers() {
        Member m = fixtures.member();
        Product p = fixtures.product(10, "600.00");
        ShopOrder o = fixtures.placeOrder(m, p, 1, "600.00");

        lifecycle.markPaid(o.getId(), PayMethod.CARD, "test");

        MemberLevel level = levels.findById(members.findById(m.getId()).orElseThrow().getLevelId()).orElseThrow();
        assertThat(level.getName()).isEqualTo("Silver"); // 600 growth >= 500

        ShopOrder second = fixtures.placeOrder(m, fixtures.product(10, "100.00"), 1, "100.00");
        lifecycle.markPaid(second.getId(), PayMethod.CARD, "test");
        assertThat(orders.findById(second.getId()).orElseThrow().getIntegrationEarned()).isEqualTo(125); // 100 x 1.25
    }

    // ------------------------------------------------------------------------------- payments

    @Test
    void aSuccessfulCardPaymentMarksTheOrderPaid() {
        Member m = fixtures.member();
        Product p = fixtures.product(10, "10.00");
        ShopOrder o = fixtures.placeOrder(m, p, 1, "10.00");

        Payment pay = paymentService.payOrder(o.getId(), m.getId(), PayMethod.CARD, "tok_ok");

        assertThat(pay.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(pay.getTransactionRef()).startsWith("MOCK-");
        assertThat(orders.findById(o.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    void aDeclinedCardIsRecordedAndTheOrderStaysPayable() {
        Member m = fixtures.member();
        Product p = fixtures.product(10, "10.00");
        ShopOrder o = fixtures.placeOrder(m, p, 1, "10.00");

        assertThatThrownBy(() -> paymentService.payOrder(o.getId(), m.getId(), PayMethod.CARD, MockPaymentGateway.DECLINE_TOKEN))
                .isInstanceOf(ApiException.class).hasMessageContaining("Card declined");

        assertThat(payments.findByOrderIdOrderByIdDesc(o.getId())).extracting(Payment::getStatus).containsExactly(PaymentStatus.FAILED);
        assertThat(orders.findById(o.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        Payment retry = paymentService.payOrder(o.getId(), m.getId(), PayMethod.CARD, "tok_ok");
        assertThat(retry.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void payingAnAlreadyPaidOrderReturnsTheSamePayment() {
        Member m = fixtures.member();
        ShopOrder o = fixtures.placeOrder(m, fixtures.product(10, "10.00"), 1, "10.00");

        Payment first = paymentService.payOrder(o.getId(), m.getId(), PayMethod.CARD, "tok_ok");
        Payment again = paymentService.payOrder(o.getId(), m.getId(), PayMethod.CARD, "tok_ok");

        assertThat(again.getId()).isEqualTo(first.getId());
        assertThat(payments.findByOrderIdOrderByIdDesc(o.getId())).hasSize(1);
    }

    @Test
    void customersCannotPayForSomeoneElsesOrder() {
        Member owner = fixtures.member();
        Member other = fixtures.member();
        ShopOrder o = fixtures.placeOrder(owner, fixtures.product(10, "10.00"), 1, "10.00");

        assertThatThrownBy(() -> paymentService.payOrder(o.getId(), other.getId(), PayMethod.CARD, "tok_ok"))
                .isInstanceOf(ApiException.class).hasMessageContaining("not found");
    }

    @Test
    void cashAtTheCounterWaitsForStaffConfirmation() {
        Member m = fixtures.member();
        ShopOrder o = fixtures.placeOrder(m, fixtures.product(10, "10.00"), 1, "10.00");

        Payment pending = paymentService.payOrder(o.getId(), m.getId(), PayMethod.CASH_AT_COUNTER, null);
        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(orders.findById(o.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        Payment confirmed = paymentService.confirmManually(o.getId(), PayMethod.CASH_AT_COUNTER, "cashier");
        assertThat(confirmed.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(orders.findById(o.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PREPARING);
        assertThat(payments.findByOrderIdOrderByIdDesc(o.getId())).hasSize(1); // the pending row was completed, not duplicated
    }
}
