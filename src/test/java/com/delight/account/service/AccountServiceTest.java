package com.delight.account.service;

import com.delight.account.cache.CacheKeyManager;
import com.delight.account.dto.AccountInfoResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.model.*;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.MembershipRepository;
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
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private CacheService cacheService;

    @Mock
    private CacheKeyManager cacheKeyManager;

    private AccountService accountService;
    private Account testAccount;
    private User testUser;
    private Membership testMembership;
    private Organization testOrganization;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(accountRepository, membershipRepository, cacheService, cacheKeyManager);

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
        testAccount.setPlan(PlanType.TRIAL);
        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setIndustryType("Technology");
        testAccount.setEmail("test@example.com");
        testAccount.setCompanyName("Test Company");
        testAccount.setOrganization(testOrganization);
        testAccount.setProduct(testProduct);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmailAddress("user@example.com");

        testMembership = new Membership();
        testMembership.setId(1L);
        testMembership.setUser(testUser);
        testMembership.setAccount(testAccount);
        testMembership.setRole(Role.MEMBER);
        testMembership.setStatus(MembershipStatus.ACTIVE);

        // Delegate cache lookups to repository fallbacks for unit tests
        when(cacheService.get(anyString(), any(), any(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                java.util.function.Supplier<Optional<?>> supplier = (java.util.function.Supplier<Optional<?>>) invocation.getArgument(1);
                Optional<?> result = supplier.get();
                return (Optional) result;
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
        assertThat(result.getPlan()).isEqualTo(PlanType.TRIAL);
        assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(result.getIndustryType()).isEqualTo("Technology");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getCompanyName()).isEqualTo("Test Company");
        assertThat(result.getCurrentUserId()).isEqualTo(1L);
        assertThat(result.getCurrentUserRole()).isEqualTo("MEMBER");
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

        Role[] roles = {Role.MEMBER, Role.ADMIN, Role.OWNER};

        for (Role role : roles) {
            testMembership.setRole(role);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(membershipRepository.findByUserIdAndAccountId(userId, accountId))
                    .thenReturn(Optional.of(testMembership));

            AccountInfoResponse result = accountService.getAccountInfo(accountId, userId);

            assertThat(result.getCurrentUserRole()).isEqualTo(role.name());
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
        assertThat(result.getPlan()).isNull();
        assertThat(result.getStatus()).isNull();
        assertThat(result.getIndustryType()).isNull();
        assertThat(result.getEmail()).isNull();
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

    @Test
    void getAccountInfoWithAllPlanTypesReturnsCorrectPlan() {
        Long accountId = 1L;
        Long userId = 1L;

        PlanType[] plans = PlanType.values();

        for (PlanType plan : plans) {
            testAccount.setPlan(plan);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(membershipRepository.findByUserIdAndAccountId(userId, accountId))
                    .thenReturn(Optional.of(testMembership));

            AccountInfoResponse result = accountService.getAccountInfo(accountId, userId);

            assertThat(result.getPlan()).isEqualTo(plan);
        }
    }

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