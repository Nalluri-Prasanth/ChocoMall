package com.chocomall.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chocomall.security.JwtTokenService;
import com.chocomall.security.SecurityProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

class AuthAndCatalogTest extends PortalTestSupport {

    @Value("${secure.jwt.secret}") String secret;
    @org.springframework.beans.factory.annotation.Autowired com.chocomall.model.repository.AllergenRepository allergens;

    // ------------------------------------------------------------------------- authentication

    @Test
    void aCustomerRegistersWithAPhoneCodeAndCanSignInAgain() throws Exception {
        Session s = register();

        JsonNode login = call("POST", "/sso/login", Map.of("username", s.username(), "password", "Passw0rd!123"), null, 200);
        assertThat(login.get("token").asText()).isNotBlank();
        assertThat(login.get("tokenHead").asText()).isEqualTo("Bearer");

        JsonNode me = call("GET", "/sso/info", null, s.token(), 200);
        assertThat(me.get("levelName").asText()).isEqualTo("Bronze");
        assertThat(me.get("points").asInt()).isZero();
        assertThat(me.get("nextLevelName").asText()).isEqualTo("Silver");
    }

    @Test
    void aWrongOrReusedVerificationCodeIsRejected() throws Exception {
        String phone = "+15559990001";
        String code = call("GET", "/sso/getAuthCode?telephone=%2B15559990001", null, null, 200).asText();
        Map<String, Object> body = Map.of("username", "wrongcode1", "password", "Passw0rd!123", "telephone", phone, "authCode", "000000");
        assertThat(message("POST", "/sso/register", body, null, 400)).contains("verification code");

        Map<String, Object> good = Map.of("username", "wrongcode1", "password", "Passw0rd!123", "telephone", phone, "authCode", code);
        call("POST", "/sso/register", good, null, 201);
        // the code was consumed; using it again (even for another username) fails
        Map<String, Object> reuse = Map.of("username", "wrongcode2", "password", "Passw0rd!123", "telephone", phone, "authCode", code);
        message("POST", "/sso/register", reuse, null, 400);
    }

    @Test
    void aCodeCanOnlyBeRequestedOncePerMinutePerPhone() throws Exception {
        call("GET", "/sso/getAuthCode?telephone=%2B15559990002", null, null, 200);
        assertThat(message("GET", "/sso/getAuthCode?telephone=%2B15559990002", null, null, 429)).contains("wait a minute");
    }

    @Test
    void registrationValidatesInputAndRejectsDuplicates() throws Exception {
        Session existing = register();
        String code = call("GET", "/sso/getAuthCode?telephone=%2B15559990003", null, null, 200).asText();

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/sso/register")
                        .contentType("application/json")
                        .content(json.writeValueAsString(Map.of("username", "x", "password", "short", "telephone", "abc", "authCode", code))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("username")));

