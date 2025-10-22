package com.delight.account.service.impl;

import com.delight.account.cache.CacheKeyManager;
import com.delight.account.dto.SigninRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.model.User;
import com.delight.account.model.UserCredential;
import com.delight.account.model.UserStatus;
import com.delight.account.model.Role;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.UserCredentialRepository;
import com.delight.account.repository.UserRepository;
import com.delight.account.service.CacheService;
import com.delight.account.service.SessionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class SigninServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository credentialRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SessionService sessionService;

    @Mock
    private CacheService cacheService;

    @Mock
    private CacheKeyManager cacheKeyManager;

    private SigninServiceImpl signinService;

    @BeforeEach
    void setUp() {
        signinService = new SigninServiceImpl(
            userRepository,
            credentialRepository,
            "1234567890123456",
            "sampleJwtSecret123sampleJwtSecret123",
            TimeUnit.HOURS.toMillis(24),
            membershipRepository,
            accountRepository,
            sessionService,
            cacheService,
            cacheKeyManager
        );

        // Let CacheService delegate to repository fallbacks in tests
        when(cacheService.get(anyString(), any(), any(), any()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                java.util.function.Supplier<Optional<?>> supplier = (java.util.function.Supplier<Optional<?>>) invocation.getArgument(1);
                Optional<?> result = supplier.get();
                return (Optional) result;
            });
        
        // Mock CacheKeyManager methods to return predictable cache keys
        lenient().when(cacheKeyManager.getUserAccountKey(anyLong(), anyString()))
            .thenAnswer(invocation -> "user:account:" + invocation.getArgument(0) + ":" + invocation.getArgument(1));
        lenient().when(cacheKeyManager.getUserCredentialKey(anyLong()))
            .thenAnswer(invocation -> "user:credential:" + invocation.getArgument(0));
    }

    @Test
    void signinReturnsJwt() {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("john@example.com");
        request.setPassword("pass");

        Account account = new Account();
        account.setId(1L);
        account.setStatus(com.delight.account.model.AccountStatus.ACTIVE);

        com.delight.account.model.Organization organization = new com.delight.account.model.Organization();
        organization.setId(10L);
        organization.setStatus(com.delight.account.model.OrganizationStatus.ACTIVE);
        account.setOrganization(organization);

        com.delight.account.model.Product product = new com.delight.account.model.Product();
        product.setCode("HRMS");
        account.setProduct(product);

        User user = new User();
        user.setId(2L);
        user.setAccount(account);
        user.setEmailAddress("john@example.com");
        user.setStatus(UserStatus.ACTIVE);

        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setAccount(account);
        credential.setPasswordHash(encrypt("pass"));

        com.delight.account.model.Membership membership = new com.delight.account.model.Membership();
        membership.setUser(user);
        membership.setAccount(account);
        membership.setRole(Role.ADMIN);
        membership.setStatus(com.delight.account.model.MembershipStatus.ACTIVE);

        when(userRepository.findByAccountIdAndEmailAddress(1L, "john@example.com"))
            .thenReturn(Optional.of(user));
        when(credentialRepository.findByUser(user)).thenReturn(Optional.of(credential));
        when(membershipRepository.findByUserIdAndAccountId(2L, 1L)).thenReturn(Optional.of(membership));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        String token = signinService.signin(1L, "localhost", request);

        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor("sampleJwtSecret123sampleJwtSecret123".getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload();

        assertThat(claims.get("accountId", Long.class)).isEqualTo(1L);
        assertThat(claims.get("userId", Long.class)).isEqualTo(2L);
        long diff = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(diff).isBetween(TimeUnit.HOURS.toMillis(24) - 1000, TimeUnit.HOURS.toMillis(24) + 1000);
    }

    @Test
    void signinInvalidPasswordThrowsException() {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("john@example.com");
        request.setPassword("wrong");

        Account account = new Account();
        account.setId(1L);

        User user = new User();
        user.setId(2L);
        user.setAccount(account);
        user.setStatus(UserStatus.ACTIVE);

        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setAccount(account);
        credential.setPasswordHash(encrypt("pass"));

        when(userRepository.findByAccountIdAndEmailAddress(1L, "john@example.com"))
            .thenReturn(Optional.of(user));
        when(credentialRepository.findByUser(user)).thenReturn(Optional.of(credential));

        assertThrows(ApiException.class, () -> signinService.signin(1L, "localhost", request));
    }

    @Test
    void signinUserNotFoundThrowsException() {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("missing@example.com");
        request.setPassword("pass");

        when(userRepository.findByAccountIdAndEmailAddress(1L, "missing@example.com"))
            .thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> signinService.signin(1L, "localhost", request));
    }

    private String encrypt(String value) {
        try {
            byte[] keyBytes = "1234567890123456".getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, 0, 16, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

