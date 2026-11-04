package com.chocomall.portal.service;

import com.chocomall.common.exception.Asserts;
import com.chocomall.model.entity.Product;
import com.chocomall.model.repository.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Aggregate activity only: no names, IP addresses, user agents or visitor tokens are returned. */
@Service
public class ProductActivityService {
    private final JdbcTemplate jdbc;
    private final ProductRepository products;
    private final ZoneId zone;

    public ProductActivityService(JdbcTemplate jdbc, ProductRepository products,
                                  @Value("${chocomall.activity.zone:America/Chicago}") String zone) {
        this.jdbc = jdbc;
        this.products = products;
        this.zone = ZoneId.of(zone);
    }

    public record Activity(long viewersToday, long shoppersWithInCart, long buyersThisMonth,
                           LocalDate day, LocalDate monthStart, String timezone) {}

    public Activity recordView(Long productId, String visitor) {
        requireVisible(productId);
        LocalDate day = LocalDate.now(zone);
        String hash;
        try {
            hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((day + ":" + visitor).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        try {
            jdbc.update("insert into pms_product_daily_view (product_id, view_date, visitor_hash) values (?, ?, ?)", productId, day, hash);
        } catch (DuplicateKeyException ignored) { /* One browser counts once per product per day, including concurrent requests. */ }
        return counts(productId, day);
    }

    public Activity snapshot(Long productId) {
        requireVisible(productId);
        return counts(productId, LocalDate.now(zone));
    }

    private void requireVisible(Long id) {
        Asserts.found(products.findById(id).filter(Product::isPublished), "Product");
    }

    private Activity counts(Long id, LocalDate day) {
        LocalDate month = day.withDayOfMonth(1);
        long views = jdbc.queryForObject("select count(*) from pms_product_daily_view where product_id = ? and view_date = ?", Long.class, id, day);
        long carts = jdbc.queryForObject("select count(distinct member_id) from oms_cart_item where product_id = ? and quantity > 0", Long.class, id);
        long buyers = jdbc.queryForObject("""
                select count(distinct o.member_id) from oms_order_item i join oms_order o on o.id = i.order_id
                where i.product_id = ? and i.product_quantity > 0 and o.deleted = false
                and o.status in ('PREPARING', 'READY_FOR_PICKUP', 'SHIPPED', 'COMPLETED')
                and o.payment_time >= ? and o.payment_time < ?
                """, Long.class, id, month.atStartOfDay(), month.plusMonths(1).atStartOfDay());
        return new Activity(views, carts, buyers, day, month, zone.getId());
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "${chocomall.activity.zone:America/Chicago}")
    public void expireOldViews() {
        jdbc.update("delete from pms_product_daily_view where view_date < ?", LocalDate.now(zone).minusDays(31));
    }
}
