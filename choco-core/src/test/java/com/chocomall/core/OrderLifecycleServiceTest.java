package com.chocomall.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chocomall.common.exception.ApiException;
import com.chocomall.core.loyalty.LoyaltyService;
import com.chocomall.core.order.OrderLifecycleService;
import com.chocomall.model.entity.Coupon;
import com.chocomall.model.entity.CouponHistory;
import com.chocomall.model.entity.Member;
import com.chocomall.model.entity.Payment;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.CouponGetType;
import com.chocomall.model.enums.CouponType;
import com.chocomall.model.enums.CouponUseStatus;
import com.chocomall.model.enums.IntegrationChangeType;
import com.chocomall.model.enums.OrderStatus;
import com.chocomall.model.enums.PayMethod;
import com.chocomall.model.enums.PaymentStatus;
import com.chocomall.model.repository.CouponHistoryRepository;
import com.chocomall.model.repository.CouponRepository;
import com.chocomall.model.repository.IntegrationHistoryRepository;
import com.chocomall.model.repository.MemberRepository;
import com.chocomall.model.repository.PaymentRepository;
import com.chocomall.model.repository.ShopOrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = CoreTestApplication.class)
class OrderLifecycleServiceTest {

    @Autowired OrderLifecycleService lifecycle;
    @Autowired Fixtures fixtures;
    @Autowired TransactionTemplate tx;
    @Autowired MemberRepository members;
    @Autowired ShopOrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired CouponRepository coupons;
    @Autowired CouponHistoryRepository couponHistories;
    @Autowired IntegrationHistoryRepository pointsHistory;
    @Autowired LoyaltyService loyalty;
    @Autowired JdbcTemplate jdbc;

