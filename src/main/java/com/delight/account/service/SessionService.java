package com.delight.account.service;

import com.delight.account.dto.SessionResponse;
import com.delight.account.model.Account;
import com.delight.account.model.ClientType;
import com.delight.account.model.RoleEntity;
import com.delight.account.model.Session;
import com.delight.account.model.User;

import java.util.Optional;

public interface SessionService {
    
    /**
     * Creates a new session for the user with client type detection
     */
    Session createSession(User user, Account account, RoleEntity role, String jwtToken, 
                         String userAgent, String ipAddress, ClientType clientType);
    
    /**
     * Validates a session by session ID
     */
    Optional<SessionResponse> validateSession(String sessionId);
    
    /**
     * Validates a session by JWT token
     */
    Optional<SessionResponse> validateSessionByToken(String jwtToken);
    
    /**
     * Invalidates a session by session ID
     */
    void invalidateSession(String sessionId);
    
    /**
     * Invalidates all sessions for a user
     */
    void invalidateAllUserSessions(Long userId);
    
    /**
     * Invalidates all active sessions for a user, account, and client type combination
     */
    void invalidateSessionsByUserAccountAndClientType(Long userId, Long accountId, ClientType clientType);
    
    /**
     * Updates the last accessed time for a session
     */
    void updateLastAccessed(String sessionId);
    
    /**
     * Cleans up expired sessions
     */
    void cleanupExpiredSessions();

    /**
     * Updates the JWT token for an existing session
     */
    void updateSessionToken(String sessionId, String jwtToken);
}