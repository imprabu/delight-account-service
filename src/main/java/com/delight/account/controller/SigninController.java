package com.delight.account.controller;

import com.delight.account.annotation.RateLimit;
import com.delight.account.dto.SigninRequest;
import com.delight.account.dto.SigninResponse;
import com.delight.account.model.ClientType;
import com.delight.account.model.Session;
import com.delight.account.service.SigninService;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.SessionRepository;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.exception.ApiException;
import com.delight.account.util.LoggingUtils;
import com.delight.account.util.DomainHelper;
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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@RequestMapping("/accounts")
@Tag(name = "Authentication", description = "User authentication endpoints including signup, signin, and account activation")
public class SigninController {

    private static final Logger logger = LoggerFactory.getLogger(SigninController.class);
    private static final String SESSION_COOKIE_NAME = "hrsession";
    
    private final SigninService signinService;
    private final AccountRepository accountRepository;
    private final SessionRepository sessionRepository;
    private final DomainHelper domainHelper;

    public SigninController(SigninService signinService, 
                           AccountRepository accountRepository,
                           SessionRepository sessionRepository,
                           DomainHelper domainHelper) {
        this.signinService = signinService;
        this.accountRepository = accountRepository;
        this.sessionRepository = sessionRepository;
        this.domainHelper = domainHelper;
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
        String domain = domainHelper.extractSubdomainFromServerName(attr.getRequest());
        
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
        
        // Get the session to calculate cookie expiration (createdAt + TTL)
        Optional<Session> sessionOpt = sessionRepository.findBySessionIdAndIsActiveTrue(signinResponse.getSessionId());
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            setSessionCookie(session, signinResponse.getJwtToken(), httpResponse);
        } else {
            // Fallback to default 24 hours if session not found
            Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, signinResponse.getJwtToken());
            sessionCookie.setHttpOnly(true);
            sessionCookie.setSecure(false); // Set to true in production with HTTPS
            sessionCookie.setPath("/");
            sessionCookie.setMaxAge(24 * 60 * 60); // 24 hours
            httpResponse.addCookie(sessionCookie);
        }
        
        LoggingUtils.logMethodExit(logger, "signin", signinResponse);
        // Return the SigninResponse object as JSON
        return ResponseEntity.ok(signinResponse);
    }
    
    /**
     * Set session cookie with expiration based on session createdAt + TTL.
     */
    private void setSessionCookie(Session session, String jwtToken, HttpServletResponse response) {
        OffsetDateTime createdAt = session.getCreatedAt();
        int ttlHours = session.getClientType().getDefaultTtlHours();
        OffsetDateTime expiresAt = createdAt.plusHours(ttlHours);
        
        // Calculate maxAge in seconds
        Duration duration = Duration.between(OffsetDateTime.now(), expiresAt);
        int maxAge = (int) duration.getSeconds();
        
        // Don't set negative maxAge
        if (maxAge <= 0) {
            maxAge = 0; // Cookie will expire immediately
        }
        
        Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, jwtToken);
        sessionCookie.setHttpOnly(true);
        sessionCookie.setSecure(false); // Set to true in production with HTTPS
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(maxAge);
        response.addCookie(sessionCookie);
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
