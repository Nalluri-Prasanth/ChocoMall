package com.chocomall.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chocomall.model.entity.Product;
import com.chocomall.portal.service.ProductActivityService;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ProductActivityTest extends PortalTestSupport {
    @Autowired ProductActivityService activity;
    @Autowired JdbcTemplate jdbc;

    @Test
    void deduplicatesBrowserViewsAndDoesNotCountReadOnlyRequests() throws Exception {
        Product p = newProduct(20, "2.50");
        assertThat(activity.snapshot(p.getId()).viewersToday()).isZero();
        Cookie visitor = new Cookie("choco_visitor", "0123456789abcdef0123456789abcdef");
        for (int i = 0; i < 3; i++) mvc.perform(post("/product/" + p.getId() + "/activity/view").cookie(visitor)).andExpect(status().isOk());
        assertThat(activity.snapshot(p.getId()).viewersToday()).isEqualTo(1);
        activity.recordView(p.getId(), "second-visitor");
        assertThat(activity.snapshot(p.getId()).viewersToday()).isEqualTo(2);
        LocalDate yesterday = LocalDate.now(ZoneId.of("America/Chicago")).minusDays(1);
        jdbc.update("insert into pms_product_daily_view values (?, ?, ?)", p.getId(), yesterday, "oldhash");
        assertThat(activity.snapshot(p.getId()).viewersToday()).isEqualTo(2);
    }

    @Test
    void setsPrivateCookieAndHidesUnpublishedProducts() throws Exception {
        Product p = newProduct(20, "2.50");
        var response = mvc.perform(post("/product/" + p.getId() + "/activity/view")).andExpect(status().isOk()).andReturn().getResponse();
        assertThat(response.getHeader("Set-Cookie")).contains("HttpOnly", "SameSite=Lax", "Path=/product");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        p.setPublished(false); products.save(p);
        call("GET", "/product/" + p.getId() + "/activity", null, null, 404);
        call("POST", "/product/" + p.getId() + "/activity/view", Map.of(), null, 404);
    }

    @Test
    void countsDistinctBasketAccountsNotQuantities() throws Exception {
        Product p = newProduct(20, "2.50");
        Session a = register(), b = register();
        addToCart(a.token(), p, 3);
        addToCart(b.token(), p, 2);
        assertThat(activity.snapshot(p.getId()).shoppersWithInCart()).isEqualTo(2);
        assertThat(activity.snapshot(p.getId()).buyersThisMonth()).isZero();
    }
}
