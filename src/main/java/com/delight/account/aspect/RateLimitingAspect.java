package com.delight.account.aspect;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import com.delight.account.annotation.RateLimit;
import com.delight.account.config.ApiRoutingConfig;
import com.delight.account.exception.ApiException;
import com.delight.account.service.RateLimitingService;

@Aspect
@Component
public class RateLimitingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingAspect.class);
    private final RateLimitingService rateLimitingService;
    private final ApiRoutingConfig apiRoutingConfig;
    
    public RateLimitingAspect(RateLimitingService rateLimitingService, ApiRoutingConfig apiRoutingConfig) {
        this.rateLimitingService = rateLimitingService;
        this.apiRoutingConfig = apiRoutingConfig;
    }
    
    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String clientKey = getClientKey();
        String endpoint = rateLimit.keyPrefix();
        String requestPath = getCurrentRequestPath();
        
        // Check if this is a global endpoint (signup, signin, activation)
        if (apiRoutingConfig.isGlobalEndpoint(endpoint)) {
            boolean allowed = rateLimitingService.isGlobalEndpointAllowed(endpoint, clientKey);
            if (!allowed) {
                logger.warn("Rate limit exceeded for global endpoint: {} with key: {}", endpoint, clientKey);
                throw new ApiException("error.rate.limit.exceeded", HttpStatus.TOO_MANY_REQUESTS);
            }
            return joinPoint.proceed();
        }
        
        // For non-global endpoints, only apply rate limiting if accessed via /api path
        if (apiRoutingConfig.isApiPath(requestPath)) {
            // Get account ID from request attributes (set by DomainAccountInterceptor)
            Long accountId = getAccountIdFromRequest();
            boolean allowed = rateLimitingService.isAccountEndpointAllowed(accountId, endpoint, clientKey);
            if (!allowed) {
                logger.warn("Rate limit exceeded for API endpoint: {} with key: {}", requestPath, clientKey);
                throw new ApiException("error.rate.limit.exceeded", HttpStatus.TOO_MANY_REQUESTS);
            }
        } else {
            logger.debug("Skipping rate limiting for non-API endpoint: {}", requestPath);
        }
        
        return joinPoint.proceed();
    }
    
    private String getCurrentRequestPath() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRequestURI();
        }
        return "";
    }
    
    private Long getAccountIdFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            Object accountId = request.getAttribute("accountId");
            if (accountId instanceof Long) {
                return (Long) accountId;
            }
        }
        return null;
    }
    
    private boolean isGlobalEndpoint(String endpoint) {
        return "signup".equals(endpoint) || "signin".equals(endpoint) || "activation".equals(endpoint);
    }
    
    private String getClientKey() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return getClientIpAddress(request);
        }
        return "unknown";
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}