package com.delight.account.service.impl;

import com.delight.account.cache.AccountCacheKeyManager;
import com.delight.account.cache.CacheKeyManager;
import com.delight.account.cache.UserCacheKeyManager;
import com.delight.account.dto.AccountInfoResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.model.Membership;
import com.delight.account.model.SubscriptionStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.SubscriptionRepository;
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
    private final SubscriptionRepository subscriptionRepository;
    private final CacheService cacheService;
    private final AccountCacheKeyManager accountCacheKeyManager;
    private final UserCacheKeyManager userCacheKeyManager;
    
    @Value("${cache.account.ttl:PT30M}")
    private Duration accountCacheTtl;

    @org.springframework.beans.factory.annotation.Autowired
    public AccountServiceImpl(AccountRepository accountRepository, 
                            MembershipRepository membershipRepository,
                            SubscriptionRepository subscriptionRepository,
                            CacheService cacheService,
                            AccountCacheKeyManager accountCacheKeyManager,
                            UserCacheKeyManager userCacheKeyManager) {
        this.accountRepository = accountRepository;
        this.membershipRepository = membershipRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.cacheService = cacheService;
        this.accountCacheKeyManager = accountCacheKeyManager;
        this.userCacheKeyManager = userCacheKeyManager;
    }

    // Backward-compatible constructor for tests with unified CacheKeyManager
    public AccountServiceImpl(AccountRepository accountRepository,
                              MembershipRepository membershipRepository,
                              SubscriptionRepository subscriptionRepository,
                              CacheService cacheService,
                              CacheKeyManager cacheKeyManager) {
        this.accountRepository = accountRepository;
        this.membershipRepository = membershipRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.cacheService = cacheService;
        // Cast the unified manager to both interfaces (works since VersionedCacheKeyManager implements both)
        this.accountCacheKeyManager = (AccountCacheKeyManager) cacheKeyManager;
        this.userCacheKeyManager = (UserCacheKeyManager) cacheKeyManager;
    }

    // Backward-compatible constructor for tests without CacheKeyManager
    public AccountServiceImpl(AccountRepository accountRepository,
                              MembershipRepository membershipRepository,
                              SubscriptionRepository subscriptionRepository,
                              CacheService cacheService) {
        this.accountRepository = accountRepository;
        this.membershipRepository = membershipRepository;
        this.subscriptionRepository = subscriptionRepository;
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
            
            // Fetch planCode from subscription
            String planCode = getAccountPlanCode(accountId);
            
            AccountInfoCacheResponse response = AccountInfoCacheResponse.builder()
                .id(account.getId())
                .domain(account.getDomain())
                .plan(planCode)
                .status(account.getStatus())
                .industryType(account.getIndustryType())
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
            Membership membership = membershipRepository.findByUserIdAndAccountIdWithRole(userId, accountId).orElse(null);
            UserMembershipCacheResponse response = UserMembershipCacheResponse.builder()
                .userId(userId)
                .role(membership != null && membership.getRole() != null ? membership.getRole().getCode() : null)
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
            
            // Fetch planCode from subscription
            String planCode = getAccountPlanCode(accountId);
            
            AccountInfoCacheResponse response = AccountInfoCacheResponse.builder()
                .id(account.getId())
                .domain(account.getDomain())
                .plan(planCode)
                .status(account.getStatus())
                .industryType(account.getIndustryType())
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
            .map(account -> {
                // Fetch planCode from subscription
                String planCode = getAccountPlanCode(account.getId());
                return AccountInfoResponse.builder()
                    .id(account.getId())
                    .domain(account.getDomain())
                    .plan(planCode)
                    .status(account.getStatus())
                    .industryType(account.getIndustryType())
                    .companyName(account.getCompanyName())
                    .currentUserId(null) // No user context for getAllAccounts
                    .currentUserRole(null) // No user context for getAllAccounts
                    .organizationId(account.getOrganization() != null ? account.getOrganization().getId() : null)
                    .organizationName(account.getOrganization() != null ? account.getOrganization().getName() : null)
                    .organizationPrimaryDomain(account.getOrganization() != null ? account.getOrganization().getPrimaryDomain() : null)
                    .productCode(account.getProduct() != null ? account.getProduct().getCode() : null)
                    .productName(account.getProduct() != null ? account.getProduct().getName() : null)
                    .build();
            })
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isAccountActive(Long accountId) {
        // Check account status
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ApiException("error.account.notfound"));
        
        if (account.getStatus() != AccountStatus.ACTIVE) {
            return false;
        }
        
        // Check subscription status for primary product
        if (account.getProduct() == null || account.getProduct().getCode() == null) {
            return false;
        }
        
        String productCode = account.getProduct().getCode();
        java.util.List<SubscriptionStatus> activeStatuses = java.util.Arrays.asList(
            SubscriptionStatus.TRIAL, 
            SubscriptionStatus.ACTIVE
        );
        
        Optional<com.delight.account.model.Subscription> subscriptionOpt = 
            subscriptionRepository.findActiveByAccountIdAndProductCode(accountId, productCode, activeStatuses);
        
        return subscriptionOpt.isPresent();
    }
    
    /**
     * Helper method to get planCode from active subscription for an account
     * @param accountId the account ID
     * @return planCode if found, null otherwise
     */
    @Transactional(readOnly = true)
    public String getAccountPlanCode(Long accountId) {
        java.util.List<SubscriptionStatus> activeStatuses = java.util.Arrays.asList(
            SubscriptionStatus.TRIAL, 
            SubscriptionStatus.ACTIVE
        );
        
        return subscriptionRepository.findPlanCodeByAccountId(accountId, activeStatuses)
            .orElse(null);
    }
}
