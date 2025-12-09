package com.delight.account.controller;

import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Abstract base controller for API endpoints that support dual routing.
 * This controller provides a common pattern for implementing endpoints that can be accessed
 * both via regular paths (e.g., /accounts/info) and API paths (e.g., /api/accounts/info).
 * 
 * When accessed via /api paths:
 * - Rate limiting is applied
 * - Only Bearer token authentication is accepted (no cookies)
 * 
 * When accessed via regular paths:
 * - No rate limiting
 * - Both Bearer token and cookie authentication are accepted
 * 
 * Usage:
 * 1. Extend this class in your controller
 * 2. Use @RequestMapping with both regular and API paths
 * 3. Add @RateLimit annotation to methods that should be rate-limited when accessed via /api
 * 
 * Example:
 * @RestController
 * @RequestMapping({"/accounts/info", "/api/accounts/info"})
 * public class AccountInfoController extends BaseApiController {
 *     
 *     @GetMapping
 *     @RateLimit(keyPrefix = "account-info")
 *     public ResponseEntity<AccountInfo> getAccountInfo() {
 *         // Implementation
 *     }
 * }
 */
public abstract class BaseApiController {
    
    /**
     * Gets the base path for this controller (without /api prefix).
     * This method can be overridden by subclasses to provide custom base paths.
     * 
     * @return the base path for this controller
     */
    protected String getBasePath() {
        RequestMapping requestMapping = this.getClass().getAnnotation(RequestMapping.class);
        if (requestMapping != null && requestMapping.value().length > 0) {
            String path = requestMapping.value()[0];
            // Return the non-API path (first path that doesn't start with /api)
            for (String mappingPath : requestMapping.value()) {
                if (!mappingPath.startsWith("/api")) {
                    return mappingPath;
                }
            }
            return path;
        }
        return "";
    }
    
    /**
     * Gets the API path for this controller (with /api prefix).
     * 
     * @return the API path for this controller
     */
    protected String getApiPath() {
        RequestMapping requestMapping = this.getClass().getAnnotation(RequestMapping.class);
        if (requestMapping != null && requestMapping.value().length > 0) {
            // Return the API path (first path that starts with /api)
            for (String mappingPath : requestMapping.value()) {
                if (mappingPath.startsWith("/api")) {
                    return mappingPath;
                }
            }
        }
        return "/api" + getBasePath();
    }
}