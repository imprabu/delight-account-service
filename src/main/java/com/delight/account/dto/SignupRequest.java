package com.delight.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SignupRequest {
    @NotBlank
    @Size(max = 255)
    private String companyName;

    @NotBlank
    @Email
    @Size(max = 255)
    private String emailAddress;

    @NotBlank
    @Size(max = 255)
    private String industryType;

    @NotBlank
    @Size(max = 255)
    private String firstName;

    @NotBlank
    @Size(max = 255)
    private String lastName;
}
