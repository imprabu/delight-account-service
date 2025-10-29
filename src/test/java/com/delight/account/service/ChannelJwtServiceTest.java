package com.delight.account.service;

import com.delight.account.config.ChannelAuthConfig;
import com.delight.account.dto.ChannelJwtClaims;
import com.delight.account.service.impl.ChannelJwtServiceImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelJwtServiceTest {

    private static final String TEST_SECRET_V1 = "channel-secret-key-v1-that-is-long-enough-for-hmac-sha256";
    private static final String TEST_SECRET_V2 = "channel-secret-key-v2-that-is-long-enough-for-hmac-sha256";
    private static final String ACCOUNT_ID = "12345";
    private static final String KEY_VERSION_V1 = "v1";
    private static final String KEY_VERSION_V2 = "v2";

    @Mock
    private ChannelAuthConfig channelAuthConfig;

    @InjectMocks
    private ChannelJwtServiceImpl channelJwtService;

    private Map<String, String> versionedKeys;

    @BeforeEach
    void setUp() {
        versionedKeys = new HashMap<>();
        versionedKeys.put(KEY_VERSION_V1, TEST_SECRET_V1);
        versionedKeys.put(KEY_VERSION_V2, TEST_SECRET_V2);
        
        when(channelAuthConfig.getKeys()).thenReturn(versionedKeys);
    }

    @Test
    void validateChannelToken_ValidTokenV1_ReturnsCorrectClaims() {
        // Given
        String token = createValidChannelToken(ACCOUNT_ID, KEY_VERSION_V1, TEST_SECRET_V1);

        // When
        ChannelJwtClaims claims = channelJwtService.validateChannelToken(token);

        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(claims.getKeyVersion()).isEqualTo(KEY_VERSION_V1);
    }

    @Test
    void validateChannelToken_ValidTokenV2_ReturnsCorrectClaims() {
        // Given
        String token = createValidChannelToken(ACCOUNT_ID, KEY_VERSION_V2, TEST_SECRET_V2);

        // When
        ChannelJwtClaims claims = channelJwtService.validateChannelToken(token);

        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(claims.getKeyVersion()).isEqualTo(KEY_VERSION_V2);
    }

    @Test
    void validateChannelToken_ExpiredToken_ThrowsException() {
        // Given
        String token = createExpiredChannelToken(ACCOUNT_ID, KEY_VERSION_V1, TEST_SECRET_V1);

        // When & Then
        assertThatThrownBy(() -> channelJwtService.validateChannelToken(token))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Invalid or expired channel JWT token");
    }

    @Test
    void validateChannelToken_InvalidSignature_ThrowsException() {
        // Given
        String token = createValidChannelToken(ACCOUNT_ID, KEY_VERSION_V1, "wrong-secret-key-that-is-long-enough-for-hmac-sha256");

        // When & Then
        assertThatThrownBy(() -> channelJwtService.validateChannelToken(token))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Invalid or expired channel JWT token");
    }

    @Test
    void validateChannelToken_MissingAccountId_ThrowsException() {
        // Given
        String token = createChannelTokenWithoutAccountId(KEY_VERSION_V1, TEST_SECRET_V1);

        // When & Then
        assertThatThrownBy(() -> channelJwtService.validateChannelToken(token))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void validateChannelToken_MissingKeyVersion_ThrowsException() {
        // Given
        String token = createChannelTokenWithoutKeyVersion(ACCOUNT_ID, TEST_SECRET_V1);

        // When & Then
        assertThatThrownBy(() -> channelJwtService.validateChannelToken(token))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void validateChannelToken_KeyVersionMismatch_ThrowsException() {
        // Given - Create token with v1 key but claim v2 version
        String token = createValidChannelToken(ACCOUNT_ID, KEY_VERSION_V2, TEST_SECRET_V1);

        // When & Then
        assertThatThrownBy(() -> channelJwtService.validateChannelToken(token))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Invalid or expired channel JWT token");
    }

    @Test
    void validateChannelToken_InvalidTokenFormat_ThrowsException() {
        // Given
        String invalidToken = "invalid.token";

        // When & Then
        assertThatThrownBy(() -> channelJwtService.validateChannelToken(invalidToken))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JWT");
    }

    @Test
    void validateChannelToken_NoVersionedKeys_ThrowsException() {
        // Given
        when(channelAuthConfig.getKeys()).thenReturn(new HashMap<>());
        String token = createValidChannelToken(ACCOUNT_ID, KEY_VERSION_V1, TEST_SECRET_V1);

        // When & Then
        assertThatThrownBy(() -> channelJwtService.validateChannelToken(token))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void isKeyVersionSupported_ExistingVersion_ReturnsTrue() {
        // When & Then
        assertThat(channelJwtService.isKeyVersionSupported(KEY_VERSION_V1)).isTrue();
        assertThat(channelJwtService.isKeyVersionSupported(KEY_VERSION_V2)).isTrue();
    }

    @Test
    void isKeyVersionSupported_NonExistingVersion_ReturnsFalse() {
        // When & Then
        assertThat(channelJwtService.isKeyVersionSupported("v3")).isFalse();
        assertThat(channelJwtService.isKeyVersionSupported("invalid")).isFalse();
    }

    private String createValidChannelToken(String accountId, String keyVersion, String secret) {
        return Jwts.builder()
                .claim("account_id", accountId)
                .claim("key_version", keyVersion)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String createExpiredChannelToken(String accountId, String keyVersion, String secret) {
        return Jwts.builder()
                .claim("account_id", accountId)
                .claim("key_version", keyVersion)
                .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String createChannelTokenWithoutAccountId(String keyVersion, String secret) {
        return Jwts.builder()
                .claim("key_version", keyVersion)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String createChannelTokenWithoutKeyVersion(String accountId, String secret) {
        return Jwts.builder()
                .claim("account_id", accountId)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}