    @Test
    void payingConvertsReservationToSaleAndAwardsPoints() {
        Member m = fixtures.member();
        Product p = fixtures.product(10, "12.50");
        ShopOrder o = fixtures.placeOrder(m, p, 2, "25.00");

        ShopOrder paid = lifecycle.markPaid(o.getId(), PayMethod.CARD, "test");

        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PREPARING);
        assertThat(paid.getPaymentTime()).isNotNull();
        Product after = fixtures.fresh(p);
        assertThat(after.getStock()).isEqualTo(8);
        assertThat(after.getLockStock()).isZero();
        assertThat(after.getSale()).isEqualTo(2);
        assertThat(members.integrationOf(m.getId())).isEqualTo(25);   // 25.00 x 1 point x Bronze 1.00
        assertThat(members.growthOf(m.getId())).isEqualTo(25);
    }

    @Test
    void payingTwiceDoesNotDoubleCountStockOrPoints() {
        Member m = fixtures.member();
        Product p = fixtures.product(10, "10.00");
        ShopOrder o = fixtures.placeOrder(m, p, 1, "10.00");

        lifecycle.markPaid(o.getId(), PayMethod.CARD, "test");
        lifecycle.markPaid(o.getId(), PayMethod.CARD, "test"); // a repeated payment callback

        assertThat(fixtures.fresh(p).getStock()).isEqualTo(9);
        assertThat(members.integrationOf(m.getId())).isEqualTo(10);
        assertThat(pointsHistory.existsByOrderSnAndChangeType(o.getOrderSn(), IntegrationChangeType.ORDER_REWARD)).isTrue();
    }

    @Test
    void cancellingAnUnpaidOrderReleasesStockCouponAndRedeemedPoints() {
        Member m = fixtures.member();
        tx.executeWithoutResult(s -> loyalty.adjust(m.getId(), 500, "grant"));
        Product p = fixtures.product(10, "20.00");
        ShopOrder placed = fixtures.placeOrder(m, p, 3, "20.00");

        CouponHistory used = tx.execute(s -> {
            loyalty.redeem(m.getId(), 100, placed.getOrderSn());
            Coupon c = new Coupon();
            c.setCouponType(CouponType.ALL);
            c.setName("Test coupon");
            c.setTotalCount(10);
            c.setAmount(new BigDecimal("2.00"));
            c.setStartTime(LocalDateTime.now().minusDays(1));
            c.setEndTime(LocalDateTime.now().plusDays(1));
            c = coupons.save(c);
            coupons.incrementReceived(c.getId());
            coupons.addUsed(c.getId(), 1);
            CouponHistory h = new CouponHistory();
            h.setCouponId(c.getId());
            h.setMemberId(m.getId());
            h.setGetType(CouponGetType.ADMIN_GRANT);
            h.setUseStatus(CouponUseStatus.USED);
            h.setOrderId(placed.getId());
            h.setOrderSn(placed.getOrderSn());
            ShopOrder o = orders.findById(placed.getId()).orElseThrow();
            o.setUseIntegration(100);
            orders.save(o);
            return couponHistories.save(h);
        });
        assertThat(members.integrationOf(m.getId())).isEqualTo(400);

        ShopOrder cancelled = lifecycle.cancel(placed.getId(), "changed my mind", "customer");

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(fixtures.fresh(p).getLockStock()).isZero();
        assertThat(fixtures.fresh(p).getStock()).isEqualTo(10);
        assertThat(members.integrationOf(m.getId())).isEqualTo(500);
        CouponHistory restored = couponHistories.findById(used.getId()).orElseThrow();
        assertThat(restored.getUseStatus()).isEqualTo(CouponUseStatus.UNUSED);
        assertThat(restored.getOrderId()).isNull();
    }

    @Test
    void cancellingTwiceIsHarmless() {
        Member m = fixtures.member();
        Product p = fixtures.product(5, "5.00");
        ShopOrder o = fixtures.placeOrder(m, p, 1, "5.00");

        lifecycle.cancel(o.getId(), "once", "customer");
        lifecycle.cancel(o.getId(), "twice", "customer");

        assertThat(fixtures.fresh(p).getStock()).isEqualTo(5);
        assertThat(fixtures.fresh(p).getLockStock()).isZero();
    }

    @Test
    void refundingAPaidOrderRestocksReversesPointsAndMarksThePaymentRefunded() {
        Member m = fixtures.member();
        Product p = fixtures.product(10, "30.00");
        ShopOrder o = fixtures.placeOrder(m, p, 1, "30.00");
        Payment pay = new Payment();
        pay.setOrderId(o.getId());
        pay.setAmount(new BigDecimal("30.00"));
        pay.setMethod(PayMethod.CARD);
        pay.setStatus(PaymentStatus.SUCCEEDED);
        payments.save(pay);
        lifecycle.markPaid(o.getId(), PayMethod.CARD, "test");
        assertThat(members.integrationOf(m.getId())).isEqualTo(30);

        lifecycle.cancel(o.getId(), "kitchen closed", "staff");

        assertThat(fixtures.fresh(p).getStock()).isEqualTo(10);
        assertThat(members.integrationOf(m.getId())).isZero();
        assertThat(payments.findByOrderIdOrderByIdDesc(o.getId())).extracting(Payment::getStatus).containsOnly(PaymentStatus.REFUNDED);
    }

    @Test
    void pickupOrdersFollowThePickupPathOnly() {
        Member m = fixtures.member();
        Product p = fixtures.product(10, "10.00");
        ShopOrder o = fixtures.placeOrder(m, p, 1, "10.00");
        lifecycle.markPaid(o.getId(), PayMethod.CARD, "test");

        assertThatThrownBy(() -> lifecycle.ship(o.getId(), "Courier", "TRK1", "staff"))
                .isInstanceOf(ApiException.class).hasMessageContaining("cannot become SHIPPED");

        lifecycle.advance(o.getId(), OrderStatus.READY_FOR_PICKUP, "staff", null);
        ShopOrder done = lifecycle.advance(o.getId(), OrderStatus.COMPLETED, "staff", null);
        assertThat(done.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(done.getReceiveTime()).isNotNull();
    }

    @Test
    void aCompletedOrderCannotBeCancelledOrPaidAgain() {
        Member m = fixtures.member();
        Product p = fixtures.product(10, "10.00");
        ShopOrder o = fixtures.placeOrder(m, p, 1, "10.00");
        lifecycle.markPaid(o.getId(), PayMethod.CARD, "test");
        lifecycle.advance(o.getId(), OrderStatus.READY_FOR_PICKUP, "staff", null);
        lifecycle.advance(o.getId(), OrderStatus.COMPLETED, "staff", null);

        assertThatThrownBy(() -> lifecycle.cancel(o.getId(), "late", "staff")).isInstanceOf(ApiException.class);
    }

    @Test
    void aCancelledOrderCannotBePaidLate() {
        Member m = fixtures.member();
        Product p = fixtures.product(10, "10.00");
        ShopOrder o = fixtures.placeOrder(m, p, 1, "10.00");
        lifecycle.cancel(o.getId(), "timeout", "system");

        assertThatThrownBy(() -> lifecycle.markPaid(o.getId(), PayMethod.CARD, "late-callback"))
                .isInstanceOf(ApiException.class).hasMessageContaining("can no longer be paid");
        assertThat(fixtures.fresh(p).getStock()).isEqualTo(10);
    }

    @Test
    void theTimeoutJobCancelsOnlyExpiredUnpaidOrders() {
        Member m = fixtures.member();
        Product p = fixtures.product(10, "10.00");
        ShopOrder old = fixtures.placeOrder(m, p, 2, "20.00");
        ShopOrder fresh = fixtures.placeOrder(m, p, 1, "10.00");
        jdbc.update("update oms_order set create_time = ? where id = ?", LocalDateTime.now().minusHours(2), old.getId());

        int cancelled = lifecycle.cancelExpired();

        assertThat(cancelled).isGreaterThanOrEqualTo(1);
        assertThat(orders.findById(old.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(orders.findById(fresh.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(fixtures.fresh(p).getLockStock()).isEqualTo(1);
    }

    @Test
    void shippedOrdersAreAutoCompletedAfterTheConfirmationPeriod() {
        Member m = fixtures.member();
        Product p = fixtures.product(10, "10.00");
        ShopOrder o = fixtures.placeOrder(m, p, 1, "10.00");
        lifecycle.markPaid(o.getId(), PayMethod.CARD, "test");
        jdbc.update("update oms_order set fulfillment_type = 'DELIVERY' where id = ?", o.getId());
        lifecycle.ship(o.getId(), "Courier", "TRK9", "staff");
        jdbc.update("update oms_order set delivery_time = ? where id = ?", LocalDateTime.now().minusDays(30), o.getId());

        lifecycle.autoComplete();

        assertThat(orders.findById(o.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }
}
