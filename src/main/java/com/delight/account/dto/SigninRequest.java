package com.delight.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SigninRequest {

    @Email
    @NotBlank
    private String emailAddress;

    @NotBlank
    private String password;
}

