package com.delight.account.service.impl;

import com.delight.account.dto.SignupRequest;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.model.PlanType;
import com.delight.account.model.User;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.UserRepository;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignupServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SignupServiceImpl signupService;

    private SignupRequest request;

    @BeforeEach
    void setUp() {
        request = new SignupRequest();
        request.setCompanyName("TestCo");
        request.setEmailAddress("test@example.com");
        request.setIndustryType("IT");
        request.setFirstName("John");
        request.setLastName("Doe");
    }

    @Test
    void signupCreatesAccountAndUser() {
        when(accountRepository.findByDomainAndStatus("TestCo", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());

        signupService.signup(request);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getDomain()).isEqualTo("TestCo");
        assertThat(savedAccount.getPlan()).isEqualTo(PlanType.TRIAL);
        assertThat(savedAccount.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(savedAccount.getIndustryType()).isEqualTo("IT");
        assertThat(savedAccount.getEmail()).isEqualTo("test@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getAccount()).isEqualTo(savedAccount);
        assertThat(savedUser.getUserName()).isEqualTo("JohnDoe");
        assertThat(savedUser.getEmailAddress()).isEqualTo("test@example.com");
        assertThat(savedUser.getFirstName()).isEqualTo("John");
        assertThat(savedUser.getLastName()).isEqualTo("Doe");
        assertThat(savedUser.getCompanyName()).isEqualTo("TestCo");
    }

    @Test
    void signupAddsRandomNumberWhenDomainExists() {
        when(accountRepository.findByDomainAndStatus("TestCo", AccountStatus.ACTIVE))
            .thenReturn(Optional.of(new Account()));
        // make random deterministic
        ReflectionTestUtils.setField(signupService, "random", new Random(0));

        signupService.signup(request);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getDomain()).startsWith("TestCo");
        assertNotEquals("TestCo", savedAccount.getDomain());
    }
}
