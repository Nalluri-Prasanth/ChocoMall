package com.chocomall.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.chocomall.core.order.OrderLifecycleService;
import com.chocomall.core.personalization.PersonalizationService;
import com.chocomall.model.entity.Member;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ReturnApply;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.OrderStatus;
import com.chocomall.model.enums.PayMethod;
import com.chocomall.model.enums.PersonalizationKind;
import com.chocomall.model.enums.ReturnStatus;
import com.chocomall.model.repository.PaymentRepository;
import com.chocomall.model.repository.ReturnApplyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrdersAndCrmAdminTest extends AdminTestSupport {

    @Autowired OrderLifecycleService lifecycle;
    @Autowired ReturnApplyRepository returns;
    @Autowired PaymentRepository payments;
    @Autowired PersonalizationService personalization;

    // ------------------------------------------------------------------------- order handling

    @Test
    void staffWalkAPickupOrderFromPaymentToCollection() throws Exception {
        String t = superAdmin();
        Member buyer = newMember();
        Product p = bySn("P26");
        ShopOrder o = placeOrder(buyer, p, 2, "11.00");

        assertThat(call("GET", "/order/list?orderSn=" + o.getOrderSn(), null, t, 200).get("total").asInt()).isEqualTo(1);

        message("POST", "/order/update/ready?ids=" + o.getId(), null, t, 409);                    // unpaid
        call("POST", "/order/update/confirmPayment", Map.of("orderId", o.getId(), "method", "CASH_AT_COUNTER"), t, 200);
        assertThat(call("GET", "/order/" + o.getId(), null, t, 200).get("order").get("status").asText()).isEqualTo("PREPARING");

        message("POST", "/order/update/delivery", List.of(Map.of("orderId", o.getId(), "company", "DHL", "trackingNumber", "T1")), t, 409); // pickup order
        call("POST", "/order/update/ready?ids=" + o.getId(), null, t, 200);
        call("POST", "/order/update/complete?ids=" + o.getId() + "&note=Collected+by+customer", null, t, 200);

        JsonNode detail = call("GET", "/order/" + o.getId(), null, t, 200);
        assertThat(detail.get("order").get("status").asText()).isEqualTo("COMPLETED");
        assertThat(detail.get("history")).hasSizeGreaterThanOrEqualTo(3);
        assertThat(detail.get("payments").get(0).get("method").asText()).isEqualTo("CASH_AT_COUNTER");
        assertThat(detail.get("history").get(0).get("operator").asText()).isEqualTo("admin");
    }

    @Test
    void closingAPaidOrderRefundsAndRestocksIt() throws Exception {
        String t = superAdmin();
        Member buyer = newMember();
        Product p = bySn("P27");
        int stockBefore = fresh(p).getStock();
        ShopOrder o = placeOrder(buyer, p, 3, "18.00");
        lifecycle.markPaid(o.getId(), PayMethod.CARD, "test");
        assertThat(fresh(p).getStock()).isEqualTo(stockBefore - 3);

        call("POST", "/order/update/close", Map.of("ids", Set.of(o.getId()), "reason", "kitchen incident"), t, 200);

        assertThat(fresh(p).getStock()).isEqualTo(stockBefore);
        assertThat(orders.findById(o.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(orders.findById(o.getId()).orElseThrow().getCancelReason()).isEqualTo("kitchen incident");
    }

    @Test
    void ordersCanBeFilteredAndFinishedOnesRemovedFromTheList() throws Exception {
        String t = superAdmin();
        Member buyer = newMember();
        ShopOrder o = placeOrder(buyer, bySn("P28"), 1, "5.00");

        assertThat(call("GET", "/order/list?status=PENDING_PAYMENT&receiver=" + buyer.getUsername(), null, t, 200).get("total").asInt()).isEqualTo(1);
        assertThat(call("GET", "/order/list?status=COMPLETED&receiver=" + buyer.getUsername(), null, t, 200).get("total").asInt()).isZero();

        assertThat(message("POST", "/order/delete?ids=" + o.getId(), null, t, 409)).contains("Only completed, cancelled or invalid");
        call("POST", "/order/update/invalid?ids=" + o.getId(), null, t, 200);
        call("POST", "/order/delete?ids=" + o.getId(), null, t, 200);
        assertThat(call("GET", "/order/list?receiver=" + buyer.getUsername(), null, t, 200).get("total").asInt()).isZero();
    }

    // ------------------------------------------------------------------------- returns

    private ReturnApply returnRequest(ShopOrder o, Product p, int quantity, String amount) {
        ReturnApply r = new ReturnApply();
        r.setOrderId(o.getId());
        r.setOrderSn(o.getOrderSn());
        r.setMemberId(o.getMemberId());
        r.setMemberUsername(o.getMemberUsername());
        r.setProductId(p.getId());
        r.setProductName(p.getName());
        r.setProductQuantity(quantity);
        r.setReturnAmount(new BigDecimal(amount));
        r.setReason("Melted in transit");
        return returns.save(r);
    }

    @Test
    void returnsAreAcceptedReceivedRefundedAndOptionallyRestocked() throws Exception {
        String t = superAdmin();
        Member buyer = newMember();
        Product p = bySn("P31");
        ShopOrder o = placeOrder(buyer, p, 4, "20.00");
        lifecycle.markPaid(o.getId(), PayMethod.CARD, "test");
        lifecycle.advance(o.getId(), OrderStatus.READY_FOR_PICKUP, "staff", null);
        lifecycle.advance(o.getId(), OrderStatus.COMPLETED, "staff", null);
        int stockAfterSale = fresh(p).getStock();
        ReturnApply r = returnRequest(o, p, 2, "10.00");

        message("POST", "/returnApply/update/status/" + r.getId(), Map.of("status", "COMPLETED"), t, 409); // must be accepted first
        call("POST", "/returnApply/update/status/" + r.getId(), Map.of("status", "ACCEPTED", "returnAmount", 10.00, "handleNote", "Photos received"), t, 200);
        JsonNode done = call("POST", "/returnApply/update/status/" + r.getId(), Map.of("status", "COMPLETED", "restock", true, "receiveNote", "Sealed pack"), t, 200);

        assertThat(done.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(fresh(p).getStock()).isEqualTo(stockAfterSale + 2);
        assertThat(payments.findByOrderIdOrderByIdDesc(o.getId())).anySatisfy(pay -> {
            assertThat(pay.getStatus().name()).isEqualTo("REFUNDED");
            assertThat(pay.getAmount()).isEqualByComparingTo("10.00");
        });
    }

    @Test
    void refundsCanNeverExceedWhatTheCustomerPaid() throws Exception {
        String t = superAdmin();
        Member buyer = newMember();
        Product p = bySn("P32");
        ShopOrder o = placeOrder(buyer, p, 1, "12.00");
        lifecycle.markPaid(o.getId(), PayMethod.CARD, "test");
        lifecycle.advance(o.getId(), OrderStatus.READY_FOR_PICKUP, "staff", null);
        lifecycle.advance(o.getId(), OrderStatus.COMPLETED, "staff", null);
        ReturnApply r = returnRequest(o, p, 1, "12.00");

        assertThat(message("POST", "/returnApply/update/status/" + r.getId(), Map.of("status", "ACCEPTED", "returnAmount", 15.00), t, 400))
                .contains("exceed");
        ReturnApply other = returnRequest(o, p, 1, "12.00");
        assertThat(message("POST", "/returnApply/update/status/" + other.getId(), Map.of("status", "REJECTED"), t, 400)).contains("Explain why");
        call("POST", "/returnApply/update/status/" + other.getId(), Map.of("status", "REJECTED", "handleNote", "Outside policy"), t, 200);
        assertThat(returns.findById(other.getId()).orElseThrow().getStatus()).isEqualTo(ReturnStatus.REJECTED);
    }

    // ------------------------------------------------------------------------- settings

    @Test
    void orderRulesCanBeTunedAndAreValidated() throws Exception {
        String t = superAdmin();
        JsonNode s = call("GET", "/orderSetting", null, t, 200);
        Map<String, Object> update = new HashMap<>();
        update.put("normalOrderTimeoutMinutes", 45);
        update.put("autoConfirmDays", 5);
        update.put("commentTimeoutDays", 10);
        update.put("deliveryFee", 5.00);
        update.put("freeDeliveryThreshold", 50.00);
        update.put("giftPackagingFee", 2.50);
        update.put("pointsPerCurrencyUnit", 100);
        update.put("maxRedeemPercent", 150);
        update.put("deliveryEnabled", true);
        update.put("pickupLeadMinutes", 30);
        update.put("preorderLeadDays", 3);
        update.put("pointsPerCurrencySpent", 1);
        update.put("returnWindowDays", 7);
        update.put("experienceCancelHours", 24);

        assertThat(message("POST", "/orderSetting/update", update, t, 400)).contains("cannot exceed 100%");
        update.put("maxRedeemPercent", s.get("maxRedeemPercent").asInt());
        assertThat(call("POST", "/orderSetting/update", update, t, 200).get("normalOrderTimeoutMinutes").asInt()).isEqualTo(45);
        update.put("normalOrderTimeoutMinutes", 30);
        call("POST", "/orderSetting/update", update, t, 200);
    }

    // ------------------------------------------------------------------------- personalisation

    @Test
    void staffQuoteAndProofARequestThroughTheApi() throws Exception {
        String t = superAdmin();
        Member m = newMember();
        long id = personalization.submit(m.getId(), PersonalizationKind.NAME_PLAQUE, "Olivia", 1, null, LocalDate.now().plusDays(30)).getId();

        call("POST", "/personalization/" + id + "/quote", Map.of("quoteAmount", 24.0, "depositAmount", 8.0, "leadTimeDays", 5), t, 200);
        message("POST", "/personalization/" + id + "/start", null, t, 409);                       // no deposit yet
        call("POST", "/personalization/" + id + "/proof", Map.of("artworkNote", "Script font, 10 cm"), t, 200);

        JsonNode detail = call("GET", "/personalization/" + id, null, t, 200);
        assertThat(detail.get("request").get("status").asText()).isEqualTo("PROOF_SENT");
        assertThat(detail.get("proofs")).hasSize(1);
        call("GET", "/personalization/list?status=PROOF_SENT", null, t, 200);
    }

    // ------------------------------------------------------------------------- members, coupons, content

    @Test
    void pointsAdjustmentsAreLoggedAndCannotMakeTheBalanceNegative() throws Exception {
        String t = superAdmin();
        Member m = newMember();

        call("POST", "/member/" + m.getId() + "/points", Map.of("delta", 200, "note", "Apology for a late order"), t, 200);
        assertThat(message("POST", "/member/" + m.getId() + "/points", Map.of("delta", -500, "note", "Too much"), t, 409)).contains("negative");

        JsonNode view = call("GET", "/member/" + m.getId(), null, t, 200);
        assertThat(view.get("integration").asInt()).isEqualTo(200);
        assertThat(view.has("password")).isFalse();
        assertThat(call("GET", "/member/" + m.getId() + "/points/history", null, t, 200).get("list").get(0).get("note").asText())
                .isEqualTo("Apology for a late order");
    }

    @Test
    void couponsValidateTheirTargetsAndGrantsRespectTheLimits() throws Exception {
        String t = superAdmin();
        Member m = newMember();
        Map<String, Object> coupon = new HashMap<>();
        coupon.put("couponType", "PRODUCT");
        coupon.put("name", "Pretzel treat");
        coupon.put("totalCount", 2);
        coupon.put("amount", 1.5);
        coupon.put("perLimit", 1);
        coupon.put("minSpend", 0);
        coupon.put("startTime", LocalDateTime.now().minusDays(1).toString());
        coupon.put("endTime", LocalDateTime.now().plusDays(10).toString());

        assertThat(message("POST", "/coupon/create", coupon, t, 400)).contains("Choose the products");
        coupon.put("productIds", Set.of(bySn("P25").getId()));
        long id = call("POST", "/coupon/create", coupon, t, 200).get("id").asLong();

        call("POST", "/coupon/grant", Map.of("couponId", id, "memberId", m.getId()), t, 200);
        assertThat(message("POST", "/coupon/grant", Map.of("couponId", id, "memberId", m.getId()), t, 409)).contains("maximum");
        call("POST", "/coupon/grant", Map.of("couponId", id, "memberId", newMember().getId()), t, 200);
        assertThat(message("POST", "/coupon/grant", Map.of("couponId", id, "memberId", newMember().getId()), t, 409)).contains("No coupons left");

        assertThat(message("POST", "/coupon/delete/" + id, null, t, 409)).contains("disable it");
        assertThat(call("GET", "/couponHistory/list?couponId=" + id, null, t, 200).get("total").asInt()).isEqualTo(2);
    }

    @Test
    void homeSlotsAndContentCanBeManaged() throws Exception {
        String t = superAdmin();
        assertThat(message("POST", "/home/slot/create", Map.of("slotType", "NEW_PRODUCT", "name", "No ref"), t, 400)).contains("must reference");
        long slot = call("POST", "/home/slot/create", Map.of("slotType", "NEW_PRODUCT", "refId", bySn("P38").getId(), "name", "Cookie"), t, 200).get("id").asLong();
        call("POST", "/home/slot/status/" + slot + "?status=false", null, t, 200);

        String code = "Z" + UUID.randomUUID().toString().substring(0, 6);
        long pairing = call("POST", "/pairing/create", Map.of("code", code, "foodGroup", "Fruit", "food", "Kiwi", "priority", "TEST"), t, 200).get("id").asLong();
        assertThat(message("POST", "/pairing/create", Map.of("code", code, "food", "Duplicate", "priority", "TEST"), t, 409)).contains("already exists");
        call("POST", "/pairing/delete/" + pairing, null, t, 200);

        long subject = call("POST", "/subject/create", Map.of("title", "Gift ideas", "content", "Ideas...", "productIds", Set.of(bySn("P47").getId())), t, 200).get("id").asLong();
        assertThat(call("GET", "/subject/" + subject, null, t, 200).get("productIds")).hasSize(1);
        message("POST", "/subject/create", Map.of("title", "Broken", "productIds", Set.of(9_999_999)), t, 400);
    }

    // ------------------------------------------------------------------------- reports

    @Test
    void reportsReflectPaidOrdersAndContributionDeductsCosts() throws Exception {
        String t = superAdmin();
        Member buyer = newMember();
        Product p = bySn("P23"); // Chocolate almond clusters, demo price 6.00
        ShopOrder o = placeOrder(buyer, p, 10, "60.00");
        lifecycle.markPaid(o.getId(), PayMethod.CARD, "test");
        String today = LocalDate.now().toString();

        JsonNode dash = call("GET", "/report/dashboard", null, t, 200);
        assertThat(dash.get("revenueToday").asDouble()).isGreaterThanOrEqualTo(60.0);
        assertThat(dash.get("orderQueue").get("PREPARING").asInt()).isGreaterThanOrEqualTo(1);

        JsonNode daily = call("GET", "/report/sales/daily?from=" + today + "&to=" + today, null, t, 200);
        assertThat(daily).hasSize(1);
        assertThat(daily.get(0).get("revenue").asDouble()).isGreaterThanOrEqualTo(60.0);

        JsonNode contribution = call("GET", "/report/contribution?from=" + today + "&to=" + today, null, t, 200);
        JsonNode row = null;
        for (JsonNode r : contribution) {
            if (r.get("productId").asLong() == p.getId()) {
                row = r;
            }
        }
        assertThat(row).isNotNull();
        // 60.00 revenue - 10 x (1.44 + 0.36 + 1.20) costs - 0.90 selling fees
        assertThat(row.get("contribution").asDouble()).isEqualTo(29.10);
        assertThat(row.get("marginPercent").asDouble()).isEqualTo(48.5);

        message("GET", "/report/sales/daily?from=" + today + "&to=2000-01-01", null, t, 400);
        assertThat(call("GET", "/report/products/top?from=" + today + "&to=" + today + "&limit=3", null, t, 200).size()).isLessThanOrEqualTo(3);
    }
}
