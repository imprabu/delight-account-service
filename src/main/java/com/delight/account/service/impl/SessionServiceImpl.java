package com.delight.account.service.impl;

import com.delight.account.dto.SessionResponse;
import com.delight.account.model.Account;
import com.delight.account.model.ClientType;
import com.delight.account.model.Role;
import com.delight.account.model.Session;
import com.delight.account.model.User;
import com.delight.account.repository.SessionRepository;
import com.delight.account.service.SessionService;
import com.delight.account.util.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class SessionServiceImpl implements SessionService {
    
    private final SessionRepository sessionRepository;
    
    public SessionServiceImpl(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }
    
    @Override
    @Transactional
    public Session createSession(User user, Account account, Role role, String jwtToken, 
                                String userAgent, String ipAddress, ClientType clientType) {
        Session session = new Session();
        session.setUser(user);
        session.setAccount(account);
        session.setRole(role);
        session.setJwtToken(jwtToken);
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserAgent(userAgent);
        session.setIpAddress(ipAddress);
        session.setClientType(clientType);
        session.setCreatedAt(OffsetDateTime.now());
        
        // Set expiration based on client type
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(clientType.getDefaultTtlHours());
        session.setExpiresAt(expiresAt);
        
        session.setLastAccessedAt(OffsetDateTime.now());
        session.setIsActive(true);
        
        return sessionRepository.save(session);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<SessionResponse> validateSession(String sessionId) {
        Optional<Session> sessionOpt = sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
        
        if (sessionOpt.isEmpty()) {
            log.info("Session not found or inactive: {}", sessionId);
            return Optional.empty();
        }
        
        Session session = sessionOpt.get();
        
        // Check if session has expired
        if (isSessionExpired(session)) {
            log.info("Session has expired, invalidating: {}", sessionId);
            invalidateSession(sessionId);
            return Optional.empty();
        }
        
        // Update last accessed time
        updateLastAccessed(sessionId);
        
        return Optional.of(buildSessionResponse(session));
    }
    
    private boolean isSessionExpired(Session session) {
        // Calculate expiry based on lastAccessedAt + TTL instead of fixed expiresAt
        OffsetDateTime lastAccessed = session.getLastAccessedAt();
        if (lastAccessed == null) {
            lastAccessed = session.getCreatedAt();
        }
        
        // Get TTL hours based on client type
        int ttlHours = session.getClientType().getDefaultTtlHours();
        OffsetDateTime expiryTime = lastAccessed.plusHours(ttlHours);
        
        return expiryTime.isBefore(OffsetDateTime.now());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<SessionResponse> validateSessionByToken(String jwtToken) {
        Optional<Session> sessionOpt = sessionRepository.findByJwtTokenAndIsActiveTrue(jwtToken);
        
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Session session = sessionOpt.get();
        
        // Check if session has expired
        if (isSessionExpired(session)) {
            log.info("Session has expired, invalidating: {}", session.getSessionId());
            invalidateSession(session.getSessionId());
            return Optional.empty();
        }
        
        // Update last accessed time to renew the session
        updateLastAccessed(session.getSessionId());
        
        return Optional.of(buildSessionResponse(session));
    }
    
    @Override
    @Transactional
    public void invalidateSession(String sessionId) {
        // Deactivate session in database
        sessionRepository.deactivateBySessionId(sessionId);
        
        LoggingUtils.logBusinessOperation(log, "SESSION_INVALIDATION", "SUCCESS", 
            "sessionId", sessionId);
    }
    
    @Override
    @Transactional
    public void invalidateAllUserSessions(Long userId) {
        // Get count for logging before deactivating
        List<Session> userSessions = sessionRepository.findByUserIdAndIsActiveTrue(userId);
        
        // Deactivate all user sessions in database
        sessionRepository.deactivateAllByUserId(userId);
        
        LoggingUtils.logBusinessOperation(log, "ALL_SESSIONS_INVALIDATION", "SUCCESS", 
            "userId", userId, 
            "sessionsCount", userSessions.size());
    }
    
    @Override
    @Transactional
    public void updateLastAccessed(String sessionId) {
        Optional<Session> sessionOpt = sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            session.setLastAccessedAt(OffsetDateTime.now());
            sessionRepository.save(session);
        }
    }
    
    @Override
    @Transactional
    public void cleanupExpiredSessions() {
        OffsetDateTime now = OffsetDateTime.now();
        sessionRepository.deactivateExpiredSessions(now);
        LoggingUtils.logBusinessOperation(log, "EXPIRED_SESSIONS_CLEANUP", "SUCCESS", 
            "cleanupTime", now);
    }

    @Override
    @Transactional
    public void updateSessionToken(String sessionId, String jwtToken) {
        sessionRepository.updateJwtTokenBySessionId(sessionId, jwtToken);
    }
    
    private SessionResponse buildSessionResponse(Session session) {
        if (session.getUser() == null) {
            log.error("Session user is null for sessionId: {}", session.getSessionId());
            throw new IllegalStateException("Session user relationship not loaded for sessionId: " + session.getSessionId());
        }
        
        if (session.getAccount() == null) {
            log.error("Session account is null for sessionId: {}", session.getSessionId());
            throw new IllegalStateException("Session account relationship not loaded for sessionId: " + session.getSessionId());
        }
        
        return SessionResponse.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUser().getId())
                .accountId(session.getAccount().getId())
                .role(session.getRole())
                .emailAddress(session.getUser().getEmailAddress())
                .firstName(session.getUser().getFirstName())
                .lastName(session.getUser().getLastName())
                .accountDomain(session.getAccount().getDomain())
                .companyName(session.getUser().getCompanyName())
                .expiresAt(session.getExpiresAt())
                .lastAccessedAt(session.getLastAccessedAt())
                .isActive(session.getIsActive())
                .build();
    }
}