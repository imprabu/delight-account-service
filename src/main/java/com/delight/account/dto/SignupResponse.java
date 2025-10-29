package com.delight.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response after successful signup")
public class SignupResponse {
    @Schema(description = "Success message confirming account creation", example = "Account created successfully")
    private String message;
    
    @Schema(description = "The actual domain created for the account", example = "acme-corp.delight.com")
    private String domain;
    
    public SignupResponse() {}
    
    public SignupResponse(String message, String domain) {
        this.message = message;
        this.domain = domain;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
}