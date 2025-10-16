package com.delight.account.service.impl;

import com.delight.account.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Service
public class RedisCacheServiceImpl implements CacheService {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration defaultTtl;
    
    // Cache statistics
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong cacheErrors = new AtomicLong(0);

    public RedisCacheServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                @Value("${cache.default.ttl:PT1H}") Duration defaultTtl) {
        this.redisTemplate = redisTemplate;
        this.defaultTtl = defaultTtl;
    }

    @Override
    public <T> Optional<T> get(String key, Supplier<Optional<T>> fallbackSupplier, Duration ttl, Class<T> valueType) {
        try {
            // Try to get from cache first
            Object cachedValue = redisTemplate.opsForValue().get(key);
            if (cachedValue != null) {
                cacheHits.incrementAndGet();
                logger.debug("Cache hit for key: {}", key);
                return Optional.of(valueType.cast(cachedValue));
            }
            
            // Cache miss - execute fallback
            cacheMisses.incrementAndGet();
            logger.debug("Cache miss for key: {}, executing fallback", key);
            
            Optional<T> fallbackResult = fallbackSupplier.get();
            if (fallbackResult.isPresent()) {
                // Cache the result from fallback
                put(key, fallbackResult.get(), ttl);
            }
            
            return fallbackResult;
            
        } catch (Exception e) {
            cacheErrors.incrementAndGet();
            logger.error("Redis cache error for key: {}, falling back to database", key, e);
            // If Redis fails, execute fallback directly
            return fallbackSupplier.get();
        }
    }

    @Override
    public <T> Optional<T> get(String key, Supplier<Optional<T>> fallbackSupplier, Class<T> valueType) {
        return get(key, fallbackSupplier, defaultTtl, valueType);
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            logger.debug("Cached value for key: {} with TTL: {}", key, ttl);
        } catch (Exception e) {
            cacheErrors.incrementAndGet();
            logger.error("Failed to cache value for key: {}", key, e);
            // Don't throw exception - caching failure shouldn't break the application
        }
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, defaultTtl);
    }

    @Override
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
            logger.debug("Evicted cache key: {}", key);
        } catch (Exception e) {
            cacheErrors.incrementAndGet();
            logger.error("Failed to evict cache key: {}", key, e);
        }
    }

    @Override
    public void evictByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.debug("Evicted {} keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            cacheErrors.incrementAndGet();
            logger.error("Failed to evict keys by pattern: {}", pattern, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            cacheErrors.incrementAndGet();
            logger.error("Failed to check key existence: {}", key, e);
            return false;
        }
    }

    @Override
    public void clear() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("Cleared {} cache entries", keys.size());
            }
        } catch (Exception e) {
            cacheErrors.incrementAndGet();
            logger.error("Failed to clear cache", e);
        }
    }

    @Override
    public String getStats() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long errors = cacheErrors.get();
        long total = hits + misses;
        double hitRatio = total > 0 ? (double) hits / total * 100 : 0.0;
        
        return String.format("Cache Stats - Hits: %d, Misses: %d, Errors: %d, Hit Ratio: %.2f%%", 
                           hits, misses, errors, hitRatio);
    }
}