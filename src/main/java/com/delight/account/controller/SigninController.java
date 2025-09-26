package com.delight.account.controller;

import com.delight.account.dto.SigninRequest;
import com.delight.account.service.SigninService;
import com.delight.account.repository.AccountRepository;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.exception.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;

@RestController
@RequestMapping("/account")
@Tag(name = "Auth")
public class SigninController {

    private final SigninService signinService;
    private final AccountRepository accountRepository;

    public SigninController(SigninService signinService, AccountRepository accountRepository) {
        this.signinService = signinService;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/signin")
    @Operation(
        summary = "User signin",
        description = "Validates credentials and membership, returns JWT for the current account domain",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "SigninRequest",
                    value = "{\n  \"emailAddress\": \"owner@acme.com\",\n  \"password\": \"P@ssw0rd123\"\n}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "JWT token",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "text/plain",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        value = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2NvdW50SWQiOjEsInVzZXJJZCI6MSwicm9sZSI6Ik9XTkVSIiwib3JnSWQiOjEsInByb2R1Y3RDb2RlIjoiSFJNUyJ9.signature"
                    )
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
                responseCode = "401",
                description = "Unauthorized",
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
    public ResponseEntity<String> signin(@Valid @RequestBody SigninRequest request) {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        String domain = attr.getRequest().getServerName();
        
        System.out.println("DEBUG: SigninController - domain: " + domain);
        System.out.println("DEBUG: SigninController - request email: " + request.getEmailAddress());
        
        // Since signin is excluded from DomainAccountInterceptor, we need to resolve accountId manually
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(domain, AccountStatus.ACTIVE);
        if (accountOpt.isEmpty()) {
            System.out.println("DEBUG: SigninController - No account found for domain: " + domain);
            throw new ApiException("error.account.notfound");
        }
        
        Long accountId = accountOpt.get().getId();
        System.out.println("DEBUG: SigninController - resolved accountId: " + accountId);
        
        String token = signinService.signin(accountId, domain, request);
        return ResponseEntity.ok(token);
    }
}
