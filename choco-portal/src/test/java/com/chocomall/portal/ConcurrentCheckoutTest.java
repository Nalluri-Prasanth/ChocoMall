package com.chocomall.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.chocomall.model.entity.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Many customers racing for the last units: the shop must never sell more than it has. */
class ConcurrentCheckoutTest extends PortalTestSupport {

    @Test
    void sixteenCustomersCompeteForFiveUnits() throws Exception {
        Product last = newProduct(5, "6.00");
        int buyers = 16;
        List<Session> sessions = new ArrayList<>();
        List<Long> lines = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            Session s = register();
            sessions.add(s);
            lines.add(addToCart(s.token(), last, 1));
        }

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            Session s = sessions.get(i);
            long line = lines.get(i);
            futures.add(pool.submit(() -> {
                go.await();
                int status = mvc.perform(post("/order/generate").contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + s.token())
                                .content(json.writeValueAsString(Map.of("cartItemIds", List.of(line), "fulfillmentType", "PICKUP"))))
                        .andReturn().getResponse().getStatus();
                if (status == 201) {
                    created.incrementAndGet();
                } else if (status == 409) {
                    refused.incrementAndGet();
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertThat(created.get()).isEqualTo(5);
        assertThat(refused.get()).isEqualTo(11);
        Product after = fresh(last);
        assertThat(after.getLockStock()).isEqualTo(5);
        assertThat(after.getStock() - after.getLockStock()).isZero();
    }
}
