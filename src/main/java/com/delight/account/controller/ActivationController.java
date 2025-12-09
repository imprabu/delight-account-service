package com.delight.account.controller;

import com.delight.account.annotation.RateLimit;
import com.delight.account.dto.ActivationRequest;
import com.delight.account.dto.ActivationTokenResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.model.ActivationToken;
import com.delight.account.model.User;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.UserRepository;
import com.delight.account.service.ActivationService;
import com.delight.account.service.ActivationTokenService;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.util.LoggingUtils;
import com.delight.account.util.DomainHelper;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/accounts")
@Tag(name = "Authentication", description = "User authentication endpoints including signup, signin, and account activation")
public class ActivationController {

    private static final Logger logger = LoggerFactory.getLogger(ActivationController.class);

    private final ActivationService activationService;
    private final MessageSource messageSource;
    private final AccountRepository accountRepository;
    private final DomainHelper domainHelper;
    private final ActivationTokenService activationTokenService;
    private final UserRepository userRepository;

    public ActivationController(ActivationService activationService, MessageSource messageSource, 
                                AccountRepository accountRepository, DomainHelper domainHelper,
                                ActivationTokenService activationTokenService, UserRepository userRepository) {
        this.activationService = activationService;
        this.messageSource = messageSource;
        this.accountRepository = accountRepository;
        this.domainHelper = domainHelper;
        this.activationTokenService = activationTokenService;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/activate", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE, produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(keyPrefix = "activation")
    @Operation(
        summary = "Activate user account",
        description = "Sets password and activates user membership for the current account domain. Completes the user registration process.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ActivationRequest.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "ActivationRequest",
                    value = "{\n  \"emailAddress\": \"owner@acme.com\",\n  \"firstName\": \"Alice\",\n  \"lastName\": \"Smith\",\n  \"phoneNumber\": \"+1-555-123-4567\",\n  \"password\": \"P@ssw0rd123\",\n  \"retypePassword\": \"P@ssw0rd123\"\n}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Account activation successful",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "ActivationResponse",
                        value = "{\"message\": \"Activation successful\"}"
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Bad request - Invalid activation data or password mismatch",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Account not found or already activated",
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
    public ResponseEntity<String> activate(@Valid @RequestBody ActivationRequest request,
                                           @RequestParam(name = "token", required = false) String token) {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        
        Long accountId;
        
        // If token is provided, use it to get accountId and validate
        if (token != null && !token.trim().isEmpty()) {
            LoggingUtils.logBusinessOperation(logger, "TOKEN_BASED_ACTIVATION", "STARTED", "token", "***");
            
            ActivationToken activationToken = activationTokenService.validateAndConsumeToken(token);
            if (activationToken == null) {
                LoggingUtils.logSecurityEvent(logger, "INVALID_ACTIVATION_TOKEN", "FAILED", "token", "***");
                throw new ApiException("error.activation.token.invalid");
            }
            
            // Verify email matches
            if (!activationToken.getEmailAddress().equalsIgnoreCase(request.getEmailAddress())) {
                LoggingUtils.logSecurityEvent(logger, "ACTIVATION_EMAIL_MISMATCH", "FAILED", 
                    "tokenEmail", activationToken.getEmailAddress(), 
                    "requestEmail", request.getEmailAddress());
                throw new ApiException("error.activation.email.mismatch");
            }
            
            accountId = activationToken.getAccountId();
            LoggingUtils.logBusinessOperation(logger, "TOKEN_BASED_ACTIVATION", "VALIDATED", 
                "accountId", accountId, "emailAddress", request.getEmailAddress());
        } else {
            // Traditional domain-based activation
            String domain = domainHelper.extractSubdomainFromServerName(attr.getRequest());
            
            Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(domain, AccountStatus.ACTIVE);
            if (accountOpt.isEmpty()) {
                throw new ApiException("error.account.notfound");
            }
            
            accountId = accountOpt.get().getId();
        }
        
        // Get locale from request (Spring's LocaleResolver handles Accept-Language header properly)
        Locale locale = attr.getRequest().getLocale();
        String country = locale.getCountry();
        LoggingUtils.info(logger, "Activation request accountId={} emailAddress={} firstName={} lastName={} phoneNumber={} country={}",
            accountId, request.getEmailAddress(), request.getFirstName(), request.getLastName(), request.getPhoneNumber(), country);
        activationService.activateUser(accountId, request, country);
        String message = messageSource.getMessage("activation.success", null,
            locale == null ? Locale.ENGLISH : locale);
        return ResponseEntity.ok("{\"message\":\"" + message + "\"}");
    }

    @GetMapping(value = "/activate/validate-token", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Validate activation token",
        description = "Validates an activation token and returns user information for pre-filling the activation form. Does not consume the token.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Token validation result",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ActivationTokenResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid or missing token",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            )
        }
    )
    public ResponseEntity<ActivationTokenResponse> validateToken(@RequestParam("token") String token) {
        LoggingUtils.logMethodEntry(logger, "validateToken", "token", token != null ? "***" : null);
        
        if (token == null || token.trim().isEmpty()) {
            ActivationTokenResponse response = new ActivationTokenResponse();
            response.setValid(false);
            response.setErrorMessage("Token is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        ActivationToken activationToken = activationTokenService.validateToken(token);
        
        if (activationToken == null) {
            ActivationTokenResponse response = new ActivationTokenResponse();
            response.setValid(false);
            response.setErrorMessage("Invalid, expired, or already used token");
            LoggingUtils.logSecurityEvent(logger, "INVALID_ACTIVATION_TOKEN", "FAILED", "token", "***");
            return ResponseEntity.ok(response);
        }
        
        // Fetch user information from the token
        Optional<User> userOpt = userRepository.findByAccountIdAndEmailAddress(
            activationToken.getAccountId(), 
            activationToken.getEmailAddress()
        );
        
        ActivationTokenResponse response = new ActivationTokenResponse();
        response.setValid(true);
        response.setEmailAddress(activationToken.getEmailAddress());
        response.setAccountId(activationToken.getAccountId());
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            response.setFirstName(user.getFirstName());
            response.setLastName(user.getLastName());
        }
        
        LoggingUtils.logMethodExit(logger, "validateToken", response);
        return ResponseEntity.ok(response);
    }

}
