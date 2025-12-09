package com.delight.account.config;

import com.delight.account.cache.AccountCacheKeyManager;
import com.delight.account.cache.CacheKeyManager;
import com.delight.account.cache.UserCacheKeyManager;
import com.delight.account.cache.impl.VersionedCacheKeyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for CacheKeyManager beans.
 * Provides explicit bean definitions for AccountCacheKeyManager and UserCacheKeyManager
 * to ensure Spring can inject them properly, even though they're implemented by the same class.
 */
@Configuration
public class CacheKeyManagerConfig {

    @Bean
    @Primary
    public CacheKeyManager cacheKeyManager(VersionedCacheKeyManager versionedCacheKeyManager) {
        return versionedCacheKeyManager;
    }

    @Bean
    public AccountCacheKeyManager accountCacheKeyManager(VersionedCacheKeyManager versionedCacheKeyManager) {
        return versionedCacheKeyManager;
    }

    @Bean
    public UserCacheKeyManager userCacheKeyManager(VersionedCacheKeyManager versionedCacheKeyManager) {
        return versionedCacheKeyManager;
    }
}


