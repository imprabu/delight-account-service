package com.delight.account.service.impl;

import com.delight.account.config.ChannelAuthConfig;
import com.delight.account.dto.ChannelJwtClaims;
import com.delight.account.service.ChannelJwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Implementation of ChannelJwtService for validating channel JWT tokens.
 * Supports versioned shared private keys for different subsystems.
 */
@Service
public class ChannelJwtServiceImpl implements ChannelJwtService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChannelJwtServiceImpl.class);
    
    @Autowired
    private ChannelAuthConfig channelAuthConfig;
    
    @Override
    public ChannelJwtClaims validateChannelToken(String token) {
        try {
            // DEBUG: Log the loaded keys and token being validated
            Map<String, String> versionedKeys = channelAuthConfig.getKeys();
            System.out.println("=== DEBUG: Loaded versioned keys: " + versionedKeys);
            System.out.println("=== DEBUG: Keys size: " + versionedKeys.size());
            System.out.println("=== DEBUG: Available key versions: " + versionedKeys.keySet());
            System.out.println("=== DEBUG: Validating token: " + token.substring(0, Math.min(50, token.length())) + "...");
            
            logger.info("DEBUG: Loaded versioned keys: {}", versionedKeys);
            logger.info("DEBUG: Keys size: {}", versionedKeys.size());
            logger.info("DEBUG: Available key versions: {}", versionedKeys.keySet());
            logger.info("DEBUG: Validating token: {}", token.substring(0, Math.min(50, token.length())) + "...");
            
            if (versionedKeys.isEmpty()) {
                System.out.println("=== ERROR: No versioned keys loaded from configuration!");
                logger.error("ERROR: No versioned keys loaded from configuration!");
                throw new RuntimeException("No channel authentication keys configured");
            }
            
            // Parse the token header to get the key version
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length != 3) {
                throw new RuntimeException("Invalid JWT token format");
            }
            
            // Decode header to get key version (simplified approach)
            // In a real implementation, you might want to parse the header properly
            Claims claims = null;
            RuntimeException lastException = null;
            
            // Try each versioned key until one works
            for (Map.Entry<String, String> keyEntry : versionedKeys.entrySet()) {
                try {
                    String keyVersion = keyEntry.getKey();
                    String secretKey = keyEntry.getValue();
                    
                    // DEBUG: Log each key attempt
                    logger.info("DEBUG: Trying key version: {} with secret: {}", keyVersion, secretKey.substring(0, Math.min(20, secretKey.length())) + "...");
                    
                    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
                    claims = Jwts.parser()
                            .verifyWith(key)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                    
                    // If we get here, the token was successfully parsed
                    String accountId = claims.get("account_id", String.class);
                    String tokenKeyVersion = claims.get("key_version", String.class);
                    
                    if (accountId == null) {
                        throw new RuntimeException("Missing account_id in JWT token");
                    }
                    
                    if (tokenKeyVersion == null) {
                        throw new RuntimeException("Missing key_version in JWT token");
                    }
                    
                    // Verify that the key version in the token matches the one we used
                    if (!keyVersion.equals(tokenKeyVersion)) {
                        continue; // Try next key
                    }
                    
                    logger.debug("Successfully validated channel JWT for account_id: {} with key_version: {}", 
                               accountId, tokenKeyVersion);
                    
                    return new ChannelJwtClaims(accountId, tokenKeyVersion);
                    
                } catch (Exception e) {
                    lastException = new RuntimeException("Failed to validate JWT token with key version: " + keyEntry.getKey(), e);
                    // Continue to try next key
                }
            }
            
            // If we get here, none of the keys worked
            if (lastException != null) {
                throw lastException;
            } else {
                throw new RuntimeException("No versioned keys configured for channel authentication");
            }
            
        } catch (Exception e) {
            logger.error("Failed to validate channel JWT token", e);
            throw new RuntimeException("Invalid or expired channel JWT token", e);
        }
    }
    
    @Override
    public boolean isKeyVersionSupported(String keyVersion) {
        return channelAuthConfig.getKeys().containsKey(keyVersion);
    }
}