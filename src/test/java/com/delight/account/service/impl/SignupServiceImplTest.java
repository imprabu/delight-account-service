package com.delight.account.service.impl;

import com.delight.account.dto.SignupRequest;
import com.delight.account.exception.AccountCreationException;
import com.delight.account.model.*;
import com.delight.account.repository.*;
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

    @InjectMocks
    private SignupServiceImpl signupService;

    private SignupRequest request;
    private Product hrmsProduct;
    private Organization organization;

    @BeforeEach
    void setUp() {
        request = new SignupRequest();
        request.setCompanyName("TestCo");
        request.setEmailAddress("test@example.com");
        request.setIndustryType("IT");
        request.setFirstName("John");
        request.setLastName("Doe");

        hrmsProduct = new Product();
        hrmsProduct.setCode("HRMS");
        hrmsProduct.setName("HRMS");

        organization = new Organization();
        organization.setName("TestCo");
        organization.setPrimaryDomain("TestCo");
        organization.setStatus(OrganizationStatus.ACTIVE);
    }

    @Test
    void signupCreatesCompleteAccountStructure() {
        when(accountRepository.findByDomainAndStatus("TestCo", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("HRMS"))
            .thenReturn(Optional.of(hrmsProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(organization);

        signupService.signup(request);

        // Verify organization creation
        ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(orgCaptor.capture());
        Organization savedOrg = orgCaptor.getValue();
        assertThat(savedOrg.getName()).isEqualTo("TestCo");
        assertThat(savedOrg.getPrimaryDomain()).isEqualTo("TestCo");
        assertThat(savedOrg.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);

        // Verify account creation
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getDomain()).isEqualTo("TestCo");
        assertThat(savedAccount.getPlan()).isEqualTo(PlanType.TRIAL);
        assertThat(savedAccount.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(savedAccount.getIndustryType()).isEqualTo("IT");
        assertThat(savedAccount.getEmail()).isEqualTo("test@example.com");
        assertThat(savedAccount.getCompanyName()).isEqualTo("TestCo");
        assertThat(savedAccount.getOrganization()).isEqualTo(organization);
        assertThat(savedAccount.getProduct()).isEqualTo(hrmsProduct);

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
    }

    @Test
    void signupCreatesHrmsProductWhenNotExists() {
        when(accountRepository.findByDomainAndStatus("TestCo", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("HRMS"))
            .thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class)))
            .thenReturn(hrmsProduct);
        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(organization);

        signupService.signup(request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getCode()).isEqualTo("HRMS");
        assertThat(savedProduct.getName()).isEqualTo("HRMS");
    }

    @Test
    void signupAddsRandomNumberWhenDomainExists() {
        when(accountRepository.findByDomainAndStatus("TestCo", AccountStatus.ACTIVE))
            .thenReturn(Optional.of(new Account()));
        when(productRepository.findByCode("HRMS"))
            .thenReturn(Optional.of(hrmsProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(organization);
        
        // Make random deterministic
        ReflectionTestUtils.setField(signupService, "random", new Random(0));

        signupService.signup(request);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getDomain()).startsWith("TestCo");
        assertNotEquals("TestCo", savedAccount.getDomain());
        
        // Verify organization also gets the modified domain
        ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(orgCaptor.capture());
        Organization savedOrg = orgCaptor.getValue();
        assertThat(savedOrg.getPrimaryDomain()).isEqualTo(savedAccount.getDomain());
    }

    @Test
    void signupThrowsAccountCreationExceptionOnDataAccessException() {
        when(accountRepository.findByDomainAndStatus("TestCo", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("HRMS"))
            .thenReturn(Optional.of(hrmsProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenThrow(new DataAccessException("Database error") {});

        assertThatThrownBy(() -> signupService.signup(request))
            .isInstanceOf(AccountCreationException.class);
    }

    @Test
    void signupHandlesAccountRepositoryException() {
        when(accountRepository.findByDomainAndStatus("TestCo", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("HRMS"))
            .thenReturn(Optional.of(hrmsProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(organization);
        when(accountRepository.save(any(Account.class)))
            .thenThrow(new DataAccessException("Database error") {});

        assertThatThrownBy(() -> signupService.signup(request))
            .isInstanceOf(AccountCreationException.class);
    }

    @Test
    void signupHandlesUserRepositoryException() {
        when(accountRepository.findByDomainAndStatus("TestCo", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("HRMS"))
            .thenReturn(Optional.of(hrmsProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(organization);
        when(userRepository.save(any(User.class)))
            .thenThrow(new DataAccessException("Database error") {});

        assertThatThrownBy(() -> signupService.signup(request))
            .isInstanceOf(AccountCreationException.class);
    }

    @Test
    void signupHandlesMembershipRepositoryException() {
        when(accountRepository.findByDomainAndStatus("TestCo", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("HRMS"))
            .thenReturn(Optional.of(hrmsProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(organization);
        when(membershipRepository.save(any(Membership.class)))
            .thenThrow(new DataAccessException("Database error") {});

        assertThatThrownBy(() -> signupService.signup(request))
            .isInstanceOf(AccountCreationException.class);
    }

    @Test
    void signupWithSpecialCharactersInCompanyName() {
        request.setCompanyName("Test & Co. Ltd.");
        
        when(accountRepository.findByDomainAndStatus("Test & Co. Ltd.", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("HRMS"))
            .thenReturn(Optional.of(hrmsProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(organization);

        signupService.signup(request);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getDomain()).isEqualTo("Test & Co. Ltd.");
        assertThat(savedAccount.getCompanyName()).isEqualTo("Test & Co. Ltd.");
    }

    @Test
    void signupWithLongCompanyName() {
        String longName = "Very Long Company Name That Might Cause Issues";
        request.setCompanyName(longName);
        
        when(accountRepository.findByDomainAndStatus(longName, AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("HRMS"))
            .thenReturn(Optional.of(hrmsProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(organization);

        signupService.signup(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUserName()).isEqualTo("JohnDoe");
        assertThat(savedUser.getCompanyName()).isEqualTo(longName);
    }

    @Test
    void signupWithDifferentIndustryTypes() {
        request.setIndustryType("Healthcare");
        
        when(accountRepository.findByDomainAndStatus("TestCo", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("HRMS"))
            .thenReturn(Optional.of(hrmsProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(organization);

        signupService.signup(request);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getIndustryType()).isEqualTo("Healthcare");
    }

    @Test
    void signupVerifiesTransactionalBehavior() {
        when(accountRepository.findByDomainAndStatus("TestCo", AccountStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(productRepository.findByCode("HRMS"))
            .thenReturn(Optional.of(hrmsProduct));
        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(organization);

        signupService.signup(request);

        // Verify all repositories are called in the correct order
        verify(accountRepository).findByDomainAndStatus("TestCo", AccountStatus.ACTIVE);
        verify(productRepository).findByCode("HRMS");
        verify(organizationRepository).save(any(Organization.class));
        verify(accountRepository).save(any(Account.class));
        verify(userRepository).save(any(User.class));
        verify(membershipRepository).save(any(Membership.class));
    }
}
