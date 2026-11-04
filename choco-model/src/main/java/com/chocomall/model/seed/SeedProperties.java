package com.chocomall.model.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Controls which data is loaded on start-up.
 *
 * @param referenceData  catalogs taken from the shop guide (chocolate types, flavors, brands, guides, pairings...)
 * @param demoShop       a ready-to-browse shop: products, SKUs, stock, coupons, content and demo accounts
 * @param adminUsername  bootstrap super-admin created when no admin exists and a password is configured
 * @param adminPassword  password for the bootstrap admin; leave empty to skip the bootstrap
 * @param demoPassword   password for the demo accounts (only used when demoShop is enabled)
 */
@ConfigurationProperties(prefix = "chocomall.seed")
public record SeedProperties(
        boolean referenceData,
        boolean demoShop,
        String adminUsername,
        String adminPassword,
        String demoPassword) {

    public SeedProperties {
        if (adminUsername == null || adminUsername.isBlank()) {
            adminUsername = "admin";
        }
    }
}
