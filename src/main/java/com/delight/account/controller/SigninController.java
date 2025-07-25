package com.delight.account.controller;

import com.delight.account.dto.SigninRequest;
import com.delight.account.service.SigninService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RestController
@RequestMapping("/api")
public class SigninController {

    private final SigninService signinService;

    public SigninController(SigninService signinService) {
        this.signinService = signinService;
    }

    @PostMapping("/signin")
    public ResponseEntity<String> signin(@Valid @RequestBody SigninRequest request) {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        Long accountId = (Long) attr.getRequest().getAttribute("accountId");
        String token = signinService.signin(accountId, request);
        return ResponseEntity.ok(token);
    }
}

