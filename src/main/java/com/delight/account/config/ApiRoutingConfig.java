package com.delight.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * Configuration for API routing patterns and rules.
 * Defines which endpoints support dual routing and authentication requirements.
 */
@Configuration
@ConfigurationProperties(prefix = "app.api")
public class ApiRoutingConfig {
    
    /**
     * Global endpoints that should not have dual routing or account-level rate limiting
     */
    private Set<String> globalEndpoints = Set.of("signup", "signin", "activation");
    
    /**
     * API path prefix for rate-limited endpoints
     */
    private String apiPrefix = "/api";
    
    /**
     * Check if an endpoint is a global endpoint
     */
    public boolean isGlobalEndpoint(String endpoint) {
        return globalEndpoints.contains(endpoint);
    }
    
    /**
     * Check if a request path is under the API prefix
     */
    public boolean isApiPath(String requestPath) {
        return requestPath != null && requestPath.startsWith(apiPrefix);
    }
    
    /**
     * Extract the base endpoint from a request path
     * e.g., "/api/accounts/info" -> "accounts/info"
     * e.g., "/accounts/info" -> "accounts/info"
     */
    public String extractBaseEndpoint(String requestPath) {
        if (requestPath == null) {
            return "";
        }
        
        if (requestPath.startsWith(apiPrefix)) {
            return requestPath.substring(apiPrefix.length() + 1); // +1 for the slash
        }
        
        // Remove leading slash if present
        return requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
    }
    
    // Getters and setters
    public Set<String> getGlobalEndpoints() {
        return globalEndpoints;
    }
    
    public void setGlobalEndpoints(Set<String> globalEndpoints) {
        this.globalEndpoints = globalEndpoints;
    }
    
    public String getApiPrefix() {
        return apiPrefix;
    }
    
    public void setApiPrefix(String apiPrefix) {
        this.apiPrefix = apiPrefix;
    }
}