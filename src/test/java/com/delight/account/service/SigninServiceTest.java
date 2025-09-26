package com.delight.account.service;

import com.delight.account.dto.SigninRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.model.Membership;
import com.delight.account.model.Role;
import com.delight.account.model.MembershipStatus;
import com.delight.account.model.Organization;
import com.delight.account.model.PlanType;
import com.delight.account.model.Product;
import com.delight.account.model.User;
import com.delight.account.model.UserCredential;
import com.delight.account.model.UserStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.UserCredentialRepository;
import com.delight.account.repository.UserRepository;
import com.delight.account.service.impl.SigninServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SigninServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository credentialRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private AccountRepository accountRepository;

    private SigninService signinService;

    private static final String PASSWORD_SECRET = "1234567890123456";
    private static final String JWT_SECRET = "mySecretKey12345mySecretKey12345";
    private static final long JWT_EXPIRATION = 86400000L; // 24 hours

    private User testUser;
    private UserCredential testCredential;
    private Membership testMembership;
    private Account testAccount;
    private Organization testOrganization;
    private Product testProduct;

    @BeforeEach
    void setUp() throws Exception {
        signinService = new SigninServiceImpl(
            userRepository,
            credentialRepository,
            PASSWORD_SECRET,
            JWT_SECRET,
            JWT_EXPIRATION,
            membershipRepository,
            accountRepository
        );

        // Setup test data
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setDomain("localhost");
        testAccount.setPlan(PlanType.TRIAL);
        testAccount.setStatus(AccountStatus.ACTIVE);

        testOrganization = new Organization();
        testOrganization.setId(100L);
        testOrganization.setName("Test Org");

        testProduct = new Product();
        testProduct.setId(200L);
        testProduct.setCode("TEST_PRODUCT");

        testAccount.setOrganization(testOrganization);
        testAccount.setProduct(testProduct);

        testUser = new User();
        testUser.setId(1L);
        testUser.setAccount(testAccount);
        testUser.setEmailAddress("test@example.com");
        testUser.setStatus(UserStatus.ACTIVE);

        testCredential = new UserCredential();
        testCredential.setId(1L);
        testCredential.setAccount(testAccount);
        testCredential.setUser(testUser);
        testCredential.setPasswordHash(encrypt("password123"));

        testMembership = new Membership();
        testMembership.setId(1L);
        testMembership.setUser(testUser);
        testMembership.setAccount(testAccount);
        testMembership.setRole(Role.OWNER);
        testMembership.setStatus(MembershipStatus.ACTIVE);
    }

    @Test
    void signinSuccessfullyGeneratesJwtToken() {
        // Given
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByAccountIdAndEmailAddress(1L, "test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(credentialRepository.findByUser(testUser))
            .thenReturn(Optional.of(testCredential));
        when(membershipRepository.findByUserIdAndAccountId(1L, 1L))
            .thenReturn(Optional.of(testMembership));
        when(accountRepository.findById(1L))
            .thenReturn(Optional.of(testAccount));

        // When
        String token = signinService.signin(1L, "localhost", request);

        // Then
        assertThat(token).isNotNull().isNotEmpty();

        // Verify JWT claims
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload();

        assertThat(claims.getSubject()).isEqualTo("test@example.com");
        assertThat(claims.get("accountId", Long.class)).isEqualTo(1L);
        assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
        assertThat(claims.get("role", String.class)).isEqualTo("OWNER");
        assertThat(claims.get("orgId", Long.class)).isEqualTo(100L);
        assertThat(claims.get("productCode", String.class)).isEqualTo("TEST_PRODUCT");
        assertThat(claims.getIssuedAt()).isBeforeOrEqualTo(new Date());
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    void signinWithoutOrganizationAndProductGeneratesTokenWithNullValues() {
        // Given
        testAccount.setOrganization(null);
        testAccount.setProduct(null);

        SigninRequest request = new SigninRequest();
        request.setEmailAddress("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByAccountIdAndEmailAddress(1L, "test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(credentialRepository.findByUser(testUser))
            .thenReturn(Optional.of(testCredential));
        when(membershipRepository.findByUserIdAndAccountId(1L, 1L))
            .thenReturn(Optional.of(testMembership));
        when(accountRepository.findById(1L))
            .thenReturn(Optional.of(testAccount));

        // When
        String token = signinService.signin(1L, "localhost", request);

        // Then
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload();

        assertThat(claims.get("orgId")).isNull();
        assertThat(claims.get("productCode")).isNull();
    }

    @Test
    void signinThrowsExceptionWhenUserNotFound() {
        // Given
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("nonexistent@example.com");
        request.setPassword("password123");

        when(userRepository.findByAccountIdAndEmailAddress(1L, "nonexistent@example.com"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> signinService.signin(1L, "localhost", request))
            .isInstanceOf(ApiException.class)
            .hasMessage("error.user.notfound");
    }

    @Test
    void signinThrowsExceptionWhenCredentialNotFound() {
        // Given
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByAccountIdAndEmailAddress(1L, "test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(credentialRepository.findByUser(testUser))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> signinService.signin(1L, "localhost", request))
            .isInstanceOf(ApiException.class)
            .hasMessage("error.signin.invalid");
    }

    @Test
    void signinThrowsExceptionWhenPasswordIsIncorrect() {
        // Given
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("test@example.com");
        request.setPassword("wrongpassword");

        when(userRepository.findByAccountIdAndEmailAddress(1L, "test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(credentialRepository.findByUser(testUser))
            .thenReturn(Optional.of(testCredential));

        // When & Then
        assertThatThrownBy(() -> signinService.signin(1L, "localhost", request))
            .isInstanceOf(ApiException.class)
            .hasMessage("error.signin.invalid");
    }

    @Test
    void signinThrowsExceptionWhenMembershipNotFound() {
        // Given
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByAccountIdAndEmailAddress(1L, "test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(credentialRepository.findByUser(testUser))
            .thenReturn(Optional.of(testCredential));
        when(membershipRepository.findByUserIdAndAccountId(1L, 1L))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> signinService.signin(1L, "localhost", request))
            .isInstanceOf(ApiException.class)
            .hasMessage("error.signin.invalid");
    }

    @Test
    void signinThrowsExceptionWhenMembershipIsInactive() {
        // Given
        testMembership.setStatus(MembershipStatus.SUSPENDED);

        SigninRequest request = new SigninRequest();
        request.setEmailAddress("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByAccountIdAndEmailAddress(1L, "test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(credentialRepository.findByUser(testUser))
            .thenReturn(Optional.of(testCredential));
        when(membershipRepository.findByUserIdAndAccountId(1L, 1L))
            .thenReturn(Optional.of(testMembership));

        // When & Then
        assertThatThrownBy(() -> signinService.signin(1L, "localhost", request))
            .isInstanceOf(ApiException.class)
            .hasMessage("error.signin.invalid");
    }

    @Test
    void signinWorksWithDifferentMembershipRoles() {
        // Test with ADMIN role
        testMembership.setRole(Role.ADMIN);

        SigninRequest request = new SigninRequest();
        request.setEmailAddress("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByAccountIdAndEmailAddress(1L, "test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(credentialRepository.findByUser(testUser))
            .thenReturn(Optional.of(testCredential));
        when(membershipRepository.findByUserIdAndAccountId(1L, 1L))
            .thenReturn(Optional.of(testMembership));
        when(accountRepository.findById(1L))
            .thenReturn(Optional.of(testAccount));

        String token = signinService.signin(1L, "localhost", request);

        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload();

        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void signinWorksWhenAccountNotFoundInRepository() {
        // Given
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByAccountIdAndEmailAddress(1L, "test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(credentialRepository.findByUser(testUser))
            .thenReturn(Optional.of(testCredential));
        when(membershipRepository.findByUserIdAndAccountId(1L, 1L))
            .thenReturn(Optional.of(testMembership));
        when(accountRepository.findById(1L))
            .thenReturn(Optional.empty());

        // When
        String token = signinService.signin(1L, "localhost", request);

        // Then
        assertThat(token).isNotNull().isNotEmpty();

        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload();

        assertThat(claims.get("orgId")).isNull();
        assertThat(claims.get("productCode")).isNull();
    }

    @Test
    void signinGeneratesTokenWithCorrectExpiration() {
        // Given
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByAccountIdAndEmailAddress(1L, "test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(credentialRepository.findByUser(testUser))
            .thenReturn(Optional.of(testCredential));
        when(membershipRepository.findByUserIdAndAccountId(1L, 1L))
            .thenReturn(Optional.of(testMembership));
        when(accountRepository.findById(1L))
            .thenReturn(Optional.of(testAccount));

        long beforeSignin = System.currentTimeMillis();

        // When
        String token = signinService.signin(1L, "localhost", request);

        long afterSignin = System.currentTimeMillis();

        // Then
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload();

        long expectedMinExpiration = beforeSignin + JWT_EXPIRATION;
        long expectedMaxExpiration = afterSignin + JWT_EXPIRATION;
        long actualExpiration = claims.getExpiration().getTime();

        assertThat(actualExpiration).isBetween(expectedMinExpiration, expectedMaxExpiration);
    }

    private String encrypt(String value) throws Exception {
        byte[] keyBytes = PASSWORD_SECRET.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, 0, 16, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }
}