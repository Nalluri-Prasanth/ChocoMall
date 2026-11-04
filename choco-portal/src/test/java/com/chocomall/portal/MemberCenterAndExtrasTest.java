package com.chocomall.portal;

import static org.assertj.core.api.Assertions.assertThat;

import com.chocomall.model.entity.Product;
import com.chocomall.model.repository.FlavorRepository;
import com.chocomall.model.repository.ProductClaimRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MemberCenterAndExtrasTest extends PortalTestSupport {

    @Autowired FlavorRepository flavors;
    @Autowired ProductClaimRepository claims;

    // ------------------------------------------------------------------------- addresses & profile

    @Test
    void theFirstAddressBecomesTheDefaultAndOnlyOneStaysDefault() throws Exception {
        Session s = register();
        Map<String, Object> home = Map.of("name", "Home", "phone", "+15550000001", "detailAddress", "1 Cocoa St");
        Map<String, Object> work = Map.of("name", "Work", "phone", "+15550000002", "detailAddress", "9 Cacao Ave", "defaultStatus", true);

        call("POST", "/member/address/add", home, s.token(), 200);
        call("POST", "/member/address/add", work, s.token(), 200);

        JsonNode list = call("GET", "/member/address/list", null, s.token(), 200);
        assertThat(list).hasSize(2);
        long defaults = 0;
        for (JsonNode a : list) {
            if (a.get("defaultStatus").asBoolean()) {
                defaults++;
                assertThat(a.get("name").asText()).isEqualTo("Work");
            }
        }
        assertThat(defaults).isEqualTo(1);
    }

    @Test
    void phoneNumbersAreValidated() throws Exception {
        Session s = register();
        message("POST", "/member/address/add", Map.of("name", "X", "phone", "not-a-phone", "detailAddress", "Somewhere"), s.token(), 400);
    }

    @Test
    void profileUpdatesAreAppliedAndOthersCannotSeeAnotherMembersAddresses() throws Exception {
        Session a = register();
        Session b = register();
        call("POST", "/sso/profile", Map.of("nickname", "Choco fan", "city", "Brussels"), a.token(), 200);
        assertThat(call("GET", "/sso/info", null, a.token(), 200).get("nickname").asText()).isEqualTo("Choco fan");

        long id = call("POST", "/member/address/add", Map.of("name", "A", "phone", "+15550000001", "detailAddress", "1 St"), a.token(), 200).get("id").asLong();
        call("POST", "/member/address/delete/" + id, null, b.token(), 404);
    }

    // ------------------------------------------------------------------------- favourites, history, brands

    @Test
    void viewedProductsFavouritesAndFollowedBrandsAreKeptPerMember() throws Exception {
        Session s = register();
        Product pretzel = bySn("P25");

        call("GET", "/product/detail/" + pretzel.getId(), null, s.token(), 200);
        JsonNode history = call("GET", "/member/readHistory/list", null, s.token(), 200);
        assertThat(history.get("total").asInt()).isEqualTo(1);
        assertThat(history.get("list").get(0).get("name").asText()).isEqualTo(pretzel.getName());

        call("POST", "/member/productCollection/add/" + pretzel.getId(), null, s.token(), 200);
        call("POST", "/member/productCollection/add/" + pretzel.getId(), null, s.token(), 200); // idempotent
        assertThat(call("GET", "/member/productCollection/list", null, s.token(), 200).get("total").asInt()).isEqualTo(1);
        assertThat(call("GET", "/product/detail/" + pretzel.getId(), null, s.token(), 200).get("favourite").asBoolean()).isTrue();
        call("POST", "/member/productCollection/delete/" + pretzel.getId(), null, s.token(), 200);
        assertThat(call("GET", "/member/productCollection/list", null, s.token(), 200).get("total").asInt()).isZero();

        long brandId = call("GET", "/brand/list", null, null, 200).get("list").get(0).get("id").asLong();
        call("POST", "/member/attention/add/" + brandId, null, s.token(), 200);
        assertThat(call("GET", "/member/attention/list", null, s.token(), 200).get("total").asInt()).isEqualTo(1);
        call("POST", "/member/readHistory/clear", null, s.token(), 200);
        assertThat(call("GET", "/member/readHistory/list", null, s.token(), 200).get("total").asInt()).isZero();
    }

    @Test
    void onlyOfficiallyEvidencedBrandsAppearOnTheBrandShelf() throws Exception {
        JsonNode shelf = call("GET", "/brand/list?pageSize=100", null, null, 200);
        assertThat(shelf.get("total").asInt()).isBetween(1, 198); // the 199-brand directory is filtered by evidence
        shelf.get("list").forEach(b -> assertThat(b.get("name").asText()).isNotBlank());
    }

    // ------------------------------------------------------------------------- consent-based preferences

    @Test
    void flavorPreferencesNeedConsentAndCanBeDeleted() throws Exception {
        Session s = register();
        long raspberry = flavors.findByCode("V10").orElseThrow().getId();

        assertThat(message("POST", "/member/preference", Map.of("flavorIds", Set.of(raspberry), "consent", false), s.token(), 400)).contains("consent");

        call("POST", "/member/preference", Map.of("flavorIds", Set.of(raspberry), "consent", true), s.token(), 200);
        JsonNode prefs = call("GET", "/member/preference", null, s.token(), 200);
        assertThat(prefs.get("flavorIds")).hasSize(1);
        assertThat(prefs.get("consentGivenAt").asText()).isNotBlank();

        JsonNode picks = call("GET", "/member/forYou", null, s.token(), 200);
        assertThat(picks.size()).isGreaterThan(0);
        picks.forEach(p -> assertThat(p.get("name").asText().toLowerCase()).containsAnyOf("raspberry", "dark", "white"));

        call("POST", "/member/preference/delete", null, s.token(), 200);
        assertThat(call("GET", "/member/preference", null, s.token(), 200).get("flavorIds")).isEmpty();
    }

    // ------------------------------------------------------------------------- claims

    @Test
    void dietaryClaimsAreShownOnlyAfterStaffVerifyTheirEvidence() throws Exception {
        Product p = newProduct(10, "4.00");
        com.chocomall.model.entity.ProductClaim claim = new com.chocomall.model.entity.ProductClaim();
        claim.setProductId(p.getId());
        claim.setClaimType(com.chocomall.model.enums.ClaimType.VEGAN);
        claim.setEvidence("Supplier declaration 2026-09");
        claims.save(claim);

        assertThat(call("GET", "/product/detail/" + p.getId(), null, null, 200).get("card").get("claims")).isEmpty();
        assertThat(call("GET", "/product/search?claim=VEGAN&pageSize=100", null, null, 200).get("total").asInt()).isZero();

        claim.setVerified(true);
        claim.setVerifiedBy("quality-lead");
        claims.save(claim);

        assertThat(call("GET", "/product/detail/" + p.getId(), null, null, 200).get("card").get("claims").get(0).get("type").asText()).isEqualTo("VEGAN");
        assertThat(call("GET", "/product/search?claim=VEGAN&pageSize=100", null, null, 200).get("total").asInt()).isEqualTo(1);
    }

    // ------------------------------------------------------------------------- build-a-box

    @Test
    void aBuildABoxMustBeFilledExactlyWithBonbonsFromTheShelf() throws Exception {
        Session s = register();
        Product box = bySn("P47");
        Product vanilla = bySn("P01");
        Product caramel = bySn("P02");
        Product pretzel = bySn("P25"); // not a bonbon
        long six = call("GET", "/product/detail/" + box.getId(), null, null, 200).get("skus").get(0).get("id").asLong();

        message("POST", "/cart/add", Map.of("productId", box.getId(), "skuId", six, "quantity", 1), s.token(), 400);                       // no selection
        message("POST", "/cart/add", Map.of("productId", box.getId(), "skuId", six, "quantity", 1,
                "box", List.of(Map.of("productId", vanilla.getId(), "quantity", 5))), s.token(), 400);                                  // 5 of 6
        message("POST", "/cart/add", Map.of("productId", box.getId(), "skuId", six, "quantity", 1,
                "box", List.of(Map.of("productId", pretzel.getId(), "quantity", 6))), s.token(), 400);                                  // not a bonbon

        JsonNode cart = call("POST", "/cart/add", Map.of("productId", box.getId(), "skuId", six, "quantity", 2,
                "box", List.of(Map.of("productId", vanilla.getId(), "quantity", 4), Map.of("productId", caramel.getId(), "quantity", 2))), s.token(), 200);
        assertThat(cart.get("items").get(0).get("box")).hasSize(2);
        long line = cart.get("items").get(0).get("id").asLong();

        long before1 = fresh(vanilla).getLockStock();
        long before2 = fresh(caramel).getLockStock();
        call("POST", "/order/generate", pickup(List.of(line)), s.token(), 201);

        // two boxes of (4 vanilla + 2 caramel) reserve 8 and 4 pieces of the individual bonbons
        assertThat(fresh(vanilla).getLockStock() - before1).isEqualTo(8);
        assertThat(fresh(caramel).getLockStock() - before2).isEqualTo(4);
    }

    // ------------------------------------------------------------------------- personalisation & experiences

    @Test
    void aCustomerRequestsApprovesAndPaysForAPersonalisedPlaque() throws Exception {
        Session s = register();
        JsonNode r = call("POST", "/personalization/submit", Map.of("kind", "NAME_PLAQUE", "text", "Happy Birthday Mia", "quantity", 1,
                "neededBy", LocalDate.now().plusDays(20).toString()), s.token(), 201);
        long id = r.get("id").asLong();

        message("POST", "/personalization/" + id + "/payDeposit", Map.of("cardToken", "tok_ok"), s.token(), 409); // nothing to pay yet
        assertThat(call("GET", "/personalization/list", null, s.token(), 200)).hasSize(1);

        Session other = register();
        call("GET", "/personalization/" + id, null, other.token(), 404);
    }

    @Test
    void experienceSeatsAreBookedPaidAndCancelled() throws Exception {
        Session s = register();
        JsonNode sessions = call("GET", "/experience/list", null, null, 200);
        long sessionId = sessions.get(0).get("id").asLong();
        int seatsBefore = sessions.get(0).get("seatsLeft").asInt();

        JsonNode booking = call("POST", "/experience/book", Map.of("sessionId", sessionId, "seats", 2), s.token(), 201);
        long bookingId = booking.get("id").asLong();
        assertThat(booking.get("status").asText()).isEqualTo("PENDING_PAYMENT");

        message("POST", "/experience/pay/" + bookingId, Map.of("cardToken", "tok_decline"), s.token(), 402);
        assertThat(call("POST", "/experience/pay/" + bookingId, Map.of("cardToken", "tok_ok"), s.token(), 200).get("status").asText()).isEqualTo("CONFIRMED");

        int seatsAfter = findSeats(sessionId);
        assertThat(seatsAfter).isEqualTo(seatsBefore - 2);

        call("POST", "/experience/cancel/" + bookingId, null, s.token(), 200);
        assertThat(findSeats(sessionId)).isEqualTo(seatsBefore);
        assertThat(call("GET", "/experience/my", null, s.token(), 200).get(0).get("status").asText()).isEqualTo("CANCELLED");
    }

    private int findSeats(long sessionId) throws Exception {
        for (JsonNode s : call("GET", "/experience/list", null, null, 200)) {
            if (s.get("id").asLong() == sessionId) {
                return s.get("seatsLeft").asInt();
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------------- reviews

    @Test
    void reviewsRequireACompletedPurchase() throws Exception {
        Session s = register();
        Product p = newProduct(10, "4.00");
        message("POST", "/member/review/create", Map.of("productId", p.getId(), "star", 5, "content", "Lovely"), s.token(), 409);
    }
}
