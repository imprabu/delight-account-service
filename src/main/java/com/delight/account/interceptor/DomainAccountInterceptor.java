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
        // Validate that account context has been set by TenantFilter
        String accountId = TenantContext.getAccountId();
        if (accountId == null || accountId.trim().isEmpty()) {
            logger.warn("Account ID not found in TenantContext for request: {}", request.getRequestURI());
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
}
