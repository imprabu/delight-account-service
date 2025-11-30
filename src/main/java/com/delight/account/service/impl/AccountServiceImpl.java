package com.delight.account.service.impl;

import com.delight.account.cache.AccountCacheKeyManager;
import com.delight.account.cache.CacheKeyManager;
import com.delight.account.cache.UserCacheKeyManager;
import com.delight.account.dto.AccountInfoResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.model.Membership;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.service.AccountService;
import com.delight.account.service.CacheService;
import com.delight.account.dto.AccountInfoCacheResponse;
import com.delight.account.dto.UserMembershipCacheResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final MembershipRepository membershipRepository;
    private final CacheService cacheService;
    private final AccountCacheKeyManager accountCacheKeyManager;
    private final UserCacheKeyManager userCacheKeyManager;
    
    @Value("${cache.account.ttl:PT30M}")
    private Duration accountCacheTtl;

    @org.springframework.beans.factory.annotation.Autowired
    public AccountServiceImpl(AccountRepository accountRepository, 
                            MembershipRepository membershipRepository,
                            CacheService cacheService,
                            AccountCacheKeyManager accountCacheKeyManager,
                            UserCacheKeyManager userCacheKeyManager) {
        this.accountRepository = accountRepository;
        this.membershipRepository = membershipRepository;
        this.cacheService = cacheService;
        this.accountCacheKeyManager = accountCacheKeyManager;
        this.userCacheKeyManager = userCacheKeyManager;
    }

    // Backward-compatible constructor for tests with unified CacheKeyManager
    public AccountServiceImpl(AccountRepository accountRepository,
                              MembershipRepository membershipRepository,
                              CacheService cacheService,
                              CacheKeyManager cacheKeyManager) {
        this.accountRepository = accountRepository;
        this.membershipRepository = membershipRepository;
        this.cacheService = cacheService;
        // Cast the unified manager to both interfaces (works since VersionedCacheKeyManager implements both)
        this.accountCacheKeyManager = (AccountCacheKeyManager) cacheKeyManager;
        this.userCacheKeyManager = (UserCacheKeyManager) cacheKeyManager;
    }

    // Backward-compatible constructor for tests without CacheKeyManager
    public AccountServiceImpl(AccountRepository accountRepository,
                              MembershipRepository membershipRepository,
                              CacheService cacheService) {
        this.accountRepository = accountRepository;
        this.membershipRepository = membershipRepository;
        this.cacheService = cacheService;
        this.accountCacheKeyManager = null;
        this.userCacheKeyManager = null;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountInfoResponse getAccountInfo(Long accountId, Long userId) {
        // Cache account-level data separately (common for all users)
        String accountCacheKey = accountCacheKeyManager != null
            ? accountCacheKeyManager.getAccountInfoKey(accountId)
            : String.format("account:info:%d", accountId);
        
        Optional<AccountInfoCacheResponse> cachedAccountData = cacheService.get(accountCacheKey, () -> {
            // Database fallback for account data
            Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException("error.account.notfound"));
            
            AccountInfoCacheResponse response = AccountInfoCacheResponse.builder()
                .id(account.getId())
                .domain(account.getDomain())
                .plan(account.getPlan())
                .status(account.getStatus())
                .industryType(account.getIndustryType())
                .email(account.getEmail())
                .companyName(account.getCompanyName())
                .organizationId(account.getOrganization() != null ? account.getOrganization().getId() : null)
                .organizationName(account.getOrganization() != null ? account.getOrganization().getName() : null)
                .organizationPrimaryDomain(account.getOrganization() != null ? account.getOrganization().getPrimaryDomain() : null)
                .productCode(account.getProduct() != null ? account.getProduct().getCode() : null)
                .productName(account.getProduct() != null ? account.getProduct().getName() : null)
                .build();
            
            return Optional.of(response);
        }, Duration.ofMinutes(30), AccountInfoCacheResponse.class);

        // Cache user-specific membership data separately
        String membershipCacheKey = userCacheKeyManager != null
            ? userCacheKeyManager.getUserMembershipKey(accountId, userId)
            : String.format("user:membership:%d:%d", accountId, userId);
        
        Optional<UserMembershipCacheResponse> cachedMembershipData = cacheService.get(membershipCacheKey, () -> {
            Membership membership = membershipRepository.findByUserIdAndAccountId(userId, accountId).orElse(null);
            UserMembershipCacheResponse response = UserMembershipCacheResponse.builder()
                .userId(userId)
                .role(membership != null && membership.getRole() != null ? membership.getRole().name() : null)
                .build();
            return Optional.of(response);
        }, Duration.ofMinutes(15), UserMembershipCacheResponse.class);

        // Combine cached data into response
        AccountInfoCacheResponse accountData = cachedAccountData.orElseThrow(() -> new ApiException("error.account.notfound"));
        UserMembershipCacheResponse membershipData = cachedMembershipData.orElse(
            UserMembershipCacheResponse.builder().userId(userId).role(null).build()
        );

        return AccountInfoResponse.builder()
            .id(accountData.getId())
            .domain(accountData.getDomain())
            .plan(accountData.getPlan())
            .status(accountData.getStatus())
            .industryType(accountData.getIndustryType())
            .email(accountData.getEmail())
            .companyName(accountData.getCompanyName())
            .currentUserId(membershipData.getUserId())
            .currentUserRole(membershipData.getRole())
            .organizationId(accountData.getOrganizationId())
            .organizationName(accountData.getOrganizationName())
            .organizationPrimaryDomain(accountData.getOrganizationPrimaryDomain())
            .productCode(accountData.getProductCode())
            .productName(accountData.getProductName())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountInfoResponse getAccountInfoForChannel(Long accountId) {
        // Cache account-level data (same as regular method but without user-specific data)
        String accountCacheKey = accountCacheKeyManager != null
            ? accountCacheKeyManager.getAccountInfoKey(accountId)
            : String.format("account:info:%d", accountId);
        
        Optional<AccountInfoCacheResponse> cachedAccountData = cacheService.get(accountCacheKey, () -> {
            // Database fallback for account data
            Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException("error.account.notfound"));
            
            AccountInfoCacheResponse response = AccountInfoCacheResponse.builder()
                .id(account.getId())
                .domain(account.getDomain())
                .plan(account.getPlan())
                .status(account.getStatus())
                .industryType(account.getIndustryType())
                .email(account.getEmail())
                .companyName(account.getCompanyName())
                .organizationId(account.getOrganization() != null ? account.getOrganization().getId() : null)
                .organizationName(account.getOrganization() != null ? account.getOrganization().getName() : null)
                .organizationPrimaryDomain(account.getOrganization() != null ? account.getOrganization().getPrimaryDomain() : null)
                .productCode(account.getProduct() != null ? account.getProduct().getCode() : null)
                .productName(account.getProduct() != null ? account.getProduct().getName() : null)
                .build();
            
            return Optional.of(response);
        }, Duration.ofMinutes(30), AccountInfoCacheResponse.class);

        // Return account data without user-specific fields
        AccountInfoCacheResponse accountData = cachedAccountData.orElseThrow(() -> new ApiException("error.account.notfound"));

        return AccountInfoResponse.builder()
            .id(accountData.getId())
            .domain(accountData.getDomain())
            .plan(accountData.getPlan())
            .status(accountData.getStatus())
            .industryType(accountData.getIndustryType())
            .email(accountData.getEmail())
            .companyName(accountData.getCompanyName())
            .currentUserId(null) // No user context in Channel
            .currentUserRole(null) // No user context in Channel
            .organizationId(accountData.getOrganizationId())
            .organizationName(accountData.getOrganizationName())
            .organizationPrimaryDomain(accountData.getOrganizationPrimaryDomain())
            .productCode(accountData.getProductCode())
            .productName(accountData.getProductName())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountInfoResponse> getAllAccounts() {
        // Get all accounts from repository
        List<Account> accounts = accountRepository.findAll();
        
        // Convert each account to AccountInfoResponse without user-specific data
        return accounts.stream()
            .map(account -> AccountInfoResponse.builder()
                .id(account.getId())
                .domain(account.getDomain())
                .plan(account.getPlan())
                .status(account.getStatus())
                .industryType(account.getIndustryType())
                .email(account.getEmail())
                .companyName(account.getCompanyName())
                .currentUserId(null) // No user context for getAllAccounts
                .currentUserRole(null) // No user context for getAllAccounts
                .organizationId(account.getOrganization() != null ? account.getOrganization().getId() : null)
                .organizationName(account.getOrganization() != null ? account.getOrganization().getName() : null)
                .organizationPrimaryDomain(account.getOrganization() != null ? account.getOrganization().getPrimaryDomain() : null)
                .productCode(account.getProduct() != null ? account.getProduct().getCode() : null)
                .productName(account.getProduct() != null ? account.getProduct().getName() : null)
                .build())
            .collect(Collectors.toList());
    }
}
