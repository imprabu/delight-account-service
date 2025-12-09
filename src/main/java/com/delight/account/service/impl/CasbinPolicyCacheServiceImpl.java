package com.delight.account.service.impl;

import com.delight.account.service.CasbinPolicyCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis-based implementation of Casbin policy cache service.
 * Policies are stored per account with key pattern: casbin:policies:account:{accountId}
 * Uses Redis database 1 for Casbin policy cache.
 */
@Service
public class CasbinPolicyCacheServiceImpl implements CasbinPolicyCacheService {

    private static final Logger logger = LoggerFactory.getLogger(CasbinPolicyCacheServiceImpl.class);
    private static final String CACHE_KEY_PREFIX = "casbin:policies:account:";
    private static final String CACHE_PATTERN = "casbin:policies:account:*";
    private static final Duration CACHE_TTL = Duration.ofHours(24); // Cache for 24 hours

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public CasbinPolicyCacheServiceImpl(@Qualifier("casbinRedisTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        logger.info("CasbinPolicyCacheService initialized with Redis database 1 for Casbin policy cache");
    }

    private String getCacheKey(Long accountId) {
        return CACHE_KEY_PREFIX + accountId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getCachedPolicies(Long accountId) {
        try {
            String key = getCacheKey(accountId);
            Object cached = redisTemplate.opsForValue().get(key);
            
            if (cached == null) {
                logger.debug("No cached policies found for account: {}", accountId);
                return null;
            }
            
            if (cached instanceof List) {
                List<String> policies = (List<String>) cached;
                logger.debug("Retrieved {} cached policies for account: {}", policies.size(), accountId);
                return policies;
            } else {
                logger.warn("Unexpected cache value type for account {}: {}", accountId, cached.getClass());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error retrieving cached policies for account {}: {}", accountId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void cachePolicies(Long accountId, List<String> policies) {
        try {
            String key = getCacheKey(accountId);
            redisTemplate.opsForValue().set(key, policies, CACHE_TTL);
            logger.info("Cached {} policies for account: {} (TTL: {})", policies.size(), accountId, CACHE_TTL);
        } catch (Exception e) {
            logger.error("Error caching policies for account {}: {}", accountId, e.getMessage(), e);
            // Don't throw - caching failure shouldn't break the application
        }
    }

    @Override
    public void clearCache(Long accountId) {
        try {
            String key = getCacheKey(accountId);
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                logger.info("Cleared Casbin policy cache for account: {}", accountId);
            } else {
                logger.debug("No cache found to clear for account: {}", accountId);
            }
        } catch (Exception e) {
            logger.error("Error clearing cache for account {}: {}", accountId, e.getMessage(), e);
        }
    }

    @Override
    public void clearCache(Set<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return;
        }
        
        try {
            List<String> keys = accountIds.stream()
                .map(this::getCacheKey)
                .collect(Collectors.toList());
            
            Long deleted = redisTemplate.delete(keys);
            logger.info("Cleared Casbin policy cache for {} accounts (deleted {} keys)", accountIds.size(), deleted);
        } catch (Exception e) {
            logger.error("Error clearing cache for accounts {}: {}", accountIds, e.getMessage(), e);
        }
    }

    @Override
    public boolean isCached(Long accountId) {
        try {
            String key = getCacheKey(accountId);
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            logger.error("Error checking cache existence for account {}: {}", accountId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void clearAllCaches() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PATTERN);
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                logger.warn("Cleared all Casbin policy caches (deleted {} keys)", deleted);
            } else {
                logger.debug("No Casbin policy caches found to clear");
            }
        } catch (Exception e) {
            logger.error("Error clearing all Casbin policy caches: {}", e.getMessage(), e);
        }
    }
}

