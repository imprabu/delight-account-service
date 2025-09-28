package com.delight.account.service.impl;

import com.delight.account.dto.SessionResponse;
import com.delight.account.model.Account;
import com.delight.account.model.Role;
import com.delight.account.model.Session;
import com.delight.account.model.User;
import com.delight.account.repository.SessionRepository;
import com.delight.account.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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
                                String userAgent, String ipAddress) {
        Session session = new Session();
        session.setUser(user);
        session.setAccount(account);
        session.setRole(role);
        session.setJwtToken(jwtToken);
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserAgent(userAgent);
        session.setIpAddress(ipAddress);
        session.setCreatedAt(OffsetDateTime.now());
        session.setExpiresAt(OffsetDateTime.now().plusHours(24)); // 24 hour expiration
        session.setLastAccessedAt(OffsetDateTime.now());
        session.setIsActive(true);
        
        return sessionRepository.save(session);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<SessionResponse> validateSession(String sessionId) {
        Optional<Session> sessionOpt = sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
        
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Session session = sessionOpt.get();
        
        // Check if session is expired
        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            // Mark as inactive
            session.setIsActive(false);
            sessionRepository.save(session);
            return Optional.empty();
        }
        
        return Optional.of(buildSessionResponse(session));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<SessionResponse> validateSessionByToken(String jwtToken) {
        Optional<Session> sessionOpt = sessionRepository.findByJwtTokenAndIsActiveTrue(jwtToken);
        
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Session session = sessionOpt.get();
        
        // Check if session is expired
        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            // Mark as inactive
            session.setIsActive(false);
            sessionRepository.save(session);
            return Optional.empty();
        }
        
        return Optional.of(buildSessionResponse(session));
    }
    
    @Override
    @Transactional
    public void invalidateSession(String sessionId) {
        sessionRepository.deactivateBySessionId(sessionId);
        log.info("Session invalidated: {}", sessionId);
    }
    
    @Override
    @Transactional
    public void invalidateAllUserSessions(Long userId) {
        sessionRepository.deactivateAllByUserId(userId);
        log.info("All sessions invalidated for user: {}", userId);
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
        sessionRepository.deactivateExpiredSessions(OffsetDateTime.now());
        log.info("Expired sessions cleaned up");
    }
    
    private SessionResponse buildSessionResponse(Session session) {
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