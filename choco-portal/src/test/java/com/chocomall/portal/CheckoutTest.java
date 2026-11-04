package com.chocomall.portal;

import static org.assertj.core.api.Assertions.assertThat;

import com.chocomall.model.entity.PreorderWindow;
import com.chocomall.model.entity.Product;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.model.enums.WindowStatus;
import com.chocomall.model.repository.PreorderWindowRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class CheckoutTest extends PortalTestSupport {

    @Autowired PreorderWindowRepository windows;
    @Autowired JdbcTemplate jdbc;

    private JsonNode generate(Session s, Map<String, Object> param, int status) throws Exception {
        return call("POST", "/order/generate", param, s.token(), status);
    }

    // ------------------------------------------------------------------------- pricing

    @Test
    void checkoutReservesStockClearsTheBasketAndPricesFromTheSku() throws Exception {
        Session s = register();
        Product p = newProduct(10, "4.00");
        long line = addToCart(s.token(), p, 3);

        JsonNode order = generate(s, pickup(List.of(line)), 201);

        assertThat(order.get("order").get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(order.get("order").get("payAmount").asDouble()).isEqualTo(12.0);
        assertThat(order.get("items").get(0).get("productQuantity").asInt()).isEqualTo(3);
        assertThat(fresh(p).getLockStock()).isEqualTo(3);
        assertThat(call("GET", "/cart/list", null, s.token(), 200).get("items")).isEmpty();
    }

    @Test
    void theBasketCannotBeBoughtWithAnotherCustomersLines() throws Exception {
        Session a = register();
        Session b = register();
        long line = addToCart(a.token(), newProduct(5, "3.00"), 1);

        call("POST", "/order/generate", pickup(List.of(line)), b.token(), 404);
    }

    @Test
    void aCouponAppliesOnlyWhenTheMinimumSpendIsMet() throws Exception {
        Session s = register();
        Product p = newProduct(50, "10.00");
        long coupon = claimWelcomeCoupon(s);

        long small = addToCart(s.token(), p, 2); // 20.00 is below the 25.00 minimum
        Map<String, Object> param = pickup(List.of(small));
        param.put("couponHistoryId", coupon);
        JsonNode preview = call("POST", "/order/confirm", param, s.token(), 200);
        assertThat(preview.get("couponDiscount").asDouble()).isZero();
        assertThat(preview.get("problems").toString()).contains("spend at least");
        assertThat(message("POST", "/order/generate", param, s.token(), 400)).contains("spend at least");

        addToCart(s.token(), p, 1); // now 30.00
        long both = call("GET", "/cart/list", null, s.token(), 200).get("items").get(0).get("id").asLong();
        Map<String, Object> ok = pickup(List.of(both));
        ok.put("couponHistoryId", coupon);
        JsonNode order = generate(s, ok, 201);
        assertThat(order.get("order").get("couponAmount").asDouble()).isEqualTo(5.0);
        assertThat(order.get("order").get("payAmount").asDouble()).isEqualTo(25.0);
        assertThat(order.get("items").get(0).get("couponAmount").asDouble()).isEqualTo(5.0);

        // the coupon is spent; a second order cannot reuse it
        long another = addToCart(s.token(), p, 3);
        Map<String, Object> reuse = pickup(List.of(another));
        reuse.put("couponHistoryId", coupon);
        call("POST", "/order/generate", reuse, s.token(), 400);
    }

    private long claimWelcomeCoupon(Session s) throws Exception {
        JsonNode claimable = call("GET", "/member/coupon/claimable", null, s.token(), 200);
        long couponId = 0;
        for (JsonNode c : claimable) {
            if (c.get("name").asText().startsWith("Welcome")) {
                couponId = c.get("couponId").asLong();
            }
        }
        call("POST", "/member/coupon/claim/" + couponId, null, s.token(), 200);
        return call("GET", "/member/coupon/list?useStatus=UNUSED", null, s.token(), 200).get(0).get("historyId").asLong();
    }

    @Test
    void aCouponCanBeClaimedOnlyUpToItsPerMemberLimit() throws Exception {
        Session s = register();
        claimWelcomeCoupon(s);
        JsonNode claimable = call("GET", "/member/coupon/claimable", null, s.token(), 200);
        claimable.forEach(c -> assertThat(c.get("name").asText()).doesNotStartWith("Welcome"));
        message("POST", "/member/coupon/claim/1", null, s.token(), 409);
    }

    @Test
    void giftSleeveAndDeliveryFeesFollowTheShopSettings() throws Exception {
        Session s = register();
        Product p = newProduct(50, "5.00");
        long address = call("POST", "/member/address/add", Map.of("name", "Dana", "phone", "+15550000000", "detailAddress", "1 Cocoa St"), s.token(), 200).get("id").asLong();

        long cheap = addToCart(s.token(), p, 2); // 10.00 < 40.00 free-delivery threshold
        Map<String, Object> delivery = new HashMap<>(Map.of("cartItemIds", List.of(cheap), "fulfillmentType", "DELIVERY", "addressId", address, "giftSleeve", true));
        JsonNode preview = call("POST", "/order/confirm", delivery, s.token(), 200);
        assertThat(preview.get("deliveryFee").asDouble()).isEqualTo(4.5);
        assertThat(preview.get("giftPackagingFee").asDouble()).isEqualTo(2.0);
        assertThat(preview.get("payAmount").asDouble()).isEqualTo(16.5);

        addToCart(s.token(), p, 8); // 50.00 in total -> free delivery
        long big = call("GET", "/cart/list", null, s.token(), 200).get("items").get(0).get("id").asLong();
        delivery.put("cartItemIds", List.of(big));
        JsonNode free = call("POST", "/order/confirm", delivery, s.token(), 200);
        assertThat(free.get("deliveryFee").asDouble()).isZero();
        assertThat(free.get("payAmount").asDouble()).isEqualTo(52.0);
    }

    @Test
    void pointsCanPayAPartOfTheOrderButNotMoreThanTheCap() throws Exception {
        Session s = register();
        jdbc.update("update ums_member set integration = 5000 where id = ?", s.memberId());
        Product p = newProduct(50, "10.00");
        long line = addToCart(s.token(), p, 5); // 50.00; cap 20% = 10.00 = 1000 points

        Map<String, Object> over = pickup(List.of(line));
        over.put("useIntegration", 1500);
        assertThat(message("POST", "/order/generate", over, s.token(), 400)).contains("at most 1000 points");

        Map<String, Object> ok = pickup(List.of(line));
        ok.put("useIntegration", 1000);
        JsonNode order = generate(s, ok, 201);
        assertThat(order.get("order").get("integrationAmount").asDouble()).isEqualTo(10.0);
        assertThat(order.get("order").get("payAmount").asDouble()).isEqualTo(40.0);
        assertThat(call("GET", "/sso/info", null, s.token(), 200).get("points").asInt()).isEqualTo(4000);
    }

    @Test
    void aBundleDiscountAppliesPerCompleteSet() throws Exception {
        Session s = register();
        long drink = addToCart(s.token(), bySn("P33"), 2); // house hot chocolate
        addToCart(s.token(), bySn("P37"), 1);              // house brownie: one complete "Drink + treat" set
        List<Long> all = new java.util.ArrayList<>();
        call("GET", "/cart/list", null, s.token(), 200).get("items").forEach(i -> all.add(i.get("id").asLong()));

        JsonNode preview = call("POST", "/order/confirm", pickup(all), s.token(), 200);

        assertThat(preview.get("bundleDiscount").asDouble()).isEqualTo(1.0);
        assertThat(preview.get("notices").toString()).contains("Drink + treat");
        assertThat(drink).isPositive();
    }

    // ------------------------------------------------------------------------- fulfilment rules

    @Test
    void chilledItemsAreNotDeliveredAndPickupNeedsPreparationTime() throws Exception {
        Session s = register();
        Product chilled = newProduct(10, "9.00", ProductKind.MADE_FRESH, false, true);
        long address = call("POST", "/member/address/add", Map.of("name", "Dana", "phone", "+15550000000", "detailAddress", "1 Cocoa St"), s.token(), 200).get("id").asLong();
        long line = addToCart(s.token(), chilled, 1);

        Map<String, Object> delivery = new HashMap<>(Map.of("cartItemIds", List.of(line), "fulfillmentType", "DELIVERY", "addressId", address));
        assertThat(message("POST", "/order/generate", delivery, s.token(), 400)).contains("pickup only");

        Map<String, Object> tooSoon = pickup(List.of(line));
        tooSoon.put("pickupSlot", LocalDateTime.now().plusMinutes(5).toString());
        assertThat(message("POST", "/order/generate", tooSoon, s.token(), 400)).contains("30 minutes");

        Map<String, Object> later = pickup(List.of(line));
        later.put("pickupSlot", LocalDateTime.now().plusHours(3).toString());
        generate(s, later, 201);
    }

    @Test
    void deliveryNeedsAnAddress() throws Exception {
        Session s = register();
        long line = addToCart(s.token(), newProduct(10, "9.00"), 1);

        Map<String, Object> delivery = new HashMap<>(Map.of("cartItemIds", List.of(line), "fulfillmentType", "DELIVERY"));
        assertThat(message("POST", "/order/generate", delivery, s.token(), 400)).contains("delivery address");
    }

    // ------------------------------------------------------------------------- preorders

    private PreorderWindow window(int capacity, LocalDateTime cutoff, WindowStatus status) {
        PreorderWindow w = new PreorderWindow();
        w.setName("Test window");
        w.setOrderCutoff(cutoff);
        w.setFulfilFrom(LocalDate.now().plusDays(10));
        w.setFulfilTo(LocalDate.now().plusDays(20));
        w.setCapacityUnits(capacity);
        w.setStatus(status);
        return windows.save(w);
    }

    @Test
    void preordersReserveWindowCapacityAndReleaseItOnCancellation() throws Exception {
        Session s = register();
        Product box = newProduct(0, "30.00", ProductKind.MADE_IN_SHOP, true, false);
        PreorderWindow w = window(5, LocalDateTime.now().plusDays(5), WindowStatus.OPEN);
        long line = addToCart(s.token(), box, 3);

        Map<String, Object> missing = pickup(List.of(line));
        assertThat(message("POST", "/order/generate", missing, s.token(), 400)).contains("preorder window");

        Map<String, Object> ok = pickup(List.of(line));
        ok.put("preorderWindowId", w.getId());
        ok.put("pickupSlot", LocalDate.now().plusDays(12).atTime(10, 0).toString());
        JsonNode order = generate(s, ok, 201);
        assertThat(order.get("order").get("preorder").asBoolean()).isTrue();
        assertThat(windows.findById(w.getId()).orElseThrow().getReservedUnits()).isEqualTo(3);

        long second = addToCart(s.token(), box, 3); // only 2 units left in the window
        Map<String, Object> tooMany = pickup(List.of(second));
        tooMany.put("preorderWindowId", w.getId());
        tooMany.put("pickupSlot", LocalDate.now().plusDays(12).atTime(10, 0).toString());
        assertThat(message("POST", "/order/generate", tooMany, s.token(), 400)).contains("Only 2 units are left");

        call("POST", "/order/cancel/" + order.get("order").get("id").asLong(), null, s.token(), 200);
        assertThat(windows.findById(w.getId()).orElseThrow().getReservedUnits()).isZero();
    }

    @Test
    void preorderItemsCannotBeMixedWithItemsAvailableNowAndClosedWindowsAreRefused() throws Exception {
        Session s = register();
        Product preorder = newProduct(0, "30.00", ProductKind.MADE_IN_SHOP, true, false);
        Product normal = newProduct(10, "3.00");
        PreorderWindow open = window(50, LocalDateTime.now().plusDays(5), WindowStatus.OPEN);
        PreorderWindow closed = window(50, LocalDateTime.now().plusDays(5), WindowStatus.CLOSED);
        long a = addToCart(s.token(), preorder, 1);
        long b = addToCart(s.token(), normal, 1);

        Map<String, Object> mixed = pickup(List.of(a, b));
        mixed.put("preorderWindowId", open.getId());
        mixed.put("pickupSlot", LocalDate.now().plusDays(12).atTime(10, 0).toString());
        assertThat(message("POST", "/order/generate", mixed, s.token(), 400)).contains("ordered separately");

        Map<String, Object> shut = pickup(List.of(a));
        shut.put("preorderWindowId", closed.getId());
        shut.put("pickupSlot", LocalDate.now().plusDays(12).atTime(10, 0).toString());
        assertThat(message("POST", "/order/generate", shut, s.token(), 400)).contains("has closed");

        Map<String, Object> outside = pickup(List.of(a));
        outside.put("preorderWindowId", open.getId());
        outside.put("pickupSlot", LocalDate.now().plusDays(2).atTime(10, 0).toString());
        assertThat(message("POST", "/order/generate", outside, s.token(), 400)).contains("between");
    }

    // ------------------------------------------------------------------------- payment and cancellation

    @Test
    void payingMovesTheOrderForwardAndEarnsPoints() throws Exception {
        Session s = register();
        Product p = newProduct(10, "8.00");
        JsonNode order = generate(s, pickup(List.of(addToCart(s.token(), p, 2))), 201);
        long id = order.get("order").get("id").asLong();

        message("POST", "/payment/pay", Map.of("orderId", id, "method", "CARD", "cardToken", "tok_decline"), s.token(), 402);
        assertThat(call("GET", "/order/detail/" + id, null, s.token(), 200).get("order").get("status").asText()).isEqualTo("PENDING_PAYMENT");

        JsonNode payment = call("POST", "/payment/pay", Map.of("orderId", id, "method", "CARD", "cardToken", "tok_ok"), s.token(), 200);
        assertThat(payment.get("status").asText()).isEqualTo("SUCCEEDED");

        JsonNode detail = call("GET", "/order/detail/" + id, null, s.token(), 200);
        assertThat(detail.get("order").get("status").asText()).isEqualTo("PREPARING");
        assertThat(detail.get("timeline").size()).isGreaterThanOrEqualTo(2);
        assertThat(fresh(p).getStock()).isEqualTo(8);
        assertThat(call("GET", "/sso/info", null, s.token(), 200).get("points").asInt()).isEqualTo(16);
    }

    @Test
    void customersCancelUnpaidOrdersButPaidOnesNeedTheShop() throws Exception {
        Session s = register();
        Product p = newProduct(10, "8.00");
        long unpaid = generate(s, pickup(List.of(addToCart(s.token(), p, 2))), 201).get("order").get("id").asLong();
        call("POST", "/order/cancel/" + unpaid, null, s.token(), 200);
        assertThat(fresh(p).getLockStock()).isZero();
        assertThat(fresh(p).getStock()).isEqualTo(10);

        long paid = generate(s, pickup(List.of(addToCart(s.token(), p, 1))), 201).get("order").get("id").asLong();
        call("POST", "/payment/pay", Map.of("orderId", paid, "method", "CARD", "cardToken", "tok_ok"), s.token(), 200);
        assertThat(message("POST", "/order/cancel/" + paid, null, s.token(), 409)).contains("already paid");
    }

    @Test
    void customersOnlySeeTheirOwnOrders() throws Exception {
        Session a = register();
        Session b = register();
        long id = generate(a, pickup(List.of(addToCart(a.token(), newProduct(5, "2.00"), 1))), 201).get("order").get("id").asLong();

        call("GET", "/order/detail/" + id, null, b.token(), 404);
        assertThat(call("GET", "/order/list", null, b.token(), 200).get("list")).isEmpty();
        call("POST", "/payment/pay", Map.of("orderId", id, "method", "CARD", "cardToken", "tok_ok"), b.token(), 404);
    }

    @Test
    void anOrderNeverCostsLessThanItsLinesAfterDiscountAllocation() throws Exception {
        Session s = register();
        jdbc.update("update ums_member set integration = 5000 where id = ?", s.memberId());
        Product p = newProduct(50, "3.33");
        Product q = newProduct(50, "7.77");
        long a = addToCart(s.token(), p, 3);
        long b = addToCart(s.token(), q, 4);
        long coupon = claimWelcomeCoupon(s);
        Map<String, Object> param = pickup(List.of(a, b));
        param.put("couponHistoryId", coupon);
        param.put("useIntegration", 333);

        JsonNode order = generate(s, param, 201);

        double lines = 0;
        for (JsonNode item : order.get("items")) {
            lines += item.get("realAmount").asDouble();
        }
        // line amounts (after coupon and points) add up exactly to the goods part of the payable amount
        assertThat(lines).isEqualTo(order.get("order").get("payAmount").asDouble());
    }
}
