package com.delight.account.controller;

import com.delight.account.dto.SessionResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.service.SessionService;
import com.delight.account.util.LoggingUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping({"/accounts", "/api/accounts"})
@Tag(name = "Session")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "cookieAuth")
public class SessionController {

    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);
    private static final String SESSION_COOKIE_NAME = "hrsession";

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/session")
    @Operation(
        summary = "Validate session",
        description = "Checks if the current session is valid and returns session information"
    )
    public ResponseEntity<SessionResponse> validateSession(HttpServletRequest request) {
        LoggingUtils.logBusinessOperation(logger, "SESSION_VALIDATION", "STARTED");

        // Get JWT token from cookie or Authorization header
        String jwtToken = getJwtTokenFromRequest(request);
        if (jwtToken == null) {
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

    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Invalidates the current session and clears the session cookie"
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
        description = "Invalidates all sessions for the current user"
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