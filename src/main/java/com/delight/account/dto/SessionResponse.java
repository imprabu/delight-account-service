package com.delight.account.dto;

import com.delight.account.model.Role;
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
    
    @Schema(description = "User's role within the account", example = "ADMIN", allowableValues = {"OWNER", "ADMIN", "USER"})
    private Role role;
    
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
}