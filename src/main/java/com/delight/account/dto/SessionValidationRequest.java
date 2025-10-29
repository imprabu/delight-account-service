package com.delight.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for session validation with JWT token
 * Used by subsystems to validate sessions by passing JWT tokens from incoming requests
 */
@Schema(description = "Request for session validation with JWT token")
public class SessionValidationRequest {

    @NotBlank(message = "JWT token is required")
    @Schema(description = "JWT token to validate for session authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String jwtToken;

    public SessionValidationRequest() {
    }

    public SessionValidationRequest(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }
}