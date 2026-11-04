package com.chocomall.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.chocomall.model.entity.Member;
import com.chocomall.model.entity.Product;
import com.chocomall.model.repository.AllergenRepository;
import com.chocomall.model.repository.FlavorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CatalogAdminTest extends AdminTestSupport {

    @Autowired AllergenRepository allergens;
    @Autowired FlavorRepository flavors;

    private long categoryId(String token) throws Exception {
        for (JsonNode group : call("GET", "/productCategory/list/withChildren", null, token, 200)) {
            for (JsonNode child : group.get("children")) {
                if (child.get("name").asText().equals("Bars")) {
                    return child.get("id").asLong();
                }
            }
        }
        throw new AssertionError("Bars category");
    }

    private Map<String, Object> productParam(long categoryId, String sn) {
        Map<String, Object> p = new HashMap<>();
        p.put("categoryId", categoryId);
        p.put("productSn", sn);
        p.put("name", "Admin test bar " + sn);
        p.put("price", 6.50);
        p.put("kind", "MADE_IN_SHOP");
        p.put("launchTier", "EXPAND");
        p.put("ingredientCost", 1.60);
        p.put("packagingCost", 0.40);
        p.put("laborCost", 1.30);
        p.put("sellingFeeRate", 0.015);
        p.put("initialStock", 40);
        p.put("skus", List.of(Map.of("skuCode", sn + "-STD", "label", "Standard", "units", 1, "price", 6.50)));
        return p;
    }

    @Test
    void aProductIsCreatedWithSkusStockAndStaysUnpublishedUntilReviewed() throws Exception {
        String t = superAdmin();
        String sn = "A" + UUID.randomUUID().toString().substring(0, 8);
        JsonNode created = call("POST", "/product/create", productParam(categoryId(t), sn), t, 200);

        assertThat(created.get("product").get("published").asBoolean()).isFalse();
        assertThat(created.get("skus")).hasSize(1);
        assertThat(created.get("available").asInt()).isEqualTo(40);
        long id = created.get("product").get("id").asLong();

        // allergens not reviewed -> cannot be published
        assertThat(message("POST", "/product/update/publishStatus", Map.of("ids", Set.of(id), "value", true), t, 409))
                .contains("allergens have not been reviewed");

        Map<String, Object> reviewed = productParam(categoryId(t), sn);
        reviewed.put("allergensReviewed", true);
        reviewed.put("allergenIds", Set.of(allergens.findByCode("MILK").orElseThrow().getId()));
        call("POST", "/product/update/" + id, reviewed, t, 200);
        call("POST", "/product/update/publishStatus", Map.of("ids", Set.of(id), "value", true), t, 200);

        assertThat(call("GET", "/product/" + id, null, t, 200).get("product").get("published").asBoolean()).isTrue();
        assertThat(call("GET", "/product/" + id, null, t, 200).get("allergens").get(0).get("name").asText()).isEqualTo("Milk");
    }

    @Test
    void duplicateNumbersAndSkuCodesAreRefused() throws Exception {
        String t = superAdmin();
        long cat = categoryId(t);
        String sn = "D" + UUID.randomUUID().toString().substring(0, 8);
        call("POST", "/product/create", productParam(cat, sn), t, 200);

        assertThat(message("POST", "/product/create", productParam(cat, sn), t, 409)).contains("already used");

        Map<String, Object> clash = productParam(cat, "D" + UUID.randomUUID().toString().substring(0, 8));
        clash.put("skus", List.of(Map.of("skuCode", sn + "-STD", "label", "Standard", "units", 1, "price", 6.50)));
        assertThat(message("POST", "/product/create", clash, t, 409)).contains("belongs to another product");
    }

    @Test
    void validationRejectsBadProducts() throws Exception {
        String t = superAdmin();
        long cat = categoryId(t);

        Map<String, Object> noSkus = productParam(cat, "V1" + UUID.randomUUID().toString().substring(0, 6));
        noSkus.put("skus", List.of());
        call("POST", "/product/create", noSkus, t, 400);

        Map<String, Object> badBox = productParam(cat, "V2" + UUID.randomUUID().toString().substring(0, 6));
        badBox.put("buildABox", true);
        badBox.put("kind", "MADE_TO_ORDER");
        assertThat(message("POST", "/product/create", badBox, t, 400)).contains("made in the shop");

        Map<String, Object> negative = productParam(cat, "V3" + UUID.randomUUID().toString().substring(0, 6));
        negative.put("price", -1);
        call("POST", "/product/create", negative, t, 400);
    }

    @Test
    void contributionPerUnitSubtractsCostsAndFees() throws Exception {
        String t = superAdmin();
        String sn = "C" + UUID.randomUUID().toString().substring(0, 8);
        long id = call("POST", "/product/create", productParam(categoryId(t), sn), t, 200).get("product").get("id").asLong();

        JsonNode c = call("GET", "/product/" + id + "/contribution", null, t, 200);

        // 6.50 - 1.60 ingredients - 0.40 packaging - 1.30 labour - 0.10 selling fees (1.5%)
        assertThat(c.get("sellingFee").asDouble()).isEqualTo(0.10);
        assertThat(c.get("contributionPerUnit").asDouble()).isEqualTo(3.10);
        assertThat(c.get("marginPercent").asDouble()).isEqualTo(47.7);
        assertThat(c.get("costsComplete").asBoolean()).isTrue();
    }

    @Test
    void claimsNeedEvidenceBeforeTheyCanBeVerified() throws Exception {
        String t = superAdmin();
        Product p = bySn("P25");

        long noEvidence = call("POST", "/product/" + p.getId() + "/claim", Map.of("claimType", "HALAL"), t, 200).get("id").asLong();
        assertThat(message("POST", "/product/claim/verify/" + noEvidence, null, t, 400)).contains("evidence");

        long withEvidence = call("POST", "/product/" + p.getId() + "/claim", Map.of("claimType", "VEGAN", "evidence", "Supplier letter 12/2026"), t, 200).get("id").asLong();
        JsonNode verified = call("POST", "/product/claim/verify/" + withEvidence, null, t, 200);
        assertThat(verified.get("verified").asBoolean()).isTrue();
        assertThat(verified.get("verifiedBy").asText()).isEqualTo("admin");
    }

    @Test
    void aProductInAnOpenOrderCannotBeDeletedAndDeletionHidesProducts() throws Exception {
        String t = superAdmin();
        long cat = categoryId(t);
        Member buyer = newMember();
        String sn = "X" + UUID.randomUUID().toString().substring(0, 8);
        long id = call("POST", "/product/create", productParam(cat, sn), t, 200).get("product").get("id").asLong();
        Product p = products.findById(id).orElseThrow();
        placeOrder(buyer, p, 1, "6.50");

        assertThat(message("POST", "/product/update/deleteStatus?ids=" + id, null, t, 409)).contains("open order");

        String free = "Y" + UUID.randomUUID().toString().substring(0, 8);
        long freeId = call("POST", "/product/create", productParam(cat, free), t, 200).get("product").get("id").asLong();
        call("POST", "/product/update/deleteStatus?ids=" + freeId, null, t, 200);
        call("GET", "/product/" + freeId, null, t, 404);
    }

    @Test
    void theSkuListIsSyncedOnUpdate() throws Exception {
        String t = superAdmin();
        String sn = "S" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> param = productParam(categoryId(t), sn);
        long id = call("POST", "/product/create", param, t, 200).get("product").get("id").asLong();

        param.put("skus", List.of(
                Map.of("skuCode", sn + "-STD", "label", "Standard", "units", 1, "price", 7.00),
                Map.of("skuCode", sn + "-X2", "label", "Pack of 2", "units", 2, "price", 13.00)));
        JsonNode updated = call("POST", "/product/update/" + id, param, t, 200);

        assertThat(updated.get("skus")).hasSize(2);
        assertThat(updated.get("skus").get(0).get("price").asDouble()).isEqualTo(7.0);
    }

    // ------------------------------------------------------------------------- brands & references

    @Test
    void brandsFollowTheSamplingToStockingWorkflow() throws Exception {
        String t = superAdmin();
        JsonNode brands = call("GET", "/brand/list?keyword=Haigh&pageSize=5", null, t, 200);
        long id = brands.get("list").get(0).get("id").asLong();
        assertThat(brands.get("list").get(0).get("stockingStatus").asText()).isEqualTo("CANDIDATE");

        assertThat(message("POST", "/brand/update/stocking/" + id, Map.of("status", "STOCKED"), t, 409)).contains("cannot become STOCKED");
        call("POST", "/brand/update/stocking/" + id, Map.of("status", "SAMPLING"), t, 200);
        assertThat(message("POST", "/brand/update/stocking/" + id, Map.of("status", "AUTHORIZED"), t, 400)).contains("authorized supplier");
        call("POST", "/brand/update/stocking/" + id, Map.of("status", "AUTHORIZED", "authorizedSupplier", "Haigh's Australia Pty"), t, 200);
        assertThat(message("POST", "/brand/update/stocking/" + id, Map.of("status", "STOCKED"), t, 400)).contains("landed cost");
        JsonNode stocked = call("POST", "/brand/update/stocking/" + id, Map.of("status", "STOCKED", "landedCostNote", "Landed 3.20 per bar, MOQ 24"), t, 200);

        assertThat(stocked.get("stockingStatus").asText()).isEqualTo("STOCKED");
        assertThat(call("GET", "/brand/stocking/summary", null, t, 200).get("byStatus").get("STOCKED").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void referenceListsCannotLoseEntriesThatProductsUse() throws Exception {
        String t = superAdmin();
        long peanutUsedByP06 = allergens.findByCode("PEANUT").orElseThrow().getId();
        assertThat(message("POST", "/allergen/delete/" + peanutUsedByP06, null, t, 409)).contains("used by products");

        long raspberry = flavors.findByCode("V10").orElseThrow().getId();
        assertThat(message("POST", "/flavor/delete/" + raspberry, null, t, 409)).contains("used by products");

        JsonNode created = call("POST", "/flavor/create", Map.of("code", "V" + UUID.randomUUID().toString().substring(0, 6), "name", "Test flavor", "priority", "TEST"), t, 200);
        call("POST", "/flavor/delete/" + created.get("id").asLong(), null, t, 200);
    }

    @Test
    void categoriesAreAtMostTwoLevelsDeepAndNonEmptyOnesCannotBeDeleted() throws Exception {
        String t = superAdmin();
        long bars = categoryId(t);

        message("POST", "/productCategory/create", Map.of("name", "Too deep", "parentId", bars), t, 400);
        assertThat(message("POST", "/productCategory/delete/" + bars, null, t, 409)).contains("still has products");
    }

    @Test
    void theCatalogOfTheGuideIsVisibleToStaff() throws Exception {
        String t = superAdmin();
        assertThat(call("GET", "/chocolateType/list", null, t, 200)).hasSize(28);
        assertThat(call("GET", "/flavor/list", null, t, 200)).hasSize(50);
        assertThat(call("GET", "/brand/list?pageSize=100", null, t, 200).get("total").asInt()).isEqualTo(199);
        assertThat(call("GET", "/pairing/list?pageSize=100", null, t, 200).get("total").asInt()).isEqualTo(80);
        assertThat(call("GET", "/serving/list?pageSize=100", null, t, 200).get("total").asInt()).isEqualTo(45);
        assertThat(call("GET", "/allergen/list", null, t, 200)).hasSize(9);
    }
}
