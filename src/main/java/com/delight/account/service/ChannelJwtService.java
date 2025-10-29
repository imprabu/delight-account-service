package com.delight.account.service;

import com.delight.account.dto.ChannelJwtClaims;

/**
 * Service for handling channel JWT authentication.
 * Validates JWT tokens containing account_id and key_version for /channel/ endpoints.
 */
public interface ChannelJwtService {
    
    /**
     * Validates a channel JWT token and extracts claims.
     * 
     * @param token the JWT token to validate
     * @return ChannelJwtClaims containing account_id and key_version
     * @throws RuntimeException if token is invalid or expired
     */
    ChannelJwtClaims validateChannelToken(String token);
    
    /**
     * Checks if the given key version is supported.
     * 
     * @param keyVersion the key version to check
     * @return true if the key version is supported, false otherwise
     */
    boolean isKeyVersionSupported(String keyVersion);
}