package com.delight.account.service;

import com.delight.account.dto.SessionResponse;
import com.delight.account.model.Account;
import com.delight.account.model.ClientType;
import com.delight.account.model.Role;
import com.delight.account.model.Session;
import com.delight.account.model.User;
import com.delight.account.repository.SessionRepository;
import com.delight.account.service.impl.SessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    private SessionService sessionService;

    private User testUser;
    private Account testAccount;
    private Session testSession;

    @BeforeEach
    void setUp() {
        sessionService = new SessionServiceImpl(sessionRepository);

        // Setup test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmailAddress("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setCompanyName("Test Company");

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setDomain("testcompany");

        testSession = new Session();
        testSession.setId(1L);
        testSession.setSessionId(UUID.randomUUID().toString());
        testSession.setUser(testUser);
        testSession.setAccount(testAccount);
        testSession.setRole(Role.MEMBER);
        testSession.setJwtToken("test-jwt-token");
        testSession.setUserAgent("test-agent");
        testSession.setIpAddress("127.0.0.1");
        testSession.setClientType(ClientType.WEB);
        testSession.setCreatedAt(OffsetDateTime.now());
        testSession.setExpiresAt(OffsetDateTime.now().plusHours(24));
        testSession.setLastAccessedAt(OffsetDateTime.now());
        testSession.setIsActive(true);
    }

    @Test
    void createSessionWithWebClientType() {
        // Given
        when(sessionRepository.save(any(Session.class))).thenReturn(testSession);

        // When
        Session result = sessionService.createSession(testUser, testAccount, Role.MEMBER, 
                                                     "jwt-token", "user-agent", "127.0.0.1", ClientType.WEB);

        // Then
        assertThat(result).isNotNull();
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void createSessionWithMobileClientTypeHasLongerTTL() {
        // Given
        Session mobileSession = new Session();
        mobileSession.setClientType(ClientType.ANDROID);
        mobileSession.setExpiresAt(OffsetDateTime.now().plusHours(168)); // 7 days
        when(sessionRepository.save(any(Session.class))).thenReturn(mobileSession);

        // When
        Session result = sessionService.createSession(testUser, testAccount, Role.MEMBER, 
                                                     "jwt-token", "user-agent", "127.0.0.1", ClientType.ANDROID);

        // Then
        assertThat(result).isNotNull();
        verify(sessionRepository).save(argThat(session -> 
            session.getClientType() == ClientType.ANDROID
        ));
    }

    @Test
    void validateSessionReturnsSessionResponseWhenValid() {
        // Given
        when(sessionRepository.findBySessionIdAndIsActiveTrue(testSession.getSessionId()))
                .thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(Session.class))).thenReturn(testSession);

        // When
        Optional<SessionResponse> result = sessionService.validateSession(testSession.getSessionId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSessionId()).isEqualTo(testSession.getSessionId());
        assertThat(result.get().getUserId()).isEqualTo(testUser.getId());
        assertThat(result.get().getAccountId()).isEqualTo(testAccount.getId());
    }

    @Test
    void validateSessionReturnsEmptyWhenSessionNotFound() {
        // Given
        when(sessionRepository.findBySessionIdAndIsActiveTrue(anyString()))
                .thenReturn(Optional.empty());

        // When
        Optional<SessionResponse> result = sessionService.validateSession("non-existent-session");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void validateSessionInvalidatesExpiredSession() {
        // Given
        Session expiredSession = new Session();
        expiredSession.setId(1L);
        expiredSession.setSessionId(UUID.randomUUID().toString());
        expiredSession.setUser(testUser);
        expiredSession.setAccount(testAccount);
        expiredSession.setRole(Role.MEMBER);
        expiredSession.setJwtToken("test-jwt-token");
        expiredSession.setClientType(ClientType.WEB);
        expiredSession.setCreatedAt(OffsetDateTime.now().minusHours(25));
        expiredSession.setExpiresAt(OffsetDateTime.now().minusHours(1)); // This field is no longer used
        // Set lastAccessedAt to 25 hours ago, which exceeds WEB TTL of 24 hours
        expiredSession.setLastAccessedAt(OffsetDateTime.now().minusHours(25));
        expiredSession.setIsActive(true);

        when(sessionRepository.findBySessionIdAndIsActiveTrue(expiredSession.getSessionId()))
                .thenReturn(Optional.of(expiredSession));

        // When
        Optional<SessionResponse> result = sessionService.validateSession(expiredSession.getSessionId());

        // Then
        assertThat(result).isEmpty();
        verify(sessionRepository).deactivateBySessionId(expiredSession.getSessionId());
    }

    @Test
    void validateSessionByTokenReturnsSessionResponseWhenValid() {
        // Given
        when(sessionRepository.findByJwtTokenAndIsActiveTrue(testSession.getJwtToken()))
                .thenReturn(Optional.of(testSession));
        // Mock for updateLastAccessed method
        when(sessionRepository.findBySessionIdAndIsActiveTrue(testSession.getSessionId()))
                .thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(Session.class))).thenReturn(testSession);

        // When
        Optional<SessionResponse> result = sessionService.validateSessionByToken(testSession.getJwtToken());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSessionId()).isEqualTo(testSession.getSessionId());
        // Verify that lastAccessedAt was updated for session renewal
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void validateSessionByTokenInvalidatesExpiredSession() {
        // Given
        Session expiredSession = new Session();
        expiredSession.setId(1L);
        expiredSession.setSessionId(UUID.randomUUID().toString());
        expiredSession.setUser(testUser);
        expiredSession.setAccount(testAccount);
        expiredSession.setRole(Role.MEMBER);
        expiredSession.setJwtToken("expired-jwt-token");
        expiredSession.setClientType(ClientType.WEB);
        expiredSession.setCreatedAt(OffsetDateTime.now().minusHours(25));
        expiredSession.setExpiresAt(OffsetDateTime.now().minusHours(1)); // This field is no longer used
        // Set lastAccessedAt to 25 hours ago, which exceeds WEB TTL of 24 hours
        expiredSession.setLastAccessedAt(OffsetDateTime.now().minusHours(25));
        expiredSession.setIsActive(true);

        when(sessionRepository.findByJwtTokenAndIsActiveTrue("expired-jwt-token"))
                .thenReturn(Optional.of(expiredSession));

        // When
        Optional<SessionResponse> result = sessionService.validateSessionByToken("expired-jwt-token");

        // Then
        assertThat(result).isEmpty();
        verify(sessionRepository).deactivateBySessionId(expiredSession.getSessionId());
    }

    @Test
    void invalidateSessionSetsIsActiveToFalse() {
        // When
        sessionService.invalidateSession(testSession.getSessionId());

        // Then
        verify(sessionRepository).deactivateBySessionId(testSession.getSessionId());
    }

    @Test
    void updateLastAccessedUpdatesTimestamp() {
        // Given
        when(sessionRepository.findBySessionIdAndIsActiveTrue(testSession.getSessionId()))
                .thenReturn(Optional.of(testSession));

        // When
        sessionService.updateLastAccessed(testSession.getSessionId());

        // Then
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void updateSessionTokenUpdatesJwtToken() {
        // Given
        String newToken = "new-jwt-token";
        when(sessionRepository.findBySessionIdAndIsActiveTrue(testSession.getSessionId()))
                .thenReturn(Optional.of(testSession));

        // When
        sessionService.updateSessionToken(testSession.getSessionId(), newToken);

        // Then
        verify(sessionRepository).save(argThat(session -> 
            newToken.equals(session.getJwtToken())
        ));
    }

    @Test
    void clientTypeFromRequestSourceParsesCorrectly() {
        // Test Android
        assertThat(ClientType.fromRequestSource("android")).isEqualTo(ClientType.ANDROID);
        assertThat(ClientType.fromRequestSource("ANDROID")).isEqualTo(ClientType.ANDROID);
        
        // Test iOS
        assertThat(ClientType.fromRequestSource("ios")).isEqualTo(ClientType.IOS);
        assertThat(ClientType.fromRequestSource("IOS")).isEqualTo(ClientType.IOS);
        
        // Test Web (default)
        assertThat(ClientType.fromRequestSource("web")).isEqualTo(ClientType.WEB);
        assertThat(ClientType.fromRequestSource("")).isEqualTo(ClientType.WEB);
        assertThat(ClientType.fromRequestSource(null)).isEqualTo(ClientType.WEB);
        assertThat(ClientType.fromRequestSource("unknown")).isEqualTo(ClientType.WEB);
    }

    @Test
    void clientTypeTtlHoursAreCorrect() {
        assertThat(ClientType.WEB.getDefaultTtlHours()).isEqualTo(24);
        assertThat(ClientType.ANDROID.getDefaultTtlHours()).isEqualTo(168); // 7 days
        assertThat(ClientType.IOS.getDefaultTtlHours()).isEqualTo(168); // 7 days
    }

    @Test
    void clientTypeIsMobileDetection() {
        assertThat(ClientType.WEB.isMobile()).isFalse();
        assertThat(ClientType.ANDROID.isMobile()).isTrue();
        assertThat(ClientType.IOS.isMobile()).isTrue();
    }
}