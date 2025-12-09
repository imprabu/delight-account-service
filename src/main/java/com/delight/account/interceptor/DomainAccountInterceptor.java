package com.delight.account.interceptor;

import com.delight.account.context.TenantContext;
import com.delight.account.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class DomainAccountInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(DomainAccountInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestPath = request.getRequestURI();
        
        // Skip validation for frontend routes and static assets - they don't require account context
        if (isFrontendRoute(requestPath) || isStaticAsset(requestPath)) {
            // Still try to set MDC if account context is available (for logging)
            String accountId = TenantContext.getAccountId();
            if (accountId != null && !accountId.trim().isEmpty()) {
                MDC.put("accountId", accountId);
                Object requestDomain = request.getAttribute("domain");
                if (requestDomain != null) {
                    MDC.put("domain", requestDomain.toString());
                }
            }
            return true;
        }
        
        // For API routes, validate that account context has been set by TenantFilter
        String accountId = TenantContext.getAccountId();
        if (accountId == null || accountId.trim().isEmpty()) {
            logger.warn("Account ID not found in TenantContext for request: {}", requestPath);
            throw new ApiException("error.account.notfound");
        }
        
        // Ensure account context is also available in request attributes and MDC
        // (TenantFilter should have already set these, but we verify they're consistent)
        Object requestAccountId = request.getAttribute("accountId");
        Object requestDomain = request.getAttribute("domain");
        
        if (requestAccountId == null) {
            logger.warn("Account ID missing from request attributes, but present in TenantContext");
            throw new ApiException("error.account.notfound");
        }
        
        // Verify consistency between TenantContext and request attributes
        if (!accountId.equals(requestAccountId.toString())) {
            logger.warn("Account ID mismatch between TenantContext ({}) and request attributes ({})", 
                       accountId, requestAccountId);
            throw new ApiException("error.account.notfound");
        }
        
        // Ensure MDC is properly set for logging
        MDC.put("accountId", accountId);
        if (requestDomain != null) {
            MDC.put("domain", requestDomain.toString());
        }
        
        logger.debug("Account context validated for accountId: {}", accountId);
        return true;
    }
    
    /**
     * Check if the path is a frontend route (SPA routing).
     */
    private boolean isFrontendRoute(String path) {
        return path.equals("/") ||
               path.equals("/index.html") ||
               path.equals("/login") ||
               path.equals("/signup") ||
               path.equals("/dashboard") ||
               path.equals("/settings") ||
               path.equals("/contacts") ||
               path.equals("/conversations") ||
               path.equals("/inbox") ||
               path.equals("/analytics") ||
               path.equals("/integrations") ||
               path.equals("/knowledge-base") ||
               path.equals("/flows") ||
               path.equals("/channels") ||
               path.equals("/billing") ||
               path.equals("/security");
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
}
