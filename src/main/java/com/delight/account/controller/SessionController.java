package com.delight.account.controller;

import com.delight.account.dto.SessionResponse;
import com.delight.account.dto.SessionValidationRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.service.SessionService;
import com.delight.account.util.LoggingUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping({"/accounts", "/api/accounts", "/channel/accounts"})
@Tag(name = "Session Management", description = "Session validation, logout, and JWT token management for regular, API, and channel endpoints")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "cookieAuth")
public class SessionController {

    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);
    private static final String SESSION_COOKIE_NAME = "hrsession";

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping(value = "/session", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/session", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Validate session",
        description = "Validates the current session using JWT token from cookie, Authorization header, or request body (for POST). Supports both GET (token from cookie/header) and POST (token from request body) methods. Works with regular, API, and channel endpoints.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            description = "Optional request body for POST requests containing JWT token. Not required for GET requests.",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SessionValidationRequest.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "SessionValidationRequest",
                    value = "{\n  \"jwtToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\"\n}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Session validation successful",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SessionResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Invalid or expired session",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Session not found",
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
    public ResponseEntity<SessionResponse> validateSession(
            HttpServletRequest request, 
            @RequestBody(required = false) @Valid SessionValidationRequest validationRequest) {
        LoggingUtils.logBusinessOperation(logger, "SESSION_VALIDATION", "STARTED");

        // Check if this is a channel endpoint request
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/channel/")) {
            // For channel endpoints, the JWT has already been validated by JwtAuthenticationFilter
            // We just need to return a success response with the account information
            String accountId = (String) request.getAttribute("accountId");
            String keyVersion = (String) request.getAttribute("keyVersion");
            
            if (accountId == null) {
                LoggingUtils.logSecurityEvent(logger, "CHANNEL_SESSION_VALIDATION", "FAILED", "reason", "No account ID found in request attributes");
                throw new ApiException("error.session.invalid");
            }
            
            // Create a channel session response
            SessionResponse channelResponse = SessionResponse.builder()
                    .accountId(Long.valueOf(accountId))
                    .isActive(true)
                    .sessionId("channel-" + accountId + "-" + System.currentTimeMillis())
                    .build();
            
            LoggingUtils.logBusinessOperation(logger, "CHANNEL_SESSION_VALIDATION", "SUCCESS", "accountId", accountId, "keyVersion", keyVersion);
            return ResponseEntity.ok(channelResponse);
        }

        // Regular session validation for non-channel endpoints
        String jwtToken = null;
        
        // For POST requests, try to get token from request body first
        if ("POST".equalsIgnoreCase(request.getMethod()) && validationRequest != null) {
            jwtToken = validationRequest.getJwtToken();
        }
        
        // If no token from request body, try to get from cookie or Authorization header
        if (jwtToken == null || jwtToken.trim().isEmpty()) {
            jwtToken = getJwtTokenFromRequest(request);
        }
        
        // If still no token found, throw error
        if (jwtToken == null || jwtToken.trim().isEmpty()) {
            LoggingUtils.logSecurityEvent(logger, "SESSION_VALIDATION", "FAILED", "reason", "No JWT token found");
            throw new ApiException("error.session.notfound");
        }

        try {
            Optional<SessionResponse> sessionOpt = sessionService.validateSessionByToken(jwtToken);
            if (sessionOpt.isEmpty()) {
                LoggingUtils.logSecurityEvent(logger, "SESSION_VALIDATION", "FAILED", "reason", "Session not found for JWT token");
                throw new ApiException("error.session.notfound");
            }
            
            SessionResponse sessionResponse = sessionOpt.get();
            sessionService.updateLastAccessed(sessionResponse.getSessionId());
            
            LoggingUtils.logBusinessOperation(logger, "SESSION_VALIDATION", "SUCCESS", "sessionId", sessionResponse.getSessionId());
            return ResponseEntity.ok(sessionResponse);
        } catch (Exception e) {
            LoggingUtils.logSecurityEvent(logger, "SESSION_VALIDATION", "FAILED", "error", e.getMessage());
            throw new ApiException("error.session.invalid");
        }
    }

    @PostMapping(value = "/session/validate", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Validate session with JWT token",
        description = "Validates a session using JWT token provided in request body. Used by subsystems to validate sessions from incoming requests.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SessionValidationRequest.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "SessionValidationRequest",
                    value = "{\n  \"jwtToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2NvdW50SWQiOjEsInVzZXJJZCI6MSwicm9sZSI6Ik9XTkVSIiwib3JnSWQiOjEsInByb2R1Y3RDb2RlIjoiSFJNUyJ9.signature\"\n}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Session validation successful",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SessionResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Bad request - Invalid JWT token format",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Invalid or expired session",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Session not found",
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
    public ResponseEntity<SessionResponse> validateSessionWithToken(@Valid @RequestBody SessionValidationRequest request) {
        LoggingUtils.logBusinessOperation(logger, "SESSION_VALIDATION_WITH_TOKEN", "STARTED");

        String jwtToken = request.getJwtToken();
        if (jwtToken == null || jwtToken.trim().isEmpty()) {
            LoggingUtils.logSecurityEvent(logger, "SESSION_VALIDATION_WITH_TOKEN", "FAILED", "reason", "JWT token is empty");
            throw new ApiException("error.session.token.required");
        }

        try {
            Optional<SessionResponse> sessionOpt = sessionService.validateSessionByToken(jwtToken);
            if (sessionOpt.isEmpty()) {
                LoggingUtils.logSecurityEvent(logger, "SESSION_VALIDATION_WITH_TOKEN", "FAILED", "reason", "Session not found for JWT token");
                throw new ApiException("error.session.notfound");
            }
            
            SessionResponse sessionResponse = sessionOpt.get();
            sessionService.updateLastAccessed(sessionResponse.getSessionId());
            
            LoggingUtils.logBusinessOperation(logger, "SESSION_VALIDATION_WITH_TOKEN", "SUCCESS", "sessionId", sessionResponse.getSessionId());
            return ResponseEntity.ok(sessionResponse);
        } catch (Exception e) {
            LoggingUtils.logSecurityEvent(logger, "SESSION_VALIDATION_WITH_TOKEN", "FAILED", "error", e.getMessage());
            throw new ApiException("error.session.invalid");
        }
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Invalidates the current session and clears the session cookie. Requires valid authentication.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Logout successful"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Invalid or expired session",
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
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        LoggingUtils.logBusinessOperation(logger, "LOGOUT", "STARTED");

        // Get JWT token from cookie or Authorization header
        String jwtToken = getJwtTokenFromRequest(request);
        if (jwtToken != null) {
            try {
                Optional<SessionResponse> sessionOpt = sessionService.validateSessionByToken(jwtToken);
                if (sessionOpt.isPresent()) {
                    SessionResponse sessionResponse = sessionOpt.get();
                    sessionService.invalidateSession(sessionResponse.getSessionId());
                    LoggingUtils.logBusinessOperation(logger, "SESSION_INVALIDATION", "SUCCESS", "sessionId", sessionResponse.getSessionId());
                }
            } catch (Exception e) {
                LoggingUtils.logSecurityEvent(logger, "SESSION_INVALIDATION", "FAILED", "error", e.getMessage());
            }
        }

        // Clear the session cookie
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Delete the cookie
        response.addCookie(cookie);

        LoggingUtils.logBusinessOperation(logger, "LOGOUT", "SUCCESS");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout-all")
    @Operation(
        summary = "Logout from all sessions",
        description = "Invalidates all sessions for the current user across all devices and clears the session cookie. Requires valid authentication.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "All sessions invalidated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Invalid or expired session",
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
    public ResponseEntity<Void> logoutAll(HttpServletRequest request, HttpServletResponse response) {
        LoggingUtils.logBusinessOperation(logger, "LOGOUT_ALL", "STARTED");

        // Get JWT token from cookie or Authorization header
        String jwtToken = getJwtTokenFromRequest(request);
        if (jwtToken != null) {
            try {
                Optional<SessionResponse> sessionOpt = sessionService.validateSessionByToken(jwtToken);
                if (sessionOpt.isPresent()) {
                    SessionResponse sessionResponse = sessionOpt.get();
                    sessionService.invalidateAllUserSessions(sessionResponse.getUserId());
                    LoggingUtils.logBusinessOperation(logger, "ALL_SESSIONS_INVALIDATION", "SUCCESS", "userId", sessionResponse.getUserId());
                }
            } catch (Exception e) {
                LoggingUtils.logSecurityEvent(logger, "ALL_SESSIONS_INVALIDATION", "FAILED", "error", e.getMessage());
            }
        }

        // Clear the session cookie
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Delete the cookie
        response.addCookie(cookie);

        LoggingUtils.logBusinessOperation(logger, "LOGOUT_ALL", "SUCCESS");
        return ResponseEntity.ok().build();
    }

    private String getJwtTokenFromRequest(HttpServletRequest request) {
        // First try to get from cookie
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // Fallback to Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }
}