package com.chocomall.core.store;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Single-instance store for local development and tests. Use {@link RedisExpiringStore} when running several nodes. */
public class InMemoryExpiringStore implements ExpiringStore {

    private record Entry(String value, Instant expiresAt) {
        boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public void put(String key, String value, Duration ttl) {
        entries.put(key, new Entry(value, Instant.now().plus(ttl)));
    }

    @Override
    public Optional<String> get(String key) {
        Entry e = entries.get(key);
        if (e == null) {
            return Optional.empty();
        }
        if (e.expired()) {
            entries.remove(key, e);
            return Optional.empty();
        }
        return Optional.of(e.value());
    }

    @Override
    public void delete(String key) {
        entries.remove(key);
    }

    @Override
    public long increment(String key, Duration ttl) {
        Entry updated = entries.compute(key, (k, current) -> {
            if (current == null || current.expired()) {
                return new Entry("1", Instant.now().plus(ttl));
            }
            return new Entry(String.valueOf(Long.parseLong(current.value()) + 1), current.expiresAt());
        });
        return Long.parseLong(updated.value());
    }
}
