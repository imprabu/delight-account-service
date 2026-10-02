package com.delight.account.service;

import java.util.List;
import java.util.Set;

/**
 * Service for caching Casbin policies in Redis per tenant (account).
 * Policies are stored per account and can be cleared when roles or privileges change.
 */
public interface CasbinPolicyCacheService {
    
    /**
     * Get cached policies for an account from Redis.
     * 
     * @param accountId The account ID
     * @return List of policy lines (e.g., "g, 1:100, 5" or "p, 5, user, create")
     */
    List<String> getCachedPolicies(Long accountId);
    
    /**
     * Cache policies for an account in Redis.
     * 
     * @param accountId The account ID
     * @param policies List of policy lines to cache
     */
    void cachePolicies(Long accountId, List<String> policies);
    
    /**
     * Clear cached policies for a specific account.
     * 
     * @param accountId The account ID
     */
    void clearCache(Long accountId);
    
    /**
     * Clear cached policies for multiple accounts.
     * 
     * @param accountIds Set of account IDs
     */
    void clearCache(Set<Long> accountIds);
    
    /**
     * Check if policies are cached for an account.
     * 
     * @param accountId The account ID
     * @return true if cached, false otherwise
     */
    boolean isCached(Long accountId);
    
    /**
     * Clear all Casbin policy caches (for all accounts).
     * Use with caution - only for maintenance operations.
     */
    void clearAllCaches();
}






