package com.delight.account.controller;

import com.delight.account.annotation.RateLimit;
import com.delight.account.dto.SigninRequest;
import com.delight.account.dto.SigninResponse;
import com.delight.account.service.SigninService;
import com.delight.account.repository.AccountRepository;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.exception.ApiException;
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
    public ResponseEntity<String> signin(@Valid @RequestBody SigninRequest request, 
                                         HttpServletRequest httpRequest, 
                                         HttpServletResponse httpResponse) {
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
        
        // Extract user agent and IP address for session tracking
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);
        
        // Use the new session-based signin method
        SigninResponse signinResponse = signinService.signinWithSession(accountId, domain, request, userAgent, ipAddress);
        
        // Set the JWT token as an HTTP-only cookie
        Cookie sessionCookie = new Cookie("hrsession", signinResponse.getJwtToken());
        sessionCookie.setHttpOnly(true);
        sessionCookie.setSecure(false); // Set to true in production with HTTPS
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(24 * 60 * 60); // 24 hours
        httpResponse.addCookie(sessionCookie);
        
        // Return the JWT token in the response body for backward compatibility
        return ResponseEntity.ok(signinResponse.getJwtToken());
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
