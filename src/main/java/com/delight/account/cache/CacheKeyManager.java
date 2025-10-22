package com.delight.account.cache;

/**
 * Centralized cache key manager that provides versioned Redis cache keys.
 * This interface ensures consistency across all cache operations and enables
 * easy cache key versioning for backward compatibility.
 */
public interface CacheKeyManager {

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
     * Generate cache key for account information (account-level data only)
     * @param accountId the account ID
     * @return cache key for account info
     */
    String getAccountInfoKey(Long accountId);

    /**
     * Generate cache key for user membership in account (user-specific data)
     * @param accountId the account ID
     * @param userId the user ID
     * @return cache key for user membership info
     */
    String getUserMembershipKey(Long accountId, Long userId);



    /**
     * Generate cache key for user credentials.
     * Pattern: v{version}:user:credential:{userId}
     * 
     * @param userId the user ID
     * @return the versioned cache key
     */
    String getUserCredentialKey(Long userId);

    /**
     * Generate pattern for evicting account info keys.
     * Pattern: v{version}:account:info:*
     * 
     * @return the versioned pattern for account info eviction
     */
    String getAccountInfoPattern();

    /**
     * Generate pattern for evicting user account keys.
     * Pattern: v{version}:user:account:*
     * 
     * @return the versioned pattern for user account eviction
     */
    String getUserAccountPattern();

    /**
     * Generate pattern for evicting all keys of a specific type.
     * 
     * @param keyType the key type (e.g., "session", "account", "user")
     * @return the versioned pattern for the key type
     */
    String getKeyPattern(String keyType);

    /**
     * Generate pattern for evicting all keys for the current version.
     * Pattern: v{version}:*
     * 
     * @return the versioned pattern for all keys
     */
    String getAllKeysPattern();
}