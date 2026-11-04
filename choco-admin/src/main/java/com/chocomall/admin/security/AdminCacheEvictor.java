package com.chocomall.admin.security;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/** Clears cached staff permissions after any change to accounts, roles or resources. */
@Component
public class AdminCacheEvictor {

    private final CacheManager cacheManager;

    public AdminCacheEvictor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictAll() {
        var cache = cacheManager.getCache(AdminUserDetailsService.CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    public void evict(String username) {
        var cache = cacheManager.getCache(AdminUserDetailsService.CACHE);
        if (cache != null) {
            cache.evict(username);
        }
    }
}
