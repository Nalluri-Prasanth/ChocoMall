package com.chocomall.core.store;

import java.time.Duration;
import java.util.Optional;

/** Small key/value store with expiry, used for verification codes and login throttling. */
public interface ExpiringStore {

    void put(String key, String value, Duration ttl);

    Optional<String> get(String key);

    void delete(String key);

    /** Atomically increments a counter; the expiry is set when the counter is created. Returns the new value. */
    long increment(String key, Duration ttl);
}
