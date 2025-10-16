package com.delight.account.service.impl;

import com.delight.account.cache.CacheKeyManager;
import com.delight.account.dto.AccountInfoResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.model.Membership;
import com.delight.account.service.AccountService;
import com.delight.account.service.CacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.Optional;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final MembershipRepository membershipRepository;
    private final CacheService cacheService;
    private final CacheKeyManager cacheKeyManager;
    
    @Value("${cache.account.ttl:PT30M}")
    private Duration accountCacheTtl;

    @org.springframework.beans.factory.annotation.Autowired
    public AccountServiceImpl(AccountRepository accountRepository, 
                            MembershipRepository membershipRepository,
                            CacheService cacheService,
                            CacheKeyManager cacheKeyManager) {
        this.accountRepository = accountRepository;
        this.membershipRepository = membershipRepository;
        this.cacheService = cacheService;
        this.cacheKeyManager = cacheKeyManager;
    }

    // Backward-compatible constructor for tests without CacheKeyManager
    public AccountServiceImpl(AccountRepository accountRepository,
                              MembershipRepository membershipRepository,
                              CacheService cacheService) {
        this(accountRepository, membershipRepository, cacheService, null);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountInfoResponse getAccountInfo(Long accountId, Long userId) {
        String cacheKey = cacheKeyManager != null
            ? cacheKeyManager.getAccountInfoKey(accountId, userId)
            : String.format("account:info:%d:%d", accountId, userId);
        
        // Try to get from cache first
        Optional<AccountInfoResponse> cachedResponse = cacheService.get(cacheKey, () -> {
            // Database fallback
            Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException("error.account.notfound"));
            Membership membership = membershipRepository.findByUserIdAndAccountId(userId, accountId).orElse(null);

            AccountInfoResponse response = AccountInfoResponse.builder()
                .id(account.getId())
                .domain(account.getDomain())
                .plan(account.getPlan())
                .status(account.getStatus())
                .industryType(account.getIndustryType())
                .email(account.getEmail())
                .companyName(account.getCompanyName())
                .currentUserId(userId)
                .currentUserRole(membership != null && membership.getRole() != null ? membership.getRole().name() : null)
                .organizationId(account.getOrganization() != null ? account.getOrganization().getId() : null)
                .organizationName(account.getOrganization() != null ? account.getOrganization().getName() : null)
                .organizationPrimaryDomain(account.getOrganization() != null ? account.getOrganization().getPrimaryDomain() : null)
                .productCode(account.getProduct() != null ? account.getProduct().getCode() : null)
                .productName(account.getProduct() != null ? account.getProduct().getName() : null)
                .build();
            
            return Optional.of(response);
        }, accountCacheTtl, AccountInfoResponse.class);
        
        return cachedResponse.orElseThrow(() -> new ApiException("error.account.notfound"));
    }
}
