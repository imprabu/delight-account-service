package com.delight.account.dto;

public class SigninResponse {
    private String jwtToken;
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