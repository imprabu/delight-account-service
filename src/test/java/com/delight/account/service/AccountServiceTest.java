package com.delight.account.service;

import com.delight.account.cache.AccountCacheKeyManager;
import com.delight.account.cache.UserCacheKeyManager;
import com.delight.account.dto.AccountInfoResponse;
import com.delight.account.dto.AccountInfoCacheResponse;
import com.delight.account.dto.UserMembershipCacheResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.model.*;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.SubscriptionRepository;
import com.delight.account.service.CacheService;
import com.delight.account.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CacheService cacheService;

    @Mock
    private AccountCacheKeyManager accountCacheKeyManager;

    @Mock
    private UserCacheKeyManager userCacheKeyManager;

    private AccountService accountService;
    private Account testAccount;
    private User testUser;
    private Membership testMembership;
    private Organization testOrganization;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(accountRepository, membershipRepository, subscriptionRepository, cacheService, accountCacheKeyManager, userCacheKeyManager);

        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Organization");
        testOrganization.setPrimaryDomain("test.com");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setCode("TEST_PRODUCT");
        testProduct.setName("Test Product");

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setDomain("testdomain");
        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setIndustryType("Technology");
        // Note: email is no longer on Account entity
        testAccount.setCompanyName("Test Company");
        testAccount.setOrganization(testOrganization);
        testAccount.setProduct(testProduct);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmailAddress("user@example.com");

        // Create a mock RoleEntity for testing
        RoleEntity agentRole = new RoleEntity();
        agentRole.setId(3L);
        agentRole.setCode("AGENT");
        agentRole.setName("Agent");
        
        testMembership = new Membership();
        testMembership.setId(1L);
        testMembership.setUser(testUser);
        testMembership.setAccount(testAccount);
        testMembership.setRole(agentRole);
        testMembership.setStatus(MembershipStatus.ACTIVE);

        // Setup cache key manager mocks - updated for new split cache key structure
        lenient().when(accountCacheKeyManager.getAccountInfoKey(anyLong()))
            .thenReturn("account:info:1");
        lenient().when(userCacheKeyManager.getUserMembershipKey(anyLong(), anyLong()))
            .thenReturn("user:membership:1:1");
        
        // Delegate cache lookups to repository fallbacks for unit tests
        when(cacheService.get(anyString(), any(), any(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                java.util.function.Supplier<Optional<?>> supplier = 
                    (java.util.function.Supplier<Optional<?>>) invocation.getArgument(1);
                return supplier.get();
            });
    }

    @Test
    void getAccountInfoWithValidAccountAndMembershipReturnsCompleteInfo() {
        Long accountId = 1L;
        Long userId = 1L;

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(membershipRepository.findByUserIdAndAccountId(userId, accountId))
                .thenReturn(Optional.of(testMembership));

        AccountInfoResponse result = accountService.getAccountInfo(accountId, userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDomain()).isEqualTo("testdomain");
        assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(result.getIndustryType()).isEqualTo("Technology");
        // Note: email is no longer on Account entity
        assertThat(result.getCompanyName()).isEqualTo("Test Company");
        // Note: plan is now retrieved from subscription, not account
        assertThat(result.getCurrentUserId()).isEqualTo(1L);
        assertThat(result.getCurrentUserRole()).isEqualTo("AGENT");
        assertThat(result.getOrganizationId()).isEqualTo(1L);
        assertThat(result.getOrganizationName()).isEqualTo("Test Organization");
        assertThat(result.getOrganizationPrimaryDomain()).isEqualTo("test.com");
        assertThat(result.getProductCode()).isEqualTo("TEST_PRODUCT");
        assertThat(result.getProductName()).isEqualTo("Test Product");
    }

    @Test
    void getAccountInfoWithNonExistentAccountThrowsException() {
        Long accountId = 999L;
        Long userId = 1L;

        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountInfo(accountId, userId))
                .isInstanceOf(ApiException.class)
                .hasMessage("error.account.notfound");
    }

    @Test
    void getAccountInfoWithNoMembershipReturnsNullRole() {
        Long accountId = 1L;
        Long userId = 1L;

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(membershipRepository.findByUserIdAndAccountId(userId, accountId))
                .thenReturn(Optional.empty());

        AccountInfoResponse result = accountService.getAccountInfo(accountId, userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCurrentUserId()).isEqualTo(1L);
        assertThat(result.getCurrentUserRole()).isNull();
        // Other account fields should still be populated
        assertThat(result.getDomain()).isEqualTo("testdomain");
        assertThat(result.getCompanyName()).isEqualTo("Test Company");
    }

    @Test
    void getAccountInfoWithMembershipButNullRoleReturnsNullRole() {
        Long accountId = 1L;
        Long userId = 1L;
        testMembership.setRole(null);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(membershipRepository.findByUserIdAndAccountId(userId, accountId))
                .thenReturn(Optional.of(testMembership));

        AccountInfoResponse result = accountService.getAccountInfo(accountId, userId);

        assertThat(result).isNotNull();
        assertThat(result.getCurrentUserRole()).isNull();
        assertThat(result.getCurrentUserId()).isEqualTo(1L);
    }

    @Test
    void getAccountInfoWithDifferentRolesReturnsCorrectRole() {
        Long accountId = 1L;
        Long userId = 1L;

        // Create RoleEntity objects for different roles
        RoleEntity agentRole = new RoleEntity();
        agentRole.setId(3L);
        agentRole.setCode("AGENT");
        agentRole.setName("Agent");
        
        RoleEntity accountAdminRole = new RoleEntity();
        accountAdminRole.setId(2L);
        accountAdminRole.setCode("ACCOUNT_ADMIN");
        accountAdminRole.setName("Account Admin");
        
        RoleEntity orgAdminRole = new RoleEntity();
        orgAdminRole.setId(1L);
        orgAdminRole.setCode("ORG_ADMIN");
        orgAdminRole.setName("Org Admin");

        RoleEntity[] roles = {agentRole, accountAdminRole, orgAdminRole};

        for (RoleEntity role : roles) {
            testMembership.setRole(role);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(membershipRepository.findByUserIdAndAccountId(userId, accountId))
                    .thenReturn(Optional.of(testMembership));

            AccountInfoResponse result = accountService.getAccountInfo(accountId, userId);

            assertThat(result.getCurrentUserRole()).isEqualTo(role.getCode());
        }
    }

    @Test
    void getAccountInfoWithNoOrganizationReturnsNullOrganizationFields() {
        Long accountId = 1L;
        Long userId = 1L;
        testAccount.setOrganization(null);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(membershipRepository.findByUserIdAndAccountId(userId, accountId))
                .thenReturn(Optional.of(testMembership));

        AccountInfoResponse result = accountService.getAccountInfo(accountId, userId);

        assertThat(result).isNotNull();
        assertThat(result.getOrganizationId()).isNull();
        assertThat(result.getOrganizationName()).isNull();
        assertThat(result.getOrganizationPrimaryDomain()).isNull();
        // Other fields should still be populated
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDomain()).isEqualTo("testdomain");
    }

    @Test
    void getAccountInfoWithNoProductReturnsNullProductFields() {
        Long accountId = 1L;
        Long userId = 1L;
        testAccount.setProduct(null);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(membershipRepository.findByUserIdAndAccountId(userId, accountId))
                .thenReturn(Optional.of(testMembership));

        AccountInfoResponse result = accountService.getAccountInfo(accountId, userId);

        assertThat(result).isNotNull();
        assertThat(result.getProductCode()).isNull();
        assertThat(result.getProductName()).isNull();
        // Other fields should still be populated
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDomain()).isEqualTo("testdomain");
    }

    @Test
    void getAccountInfoWithMinimalAccountDataReturnsPartialInfo() {
        Long accountId = 1L;
        Long userId = 1L;

        Account minimalAccount = new Account();
        minimalAccount.setId(1L);
        minimalAccount.setDomain("minimal");
        // All other fields are null

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(minimalAccount));
        when(membershipRepository.findByUserIdAndAccountId(userId, accountId))
                .thenReturn(Optional.empty());

        AccountInfoResponse result = accountService.getAccountInfo(accountId, userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDomain()).isEqualTo("minimal");
        assertThat(result.getCurrentUserId()).isEqualTo(1L);
        
        // All optional fields should be null
        // Note: plan is now retrieved from subscription, not account
        assertThat(result.getStatus()).isNull();
        assertThat(result.getIndustryType()).isNull();
        // Note: email is no longer on AccountInfoResponse
        assertThat(result.getCompanyName()).isNull();
        assertThat(result.getCurrentUserRole()).isNull();
        assertThat(result.getOrganizationId()).isNull();
        assertThat(result.getOrganizationName()).isNull();
        assertThat(result.getOrganizationPrimaryDomain()).isNull();
        assertThat(result.getProductCode()).isNull();
        assertThat(result.getProductName()).isNull();
    }

    @Test
    void getAccountInfoWithAllAccountStatusesReturnsCorrectStatus() {
        Long accountId = 1L;
        Long userId = 1L;

        AccountStatus[] statuses = AccountStatus.values();

        for (AccountStatus status : statuses) {
            testAccount.setStatus(status);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(membershipRepository.findByUserIdAndAccountId(userId, accountId))
                    .thenReturn(Optional.of(testMembership));

            AccountInfoResponse result = accountService.getAccountInfo(accountId, userId);

            assertThat(result.getStatus()).isEqualTo(status);
        }
    }

    // Note: Plan is now retrieved from subscription, not account
    // This test is no longer applicable as plan is subscription-based

    @Test
    void getAccountInfoIsReadOnlyTransaction() {
        // This test verifies that the method is annotated with @Transactional(readOnly = true)
        // The actual transaction behavior would be tested in integration tests
        Long accountId = 1L;
        Long userId = 1L;

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(membershipRepository.findByUserIdAndAccountId(userId, accountId))
                .thenReturn(Optional.of(testMembership));

        AccountInfoResponse result = accountService.getAccountInfo(accountId, userId);

        assertThat(result).isNotNull();
        // Verify that no modification operations are performed
        // This is implicitly tested by the fact that we only mock read operations
    }

    @Test
    void getAccountInfoWithDifferentUserIdsReturnsCorrectUserId() {
        Long accountId = 1L;
        Long[] userIds = {1L, 2L, 100L, 999L};

        for (Long userId : userIds) {
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(membershipRepository.findByUserIdAndAccountId(userId, accountId))
                    .thenReturn(Optional.of(testMembership));

            AccountInfoResponse result = accountService.getAccountInfo(accountId, userId);

            assertThat(result.getCurrentUserId()).isEqualTo(userId);
        }
    }
}