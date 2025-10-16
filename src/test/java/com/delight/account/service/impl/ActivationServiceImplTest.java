package com.delight.account.service.impl;

import com.delight.account.cache.CacheKeyManager;
import com.delight.account.dto.ActivationRequest;
import com.delight.account.model.*;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.UserCredentialRepository;
import com.delight.account.repository.UserRepository;
import com.delight.account.service.CacheService;
import com.delight.account.util.PasswordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository credentialRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private CacheService cacheService;

    @Mock
    private CacheKeyManager cacheKeyManager;

    @Mock
    private PasswordValidator passwordValidator;

    @InjectMocks
    private ActivationServiceImpl activationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(activationService, "passwordSecret", "1234567890123456");
        // Mock password validator to return true for valid passwords
        when(passwordValidator.isValid(anyString())).thenReturn(true);
    }

    @Test
    void activateUserUpdatesExistingUser() {
        ActivationRequest request = new ActivationRequest();
        request.setEmailAddress("john@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPhoneNumber("123");
        request.setPassword("pass");
        request.setRetypePassword("pass");

        Long accountId = 1L;
        Account account = new Account();
        account.setId(accountId);

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setAccount(account);
        existingUser.setStatus(UserStatus.INSERTED);

        UserCredential credential = new UserCredential();
        credential.setUser(existingUser);
        credential.setAccount(account);

        Membership membership = new Membership();
        membership.setUser(existingUser);
        membership.setAccount(account);
        membership.setRole(Role.OWNER);
        membership.setStatus(MembershipStatus.ACTIVE);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(userRepository.findByAccountIdAndEmailAddress(accountId, "john@example.com"))
            .thenReturn(Optional.of(existingUser));
        when(credentialRepository.findByUser(existingUser)).thenReturn(Optional.of(credential));
        when(membershipRepository.findByUserIdAndAccountId(existingUser.getId(), accountId))
            .thenReturn(Optional.of(membership));
        when(cacheKeyManager.getUserAccountKey(accountId, "john@example.com"))
            .thenReturn("user:account:1:email:john@example.com");
        when(cacheKeyManager.getAccountInfoKey(accountId, existingUser.getId()))
            .thenReturn("account:info:1:user:1");

        activationService.activateUser(accountId, request, "US");

        verify(userRepository).save(existingUser);
        assertThat(existingUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(existingUser.getFirstName()).isEqualTo("John");
        assertThat(existingUser.getCompanyName()).isEqualTo("US");
        assertThat(existingUser.getPhoneNumber()).isEqualTo("123");

        verify(credentialRepository).save(credential);
        assertThat(credential.getPasswordHash()).isNotNull();
        
        verify(membershipRepository).save(membership);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);

        // Verify cache invalidation for user and account info
        verify(cacheKeyManager).getUserAccountKey(accountId, "john@example.com");
        verify(cacheKeyManager).getAccountInfoKey(accountId, existingUser.getId());
        verify(cacheService, times(2)).evict(anyString());
    }
}
