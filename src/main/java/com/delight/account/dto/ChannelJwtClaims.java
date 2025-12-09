package com.delight.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for holding channel JWT claims.
 * Contains account_id and key_version extracted from channel JWT tokens.
 */
@Schema(description = "Channel JWT claims containing account information, optional user information, and key version")
public class ChannelJwtClaims {
    
    @Schema(description = "Account identifier from JWT claims (optional)", example = "12345")
    private String accountId;
    
    @Schema(description = "User identifier from JWT claims (optional)", example = "67890")
    private String userId;
    
    @Schema(description = "Key version for JWT validation", example = "v1")
    private String keyVersion;
    
    public ChannelJwtClaims() {
    }
    
    public ChannelJwtClaims(String accountId, String keyVersion) {
        this.accountId = accountId;
        this.keyVersion = keyVersion;
    }
    
    public ChannelJwtClaims(String accountId, String userId, String keyVersion) {
        this.accountId = accountId;
        this.userId = userId;
        this.keyVersion = keyVersion;
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getKeyVersion() {
        return keyVersion;
    }
    
    public void setKeyVersion(String keyVersion) {
        this.keyVersion = keyVersion;
    }
    
    @Override
    public String toString() {
        return "ChannelJwtClaims{" +
                "accountId='" + accountId + '\'' +
                ", userId='" + userId + '\'' +
                ", keyVersion='" + keyVersion + '\'' +
                '}';
    }
}