package com.delight.account.service.impl;

import com.delight.account.dto.FeatureInfo;
import com.delight.account.dto.SessionResponse;
import com.delight.account.dto.SubscriptionInfo;
import com.delight.account.model.Account;
import com.delight.account.model.ClientType;
import com.delight.account.model.Privilege;
import com.delight.account.model.RoleEntity;
import com.delight.account.model.RolePrivilege;
import com.delight.account.model.Session;
import com.delight.account.model.User;
import com.delight.account.repository.SessionRepository;
import com.delight.account.service.FeatureService;
import com.delight.account.service.SessionService;
import com.delight.account.service.SubscriptionService;
import com.delight.account.util.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SessionServiceImpl implements SessionService {
    
    private final SessionRepository sessionRepository;
    private final SubscriptionService subscriptionService;
    private final FeatureService featureService;
    
    public SessionServiceImpl(SessionRepository sessionRepository,
                             SubscriptionService subscriptionService,
                             FeatureService featureService) {
        this.sessionRepository = sessionRepository;
        this.subscriptionService = subscriptionService;
        this.featureService = featureService;
    }
    
    @Override
    @Transactional
    public Session createSession(User user, Account account, RoleEntity role, String jwtToken, 
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
        // Calculate expiry based on createdAt + TTL (fixed expiration time from creation)
        OffsetDateTime createdAt = session.getCreatedAt();
        if (createdAt == null) {
            // If createdAt is null, consider session expired
            return true;
        }
        
        // Get TTL hours based on client type
        int ttlHours = session.getClientType().getDefaultTtlHours();
        OffsetDateTime expiryTime = createdAt.plusHours(ttlHours);
        
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
    public void invalidateSessionsByUserAccountAndClientType(Long userId, Long accountId, ClientType clientType) {
        // Get count for logging before deactivating
        List<Session> sessions = sessionRepository.findByUserIdAndAccountIdAndClientTypeAndIsActiveTrue(userId, accountId, clientType);
        
        // Deactivate sessions for this user, account, and client type combination
        sessionRepository.deactivateByUserIdAndAccountIdAndClientType(userId, accountId, clientType);
        
        LoggingUtils.logBusinessOperation(log, "SESSIONS_INVALIDATION_BY_CLIENT_TYPE", "SUCCESS", 
            "userId", userId,
            "accountId", accountId,
            "clientType", clientType.name(),
            "sessionsCount", sessions.size());
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
    
    @Override
    @Transactional
    public void invalidateAccountSessions(Long accountId) {
        // Get count for logging before deactivating
        List<Session> accountSessions = sessionRepository.findByAccountIdAndIsActiveTrue(accountId);
        
        // Deactivate all active sessions for this account
        sessionRepository.deactivateAllByAccountId(accountId);
        
        LoggingUtils.logBusinessOperation(log, "ACCOUNT_SESSIONS_INVALIDATION", "SUCCESS", 
            "accountId", accountId, 
            "sessionsCount", accountSessions.size());
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
        
        RoleEntity role = session.getRole();
        List<String> privileges = extractPrivileges(role);
        
        Long accountId = session.getAccount().getId();
        
        // Load subscription info
        Optional<SubscriptionInfo> subscriptionOpt = subscriptionService.getSubscriptionInfo(accountId);
        
        // Load active features
        List<FeatureInfo> features = featureService.getActiveFeatures(accountId);
        
        return SessionResponse.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUser().getId())
                .accountId(accountId)
                .roleId(role != null ? role.getId() : null)
                .roleCode(role != null ? role.getCode() : null)
                .roleName(role != null ? role.getName() : null)
                .emailAddress(session.getUser().getEmailAddress())
                .firstName(session.getUser().getFirstName())
                .lastName(session.getUser().getLastName())
                .accountDomain(session.getAccount().getDomain())
                .companyName(session.getUser().getCompanyName())
                .expiresAt(session.getExpiresAt())
                .lastAccessedAt(session.getLastAccessedAt())
                .isActive(session.getIsActive())
                .privileges(privileges)
                .subscription(subscriptionOpt.orElse(null))
                .features(features)
                .build();
    }
    
    private List<String> extractPrivileges(RoleEntity role) {
        if (role == null) {
            return List.of();
        }
        
        Set<RolePrivilege> rolePrivileges = role.getRolePrivileges();
        if (rolePrivileges == null || rolePrivileges.isEmpty()) {
            return List.of();
        }
        
        return rolePrivileges.stream()
                .map(RolePrivilege::getPrivilege)
                .filter(privilege -> privilege != null)
                .map(Privilege::getCode)
                .filter(code -> code != null)
                .sorted()
                .collect(Collectors.toList());
    }
}