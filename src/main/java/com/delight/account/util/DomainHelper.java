package com.delight.account.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility class for domain and subdomain extraction from HTTP requests.
 * Consolidates domain extraction logic used across filters and controllers.
 */
@Component
public class DomainHelper {
    
    private final String domainName;
    
    public DomainHelper(@Value("${app.domain.name:localhost}") String domainName) {
        this.domainName = domainName;
    }
    
    /**
     * Extract subdomain from request (subdomain.{domainName} format).
     * Returns null if domain is just the base domain (no subdomain).
     * 
     * Example:
     * - "testcompany.localhost" -> "testcompany"
     * - "localhost" -> null
     * - "sso.localhost" -> "sso"
     * 
     * @param request HTTP servlet request
     * @return Subdomain string or null if base domain
     */
    public String extractSubdomain(HttpServletRequest request) {
        String serverName = getHostWithoutPort(request);
        if (serverName == null) {
            return null;
        }
        
        serverName = serverName.toLowerCase();
        String domainSuffix = "." + domainName;
        
        // Extract subdomain from subdomain.{domainName} format
        if (serverName.endsWith(domainSuffix)) {
            return serverName.substring(0, serverName.indexOf(domainSuffix));
        } else if (serverName.equals(domainName)) {
            // If just the base domain (no subdomain), return null
            return null;
        }
        
        return null;
    }
    
    /**
     * Extract domain/subdomain from request (Host header or server name).
     * Returns the subdomain part if present, or the full host if no subdomain.
     * 
     * Example:
     * - "testcompany.localhost:8080" -> "testcompany"
     * - "localhost:8080" -> "localhost"
     * - "testcompany.localhost" -> "testcompany"
     * 
     * @param request HTTP servlet request
     * @return Domain/subdomain string or null if cannot be determined
     */
    public String extractDomainFromRequest(HttpServletRequest request) {
        String host = getHostWithoutPort(request);
        if (host == null) {
            return null;
        }
        
        host = host.toLowerCase();
        
        // Extract subdomain part (e.g., "testcompany" from "testcompany.localhost")
        int dot = host.indexOf('.');
        if (dot > 0) {
            return host.substring(0, dot);
        }
        
        // If no dot, return the full host (could be base domain or IP)
        return host;
    }
    
    /**
     * Check if request is using base domain (no subdomain).
     * 
     * @param request HTTP servlet request
     * @return true if base domain, false otherwise
     */
    public boolean isBaseDomain(HttpServletRequest request) {
        String serverName = request.getServerName();
        String hostHeader = request.getHeader("Host");
        
        // Check server name
        if (serverName != null && domainName.equals(serverName.toLowerCase())) {
            return true;
        }
        
        // Check Host header
        if (hostHeader != null) {
            String host = hostHeader.toLowerCase();
            // Remove port if present
            int colonIndex = host.indexOf(':');
            if (colonIndex > 0) {
                host = host.substring(0, colonIndex);
            }
            if (domainName.equals(host)) {
                return true;
            }
        }
        
        // If extracted domain is null or empty, it means we're on base domain
        String extractedDomain = extractDomainFromRequest(request);
        if (extractedDomain == null || extractedDomain.isEmpty() || extractedDomain.equals(domainName)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get host from request without port.
     * Tries Host header first, then falls back to server name.
     * 
     * @param request HTTP servlet request
     * @return Host without port, or null if cannot be determined
     */
    public String getHostWithoutPort(HttpServletRequest request) {
        String host = request.getHeader("Host");
        if (host == null) {
            host = request.getServerName();
        }
        
        if (host != null) {
            // Remove port if present
            int colonIndex = host.indexOf(':');
            if (colonIndex > 0) {
                host = host.substring(0, colonIndex);
            }
            return host;
        }
        
        return null;
    }
    
    /**
     * Extract subdomain from server name (simple extraction, no domain name validation).
     * Used for backward compatibility with controllers that extract domain from server name.
     * 
     * Example:
     * - "testcompany.localhost" -> "testcompany"
     * - "localhost" -> "localhost"
     * 
     * @param request HTTP servlet request
     * @return Subdomain or full server name if no subdomain
     */
    public String extractSubdomainFromServerName(HttpServletRequest request) {
        String serverName = request.getServerName();
        if (serverName == null) {
            return null;
        }
        
        serverName = serverName.toLowerCase();
        
        // Extract subdomain part (e.g., "testcompany" from "testcompany.localhost")
        int dot = serverName.indexOf('.');
        if (dot > 0) {
            return serverName.substring(0, dot);
        }
        
        return serverName;
    }
    
    /**
     * Get the configured domain name.
     * 
     * @return Domain name (e.g., "localhost" or "delightapp")
     */
    public String getDomainName() {
        return domainName;
    }
}






