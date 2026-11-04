package com.chocomall.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.chocomall.model.entity.Product;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OperationsAdminTest extends AdminTestSupport {

    // ------------------------------------------------------------------------- stock

    @Test
    void manualStockChangesFollowTheirReasonAndLandInTheLedger() throws Exception {
        String t = superAdmin();
        Product p = bySn("P12");
        int before = fresh(p).getStock();

        assertThat(message("POST", "/stock/adjust", Map.of("productId", p.getId(), "delta", 5, "reason", "WASTE"), t, 400)).contains("negative");
        assertThat(message("POST", "/stock/adjust", Map.of("productId", p.getId(), "delta", -5, "reason", "PRODUCTION"), t, 400)).contains("positive");
        message("POST", "/stock/adjust", Map.of("productId", p.getId(), "delta", 0, "reason", "ADJUSTMENT"), t, 400);

        assertThat(call("POST", "/stock/adjust", Map.of("productId", p.getId(), "delta", -3, "reason", "WASTE", "reference", "dropped tray"), t, 200).asInt())
                .isEqualTo(before - 3);
        JsonNode ledger = call("GET", "/stock/ledger/" + p.getId(), null, t, 200);
        assertThat(ledger.get("list").get(0).get("reason").asText()).isEqualTo("WASTE");
        assertThat(ledger.get("list").get(0).get("operator").asText()).isEqualTo("admin");

        assertThat(call("POST", "/stock/count", Map.of("productId", p.getId(), "counted", 77), t, 200).asInt()).isEqualTo(77);
    }

    @Test
    void untrackedItemsHaveNoStockToManage() throws Exception {
        String t = superAdmin();
        Product drink = bySn("P33");
        assertThat(message("POST", "/stock/adjust", Map.of("productId", drink.getId(), "delta", 5, "reason", "ADJUSTMENT"), t, 409))
                .contains("not tracked");
    }

    @Test
    void theInventoryViewListsOnlyShelfStockAndFlagsLowItems() throws Exception {
        String t = superAdmin();
        Product p = bySn("P13");
        call("POST", "/stock/count", Map.of("productId", p.getId(), "counted", 3), t, 200);

        JsonNode low = call("GET", "/stock/list?lowOnly=true&pageSize=100", null, t, 200);
        boolean found = false;
        for (JsonNode row : low.get("list")) {
            found |= row.get("productSn").asText().equals("P13") && row.get("low").asBoolean();
            assertThat(row.get("productSn").asText()).isNotEqualTo("P33"); // made-to-order drink
        }
        assertThat(found).isTrue();
    }

    // ------------------------------------------------------------------------- production

    private long plan(String t, Product p, int quantity, String process) throws Exception {
        Map<String, Object> param = new HashMap<>();
        param.put("productId", p.getId());
        param.put("quantityUnits", quantity);
        if (process != null) {
            param.put("processCode", process);
        }
        return call("POST", "/production/create", param, t, 200).get("id").asLong();
    }

    @Test
    void releasingABatchAddsGoodUnitsToStockAndRecordsWaste() throws Exception {
        String t = superAdmin();
        Product p = bySn("P14");
        int before = fresh(p).getStock();
        long id = plan(t, p, 50, "M18");

        call("POST", "/production/" + id + "/start", null, t, 200);
        JsonNode released = call("POST", "/production/" + id + "/release", Map.of("allergenChecked", true,
                "bestBefore", LocalDate.now().plusDays(30).toString(), "wasteUnits", 4), t, 200);

        assertThat(released.get("status").asText()).isEqualTo("RELEASED");
        assertThat(fresh(p).getStock()).isEqualTo(before + 46);
        JsonNode waste = call("GET", "/production/report/waste", null, t, 200);
        boolean listed = false;
        for (JsonNode row : waste) {
            if (row.get("productSn").asText().equals("P14")) {
                listed = true;
                assertThat(row.get("wasteUnits").asInt()).isEqualTo(4);
                assertThat(row.get("releasedUnits").asInt()).isEqualTo(50);
            }
        }
        assertThat(listed).isTrue();
    }

    @Test
    void aBatchCannotBeReleasedWithoutTheRequiredControls() throws Exception {
        String t = superAdmin();
        Product p = bySn("P16");
        long id = plan(t, p, 20, "M11"); // shell molding needs a temper test

        assertThat(message("POST", "/production/" + id + "/release", Map.of("allergenChecked", false, "bestBefore", LocalDate.now().plusDays(5).toString()), t, 400))
                .contains("allergen check");
        assertThat(message("POST", "/production/" + id + "/release", Map.of("allergenChecked", true, "bestBefore", LocalDate.now().plusDays(5).toString()), t, 400))
                .contains("temper test");
        assertThat(message("POST", "/production/" + id + "/release", Map.of("allergenChecked", true, "temperTestPassed", false,
                "bestBefore", LocalDate.now().plusDays(5).toString()), t, 409)).contains("Reject a streaked or soft batch");
        assertThat(message("POST", "/production/" + id + "/release", Map.of("allergenChecked", true, "temperTestPassed", true), t, 400))
                .contains("best-before");
        assertThat(message("POST", "/production/" + id + "/release", Map.of("allergenChecked", true, "temperTestPassed", true,
                "bestBefore", LocalDate.now().minusDays(1).toString()), t, 400)).contains("after the production date");
        assertThat(message("POST", "/production/" + id + "/release", Map.of("allergenChecked", true, "temperTestPassed", true,
                "bestBefore", LocalDate.now().plusDays(5).toString(), "wasteUnits", 99), t, 400)).contains("Waste cannot exceed");

        call("POST", "/production/" + id + "/release", Map.of("allergenChecked", true, "temperTestPassed", true,
                "bestBefore", LocalDate.now().plusDays(5).toString()), t, 200);
        assertThat(message("POST", "/production/" + id + "/release", Map.of("allergenChecked", true, "temperTestPassed", true,
                "bestBefore", LocalDate.now().plusDays(5).toString()), t, 409)).contains("cannot be released");
    }

    @Test
    void rejectedBatchesAddNothingAndOnlyProductionProcessesAreAccepted() throws Exception {
        String t = superAdmin();
        Product p = bySn("P15");
        int before = fresh(p).getStock();
        long id = plan(t, p, 30, null);

        call("POST", "/production/" + id + "/reject", Map.of("reason", "streaky bloom"), t, 200);
        assertThat(fresh(p).getStock()).isEqualTo(before);
        message("POST", "/production/" + id + "/start", null, t, 409);

        Map<String, Object> bad = new HashMap<>(Map.of("productId", p.getId(), "quantityUnits", 5, "processCode", "D01")); // a 3D-printing step
        assertThat(message("POST", "/production/create", bad, t, 400)).contains("not a production process");
        Map<String, Object> drink = new HashMap<>(Map.of("productId", bySn("P33").getId(), "quantityUnits", 5));
        message("POST", "/production/create", drink, t, 409);
    }

    // ------------------------------------------------------------------------- planning

    @Test
    void preorderWindowsValidateTheirDatesAndCapacity() throws Exception {
        String t = superAdmin();
        LocalDateTime cutoff = LocalDateTime.now().plusDays(10).withNano(0);
        Map<String, Object> w = new HashMap<>(Map.of("name", "Easter", "occasion", "Easter", "orderCutoff", cutoff.toString(),
                "fulfilFrom", cutoff.plusDays(3).toLocalDate().toString(), "fulfilTo", cutoff.plusDays(8).toLocalDate().toString(), "capacityUnits", 100));

        long id = call("POST", "/preorderWindow/create", w, t, 200).get("id").asLong();

        Map<String, Object> backwards = new HashMap<>(w);
        backwards.put("fulfilTo", cutoff.plusDays(1).toLocalDate().toString());
        assertThat(message("POST", "/preorderWindow/create", backwards, t, 400)).contains("before its start");

        Map<String, Object> past = new HashMap<>(w);
        past.put("orderCutoff", LocalDateTime.now().minusDays(1).toString());
        assertThat(message("POST", "/preorderWindow/create", past, t, 400)).contains("future");

        call("POST", "/preorderWindow/status/" + id + "?status=CLOSED", null, t, 200);
        call("POST", "/preorderWindow/delete/" + id, null, t, 200);
    }

    @Test
    void sessionsAreOnlyForExperienceProductsAndCancellingOneRefundsBookings() throws Exception {
        String t = superAdmin();
        Product tasting = bySn("P51");
        Map<String, Object> s = new HashMap<>(Map.of("productId", tasting.getId(), "startsAt", LocalDateTime.now().plusDays(9).toString(),
                "capacity", 6, "price", 18.0, "location", "Main boutique"));
        long id = call("POST", "/experience/session/create", s, t, 200).get("id").asLong();

        Map<String, Object> wrongProduct = new HashMap<>(s);
        wrongProduct.put("productId", bySn("P25").getId());
        assertThat(message("POST", "/experience/session/create", wrongProduct, t, 400)).contains("experience products");

        assertThat(call("POST", "/experience/session/cancel/" + id, null, t, 200).asInt()).isZero();
        assertThat(call("GET", "/experience/session/" + id, null, t, 200).get("status").asText()).isEqualTo("CANCELLED");
        message("POST", "/experience/session/update/" + id, s, t, 409);
    }

    @Test
    void bundlesMustReferenceRealProductsAndMatchingSkus() throws Exception {
        String t = superAdmin();
        Product drink = bySn("P34");
        Map<String, Object> bundle = new HashMap<>(Map.of("name", "Iced + cookie", "price", 7.0,
                "items", List.of(Map.of("productId", drink.getId(), "quantity", 1), Map.of("productId", bySn("P38").getId(), "quantity", 1))));
        call("POST", "/bundle/create", bundle, t, 200);

        Map<String, Object> wrongSku = new HashMap<>(Map.of("name", "Broken", "price", 7.0,
                "items", List.of(Map.of("productId", drink.getId(), "skuId", 1, "quantity", 1))));
        assertThat(message("POST", "/bundle/create", wrongSku, t, 400)).contains("does not belong");
    }

    // ------------------------------------------------------------------------- launch playbook

    @Test
    void thePlaybookIsATrackableChecklist() throws Exception {
        String t = superAdmin();
        JsonNode before = call("GET", "/guide/progress", null, t, 200);
        assertThat(before.get("PRODUCTION").get("total").asInt()).isEqualTo(36);
        assertThat(before.get("PRINTING").get("total").asInt()).isEqualTo(26);
        assertThat(before.get("SALES_LAUNCH").get("total").asInt()).isEqualTo(30);

        JsonNode items = call("GET", "/guide/list?type=PRODUCTION&phase=&priority=CORE", null, t, 200);
        long id = items.get(0).get("id").asLong();
        call("POST", "/guide/update/" + id, Map.of("status", "DONE", "implementedBy", "Receiving log, lot labels"), t, 200);
        assertThat(call("GET", "/guide/progress", null, t, 200).get("PRODUCTION").get("done").asInt())
                .isGreaterThan(before.get("PRODUCTION").get("done").asInt());

        JsonNode checklist = call("GET", "/guide/launch-checklist", null, t, 200);
        assertThat(checklist.get("Launch").size()).isGreaterThan(10);
        assertThat(call("GET", "/guide/sources", null, t, 200)).hasSize(8);
        assertThat(call("GET", "/guide/list?type=ROADMAP", null, t, 200)).hasSize(5);
        assertThat(call("GET", "/guide/list?type=SHOP_FORMAT", null, t, 200)).hasSize(8);
    }
}
