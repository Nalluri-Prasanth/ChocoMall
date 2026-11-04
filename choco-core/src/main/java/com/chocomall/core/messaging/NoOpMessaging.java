package com.chocomall.core.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Default when no broker is configured ({@code chocomall.messaging.type=none}). */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "chocomall.messaging.type", havingValue = "none", matchIfMissing = true)
public class NoOpMessaging {

    @Bean
    OrderTimeoutPublisher noOpOrderTimeoutPublisher() {
        return (orderId, delayMillis) -> log.debug("No broker: order {} will be cancelled by the maintenance job", orderId);
    }

    @Bean
    SearchSyncPublisher noOpSearchSyncPublisher() {
        return new SearchSyncPublisher() {
            @Override
            public void productChanged(Long productId) {
                log.debug("No broker: product {} change not published", productId);
            }

            @Override
            public void productRemoved(Long productId) {
                log.debug("No broker: product {} removal not published", productId);
            }
        };
    }
}
