package com.delight.account.filter;

import com.delight.account.context.TenantContext;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.repository.AccountRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
    
    // Paths that should be excluded from tenant resolution
    private static final Set<String> EXCLUDED_PATHS = Set.of(
        "/account/signup",
        "/swagger-ui.html",
        "/actuator/health"
    );
    
    // Path prefixes that should be excluded
    private static final Set<String> EXCLUDED_PATH_PREFIXES = Set.of(
        "/swagger-ui/",
        "/v3/api-docs",
        "/actuator/"
    );

    public TenantFilter(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
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
            
            // Extract subdomain (company identifier) from URL server name
            String domain = httpRequest.getServerName();
            if (domain != null) {
                domain = domain.toLowerCase();
                // Extract subdomain part (e.g., "testcompany1760781995" from "testcompany1760781995.localhost")
                int dot = domain.indexOf('.');
                if (dot > 0) {
                    domain = domain.substring(0, dot);
                }
            }
            
            // Look up account by domain
            Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(domain, AccountStatus.ACTIVE);
            if (accountOpt.isEmpty()) {
                logger.warn("Account not found for domain: {}", domain);
                throw new ApiException("error.account.notfound");
            }
            
            Account account = accountOpt.get();
            Long accountId = account.getId();
            String accountDomain = account.getDomain();
            
            // Set account context in TenantContext (ThreadLocal)
            TenantContext.setAccountId(accountId.toString());
            
            // Set account context in request attributes for backward compatibility
            httpRequest.setAttribute("accountId", accountId);
            httpRequest.setAttribute("domain", accountDomain);
            
            // Set account context in MDC for automatic logging
            MDC.put("accountId", accountId.toString());
            MDC.put("domain", accountDomain);
            
            // Add headers if request is mutable
            if (httpRequest instanceof MutableHttpServletRequest mutable) {
                mutable.putHeader("accountId", String.valueOf(accountId));
                mutable.putHeader("domain", accountDomain);
            }
            
            logger.debug("Tenant context set for accountId: {}, domain: {}", accountId, accountDomain);
            
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
        
        return false;
    }
}