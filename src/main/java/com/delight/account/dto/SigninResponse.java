package com.delight.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response after successful signin containing authentication tokens")
public class SigninResponse {
    @Schema(description = "JWT token for authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String jwtToken;
    
    @Schema(description = "Session identifier", example = "sess_abc123def456")
    private String sessionId;
    
    public SigninResponse() {}
    
    public SigninResponse(String jwtToken, String sessionId) {
        this.jwtToken = jwtToken;
        this.sessionId = sessionId;
    }
    
    public String getJwtToken() {
        return jwtToken;
    }
    
    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}