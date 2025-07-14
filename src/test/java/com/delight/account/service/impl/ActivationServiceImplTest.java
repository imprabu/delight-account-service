package com.delight.account.service.impl;

import com.delight.account.dto.ActivationRequest;
import com.delight.account.model.*;
import com.delight.account.repository.UserCredentialRepository;
import com.delight.account.repository.UserRepository;
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

    @InjectMocks
    private ActivationServiceImpl activationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(activationService, "secretKey", "1234567890123456");
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
        existingUser.setAccount(account);
        existingUser.setStatus(UserStatus.INSERTED);

        UserCredential credential = new UserCredential();
        credential.setUser(existingUser);
        credential.setAccount(account);

        when(userRepository.findByAccountId(accountId)).thenReturn(Optional.of(existingUser));
        when(credentialRepository.findByUser(existingUser)).thenReturn(Optional.of(credential));

        activationService.activateUser(accountId, request, "US");

        verify(userRepository).save(existingUser);
        assertThat(existingUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(existingUser.getFirstName()).isEqualTo("John");
        assertThat(existingUser.getCompanyName()).isEqualTo("US");
        assertThat(existingUser.getPhoneNumber()).isEqualTo("123");

        verify(credentialRepository).save(credential);
        assertThat(credential.getPasswordHash()).isNotNull();
    }
}
