package com.delight.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing activation token validation result and user information")
public class ActivationTokenResponse {
    
    @Schema(description = "Whether the token is valid", example = "true")
    private boolean valid;
    
    @Schema(description = "Email address from the token", example = "user@example.com")
    private String emailAddress;
    
    @Schema(description = "First name of the user", example = "John")
    private String firstName;
    
    @Schema(description = "Last name of the user", example = "Doe")
    private String lastName;
    
    @Schema(description = "Account ID associated with the token", example = "123456789")
    private Long accountId;
    
    @Schema(description = "Error message if token is invalid", example = "Token expired or already used")
    private String errorMessage;
}





