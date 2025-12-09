package com.delight.account.service;

import com.delight.account.model.ActivationToken;

/**
 * Service for managing activation tokens
 */
public interface ActivationTokenService {
    
    /**
     * Generate and store a new activation token
     * 
     * @param emailAddress The email address for activation
     * @param accountId The account ID
     * @return The generated activation token
     */
    ActivationToken generateToken(String emailAddress, Long accountId);
    
    /**
     * Validate token without consuming it (for reading token info)
     * 
     * @param token The token to validate
     * @return The activation token if valid, null otherwise
     */
    ActivationToken validateToken(String token);
    
    /**
     * Validate and consume an activation token
     * 
     * @param token The token to validate
     * @return The activation token if valid, null otherwise
     */
    ActivationToken validateAndConsumeToken(String token);
    
    /**
     * Check if too many tokens have been generated recently for rate limiting
     * 
     * @param emailAddress The email address
     * @param accountId The account ID
     * @return true if rate limit exceeded
     */
    boolean isTokenGenerationRateLimited(String emailAddress, Long accountId);
    
    /**
     * Clean up expired tokens
     */
    void cleanupExpiredTokens();
}