package com.delight.account.controller;

import com.delight.account.dto.SessionResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.service.SessionService;
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
@RequestMapping("/accounts")
@Tag(name = "Session")
@SecurityRequirement(name = "bearerAuth")
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
        logger.info("Session validation request");

        // Get JWT token from cookie or Authorization header
        String jwtToken = getJwtTokenFromRequest(request);
        if (jwtToken == null) {
            logger.warn("No JWT token found in request");
            throw new ApiException("error.session.notfound");
        }

        try {
            Optional<SessionResponse> sessionOpt = sessionService.validateSessionByToken(jwtToken);
            if (sessionOpt.isEmpty()) {
                logger.warn("Session not found for JWT token");
                throw new ApiException("error.session.notfound");
            }
            
            SessionResponse sessionResponse = sessionOpt.get();
            sessionService.updateLastAccessed(sessionResponse.getSessionId());
            
            logger.info("Session validation successful for sessionId={}", sessionResponse.getSessionId());
            return ResponseEntity.ok(sessionResponse);
        } catch (Exception e) {
            logger.warn("Session validation failed: {}", e.getMessage());
            throw new ApiException("error.session.invalid");
        }
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Invalidates the current session and clears the session cookie"
    )
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        logger.info("Logout request");

        // Get JWT token from cookie or Authorization header
        String jwtToken = getJwtTokenFromRequest(request);
        if (jwtToken != null) {
            try {
                Optional<SessionResponse> sessionOpt = sessionService.validateSessionByToken(jwtToken);
                if (sessionOpt.isPresent()) {
                    SessionResponse sessionResponse = sessionOpt.get();
                    sessionService.invalidateSession(sessionResponse.getSessionId());
                    logger.info("Session invalidated for sessionId={}", sessionResponse.getSessionId());
                }
            } catch (Exception e) {
                logger.warn("Failed to invalidate session: {}", e.getMessage());
            }
        }

        // Clear the session cookie
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Delete the cookie
        response.addCookie(cookie);

        logger.info("Logout successful");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout-all")
    @Operation(
        summary = "Logout from all sessions",
        description = "Invalidates all sessions for the current user"
    )
    public ResponseEntity<Void> logoutAll(HttpServletRequest request, HttpServletResponse response) {
        logger.info("Logout all sessions request");

        // Get JWT token from cookie or Authorization header
        String jwtToken = getJwtTokenFromRequest(request);
        if (jwtToken != null) {
            try {
                Optional<SessionResponse> sessionOpt = sessionService.validateSessionByToken(jwtToken);
                if (sessionOpt.isPresent()) {
                    SessionResponse sessionResponse = sessionOpt.get();
                    sessionService.invalidateAllUserSessions(sessionResponse.getUserId());
                    logger.info("All sessions invalidated for userId={}", sessionResponse.getUserId());
                }
            } catch (Exception e) {
                logger.warn("Failed to invalidate all sessions: {}", e.getMessage());
            }
        }

        // Clear the session cookie
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Delete the cookie
        response.addCookie(cookie);

        logger.info("Logout all successful");
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