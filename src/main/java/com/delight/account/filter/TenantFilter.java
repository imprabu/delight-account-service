package com.delight.account.filter;

import com.delight.account.context.TenantContext;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.util.DomainHelper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Component
@Order(1) // Run early in the filter chain, before Spring Security
public class TenantFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TenantFilter.class);
    
    private final AccountRepository accountRepository;
    private final DomainHelper domainHelper;
    
    // Paths that should be excluded from tenant resolution
    private static final Set<String> EXCLUDED_PATHS = Set.of(
        "/accounts/signup",
        "/signup", // Handle normalized paths
        "/swagger-ui.html",
        "/actuator/health",
        "/index.html", // Frontend index.html
        // Frontend routes (SPA)
        "/", "/login", "/dashboard", "/settings", "/contacts", "/conversations",
        "/inbox", "/analytics", "/integrations", "/knowledge-base", "/flows",
        "/channels", "/billing", "/security"
    );
    
    // Path prefixes that should be excluded
    private static final Set<String> EXCLUDED_PATH_PREFIXES = Set.of(
        "/swagger-ui/",
        "/v3/api-docs",
        "/actuator/",
        "/assets/" // Frontend static assets
        // Note: /channel/ paths are NOT excluded - they need special handling
    );

    public TenantFilter(AccountRepository accountRepository,
                       @Autowired DomainHelper domainHelper) {
        this.accountRepository = accountRepository;
        this.domainHelper = domainHelper;
        logger.info("TenantFilter initialized with domain name: {}", domainHelper.getDomainName());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            String requestPath = httpRequest.getRequestURI();
            
            // Skip tenant resolution for excluded paths
            if (shouldSkipTenantResolution(requestPath)) {
                chain.doFilter(request, response);
                return;
            }
            
            Long accountId = null;
            String accountDomain = null;
            boolean skipAccountResolution = false;
            
            // Special handling for channel requests
            if (requestPath.startsWith("/channel/")) {
                String subdomain = domainHelper.extractSubdomain(httpRequest);
                
                // Check if domain matches the configured domain name (no subdomain)
                boolean isBaseDomain = domainHelper.isBaseDomain(httpRequest);
                
                // For "sso" subdomain or base domain (no subdomain), account context will be set by ChannelAuthFilter from JWT payload
                // Skip account resolution here and let ChannelAuthFilter handle it
                if ("sso".equals(subdomain) || (subdomain == null && isBaseDomain)) {
                    logger.debug("Skipping account resolution for {} - ChannelAuthFilter will set it from JWT payload", 
                                subdomain != null ? "sso subdomain" : domainHelper.getDomainName());
                    skipAccountResolution = true;
                } else {
                    // For other subdomains, look up account by subdomain
                    accountId = resolveAccountContextForChannel(httpRequest);
                    if (accountId != null) {
                        // Get account domain for logging
                        Optional<Account> accountOpt = accountRepository.findById(accountId);
                        if (accountOpt.isPresent()) {
                            accountDomain = accountOpt.get().getDomain();
                        }
                    }
                }
            } else {
                // Regular tenant resolution from subdomain
                accountId = resolveAccountContextFromSubdomain(httpRequest);
                if (accountId != null) {
                    Optional<Account> accountOpt = accountRepository.findById(accountId);
                    if (accountOpt.isPresent()) {
                        accountDomain = accountOpt.get().getDomain();
                    }
                }
            }
            
            // Only throw error if account resolution was attempted but failed AND it's an API path
            // For frontend routes, allow the request to proceed even if account resolution fails
            if (!skipAccountResolution && accountId == null && isApiPath(requestPath)) {
                logger.warn("Account context could not be resolved for request: {}", requestPath);
                throw new ApiException("error.account.notfound");
            } else if (!skipAccountResolution && accountId == null && !isApiPath(requestPath)) {
                // For frontend routes, log a warning but allow the request to proceed
                logger.debug("Account context could not be resolved for frontend route: {} (allowing request to proceed)", requestPath);
            }
            
            // Set account context only if we have it (skip for sso subdomain channel requests)
            if (skipAccountResolution) {
                // ChannelAuthFilter will set the account context
                chain.doFilter(request, response);
                return;
            }
            
            // Set account context in TenantContext (ThreadLocal) - only if accountId is resolved
            if (accountId != null) {
                TenantContext.setAccountId(accountId.toString());
                
                // Set account context in request attributes for backward compatibility
                httpRequest.setAttribute("accountId", accountId);
                if (accountDomain != null) {
                    httpRequest.setAttribute("domain", accountDomain);
                }
                
                // Set account context in MDC for automatic logging
                MDC.put("accountId", accountId.toString());
                if (accountDomain != null) {
                    MDC.put("domain", accountDomain);
                }
                
                // Add headers if request is mutable
                if (httpRequest instanceof MutableHttpServletRequest mutable) {
                    mutable.putHeader("accountId", String.valueOf(accountId));
                    if (accountDomain != null) {
                        mutable.putHeader("domain", accountDomain);
                    }
                }
                
                logger.debug("Tenant context set for accountId: {}, domain: {}", accountId, accountDomain);
            }
            
            // Continue with the filter chain
            chain.doFilter(request, response);
            
        } finally {
            // Clean up ThreadLocal to prevent memory leaks
            TenantContext.clear();
        }
    }
    
    private boolean shouldSkipTenantResolution(String requestPath) {
        // Check exact path matches
        if (EXCLUDED_PATHS.contains(requestPath)) {
            return true;
        }
        
        // Check path prefix matches
        for (String prefix : EXCLUDED_PATH_PREFIXES) {
            if (requestPath.startsWith(prefix)) {
                return true;
            }
        }
        
        // Skip tenant resolution for static assets (files with extensions), but allow index.html to resolve account
        if (isStaticAsset(requestPath) && !requestPath.equals("/index.html") && !requestPath.equals("/")) {
            return true;
        }
        
        // For frontend routes, we still want to resolve the account from subdomain
        // but we won't throw an error if resolution fails (unlike API routes)
        // This allows the frontend to have account context if available
        return false;
    }
    
    /**
     * Check if the path is a static asset (has a file extension).
     */
    private boolean isStaticAsset(String path) {
        return path.contains(".") && 
               (path.endsWith(".html") || path.endsWith(".js") || path.endsWith(".css") || 
                path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                path.endsWith(".gif") || path.endsWith(".svg") || path.endsWith(".ico") ||
                path.endsWith(".woff") || path.endsWith(".woff2") || path.endsWith(".ttf") ||
                path.endsWith(".eot") || path.endsWith(".json") || path.endsWith(".map") ||
                path.endsWith(".webp") || path.endsWith(".avif"));
    }
    
    /**
     * Check if the path is an API endpoint.
     */
    private boolean isApiPath(String path) {
        return path.startsWith("/api/") ||
               path.startsWith("/accounts/") ||
               path.startsWith("/channel/") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/actuator") ||
               path.startsWith("/test/");
    }
    
    /**
     * Resolve account context for channel requests (non-sso subdomains).
     * Extract subdomain from subdomain.{domainName} and look up in account table.
     */
    private Long resolveAccountContextForChannel(HttpServletRequest request) {
        String subdomain = domainHelper.extractSubdomain(request);
        
        if (subdomain == null || subdomain.isEmpty()) {
            logger.debug("Could not extract subdomain from request");
            return null;
        }
        
        // Look up account by subdomain (which should match the account domain)
        logger.debug("Looking up account by subdomain: {}", subdomain);
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(subdomain, AccountStatus.ACTIVE);
        if (accountOpt.isPresent()) {
            Long accountId = accountOpt.get().getId();
            logger.debug("Found account by subdomain {}: accountId={}", subdomain, accountId);
            return accountId;
        } else {
            logger.warn("Account not found for subdomain: {}", subdomain);
            return null;
        }
    }
    
    /**
     * Resolve account context from subdomain for regular (non-channel) requests.
     */
    private Long resolveAccountContextFromSubdomain(HttpServletRequest request) {
        String domain = domainHelper.extractSubdomainFromServerName(request);
        if (domain == null) {
            return null;
        }
        
        // Look up account by domain
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(domain, AccountStatus.ACTIVE);
        if (accountOpt.isPresent()) {
            return accountOpt.get().getId();
        }
        
        return null;
    }
}