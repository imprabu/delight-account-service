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
@Schema(description = "Signup request to create new Organization and HRMS account")
public class SignupRequest {
    @NotBlank
    @Size(max = 255)
    @Schema(example = "Acme Corp")
    private String companyName;

    @NotBlank
    @Email
    @Size(max = 255)
    @Schema(example = "owner@acme.com")
    private String emailAddress;

    @NotBlank
    @Size(max = 255)
    @Schema(example = "Software")
    private String industryType;

    @NotBlank
    @Size(max = 255)
    @Schema(example = "Alice")
    private String firstName;

    @NotBlank
    @Size(max = 255)
    @Schema(example = "Smith")
    private String lastName;
}
