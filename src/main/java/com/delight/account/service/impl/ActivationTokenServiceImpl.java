package com.delight.account.service.impl;

import com.delight.account.model.ActivationToken;
import com.delight.account.repository.ActivationTokenRepository;
import com.delight.account.service.ActivationTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@Transactional
public class ActivationTokenServiceImpl implements ActivationTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(ActivationTokenServiceImpl.class);
    private static final int TOKEN_LENGTH = 32; // 32 bytes = 256 bits
    private static final int MAX_TOKENS_PER_HOUR = 3; // Rate limit for token generation
    
    private final ActivationTokenRepository tokenRepository;
    private final SecureRandom secureRandom;
    
    public ActivationTokenServiceImpl(ActivationTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
        this.secureRandom = new SecureRandom();
    }
    
    @Override
    public ActivationToken generateToken(String emailAddress, Long accountId) {
        // Check rate limiting
        if (isTokenGenerationRateLimited(emailAddress, accountId)) {
            logger.warn("Token generation rate limited for emailAddress={} accountId={}", emailAddress, accountId);
            throw new RuntimeException("Too many activation tokens requested. Please try again later.");
        }
        
        // Generate secure random token
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        // Create and save token
        ActivationToken activationToken = new ActivationToken();
        activationToken.setToken(token);
        activationToken.setEmailAddress(emailAddress);
        activationToken.setAccountId(accountId);
        activationToken.setExpiresAt(LocalDateTime.now().plusHours(24)); // 24 hour expiration
        
        ActivationToken savedToken = tokenRepository.save(activationToken);
        logger.info("Generated activation token for emailAddress={} accountId={} tokenId={}", 
            emailAddress, accountId, savedToken.getId());
        
        return savedToken;
    }
    
    @Override
    public ActivationToken validateAndConsumeToken(String token) {
        LocalDateTime now = LocalDateTime.now();
        
        return tokenRepository.findValidToken(token, now)
            .map(activationToken -> {
                // Mark token as used
                activationToken.setUsed(true);
                activationToken.setUsedAt(now);
                
                ActivationToken savedToken = tokenRepository.save(activationToken);
                logger.info("Consumed activation token tokenId={} emailAddress={} accountId={}", 
                    savedToken.getId(), savedToken.getEmailAddress(), savedToken.getAccountId());
                
                return savedToken;
            })
            .orElse(null);
    }
    
    @Override
    public boolean isTokenGenerationRateLimited(String emailAddress, Long accountId) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentTokenCount = tokenRepository.countUnusedTokensSince(emailAddress, accountId, oneHourAgo);
        
        return recentTokenCount >= MAX_TOKENS_PER_HOUR;
    }
    
    @Override
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.deleteExpiredTokens(now);
        logger.debug("Cleaned up expired activation tokens");
    }
}