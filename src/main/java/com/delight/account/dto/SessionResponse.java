package com.delight.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Session validation response")
public class SessionResponse {
    @Schema(description = "Unique session identifier", example = "sess_abc123def456")
    private String sessionId;
    
    @Schema(description = "ID of the user who owns this session", example = "67890")
    private Long userId;
    
    @Schema(description = "ID of the account associated with this session", example = "12345")
    private Long accountId;
    
    @Schema(description = "Role ID", example = "1")
    private Long roleId;
    
    @Schema(description = "Role code", example = "ORG_ADMIN")
    private String roleCode;
    
    @Schema(description = "Role name", example = "Org Admin")
    private String roleName;
    
    @Schema(description = "User's email address", example = "john.doe@acme.com")
    private String emailAddress;
    
    @Schema(description = "User's first name", example = "John")
    private String firstName;
    
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;
    
    @Schema(description = "Account domain for routing", example = "acme-corp.delight.com")
    private String accountDomain;
    
    @Schema(description = "Company or organization name", example = "Acme Corp")
    private String companyName;
    
    @Schema(description = "Session expiration timestamp in ISO 8601 format", example = "2023-12-01T18:30:00Z")
    private OffsetDateTime expiresAt;
    
    @Schema(description = "Timestamp when session was last accessed", example = "2023-12-01T16:45:00Z")
    private OffsetDateTime lastAccessedAt;
    
    @Schema(description = "Whether the session is currently active", example = "true")
    private Boolean isActive;

    @Schema(description = "JWT token for authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
    
    @Schema(description = "List of privilege codes accessible by the session user", example = "[\"user.create\", \"user.read\", \"account.update\"]")
    private java.util.List<String> privileges;
    
    @Schema(description = "Subscription information for the account")
    private SubscriptionInfo subscription;
    
    @Schema(description = "List of active features with limits and usage")
    private java.util.List<FeatureInfo> features;
}