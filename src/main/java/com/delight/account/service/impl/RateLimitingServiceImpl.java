package com.delight.account.service.impl;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.delight.account.service.AccountConfigService;
import com.delight.account.service.RateLimitingService;
import com.delight.account.util.LoggingUtils;

@Service
public class RateLimitingServiceImpl implements RateLimitingService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingServiceImpl.class);
    private static final String RATE_LIMITING_CONFIG_TYPE = "rate_limiting";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final AccountConfigService accountConfigService;
    
    // Global rate limiting configuration from application.properties
    @Value("${rate.limit.signup.max.requests:100}")
    private int signupMaxRequests;
    
    @Value("${rate.limit.signup.window.seconds:3600}")
    private int signupWindowSeconds;
    
    @Value("${rate.limit.signin.max.requests:200}")
    private int signinMaxRequests;
    
    @Value("${rate.limit.signin.window.seconds:3600}")
    private int signinWindowSeconds;
    
    @Value("${rate.limit.activation.max.requests:50}")
    private int activationMaxRequests;
    
    @Value("${rate.limit.activation.window.seconds:300}")
    private int activationWindowSeconds;
    
    // Default per-account rate limiting configuration
    @Value("${rate.limit.per.account.default.max.requests:600}")
    private int defaultPerAccountMaxRequests;
    
    @Value("${rate.limit.per.account.default.window.seconds:60}")
    private int defaultPerAccountWindowSeconds;
    
    public RateLimitingServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                 AccountConfigService accountConfigService) {
        this.redisTemplate = redisTemplate;
        this.accountConfigService = accountConfigService;
    }
    
    @Override
    public boolean isAllowed(String key, int maxRequests, int windowSizeInSeconds) {
        LoggingUtils.logMethodEntry(logger, "isAllowed", 
            "key", key, 
            "maxRequests", maxRequests, 
            "windowSizeInSeconds", windowSizeInSeconds);
        
        try {
            String rateLimitKey = "rate_limit:" + key;
            long currentTime = Instant.now().getEpochSecond();
            long windowStart = currentTime - windowSizeInSeconds;
            
            LoggingUtils.logCacheOperation(logger, "REMOVE_EXPIRED", rateLimitKey, "STARTED");
            // Remove expired entries
            redisTemplate.opsForZSet().removeRangeByScore(rateLimitKey, 0, windowStart);
            
            // Count current requests in the window
            Long currentCount = redisTemplate.opsForZSet().count(rateLimitKey, windowStart, currentTime);
            
            if (currentCount != null && currentCount >= maxRequests) {
                LoggingUtils.logSecurityEvent(logger, "RATE_LIMIT_EXCEEDED", "BLOCKED", 
                    "key", key, 
                    "currentCount", currentCount, 
                    "maxRequests", maxRequests, 
                    "windowSizeInSeconds", windowSizeInSeconds);
                LoggingUtils.logMethodExit(logger, "isAllowed", false);
                return false;
            }
            
            // Add current request
            redisTemplate.opsForZSet().add(rateLimitKey, currentTime, currentTime);
            
            // Set expiration for the key
            redisTemplate.expire(rateLimitKey, windowSizeInSeconds + 1, TimeUnit.SECONDS);
            
            LoggingUtils.logCacheOperation(logger, "ADD_REQUEST", rateLimitKey, "SUCCESS", 
                "currentCount", currentCount != null ? currentCount + 1 : 1, 
                "maxRequests", maxRequests);
            
            LoggingUtils.logMethodExit(logger, "isAllowed", true);
            return true;
        } catch (Exception e) {
            LoggingUtils.logSecurityEvent(logger, "RATE_LIMIT_ERROR", "ERROR", 
                "key", key, 
                "error", e.getMessage());
            logger.error("Error checking rate limit for key: {}", key, e);
            LoggingUtils.logMethodExit(logger, "isAllowed", true);
            return true; // Allow on error to avoid blocking legitimate requests
        }
    }
    
    @Override
    public int getRemainingRequests(String key, int maxRequests, int windowSizeInSeconds) {
        try {
            String rateLimitKey = "rate_limit:" + key;
            long currentTime = Instant.now().getEpochSecond();
            long windowStart = currentTime - windowSizeInSeconds;
            
            // Remove expired entries
            redisTemplate.opsForZSet().removeRangeByScore(rateLimitKey, 0, windowStart);
            
            // Count current requests in the window
            Long currentCount = redisTemplate.opsForZSet().count(rateLimitKey, windowStart, currentTime);
            
            if (currentCount == null) {
                return maxRequests;
            }
            
            return Math.max(0, maxRequests - currentCount.intValue());
            
        } catch (Exception e) {
            logger.error("Error getting remaining requests for key: {}", key, e);
            // In case of Redis failure, return max requests (fail open)
            return maxRequests;
        }
    }
    
    /**
     * Check rate limit for global endpoints (signup, signin, activation)
     */
    public boolean isGlobalEndpointAllowed(String endpoint, String clientKey) {
        int maxRequests;
        int windowSeconds;
        
        switch (endpoint.toLowerCase()) {
            case "signup":
                maxRequests = signupMaxRequests;
                windowSeconds = signupWindowSeconds;
                break;
            case "signin":
                maxRequests = signinMaxRequests;
                windowSeconds = signinWindowSeconds;
                break;
            case "activation":
                maxRequests = activationMaxRequests;
                windowSeconds = activationWindowSeconds;
                break;
            default:
                logger.warn("Unknown global endpoint: {}", endpoint);
                return true; // Allow unknown endpoints
        }
        
        String key = String.format("global:%s:%s", endpoint, clientKey);
        return isAllowed(key, maxRequests, windowSeconds);
    }
    
    /**
     * Check rate limit for account-specific endpoints
     */
    public boolean isAccountEndpointAllowed(Long accountId, String endpoint, String clientKey) {
        // Get account-specific rate limiting configuration
        Integer maxRequests = accountConfigService.getConfigValue(
            accountId, RATE_LIMITING_CONFIG_TYPE, endpoint + ".maxRequests", null);
        Integer windowSeconds = accountConfigService.getConfigValue(
            accountId, RATE_LIMITING_CONFIG_TYPE, endpoint + ".windowSeconds", null);
        
        // Use defaults if no custom configuration
        if (maxRequests == null) {
            maxRequests = defaultPerAccountMaxRequests;
        }
        if (windowSeconds == null) {
            windowSeconds = defaultPerAccountWindowSeconds;
        }
        
        String key = String.format("account:%d:%s:%s", accountId, endpoint, clientKey);
        return isAllowed(key, maxRequests, windowSeconds);
    }
    
    /**
     * Get remaining requests for global endpoints
     */
    public int getGlobalEndpointRemainingRequests(String endpoint, String clientKey) {
        int maxRequests;
        int windowSeconds;
        
        switch (endpoint.toLowerCase()) {
            case "signup":
                maxRequests = signupMaxRequests;
                windowSeconds = signupWindowSeconds;
                break;
            case "signin":
                maxRequests = signinMaxRequests;
                windowSeconds = signinWindowSeconds;
                break;
            case "activation":
                maxRequests = activationMaxRequests;
                windowSeconds = activationWindowSeconds;
                break;
            default:
                return Integer.MAX_VALUE; // Unknown endpoints have no limit
        }
        
        String key = String.format("global:%s:%s", endpoint, clientKey);
        return getRemainingRequests(key, maxRequests, windowSeconds);
    }
    
    /**
     * Get remaining requests for account-specific endpoints
     */
    public int getAccountEndpointRemainingRequests(Long accountId, String endpoint, String clientKey) {
        // Get account-specific rate limiting configuration
        Integer maxRequests = accountConfigService.getConfigValue(
            accountId, RATE_LIMITING_CONFIG_TYPE, endpoint + ".maxRequests", null);
        Integer windowSeconds = accountConfigService.getConfigValue(
            accountId, RATE_LIMITING_CONFIG_TYPE, endpoint + ".windowSeconds", null);
        
        // Use defaults if no custom configuration
        if (maxRequests == null) {
            maxRequests = defaultPerAccountMaxRequests;
        }
        if (windowSeconds == null) {
            windowSeconds = defaultPerAccountWindowSeconds;
        }
        
        String key = String.format("account:%d:%s:%s", accountId, endpoint, clientKey);
        return getRemainingRequests(key, maxRequests, windowSeconds);
    }
    
    @Override
    public com.delight.account.dto.AccountRateLimitResponse getAccountRateLimitConfig(Long accountId) {
        // Get default configuration
        com.delight.account.dto.AccountRateLimitResponse.RateLimitConfig defaultConfig = 
            getDefaultAccountRateLimitConfig();
        
        // Get account-specific rate limiting configuration
        java.util.Optional<java.util.Map<String, Object>> accountConfigOpt = 
            accountConfigService.getConfig(accountId, RATE_LIMITING_CONFIG_TYPE);
        
        java.util.Map<String, com.delight.account.dto.AccountRateLimitResponse.RateLimitConfig> endpointOverrides = 
            new java.util.HashMap<>();
        
        if (accountConfigOpt.isPresent()) {
            java.util.Map<String, Object> config = accountConfigOpt.get();
            
            // Extract endpoint-specific overrides
            // Config keys are in format: "{endpoint}.maxRequests" and "{endpoint}.windowSeconds"
            java.util.Set<String> endpoints = new java.util.HashSet<>();
            
            for (String key : config.keySet()) {
                if (key.endsWith(".maxRequests") || key.endsWith(".windowSeconds")) {
                    String endpoint = key.substring(0, key.lastIndexOf('.'));
                    endpoints.add(endpoint);
                }
            }
            
            // Build rate limit config for each endpoint
            for (String endpoint : endpoints) {
                Integer maxRequests = accountConfigService.getConfigValue(
                    accountId, RATE_LIMITING_CONFIG_TYPE, endpoint + ".maxRequests", null);
                Integer windowSeconds = accountConfigService.getConfigValue(
                    accountId, RATE_LIMITING_CONFIG_TYPE, endpoint + ".windowSeconds", null);
                
                if (maxRequests != null || windowSeconds != null) {
                    // Use defaults if not specified
                    if (maxRequests == null) {
                        maxRequests = defaultPerAccountMaxRequests;
                    }
                    if (windowSeconds == null) {
                        windowSeconds = defaultPerAccountWindowSeconds;
                    }
                    
                    endpointOverrides.put(endpoint, 
                        com.delight.account.dto.AccountRateLimitResponse.RateLimitConfig.builder()
                            .maxRequests(maxRequests)
                            .windowSeconds(windowSeconds)
                            .build());
                }
            }
        }
        
        return com.delight.account.dto.AccountRateLimitResponse.builder()
            .accountId(accountId)
            .defaultConfig(defaultConfig)
            .endpointOverrides(endpointOverrides)
            .build();
    }
    
    @Override
    public com.delight.account.dto.AccountRateLimitResponse.RateLimitConfig getDefaultAccountRateLimitConfig() {
        return com.delight.account.dto.AccountRateLimitResponse.RateLimitConfig.builder()
            .maxRequests(defaultPerAccountMaxRequests)
            .windowSeconds(defaultPerAccountWindowSeconds)
            .build();
    }
    
    @Override
    public com.delight.account.dto.RateLimitCheckResponse checkAndIncrementAccountRateLimit(Long accountId, String endpoint, String clientKey) {
        LoggingUtils.logMethodEntry(logger, "checkAndIncrementAccountRateLimit", 
            "accountId", accountId, 
            "endpoint", endpoint, 
            "clientKey", clientKey);
        
        try {
            // Get account-specific rate limiting configuration
            Integer maxRequests = accountConfigService.getConfigValue(
                accountId, RATE_LIMITING_CONFIG_TYPE, endpoint + ".maxRequests", null);
            Integer windowSeconds = accountConfigService.getConfigValue(
                accountId, RATE_LIMITING_CONFIG_TYPE, endpoint + ".windowSeconds", null);
            
            // Use defaults if no custom configuration
            if (maxRequests == null) {
                maxRequests = defaultPerAccountMaxRequests;
            }
            if (windowSeconds == null) {
                windowSeconds = defaultPerAccountWindowSeconds;
            }
            
            // Atomically check and increment (if allowed)
            boolean allowed = isAccountEndpointAllowed(accountId, endpoint, clientKey);
            
            // Get remaining requests after the check/increment
            int remainingRequests = getAccountEndpointRemainingRequests(accountId, endpoint, clientKey);
            
            // Calculate current count
            int currentCount = maxRequests - remainingRequests;
            
            // Calculate reset timestamp (current time + window seconds)
            long currentTime = Instant.now().getEpochSecond();
            long resetAt = currentTime + windowSeconds;
            
            com.delight.account.dto.RateLimitCheckResponse response = com.delight.account.dto.RateLimitCheckResponse.builder()
                .allowed(allowed)
                .remainingRequests(remainingRequests)
                .currentCount(currentCount)
                .maxRequests(maxRequests)
                .windowSeconds(windowSeconds)
                .resetAt(resetAt)
                .build();
            
            LoggingUtils.logMethodExit(logger, "checkAndIncrementAccountRateLimit", 
                String.format("allowed=%s, remainingRequests=%d, currentCount=%d", allowed, remainingRequests, currentCount));
            
            return response;
        } catch (Exception e) {
            logger.error("Error checking and incrementing rate limit for accountId: {}, endpoint: {}, clientKey: {}", 
                accountId, endpoint, clientKey, e);
            // Fail open - return allowed=true to avoid blocking legitimate requests
            long currentTime = Instant.now().getEpochSecond();
            return com.delight.account.dto.RateLimitCheckResponse.builder()
                .allowed(true)
                .remainingRequests(Integer.MAX_VALUE)
                .currentCount(0)
                .maxRequests(defaultPerAccountMaxRequests)
                .windowSeconds(defaultPerAccountWindowSeconds)
                .resetAt(currentTime + defaultPerAccountWindowSeconds)
                .build();
        }
    }
    
    @Override
    public com.delight.account.dto.RateLimitCheckResponse getAccountRateLimitStatus(Long accountId, String endpoint, String clientKey) {
        LoggingUtils.logMethodEntry(logger, "getAccountRateLimitStatus", 
            "accountId", accountId, 
            "endpoint", endpoint, 
            "clientKey", clientKey);
        
        try {
            // Get account-specific rate limiting configuration
            Integer maxRequests = accountConfigService.getConfigValue(
                accountId, RATE_LIMITING_CONFIG_TYPE, endpoint + ".maxRequests", null);
            Integer windowSeconds = accountConfigService.getConfigValue(
                accountId, RATE_LIMITING_CONFIG_TYPE, endpoint + ".windowSeconds", null);
            
            // Use defaults if no custom configuration
            if (maxRequests == null) {
                maxRequests = defaultPerAccountMaxRequests;
            }
            if (windowSeconds == null) {
                windowSeconds = defaultPerAccountWindowSeconds;
            }
            
            // Get remaining requests without incrementing
            int remainingRequests = getAccountEndpointRemainingRequests(accountId, endpoint, clientKey);
            
            // Calculate current count
            int currentCount = maxRequests - remainingRequests;
            
            // Determine if a new request would be allowed
            boolean allowed = remainingRequests > 0;
            
            // Calculate reset timestamp (current time + window seconds)
            long currentTime = Instant.now().getEpochSecond();
            long resetAt = currentTime + windowSeconds;
            
            com.delight.account.dto.RateLimitCheckResponse response = com.delight.account.dto.RateLimitCheckResponse.builder()
                .allowed(allowed)
                .remainingRequests(remainingRequests)
                .currentCount(currentCount)
                .maxRequests(maxRequests)
                .windowSeconds(windowSeconds)
                .resetAt(resetAt)
                .build();
            
            LoggingUtils.logMethodExit(logger, "getAccountRateLimitStatus", 
                String.format("allowed=%s, remainingRequests=%d, currentCount=%d", allowed, remainingRequests, currentCount));
            
            return response;
        } catch (Exception e) {
            logger.error("Error getting rate limit status for accountId: {}, endpoint: {}, clientKey: {}", 
                accountId, endpoint, clientKey, e);
            // Fail open - return allowed=true to avoid blocking legitimate requests
            long currentTime = Instant.now().getEpochSecond();
            return com.delight.account.dto.RateLimitCheckResponse.builder()
                .allowed(true)
                .remainingRequests(Integer.MAX_VALUE)
                .currentCount(0)
                .maxRequests(defaultPerAccountMaxRequests)
                .windowSeconds(defaultPerAccountWindowSeconds)
                .resetAt(currentTime + defaultPerAccountWindowSeconds)
                .build();
        }
    }
}