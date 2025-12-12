package com.delight.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for account rate limiting information.
 * Used by microservices to get rate limiting configuration for accounts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRateLimitResponse {
    
    /**
     * Account ID
     */
    private Long accountId;
    
    /**
     * Default per-account rate limiting configuration
     */
    private RateLimitConfig defaultConfig;
    
    /**
     * Account-specific rate limiting overrides (endpoint -> config)
     * Key: endpoint name (e.g., "api", "chat", "upload")
     * Value: rate limit configuration for that endpoint
     */
    private Map<String, RateLimitConfig> endpointOverrides;
    
    /**
     * Rate limit configuration for a specific endpoint
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateLimitConfig {
        /**
         * Maximum number of requests allowed
         */
        private Integer maxRequests;
        
        /**
         * Time window in seconds
         */
        private Integer windowSeconds;
    }
}




