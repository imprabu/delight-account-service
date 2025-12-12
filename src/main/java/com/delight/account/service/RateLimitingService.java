package com.delight.account.service;

/**
 * Service for implementing rate limiting functionality
 */
public interface RateLimitingService {
    
    /**
     * Check if a request is allowed based on rate limiting rules
     * 
     * @param key The unique identifier for the rate limit (e.g., IP address, user ID)
     * @param maxRequests Maximum number of requests allowed
     * @param windowSizeInSeconds Time window in seconds
     * @return true if request is allowed, false if rate limit exceeded
     */
    boolean isAllowed(String key, int maxRequests, int windowSizeInSeconds);
    
    /**
     * Get the remaining number of requests for a given key
     * 
     * @param key The unique identifier for the rate limit
     * @param maxRequests Maximum number of requests allowed
     * @param windowSizeInSeconds Time window in seconds
     * @return Number of remaining requests
     */
    int getRemainingRequests(String key, int maxRequests, int windowSizeInSeconds);
    
    /**
     * Check rate limit for global endpoints (signup, signin, activation)
     * Uses configuration from application.properties
     * 
     * @param endpoint The endpoint name (signup, signin, activation)
     * @param clientKey The client identifier (e.g., IP address)
     * @return true if request is allowed, false if rate limit exceeded
     */
    boolean isGlobalEndpointAllowed(String endpoint, String clientKey);
    
    /**
     * Check rate limit for account-specific endpoints
     * Uses account-specific configuration from AccountConfig or defaults
     * 
     * @param accountId The account ID
     * @param endpoint The endpoint name
     * @param clientKey The client identifier (e.g., IP address)
     * @return true if request is allowed, false if rate limit exceeded
     */
    boolean isAccountEndpointAllowed(Long accountId, String endpoint, String clientKey);
    
    /**
     * Get remaining requests for global endpoints
     * 
     * @param endpoint The endpoint name (signup, signin, activation)
     * @param clientKey The client identifier (e.g., IP address)
     * @return Number of remaining requests
     */
    int getGlobalEndpointRemainingRequests(String endpoint, String clientKey);
    
    /**
     * Get remaining requests for account-specific endpoints
     * 
     * @param accountId The account ID
     * @param endpoint The endpoint name
     * @param clientKey The client identifier (e.g., IP address)
     * @return Number of remaining requests
     */
    int getAccountEndpointRemainingRequests(Long accountId, String endpoint, String clientKey);
    
    /**
     * Get rate limiting configuration for an account
     * Returns default configuration and any account-specific overrides
     * 
     * @param accountId The account ID
     * @return AccountRateLimitResponse containing default config and endpoint overrides
     */
    com.delight.account.dto.AccountRateLimitResponse getAccountRateLimitConfig(Long accountId);
    
    /**
     * Get default per-account rate limiting configuration
     * 
     * @return RateLimitConfig with default maxRequests and windowSeconds
     */
    com.delight.account.dto.AccountRateLimitResponse.RateLimitConfig getDefaultAccountRateLimitConfig();
    
    /**
     * Atomically check if a request is allowed and increment the counter if allowed.
     * This method performs both operations atomically to ensure consistency.
     * 
     * @param accountId The account ID
     * @param endpoint The endpoint name (e.g., "api", "chat", "upload")
     * @param clientKey The client identifier (e.g., IP address)
     * @return RateLimitCheckResponse containing allowed status, remaining requests, and rate limit information
     */
    com.delight.account.dto.RateLimitCheckResponse checkAndIncrementAccountRateLimit(Long accountId, String endpoint, String clientKey);
    
    /**
     * Get current rate limit status without incrementing the counter.
     * This is useful for checking remaining requests without consuming a rate limit slot.
     * 
     * @param accountId The account ID
     * @param endpoint The endpoint name (e.g., "api", "chat", "upload")
     * @param clientKey The client identifier (e.g., IP address)
     * @return RateLimitCheckResponse containing current status without incrementing
     */
    com.delight.account.dto.RateLimitCheckResponse getAccountRateLimitStatus(Long accountId, String endpoint, String clientKey);
}