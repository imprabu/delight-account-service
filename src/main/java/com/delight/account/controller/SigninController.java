package com.delight.account.controller;

import com.delight.account.annotation.RateLimit;
import com.delight.account.dto.SigninRequest;
import com.delight.account.dto.SigninResponse;
import com.delight.account.model.ClientType;
import com.delight.account.service.SigninService;
import com.delight.account.repository.AccountRepository;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.exception.ApiException;
import com.delight.account.util.LoggingUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@RequestMapping("/account")
@Tag(name = "Authentication", description = "User authentication endpoints including signup, signin, and account activation")
public class SigninController {

    private static final Logger logger = LoggerFactory.getLogger(SigninController.class);
    private final SigninService signinService;
    private final AccountRepository accountRepository;

    public SigninController(SigninService signinService, AccountRepository accountRepository) {
        this.signinService = signinService;
        this.accountRepository = accountRepository;
    }

    @PostMapping(value = "/signin", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE, produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(keyPrefix = "signin")
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
                description = "Signin response with JWT token and session ID",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SigninResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        value = "{\n  \"jwtToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2NvdW50SWQiOjEsInVzZXJJZCI6MSwicm9sZSI6Ik9XTkVSIiwib3JnSWQiOjEsInByb2R1Y3RDb2RlIjoiSFJNUyJ9.signature\",\n  \"sessionId\": \"sess_123456789\"\n}"
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
    public ResponseEntity<SigninResponse> signin(@Valid @RequestBody SigninRequest request, 
                                         HttpServletRequest httpRequest, 
                                         HttpServletResponse httpResponse) {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        String domain = attr.getRequest().getServerName();
        
        // Extract subdomain (company identifier) from server name
        if (domain != null) {
            domain = domain.toLowerCase();
            int dot = domain.indexOf('.');
            if (dot > 0) {
                domain = domain.substring(0, dot);
            }
        }
        
        LoggingUtils.logMethodEntry(logger, "signin", "domain", domain, "emailAddress", request.getEmailAddress());
        
        // Since signin is excluded from DomainAccountInterceptor, we need to resolve accountId manually
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(domain, AccountStatus.ACTIVE);
        if (accountOpt.isEmpty()) {
            LoggingUtils.logSecurityEvent(logger, "ACCOUNT_NOT_FOUND", "FAILED", "domain", domain);
            throw new ApiException("error.account.notfound");
        }
        
        Long accountId = accountOpt.get().getId();
        LoggingUtils.logBusinessOperation(logger, "ACCOUNT_RESOLUTION", "SUCCESS", "accountId", accountId, "domain", domain);
        
        // Extract user agent and IP address for session tracking
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);
        
        // Detect client type from x-request-source header
        String requestSource = httpRequest.getHeader("x-request-source");
        ClientType clientType = ClientType.fromRequestSource(requestSource);
        
        // Use the new session-based signin method with client type
        SigninResponse signinResponse = signinService.signinWithSession(accountId, domain, request, userAgent, ipAddress, clientType);
        
        // Set the JWT token as an HTTP-only cookie
        Cookie sessionCookie = new Cookie("hrsession", signinResponse.getJwtToken());
        sessionCookie.setHttpOnly(true);
        sessionCookie.setSecure(false); // Set to true in production with HTTPS
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(24 * 60 * 60); // 24 hours
        httpResponse.addCookie(sessionCookie);
        
        LoggingUtils.logMethodExit(logger, "signin", signinResponse);
        // Return the SigninResponse object as JSON
        return ResponseEntity.ok(signinResponse);
    }
    
    /**
     * Extracts the client IP address from the request, considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
