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
        try {
            String rateLimitKey = "rate_limit:" + key;
            long currentTime = Instant.now().getEpochSecond();
            long windowStart = currentTime - windowSizeInSeconds;
            
            // Remove expired entries
            redisTemplate.opsForZSet().removeRangeByScore(rateLimitKey, 0, windowStart);
            
            // Count current requests in the window
            Long currentCount = redisTemplate.opsForZSet().count(rateLimitKey, windowStart, currentTime);
            
            if (currentCount != null && currentCount >= maxRequests) {
                logger.debug("Rate limit exceeded for key: {} (current: {}, max: {})", key, currentCount, maxRequests);
                return false;
            }
            
            // Add current request
            redisTemplate.opsForZSet().add(rateLimitKey, currentTime, currentTime);
            
            // Set expiration for the key
            redisTemplate.expire(rateLimitKey, windowSizeInSeconds + 1, TimeUnit.SECONDS);
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking rate limit for key: {}", key, e);
            // In case of Redis failure, allow the request (fail open)
            return true;
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
}