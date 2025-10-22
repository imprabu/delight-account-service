package com.delight.account.cache;

/**
 * Cache key manager for user-level operations.
 * Handles cache keys for data that is specific to users and their interactions with accounts.
 */
public interface UserCacheKeyManager {

    /**
     * Get the current cache version.
     * @return the current cache version
     */
    String getVersion();

    /**
     * Generate cache key for user account lookup by email.
     * Pattern: v{version}:user:account:{accountId}:email:{email}
     * 
     * @param accountId the account ID
     * @param email the user email address
     * @return the versioned cache key
     */
    String getUserAccountKey(Long accountId, String email);

    /**
     * Generate cache key for user membership in account (user-specific data)
     * @param accountId the account ID
     * @param userId the user ID
     * @return cache key for user membership info
     */
    String getUserMembershipKey(Long accountId, Long userId);



    /**
     * Generate cache key for user credentials by user ID.
     * Pattern: v{version}:user:credential:{userId}
     * 
     * @param userId the user ID
     * @return the versioned cache key
     */
    String getUserCredentialKey(Long userId);

    /**
     * Generate cache key pattern for user account lookups.
     * @return the pattern for user account cache keys
     */
    String getUserAccountPattern();

    /**
     * Generate cache key pattern for a specific key type.
     * @param keyType the type of key
     * @return the pattern for the specified key type
     */
    String getKeyPattern(String keyType);

    /**
     * Generate pattern to match all cache keys managed by this manager.
     * @return pattern to match all user-level cache keys
     */
    String getAllKeysPattern();
}