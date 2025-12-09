package com.delight.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Activation request to set password and activate membership")
public class ActivationRequest {
    @NotBlank
    @Email
    @Size(max = 255)
    @Schema(description = "Email address of the user to activate", example = "owner@acme.com")
    private String emailAddress;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "First name of the user", example = "Alice")
    private String firstName;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Last name of the user", example = "Smith")
    private String lastName;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "Phone number for contact purposes", example = "+1-555-123-4567")
    private String phoneNumber;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Password for the user account", example = "P@ssw0rd123")
    private String password;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Password confirmation (must match password)", example = "P@ssw0rd123")
    private String retypePassword;
}
