package com.delight.account.service;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Generic cache service interface that provides caching operations with database fallback.
 * Implements cache-aside pattern for read operations and write-through for updates.
 */
public interface CacheService {

    /**
     * Get value from cache by key. If not found in cache, execute the fallback supplier
     * and cache the result before returning it.
     *
     * @param key the cache key
     * @param fallbackSupplier supplier to execute if cache miss occurs
     * @param ttl time to live for the cached value
     * @param valueType the class type of the cached value
     * @param <T> the type of the cached value
     * @return the cached or fallback value
     */
    <T> Optional<T> get(String key, Supplier<Optional<T>> fallbackSupplier, Duration ttl, Class<T> valueType);

    /**
     * Get value from cache by key. If not found in cache, execute the fallback supplier
     * and cache the result before returning it. Uses default TTL.
     *
     * @param key the cache key
     * @param fallbackSupplier supplier to execute if cache miss occurs
     * @param valueType the class type of the cached value
     * @param <T> the type of the cached value
     * @return the cached or fallback value
     */
    <T> Optional<T> get(String key, Supplier<Optional<T>> fallbackSupplier, Class<T> valueType);

    /**
     * Put value into cache with specified TTL.
     *
     * @param key the cache key
     * @param value the value to cache
     * @param ttl time to live for the cached value
     */
    void put(String key, Object value, Duration ttl);

    /**
     * Put value into cache with default TTL.
     *
     * @param key the cache key
     * @param value the value to cache
     */
    void put(String key, Object value);

    /**
     * Remove value from cache by key.
     *
     * @param key the cache key
     */
    void evict(String key);

    /**
     * Remove all values from cache that match the pattern.
     *
     * @param pattern the key pattern (supports wildcards)
     */
    void evictByPattern(String pattern);

    /**
     * Check if cache contains the specified key.
     *
     * @param key the cache key
     * @return true if key exists in cache, false otherwise
     */
    boolean exists(String key);

    /**
     * Clear all cache entries.
     */
    void clear();

    /**
     * Get cache statistics (hits, misses, etc.).
     *
     * @return cache statistics as a string
     */
    String getStats();
}