        String code2 = call("GET", "/sso/getAuthCode?telephone=%2B15559990004", null, null, 200).asText();
        assertThat(message("POST", "/sso/register", Map.of("username", existing.username(), "password", "Passw0rd!123",
                "telephone", "+15559990004", "authCode", code2), null, 409)).contains("already taken");
    }

    @Test
    void repeatedWrongPasswordsLockTheAccountTemporarily() throws Exception {
        Session s = register();
        Map<String, Object> wrong = Map.of("username", s.username(), "password", "nope-nope-nope");
        call("POST", "/sso/login", wrong, null, 401);
        call("POST", "/sso/login", wrong, null, 401);
        call("POST", "/sso/login", wrong, null, 401);

        assertThat(message("POST", "/sso/login", Map.of("username", s.username(), "password", "Passw0rd!123"), null, 429))
                .contains("Too many failed sign-in attempts");
    }

    @Test
    void protectedEndpointsNeedAMemberToken() throws Exception {
        call("GET", "/cart/list", null, null, 401);
        call("GET", "/order/list", null, null, 401);
        call("GET", "/sso/info", null, "garbage.token.value", 401);
    }

    @Test
    void aTokenIssuedForTheBackOfficeIsNotAcceptedByTheStorefront() throws Exception {
        Session s = register();
        SecurityProperties.Jwt adminJwt = new SecurityProperties.Jwt(secret, 3600, "admin", null, null);
        String adminToken = new JwtTokenService(new SecurityProperties(List.of(), List.of(), true, List.of(), adminJwt)).generate(s.username());

        call("GET", "/sso/info", null, adminToken, 401);
    }

    // ------------------------------------------------------------------------- catalog

    @Test
    void browsingIsPublicAndOnlyShowsPublishedProducts() throws Exception {
        JsonNode all = call("GET", "/product/search?pageSize=100", null, null, 200);
        List<String> sns = new ArrayList<>();
        all.get("list").forEach(p -> sns.add(p.get("productSn").asText()));

        assertThat(sns).contains("P01", "P25", "P47").doesNotContain("P43", "P44"); // gelato and sorbet are SPECIALIST drafts
        mvc.perform(get("/product/detail/" + bySn("P43").getId())).andExpect(status().isNotFound());
    }

    @Test
    void searchFiltersByKeywordPriceKindAndSorts() throws Exception {
        JsonNode pretzel = call("GET", "/product/search?keyword=pretzel", null, null, 200);
        assertThat(pretzel.get("total").asInt()).isEqualTo(1);
        assertThat(pretzel.get("list").get(0).get("allergens").toString()).contains("Wheat");

        JsonNode cheapFirst = call("GET", "/product/search?sort=3&pageSize=50", null, null, 200);
        double previous = -1;
        for (JsonNode p : cheapFirst.get("list")) {
            assertThat(p.get("price").asDouble()).isGreaterThanOrEqualTo(previous);
            previous = p.get("price").asDouble();
        }

        JsonNode drinks = call("GET", "/product/search?kind=MADE_TO_ORDER", null, null, 200);
        drinks.get("list").forEach(p -> assertThat(p.get("kind").asText()).isEqualTo("MADE_TO_ORDER"));

        JsonNode midPriced = call("GET", "/product/search?minPrice=5&maxPrice=6&pageSize=50", null, null, 200);
        midPriced.get("list").forEach(p -> assertThat(p.get("price").asDouble()).isBetween(5.0, 6.0));
    }

    @Test
    void shoppersCanExcludeAllergens() throws Exception {
        long peanut = call("GET", "/product/search?keyword=peanut", null, null, 200).get("list").size();
        assertThat(peanut).isGreaterThan(0);
        JsonNode all = call("GET", "/product/search?pageSize=100", null, null, 200);
        long total = all.get("total").asLong();

        // allergen ids come from the reference list; find "Peanut"
        long peanutAllergenId = jsonAllergenId("Peanut");
        JsonNode nutFree = call("GET", "/product/search?pageSize=100&excludeAllergens=" + peanutAllergenId, null, null, 200);

        assertThat(nutFree.get("total").asLong()).isLessThan(total);
        nutFree.get("list").forEach(p -> assertThat(p.get("allergens").toString()).doesNotContain("Peanut"));
    }

    private long jsonAllergenId(String name) {
        return allergens.findByCode(name.toUpperCase()).orElseThrow().getId();
    }

    @Test
    void theProductPageShowsSkusAllergensAndLaunchAwareDetails() throws Exception {
        JsonNode detail = call("GET", "/product/detail/" + bySn("P01").getId(), null, null, 200);

        assertThat(detail.get("skus")).hasSize(3);
        assertThat(detail.get("skus").get(1).get("label").asText()).isEqualTo("Box of 6");
        assertThat(detail.get("skus").get(1).get("price").asDouble()).isLessThan(6 * 2.20); // box discount
        assertThat(detail.get("card").get("inStock").asBoolean()).isTrue();

        JsonNode strawberries = call("GET", "/product/detail/" + bySn("P29").getId(), null, null, 200);
        assertThat(strawberries.get("card").get("preorderOnly").asBoolean()).isTrue();
        assertThat(strawberries.get("card").get("requiresColdChain").asBoolean()).isTrue();
        assertThat(strawberries.get("preorderWindows").size()).isGreaterThan(0);
    }

    @Test
    void experiencesListUpcomingSessionsWithSeatsLeft() throws Exception {
        JsonNode sessions = call("GET", "/experience/list", null, null, 200);
        assertThat(sessions.size()).isGreaterThan(0);
        assertThat(sessions.get(0).get("seatsLeft").asInt()).isGreaterThan(0);
    }

    @Test
    void homePageGuidesAndShopInfoArePublic() throws Exception {
        JsonNode home = call("GET", "/home/content", null, null, 200);
        assertThat(home.get("categories").size()).isGreaterThan(3);
        assertThat(home.get("recommendProducts").size()).isGreaterThan(0);
        assertThat(home.get("preorderWindows").size()).isGreaterThan(0);

        JsonNode pairings = call("GET", "/guide/pairings?pageSize=100", null, null, 200);
        pairings.get("list").forEach(p -> assertThat(p.get("priority").asText()).isIn("CORE", "EXPAND")); // internal tiers stay hidden

        JsonNode info = call("GET", "/shop/info", null, null, 200);
        assertThat(info.get("pickupLeadMinutes").asInt()).isEqualTo(30);
        assertThat(call("GET", "/help/centre", null, null, 200).size()).isGreaterThan(3);
    }
}
