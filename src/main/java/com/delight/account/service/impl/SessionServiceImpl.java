package com.delight.account.service.impl;

import com.delight.account.cache.CacheKeyManager;
import com.delight.account.dto.SessionResponse;
import com.delight.account.model.Account;
import com.delight.account.model.Role;
import com.delight.account.model.Session;
import com.delight.account.model.User;
import com.delight.account.repository.SessionRepository;
import com.delight.account.service.SessionService;
import com.delight.account.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class SessionServiceImpl implements SessionService {
    
    private final SessionRepository sessionRepository;
    private final CacheService cacheService;
    private final CacheKeyManager cacheKeyManager;
    
    @Value("${cache.session.ttl:PT30M}")
    private Duration sessionCacheTtl;
    
    public SessionServiceImpl(SessionRepository sessionRepository, CacheService cacheService, CacheKeyManager cacheKeyManager) {
        this.sessionRepository = sessionRepository;
        this.cacheService = cacheService;
        this.cacheKeyManager = cacheKeyManager;
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
        // Cache session lookup by sessionId
        String sessionCacheKey = cacheKeyManager.getSessionKey(sessionId);
        Optional<Session> sessionOpt = cacheService.get(sessionCacheKey, () -> {
            return sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
        }, sessionCacheTtl, Session.class);
        
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Session session = sessionOpt.get();
        
        // Check if session is expired
        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            // Mark as inactive and evict from cache
            session.setIsActive(false);
            sessionRepository.save(session);
            cacheService.evict(sessionCacheKey);
            return Optional.empty();
        }
        
        return Optional.of(buildSessionResponse(session));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<SessionResponse> validateSessionByToken(String jwtToken) {
        // Cache session lookup by JWT token
        String tokenCacheKey = cacheKeyManager.getSessionTokenKey(String.valueOf(jwtToken.hashCode()));
        Optional<Session> sessionOpt = cacheService.get(tokenCacheKey, () -> {
            return sessionRepository.findByJwtTokenAndIsActiveTrue(jwtToken);
        }, sessionCacheTtl, Session.class);
        
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Session session = sessionOpt.get();
        
        // Check if session is expired
        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            // Mark as inactive and evict from cache
            session.setIsActive(false);
            sessionRepository.save(session);
            cacheService.evict(tokenCacheKey);
            // Also evict session ID cache
            cacheService.evict(cacheKeyManager.getSessionKey(session.getSessionId()));
            return Optional.empty();
        }
        
        return Optional.of(buildSessionResponse(session));
    }
    
    @Override
    @Transactional
    public void invalidateSession(String sessionId) {
        // First, get the session to obtain the JWT token before deactivating
        Optional<Session> sessionOpt = sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
        
        // Deactivate session in database
        sessionRepository.deactivateBySessionId(sessionId);
        
        // Evict session from cache by session ID
        cacheService.evict(cacheKeyManager.getSessionKey(sessionId));
        
        // Also evict JWT token cache if session was found
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            String jwtToken = session.getJwtToken();
            if (jwtToken != null && !jwtToken.isEmpty()) {
                String tokenCacheKey = cacheKeyManager.getSessionTokenKey(String.valueOf(jwtToken.hashCode()));
                cacheService.evict(tokenCacheKey);
                log.info("Session and JWT token cache invalidated for sessionId={}", sessionId);
            } else {
                log.info("Session invalidated for sessionId={} (no JWT token to evict)", sessionId);
            }
        } else {
            log.info("Session invalidated for sessionId={} (session not found)", sessionId);
        }
    }
    
    @Override
    @Transactional
    public void invalidateAllUserSessions(Long userId) {
        // First, get all active sessions for the user to obtain JWT tokens before deactivating
        List<Session> userSessions = sessionRepository.findByUserIdAndIsActiveTrue(userId);
        
        // Deactivate all user sessions in database
        sessionRepository.deactivateAllByUserId(userId);
        
        // Evict specific user session caches (not using pattern to avoid affecting other users)
        int sessionCachesEvicted = 0;
        int jwtTokensEvicted = 0;
        
        for (Session session : userSessions) {
            // Evict session ID cache
            String sessionCacheKey = cacheKeyManager.getSessionKey(session.getSessionId());
            cacheService.evict(sessionCacheKey);
            sessionCachesEvicted++;
            
            // Evict JWT token cache
            String jwtToken = session.getJwtToken();
            if (jwtToken != null && !jwtToken.isEmpty()) {
                String tokenCacheKey = cacheKeyManager.getSessionTokenKey(String.valueOf(jwtToken.hashCode()));
                cacheService.evict(tokenCacheKey);
                jwtTokensEvicted++;
            }
        }
        
        log.info("All sessions invalidated for user: {} (sessions: {}, session caches evicted: {}, JWT tokens evicted: {})", 
                userId, userSessions.size(), sessionCachesEvicted, jwtTokensEvicted);
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

    @Override
    @Transactional
    public void updateSessionToken(String sessionId, String jwtToken) {
        Optional<Session> sessionOpt = sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            String oldToken = session.getJwtToken();

            // Update token and persist
            session.setJwtToken(jwtToken);
            sessionRepository.save(session);

            // Refresh session cache entry
            String sessionCacheKey = cacheKeyManager.getSessionKey(sessionId);
            cacheService.put(sessionCacheKey, session, sessionCacheTtl);

            // Evict old token cache if present
            if (oldToken != null && !oldToken.isEmpty()) {
                String oldTokenCacheKey = cacheKeyManager.getSessionTokenKey(String.valueOf(oldToken.hashCode()));
                cacheService.evict(oldTokenCacheKey);
            }

            // Cache by new token for quick validation
            String newTokenCacheKey = cacheKeyManager.getSessionTokenKey(String.valueOf(jwtToken.hashCode()));
            cacheService.put(newTokenCacheKey, session, sessionCacheTtl);
        }
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