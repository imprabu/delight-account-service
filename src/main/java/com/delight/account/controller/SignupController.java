package com.delight.account.controller;

import com.delight.account.annotation.RateLimit;
import com.delight.account.dto.SignupRequest;
import com.delight.account.service.SignupService;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/account")
@Tag(name = "Auth", description = "Signup, activation, and signin")
public class SignupController {

    private final SignupService signupService;
    private final MessageSource messageSource;

    public SignupController(SignupService signupService, MessageSource messageSource) {
        this.signupService = signupService;
        this.messageSource = messageSource;
    }

    @PostMapping("/signup")
    @RateLimit(keyPrefix = "signup")
    @Operation(
        summary = "Signup new organization and HRMS account",
        description = "Creates Organization, HRMS Account, initial User and OWNER membership",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "SignupRequest",
                    value = "{\n  \"companyName\": \"Acme Corp\",\n  \"emailAddress\": \"owner@acme.com\",\n  \"industryType\": \"Software\",\n  \"firstName\": \"Alice\",\n  \"lastName\": \"Smith\"\n}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Signup success",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "text/plain",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "Signup successful")
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Bad request",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            )
        }
    )
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request,
                                         @RequestHeader(name = "Accept-Language", required = false) Locale locale) {
        signupService.signup(request);
        String message = messageSource.getMessage("signup.success", null,
            locale == null ? Locale.ENGLISH : locale);
        return ResponseEntity.ok(message);
    }
}
