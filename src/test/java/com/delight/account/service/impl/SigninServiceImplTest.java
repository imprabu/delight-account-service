package com.delight.account.service.impl;

import com.delight.account.dto.SigninRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.model.User;
import com.delight.account.model.UserCredential;
import com.delight.account.model.UserStatus;
import com.delight.account.repository.UserCredentialRepository;
import com.delight.account.repository.UserRepository;
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

@ExtendWith(MockitoExtension.class)
class SigninServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository credentialRepository;

    private SigninServiceImpl signinService;

    @BeforeEach
    void setUp() {
        signinService = new SigninServiceImpl(
            userRepository,
            credentialRepository,
            "1234567890123456",
            "sampleJwtSecret123sampleJwtSecret123",
            TimeUnit.HOURS.toMillis(24)
        );
    }

    @Test
    void signinReturnsJwt() {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("john@example.com");
        request.setPassword("pass");

        Account account = new Account();
        account.setId(1L);

        User user = new User();
        user.setId(2L);
        user.setAccount(account);
        user.setEmailAddress("john@example.com");
        user.setStatus(UserStatus.ACTIVE);

        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setAccount(account);
        credential.setPasswordHash(encrypt("pass"));

        when(userRepository.findByAccountIdAndEmailAddress(1L, "john@example.com"))
            .thenReturn(Optional.of(user));
        when(credentialRepository.findByUser(user)).thenReturn(Optional.of(credential));

        String token = signinService.signin(1L, request);

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

        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setAccount(account);
        credential.setPasswordHash(encrypt("pass"));

        when(userRepository.findByAccountIdAndEmailAddress(1L, "john@example.com"))
            .thenReturn(Optional.of(user));
        when(credentialRepository.findByUser(user)).thenReturn(Optional.of(credential));

        assertThrows(ApiException.class, () -> signinService.signin(1L, request));
    }

    @Test
    void signinUserNotFoundThrowsException() {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("missing@example.com");
        request.setPassword("pass");

        when(userRepository.findByAccountIdAndEmailAddress(1L, "missing@example.com"))
            .thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> signinService.signin(1L, request));
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

