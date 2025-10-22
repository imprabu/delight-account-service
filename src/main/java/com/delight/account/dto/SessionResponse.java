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
    @Schema(description = "Session ID")
    private String sessionId;
    
    @Schema(description = "User ID")
    private Long userId;
    
    @Schema(description = "Account ID")
    private Long accountId;
    
    @Schema(description = "User role in the account")
    private Role role;
    
    @Schema(description = "User email address")
    private String emailAddress;
    
    @Schema(description = "User first name")
    private String firstName;
    
    @Schema(description = "User last name")
    private String lastName;
    
    @Schema(description = "Account domain")
    private String accountDomain;
    
    @Schema(description = "Company name")
    private String companyName;
    
    @Schema(description = "Session expiration time")
    private OffsetDateTime expiresAt;
    
    @Schema(description = "Last accessed time")
    private OffsetDateTime lastAccessedAt;
    
    @Schema(description = "Whether session is active")
    private Boolean isActive;

    @Schema(description = "JWT token for this session")
    private String token;
}