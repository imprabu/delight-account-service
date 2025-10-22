package com.delight.account.interceptor;

import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.filter.MutableHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class DomainAccountInterceptor implements HandlerInterceptor {

    private final AccountRepository accountRepository;

    public DomainAccountInterceptor(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getAttribute("accountId") != null) {
            // Account context already set, but ensure it's also in MDC
            Object accountId = request.getAttribute("accountId");
            Object domain = request.getAttribute("domain");
            if (accountId != null) {
                MDC.put("accountId", accountId.toString());
            }
            if (domain != null) {
                MDC.put("domain", domain.toString());
            }
            return true;
        }

        // Extract subdomain (company identifier) from URL server name
        String domain = request.getServerName();
        if (domain != null) {
            domain = domain.toLowerCase();
            // Extract subdomain part (e.g., "testcompany1760781995" from "testcompany1760781995.localhost")
            int dot = domain.indexOf('.');
            if (dot > 0) {
                domain = domain.substring(0, dot);
            }
        }
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(domain, AccountStatus.ACTIVE);
        if (accountOpt.isEmpty()) {
            throw new ApiException("error.account.notfound");
        }

        Account account = accountOpt.get();
        Long accountId = account.getId();
        String accountDomain = account.getDomain();
        
        // Set account context in request attributes
        request.setAttribute("accountId", accountId);
        request.setAttribute("domain", accountDomain);
        
        // Set account context in MDC for automatic logging (immediately available)
        MDC.put("accountId", accountId.toString());
        MDC.put("domain", accountDomain);
        
        if (request instanceof MutableHttpServletRequest mutable) {
            mutable.putHeader("accountId", String.valueOf(accountId));
            mutable.putHeader("domain", accountDomain);
        }

        return true;
    }
}
