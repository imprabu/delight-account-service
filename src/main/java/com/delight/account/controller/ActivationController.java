package com.delight.account.controller;

import com.delight.account.annotation.RateLimit;
import com.delight.account.dto.ActivationRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.service.ActivationService;
import com.delight.account.repository.AccountRepository;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.util.LoggingUtils;
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
        
        // Since activate is excluded from DomainAccountInterceptor, we need to resolve accountId manually
        String domain = attr.getRequest().getServerName();
        
        // Extract subdomain (company identifier) from server name
        if (domain != null) {
            domain = domain.toLowerCase();
            int dot = domain.indexOf('.');
            if (dot > 0) {
                domain = domain.substring(0, dot);
            }
        }
        
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(domain, AccountStatus.ACTIVE);
        if (accountOpt.isEmpty()) {
            throw new ApiException("error.account.notfound");
        }
        
        Long accountId = accountOpt.get().getId();
        
        String country = attr.getRequest().getLocale().getCountry();
        LoggingUtils.info(logger, "Activation request accountId={} emailAddress={} firstName={} lastName={} phoneNumber={} country={}",
            accountId, request.getEmailAddress(), request.getFirstName(), request.getLastName(), request.getPhoneNumber(), country);
        activationService.activateUser(accountId, request, country);
        String message = messageSource.getMessage("activation.success", null,
            locale == null ? Locale.ENGLISH : locale);
        return ResponseEntity.ok(message);
    }


}
