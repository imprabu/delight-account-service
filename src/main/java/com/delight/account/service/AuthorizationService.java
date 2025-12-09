package com.delight.account.service;

/**
 * Service for authorization checks using Casbin.
 */
public interface AuthorizationService {
    /**
     * Check if a user has permission to perform an action on a resource.
     * 
     * @param userId The user ID
     * @param accountId The account ID
     * @param resource The resource (e.g., "user", "account", "role")
     * @param action The action (e.g., "create", "update", "delete", "read")
     * @return true if authorized, false otherwise
     */
    boolean checkPermission(Long userId, Long accountId, String resource, String action);
    
    /**
     * Check if a user has a specific privilege.
     * 
     * @param userId The user ID
     * @param accountId The account ID
     * @param privilegeCode The privilege code (e.g., "user.create", "account.update")
     * @return true if authorized, false otherwise
     */
    boolean hasPrivilege(Long userId, Long accountId, String privilegeCode);
    
    /**
     * Reload policies from the database.
     * Should be called when roles, privileges, or memberships are updated.
     * This reloads all policies (for backward compatibility).
     */
    void reloadPolicies();
    
    /**
     * Reload policies for a specific account.
     * Clears Redis cache and reloads from database.
     * 
     * @param accountId The account ID
     */
    void reloadPolicies(Long accountId);
    
    /**
     * Clear cached policies for a specific account.
     * 
     * @param accountId The account ID
     */
    void clearCache(Long accountId);
    
    /**
     * Check if the current request is a channel-authenticated request.
     * Channel requests bypass privilege checks as they are authenticated via Channel JWT tokens.
     * 
     * @return true if the request is channel-authenticated, false otherwise
     */
    boolean isChannelRequest();
}

