package com.delight.account.interceptor;

import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.filter.MutableHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
            return true;
        }

        // Prefer Host header to support subdomain emulation without DNS changes
        String host = request.getHeader("Host");
        if (host == null || host.isBlank()) {
            host = request.getServerName();
        }
        if (host != null) {
            host = host.toLowerCase();
            int colon = host.indexOf(':');
            if (colon > 0) {
                host = host.substring(0, colon);
            }
            int dot = host.indexOf('.');
            if (dot > 0) {
                host = host.substring(0, dot);
            }
        }
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(host, AccountStatus.ACTIVE);
        if (accountOpt.isEmpty()) {
            throw new ApiException("error.account.notfound");
        }

        Long accountId = accountOpt.get().getId();
        request.setAttribute("accountId", accountId);
        if (request instanceof MutableHttpServletRequest mutable) {
            mutable.putHeader("accountId", String.valueOf(accountId));
        }

        return true;
    }
}
