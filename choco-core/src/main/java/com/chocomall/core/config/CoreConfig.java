package com.chocomall.core.config;

import com.chocomall.core.store.ExpiringStore;
import com.chocomall.core.store.InMemoryExpiringStore;
import com.chocomall.core.store.RedisExpiringStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wires infrastructure switches shared by the applications:
 * <ul>
 *   <li>{@code chocomall.store.type=memory|redis} selects the expiring key/value store</li>
 *   <li>{@code spring.cache.type=caffeine|redis} selects the cache provider</li>
 *   <li>{@code chocomall.messaging.type=none|rabbit} selects asynchronous messaging</li>
 * </ul>
 */
@Configuration
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({StorageProperties.class})
@ComponentScan("com.chocomall.core")
public class CoreConfig {

    @Bean
    @ConditionalOnProperty(name = "chocomall.store.type", havingValue = "redis")
    ExpiringStore redisStore(StringRedisTemplate redis) {
        return new RedisExpiringStore(redis);
    }

    @Bean
    @ConditionalOnProperty(name = "chocomall.store.type", havingValue = "memory", matchIfMissing = true)
    ExpiringStore memoryStore() {
        return new InMemoryExpiringStore();
    }
}
