package com.delight.account.interceptor;

import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.repository.AccountRepository;
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
        String host = request.getServerName();
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(host, AccountStatus.ACTIVE);
        if (accountOpt.isEmpty()) {
            throw new ApiException("error.account.notfound");
        }
        request.setAttribute("accountId", accountOpt.get().getId());
        return true;
    }
}
