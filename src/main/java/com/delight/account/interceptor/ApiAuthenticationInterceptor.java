package com.delight.account.interceptor;

import com.delight.account.config.ApiRoutingConfig;
import com.delight.account.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to enforce Bearer token authentication for /api endpoints.
 * Regular endpoints can use both Bearer tokens and cookies, but /api endpoints
 * must use Bearer tokens only.
 */
@Component
public class ApiAuthenticationInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiAuthenticationInterceptor.class);
    private final ApiRoutingConfig apiRoutingConfig;
    
    public ApiAuthenticationInterceptor(ApiRoutingConfig apiRoutingConfig) {
        this.apiRoutingConfig = apiRoutingConfig;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        
        // Only enforce Bearer-only auth for /api endpoints
        if (!apiRoutingConfig.isApiPath(requestPath)) {
            return true; // Allow regular endpoints to use any auth method
        }
        
        // Check if this is a global endpoint under /api (which shouldn't exist, but just in case)
        String baseEndpoint = apiRoutingConfig.extractBaseEndpoint(requestPath);
        if (apiRoutingConfig.isGlobalEndpoint(baseEndpoint)) {
            return true; // Global endpoints don't need special auth handling
        }
        
        // For /api endpoints, ensure Bearer token is present
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("API endpoint {} accessed without Bearer token. Auth header: {}", requestPath, authHeader);
            throw new ApiException("error.auth.bearer.required", HttpStatus.UNAUTHORIZED, 
                "API endpoints require Bearer token authentication");
        }
        
        // Check that no session cookie is being used for /api endpoints
        if (hasSessionCookie(request)) {
            logger.warn("API endpoint {} accessed with session cookie, which is not allowed", requestPath);
            throw new ApiException("error.auth.cookie.not.allowed", HttpStatus.UNAUTHORIZED,
                "API endpoints do not accept cookie-based authentication");
        }
        
        logger.debug("Bearer token authentication validated for API endpoint: {}", requestPath);
        return true;
    }
    
    /**
     * Check if the request has a session cookie
     */
    private boolean hasSessionCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return false;
        }
        
        for (var cookie : request.getCookies()) {
            if ("hrsession".equals(cookie.getName()) || "JSESSIONID".equals(cookie.getName())) {
                return cookie.getValue() != null && !cookie.getValue().isEmpty();
            }
        }
        return false;
    }
}