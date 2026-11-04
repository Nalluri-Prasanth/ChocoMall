package com.chocomall.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param engine   {@code database} (default) or {@code elasticsearch}
 * @param adminKey shared secret required in the {@code X-Search-Admin-Key} header to rebuild or change the index;
 *                 when empty, the index endpoints are disabled
 */
@ConfigurationProperties(prefix = "search")
public record SearchProperties(String engine, String adminKey) {

    public SearchProperties {
        engine = engine == null || engine.isBlank() ? "database" : engine;
        adminKey = adminKey == null ? "" : adminKey;
    }
}
