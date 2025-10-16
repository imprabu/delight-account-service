package com.delight.account.controller;

import com.delight.account.annotation.RateLimit;
import com.delight.account.dto.ActivationRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.service.ActivationService;
import com.delight.account.repository.AccountRepository;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/account")
@Tag(name = "Auth")
public class ActivationController {

    private static final Logger logger = LoggerFactory.getLogger(ActivationController.class);

    private final ActivationService activationService;
    private final MessageSource messageSource;
    private final AccountRepository accountRepository;

    public ActivationController(ActivationService activationService, MessageSource messageSource, AccountRepository accountRepository) {
        this.activationService = activationService;
        this.messageSource = messageSource;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/activate")
    @RateLimit(keyPrefix = "activation")
    @Operation(
        summary = "Activate user",
        description = "Sets password and activates membership for the current account domain",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "ActivationRequest",
                    value = "{\n  \"emailAddress\": \"owner@acme.com\",\n  \"firstName\": \"Alice\",\n  \"lastName\": \"Smith\",\n  \"phoneNumber\": \"+1-555-123-4567\",\n  \"password\": \"P@ssw0rd123\",\n  \"retypePassword\": \"P@ssw0rd123\"\n}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Activation success",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "text/plain",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "Activation successful")
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
    public ResponseEntity<String> activate(@Valid @RequestBody ActivationRequest request,
                                           @RequestHeader(name = "Accept-Language", required = false) Locale locale) {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        
        // Since activation is excluded from DomainAccountInterceptor, we need to resolve accountId manually
        String host = attr.getRequest().getHeader("Host");
        if (host == null || host.isBlank()) {
            host = attr.getRequest().getServerName();
        }
        if (host != null) {
            host = host.toLowerCase();
            int colon = host.indexOf(':');
            if (colon > 0) {
                host = host.substring(0, colon);
            }
            int dot = host.indexOf('.');
            if (dot > 0) {
                host = host.substring(0, dot);
            }
        }
        
        logger.info("Activation request - processing domain: {} for email: {}", host, request.getEmailAddress());
        
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(host, AccountStatus.ACTIVE);
        if (accountOpt.isEmpty()) {
            logger.warn("No active account found for domain: {}", host);
            throw new ApiException("error.account.notfound");
        }
        
        Long accountId = accountOpt.get().getId();
        logger.info("Resolved accountId: {} for domain: {}", accountId, host);
        
        String country = attr.getRequest().getLocale().getCountry();
        logger.info("Activation request accountId={} emailAddress={} firstName={} lastName={} phoneNumber={} country={}",
            accountId, request.getEmailAddress(), request.getFirstName(), request.getLastName(), request.getPhoneNumber(), country);
        activationService.activateUser(accountId, request, country);
        String message = messageSource.getMessage("activation.success", null,
            locale == null ? Locale.ENGLISH : locale);
        return ResponseEntity.ok(message);
    }


}
