package com.chocomall.portal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ProductCategory;
import com.chocomall.model.entity.ProductSku;
import com.chocomall.model.enums.Priority;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.model.repository.ProductCategoryRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ProductSkuRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Shared helpers for storefront integration tests: real HTTP layer, real security, real services, in-memory DB. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class PortalTestSupport {

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected ProductRepository products;
    @Autowired protected ProductSkuRepository skus;
    @Autowired protected ProductCategoryRepository categories;

    protected record Session(String token, long memberId, String username) {
    }

    /** Registers a new customer through the public sign-up flow and returns the token. */
    protected Session register() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String username = "cust" + suffix.substring(0, 10);
        String phone = "+1" + (1_000_000_000L + java.util.concurrent.ThreadLocalRandom.current().nextLong(8_000_000_000L));
        JsonNode code = call("GET", "/sso/getAuthCode?telephone=" + java.net.URLEncoder.encode(phone, "UTF-8"), null, null, 200);
        JsonNode token = call("POST", "/sso/register", Map.of("username", username, "password", "Passw0rd!123", "telephone", phone,
                "authCode", code.asText()), null, 201);
        String jwt = token.get("token").asText();
        long id = call("GET", "/sso/info", null, jwt, 200).get("id").asLong();
        return new Session(jwt, id, username);
    }

    /** Performs a request and returns the {@code data} node after asserting the HTTP status. */
    protected JsonNode call(String method, String path, Object body, String token, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder b = method.equals("GET") ? get(java.net.URI.create(path)) : post(java.net.URI.create(path));
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        String content = mvc.perform(b).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
        JsonNode root = content.isBlank() ? json.createObjectNode() : json.readTree(content);
        JsonNode data = root.get("data");
        return data == null ? root : data;
    }

    protected String message(String method, String path, Object body, String token, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder b = method.equals("GET") ? get(java.net.URI.create(path)) : post(java.net.URI.create(path));
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        String content = mvc.perform(b).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
        return json.readTree(content).path("message").asText();
    }

    /** A published, stocked product with a single SKU, created directly in the database. */
    protected Product newProduct(int stock, String price) {
        return newProduct(stock, price, ProductKind.MADE_IN_SHOP, false, false);
    }

    protected Product newProduct(int stock, String price, ProductKind kind, boolean preorderOnly, boolean coldChain) {
        ProductCategory category = categories.findByNameAndParentId("Portal test", 0L).orElseGet(() -> {
            ProductCategory c = new ProductCategory();
            c.setName("Portal test");
            c.setParentId(0L);
            return categories.save(c);
        });
        Product p = new Product();
        p.setCategoryId(category.getId());
        p.setProductSn("Z" + UUID.randomUUID().toString().substring(0, 12));
        p.setName("Test item " + p.getProductSn());
        p.setPrice(new BigDecimal(price));
        p.setStock(stock);
        p.setKind(kind);
        p.setLaunchTier(Priority.CORE);
        p.setPublished(true);
        p.setAllergensReviewed(true);
        p.setPreorderOnly(preorderOnly);
        p.setRequiresColdChain(coldChain);
        Product saved = products.save(p);
        ProductSku sku = new ProductSku();
        sku.setProductId(saved.getId());
        sku.setSkuCode(saved.getProductSn() + "-1");
        sku.setLabel("Single");
        sku.setUnits(1);
        sku.setPrice(saved.getPrice());
        skus.save(sku);
        return saved;
    }

    protected long skuId(Product p) {
        return skus.findByProductIdOrderBySortAscIdAsc(p.getId()).get(0).getId();
    }

    protected Product bySn(String sn) {
        return products.findByProductSn(sn).orElseThrow();
    }

    protected long addToCart(String token, Product p, int quantity) throws Exception {
        JsonNode cart = call("POST", "/cart/add", Map.of("productId", p.getId(), "skuId", skuId(p), "quantity", quantity), token, 200);
        for (JsonNode item : cart.get("items")) {
            if (item.get("productId").asLong() == p.getId()) {
                return item.get("id").asLong();
            }
        }
        throw new AssertionError("item missing from basket");
    }

    protected Map<String, Object> pickup(List<Long> cartIds) {
        return new java.util.HashMap<>(Map.of("cartItemIds", cartIds, "fulfillmentType", "PICKUP"));
    }

    protected Product fresh(Product p) {
        return products.findById(p.getId()).orElseThrow();
    }
}
