package com.chocomall.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chocomall.core.stock.StockService;
import com.chocomall.model.entity.Member;
import com.chocomall.model.entity.OrderItem;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.FulfillmentType;
import com.chocomall.model.repository.MemberRepository;
import com.chocomall.model.repository.OrderItemRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ShopOrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/** Shared helpers for back-office integration tests: real HTTP layer, real security and RBAC, in-memory database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AdminTestSupport {

    public static final String ADMIN_PASSWORD = "Admin@12345";
    public static final String DEMO_PASSWORD = "Demo@12345";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected ProductRepository products;
    @Autowired protected MemberRepository members;
    @Autowired protected ShopOrderRepository orders;
    @Autowired protected OrderItemRepository orderItems;
    @Autowired protected StockService stock;
    @Autowired protected TransactionTemplate tx;

    protected String superAdmin() throws Exception {
        return login("admin", ADMIN_PASSWORD);
    }

    protected String login(String username, String password) throws Exception {
        return call("POST", "/admin/login", java.util.Map.of("username", username, "password", password), null, 200).get("token").asText();
    }

    protected JsonNode call(String method, String path, Object body, String token, int expectedStatus) throws Exception {
        String content = perform(method, path, body, token, expectedStatus);
        JsonNode root = content.isBlank() ? json.createObjectNode() : json.readTree(content);
        JsonNode data = root.get("data");
        return data == null ? root : data;
    }

    protected String message(String method, String path, Object body, String token, int expectedStatus) throws Exception {
        String content = perform(method, path, body, token, expectedStatus);
        return content.isBlank() ? "" : json.readTree(content).path("message").asText();
    }

    private String perform(String method, String path, Object body, String token, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder b = method.equals("GET") ? get(java.net.URI.create(path)) : post(java.net.URI.create(path));
        if (body != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(b).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
    }

    protected Member newMember() {
        Member m = new Member();
        m.setUsername("m" + UUID.randomUUID().toString().substring(0, 12));
        m.setPassword("{noop}x");
        return members.save(m);
    }

    /** An unpaid pickup order with stock reserved exactly as checkout would do. */
    protected ShopOrder placeOrder(Member member, Product product, int quantity, String amount) {
        return tx.execute(s -> {
            ShopOrder o = new ShopOrder();
            o.setMemberId(member.getId());
            o.setMemberUsername(member.getUsername());
            o.setOrderSn("ORD" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
            o.setTotalAmount(new BigDecimal(amount));
            o.setPayAmount(new BigDecimal(amount));
            o.setFulfillmentType(FulfillmentType.PICKUP);
            ShopOrder saved = orders.saveAndFlush(o);
            OrderItem item = new OrderItem();
            item.setOrderId(saved.getId());
            item.setOrderSn(saved.getOrderSn());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductSn(product.getProductSn());
            item.setUnitsPerSku(1);
            item.setProductPrice(product.getPrice());
            item.setProductQuantity(quantity);
            item.setRealAmount(new BigDecimal(amount));
            orderItems.save(item);
            stock.lock(product, quantity, saved.getOrderSn());
            return saved;
        });
    }

    protected Product bySn(String sn) {
        return products.findByProductSn(sn).orElseThrow();
    }

    protected Product fresh(Product p) {
        return products.findById(p.getId()).orElseThrow();
    }
}
