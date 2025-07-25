package com.delight.account.controller;

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

@RestController
@RequestMapping("/account")
public class SignupController {

    private final SignupService signupService;
    private final MessageSource messageSource;

    public SignupController(SignupService signupService, MessageSource messageSource) {
        this.signupService = signupService;
        this.messageSource = messageSource;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request,
                                         @RequestHeader(name = "Accept-Language", required = false) Locale locale) {
        signupService.signup(request);
        String message = messageSource.getMessage("signup.success", null,
            locale == null ? Locale.ENGLISH : locale);
        return ResponseEntity.ok(message);
    }
}
