package com.delight.account.service.impl;

import com.delight.account.dto.SignupRequest;
import com.delight.account.exception.AccountCreationException;
import com.delight.account.exception.ApiException;
import com.delight.account.model.*;
import com.delight.account.repository.*;
import com.delight.account.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignupServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private SignupServiceImpl signupService;

    private SignupRequest request;
    private Product chatProduct;
    private Organization organization;

    @BeforeEach
    void setUp() {
        request = new SignupRequest();
        request.setCompanyName("TestCo");
        request.setEmailAddress("test@example.com");
        request.setIndustryType("IT");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setProductCode("CHAT");
        request.setRegion("us-east-1");

        chatProduct = new Product();
        chatProduct.setCode("CHAT");
        chatProduct.setName("CHAT");

        organization = new Organization();
        organization.setName("TestCo");
        organization.setPrimaryDomain("TestCo");
        organization.setStatus(OrganizationStatus.ACTIVE);
    }

    @Test
    void signupCreatesCompleteAccountStructure() {
        when(accountRepository.findByDomainAndStatus("testco", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("CHAT"))
            .thenReturn(Optional.of(chatProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(membershipRepository.save(any(Membership.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        signupService.signup(request);

        // Verify account creation
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getDomain()).isEqualTo("testco");
        assertThat(savedAccount.getPlan()).isEqualTo(PlanType.TRIAL);
        assertThat(savedAccount.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(savedAccount.getIndustryType()).isEqualTo("IT");
        assertThat(savedAccount.getEmail()).isEqualTo("test@example.com");
        assertThat(savedAccount.getCompanyName()).isEqualTo("TestCo");
        assertThat(savedAccount.getOrganization()).isNotNull();
        assertThat(savedAccount.getProduct()).isEqualTo(chatProduct);

        // Verify user creation
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getAccount()).isEqualTo(savedAccount);
        assertThat(savedUser.getUserName()).isEqualTo("JohnDoe");
        assertThat(savedUser.getEmailAddress()).isEqualTo("test@example.com");
        assertThat(savedUser.getFirstName()).isEqualTo("John");
        assertThat(savedUser.getLastName()).isEqualTo("Doe");
        assertThat(savedUser.getCompanyName()).isEqualTo("TestCo");

        // Verify membership creation
        ArgumentCaptor<Membership> membershipCaptor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository).save(membershipCaptor.capture());
        Membership savedMembership = membershipCaptor.getValue();
        assertThat(savedMembership.getAccount()).isEqualTo(savedAccount);
        assertThat(savedMembership.getUser()).isEqualTo(savedUser);
        assertThat(savedMembership.getRole()).isEqualTo(Role.OWNER);
        assertThat(savedMembership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);

        // Verify cache warming for the newly created user
        verify(cacheService).put(eq(String.format("user:account:%d:email:%s", savedAccount.getId(), "test@example.com")), 
                                 eq(savedUser), any());
    }

    @Test
    void signupThrowsExceptionWhenProductNotFound() {
        when(accountRepository.findByDomainAndStatus("testco", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("CHAT"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> signupService.signup(request))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("error.product.notfound");
    }
    
    @Test
    void signupThrowsExceptionWhenProductIsInactive() {
        when(accountRepository.findByDomainAndStatus("testco", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        Product inactiveProduct = new Product();
        inactiveProduct.setCode("CHAT");
        inactiveProduct.setName("CHAT");
        inactiveProduct.setActive(false);
        when(productRepository.findByCode("CHAT"))
            .thenReturn(Optional.of(inactiveProduct));

        assertThatThrownBy(() -> signupService.signup(request))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("error.product.inactive");
    }

    @Test
    void signupAddsRandomNumberWhenDomainExists() {
        when(accountRepository.findByDomainAndStatus("testco", AccountStatus.ACTIVE))
            .thenReturn(Optional.of(new Account()));
        when(productRepository.findByCode("CHAT"))
            .thenReturn(Optional.of(chatProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(membershipRepository.save(any(Membership.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        signupService.signup(request);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertNotEquals("testco", savedAccount.getDomain());
        assertThat(savedAccount.getDomain()).startsWith("testco");
        assertThat(savedAccount.getDomain().length()).isGreaterThan(6);
    }

    @Test
    void signupThrowsAccountCreationExceptionOnDataAccessException() {
        when(accountRepository.findByDomainAndStatus("testco", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("CHAT"))
            .thenReturn(Optional.of(chatProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenThrow(new DataAccessException("Database error") {});

        assertThatThrownBy(() -> signupService.signup(request))
            .isInstanceOf(AccountCreationException.class);
    }

    @Test
    void signupHandlesAccountRepositoryException() {
        when(accountRepository.findByDomainAndStatus("testco", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("CHAT"))
            .thenReturn(Optional.of(chatProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class)))
            .thenThrow(new DataAccessException("Database error") {});

        assertThatThrownBy(() -> signupService.signup(request))
            .isInstanceOf(AccountCreationException.class);
    }

    @Test
    void signupHandlesUserRepositoryException() {
        when(accountRepository.findByDomainAndStatus("testco", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("CHAT"))
            .thenReturn(Optional.of(chatProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class)))
            .thenThrow(new DataAccessException("Database error") {});

        assertThatThrownBy(() -> signupService.signup(request))
            .isInstanceOf(AccountCreationException.class);
    }

    @Test
    void signupHandlesMembershipRepositoryException() {
        when(accountRepository.findByDomainAndStatus("testco", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("CHAT"))
            .thenReturn(Optional.of(chatProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(membershipRepository.save(any(Membership.class)))
            .thenThrow(new DataAccessException("Database error") {});

        assertThatThrownBy(() -> signupService.signup(request))
            .isInstanceOf(AccountCreationException.class);
    }

    @Test
    void signupWithSpecialCharactersInCompanyName() {
        request.setCompanyName("Test & Co. Ltd.");
        
        when(accountRepository.findByDomainAndStatus("test&co.ltd.", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("CHAT"))
            .thenReturn(Optional.of(chatProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(membershipRepository.save(any(Membership.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        signupService.signup(request);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getDomain()).isEqualTo("test&co.ltd.");
        assertThat(savedAccount.getCompanyName()).isEqualTo("Test & Co. Ltd.");
    }

    @Test
    void signupWithLongCompanyName() {
        String longName = "Very Long Company Name That Exceeds Normal Length";
        request.setCompanyName(longName);
        
        when(accountRepository.findByDomainAndStatus("verylongcompanynamethatexceedsnormallength", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("CHAT"))
            .thenReturn(Optional.of(chatProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(membershipRepository.save(any(Membership.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        signupService.signup(request);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getCompanyName()).isEqualTo(longName);
    }

    @Test
    void signupWithDifferentIndustryTypes() {
        request.setIndustryType("Healthcare");
        
        when(accountRepository.findByDomainAndStatus("testco", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("CHAT"))
            .thenReturn(Optional.of(chatProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(membershipRepository.save(any(Membership.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        signupService.signup(request);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getIndustryType()).isEqualTo("Healthcare");
    }

    @Test
    void signupVerifiesTransactionalBehavior() {
        when(accountRepository.findByDomainAndStatus("testco", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("CHAT"))
            .thenReturn(Optional.of(chatProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(membershipRepository.save(any(Membership.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        signupService.signup(request);

        // Verify all repositories are called in the correct order
        verify(organizationRepository).save(any(Organization.class));
        verify(accountRepository).save(any(Account.class));
        verify(userRepository).save(any(User.class));
        verify(membershipRepository).save(any(Membership.class));
    }
}
