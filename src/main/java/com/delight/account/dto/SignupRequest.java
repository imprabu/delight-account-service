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
@Schema(description = "Signup request to create new Organization and account with specified product")
public class SignupRequest {
    @NotBlank
    @Size(max = 255)
    @Schema(description = "Company or organization name", example = "Acme Corp")
    private String companyName;

    @NotBlank
    @Email
    @Size(max = 255)
    @Schema(description = "Email address of the account owner", example = "owner@acme.com")
    private String emailAddress;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Industry type or business category", example = "Software")
    private String industryType;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "First name of the account owner", example = "Alice")
    private String firstName;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Last name of the account owner", example = "Smith")
    private String lastName;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "Product code for the account", example = "CHAT")
    private String productCode;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "Region for the account", example = "us-east-1")
    private String region;
}
