package com.chocomall.core.store;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Redis-backed store shared by all nodes. Keys are namespaced to avoid clashing with other data. */
@RequiredArgsConstructor
public class RedisExpiringStore implements ExpiringStore {

    private static final String PREFIX = "chocomall:";

    private final StringRedisTemplate redis;

    @Override
    public void put(String key, String value, Duration ttl) {
        redis.opsForValue().set(PREFIX + key, value, ttl);
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(redis.opsForValue().get(PREFIX + key));
    }

    @Override
    public void delete(String key) {
        redis.delete(PREFIX + key);
    }

    @Override
    public long increment(String key, Duration ttl) {
        Long value = redis.opsForValue().increment(PREFIX + key);
        if (value != null && value == 1L) {
            redis.expire(PREFIX + key, ttl);
        }
        return value == null ? 0 : value;
    }
}
