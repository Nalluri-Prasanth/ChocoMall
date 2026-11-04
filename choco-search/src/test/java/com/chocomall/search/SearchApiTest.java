package com.chocomall.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chocomall.model.repository.AllergenRepository;
import com.chocomall.model.repository.FlavorRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.search.engine.DatabaseSearchEngine;
import com.chocomall.search.engine.SearchEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired SearchEngine engine;
    @Autowired AllergenRepository allergens;
    @Autowired FlavorRepository flavors;
    @Autowired ProductRepository products;

    private JsonNode fetch(String path) throws Exception {
        String body = mvc.perform(get(java.net.URI.create(path))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("data");
    }

    @Test
    void theDatabaseEngineIsTheDefault() {
        assertThat(engine).isInstanceOf(DatabaseSearchEngine.class);
        assertThat(engine.name()).isEqualTo("database");
    }

    @Test
    void keywordSearchOnlyReturnsPublishedProducts() throws Exception {
        JsonNode pretzel = fetch("/search/simple?keyword=pretzel");
        assertThat(pretzel.get("total").asInt()).isEqualTo(1);
        assertThat(pretzel.get("list").get(0).get("name").asText()).isEqualTo("Chocolate pretzels");
        assertThat(pretzel.get("list").get(0).get("allergens").toString()).contains("Wheat");

        // the gelato is a SPECIALIST item that is still in draft
        assertThat(fetch("/search/simple?keyword=gelato").get("total").asInt()).isZero();
        List<String> sns = new ArrayList<>();
        fetch("/search/simple?pageSize=100").get("list").forEach(h -> sns.add(h.get("productSn").asText()));
        assertThat(sns).contains("P25", "P47").doesNotContain("P43");
    }

    @Test
    void filtersCombineAndSortingWorks() throws Exception {
        JsonNode cheapest = fetch("/search?sort=3&pageSize=100");
        double last = -1;
        for (JsonNode h : cheapest.get("list")) {
            assertThat(h.get("price").asDouble()).isGreaterThanOrEqualTo(last);
            last = h.get("price").asDouble();
        }
        JsonNode drinks = fetch("/search?kind=MADE_TO_ORDER&maxPrice=5&pageSize=100");
        drinks.get("list").forEach(h -> {
            assertThat(h.get("kind").asText()).isEqualTo("MADE_TO_ORDER");
            assertThat(h.get("price").asDouble()).isLessThanOrEqualTo(5.0);
        });
        assertThat(drinks.get("total").asInt()).isGreaterThan(0);
    }

    @Test
    void allergenExclusionAndFlavorFiltersWork() throws Exception {
        long total = fetch("/search?pageSize=100").get("total").asLong();
        long peanut = allergens.findByCode("PEANUT").orElseThrow().getId();
        JsonNode safe = fetch("/search?pageSize=100&excludeAllergens=" + peanut);
        assertThat(safe.get("total").asLong()).isLessThan(total);
        safe.get("list").forEach(h -> assertThat(h.get("allergens").toString()).doesNotContain("Peanut"));

        long raspberry = flavors.findByCode("V10").orElseThrow().getId();
        JsonNode raspberryItems = fetch("/search?flavorId=" + raspberry);
        assertThat(raspberryItems.get("total").asInt()).isGreaterThan(0);
        raspberryItems.get("list").forEach(h -> assertThat(h.get("flavors").toString()).contains("Raspberry"));
    }

    @Test
    void relatedSearchSummarisesTheResults() throws Exception {
        JsonNode facets = fetch("/search/related?keyword=dark");
        assertThat(facets.get("categories").size()).isGreaterThan(0);
        assertThat(facets.get("kinds").size()).isGreaterThan(0);
        assertThat(facets.get("minPrice").asDouble()).isLessThanOrEqualTo(facets.get("maxPrice").asDouble());
    }

    @Test
    void recommendationsComeFromTheSameCategory() throws Exception {
        long vanilla = products.findByProductSn("P01").orElseThrow().getId();
        JsonNode similar = fetch("/search/recommend/" + vanilla + "?limit=4");
        assertThat(similar.size()).isBetween(1, 4);
        similar.forEach(h -> {
            assertThat(h.get("categoryName").asText()).isEqualTo("Bonbons");
            assertThat(h.get("id").asLong()).isNotEqualTo(vanilla);
        });
    }

    @Test
    void indexManagementNeedsTheAdminKey() throws Exception {
        mvc.perform(post("/search/index/importAll")).andExpect(status().isUnauthorized());
        mvc.perform(post("/search/index/importAll").header("X-Search-Admin-Key", "wrong")).andExpect(status().isUnauthorized());
        String body = mvc.perform(post("/search/index/importAll").header("X-Search-Admin-Key", "test-key"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(body).get("data").get("indexed").asInt()).isZero(); // nothing to rebuild for the database engine
    }
}
