package com.delight.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Signin request for current account domain")
public class SigninRequest {

    @Email
    @NotBlank
    @Schema(description = "User's email address for authentication", example = "owner@acme.com")
    private String emailAddress;

    @NotBlank
    @Schema(description = "User's password for authentication", example = "P@ssw0rd123")
    private String password;
}
