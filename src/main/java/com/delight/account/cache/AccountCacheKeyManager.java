package com.delight.account.cache;

/**
 * Cache key manager for account-level operations.
 * Handles cache keys for data that is specific to accounts but not tied to individual users.
 */
public interface AccountCacheKeyManager {

    /**
     * Get the current cache version.
     * @return the current cache version
     */
    String getVersion();

    /**
     * Generate cache key for account information (account-level data only)
     * @param accountId the account ID
     * @return cache key for account info
     */
    String getAccountInfoKey(Long accountId);

    /**
     * Generate cache key pattern for account information.
     * @return the pattern for account info cache keys
     */
    String getAccountInfoPattern();

    /**
     * Generate cache key pattern for a specific key type.
     * @param keyType the type of key
     * @return the pattern for the specified key type
     */
    String getKeyPattern(String keyType);

    /**
     * Generate pattern to match all cache keys managed by this manager.
     * @return pattern to match all account-level cache keys
     */
    String getAllKeysPattern();
}