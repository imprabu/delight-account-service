package com.delight.account.controller;

import com.delight.account.dto.SessionResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.service.SessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/account")
public class SessionAliasController {

    private static final String SESSION_COOKIE_NAME = "hrsession";
    private final SessionService sessionService;

    public SessionAliasController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/validate-session")
    public ResponseEntity<SessionResponse> validateSession(HttpServletRequest request) {
        String jwtToken = getJwtTokenFromRequest(request);
        if (jwtToken == null) {
            throw new ApiException("error.session.notfound");
        }
        Optional<SessionResponse> sessionOpt = sessionService.validateSessionByToken(jwtToken);
        if (sessionOpt.isEmpty()) {
            throw new ApiException("error.session.notfound");
        }
        sessionService.updateLastAccessed(sessionOpt.get().getSessionId());
        return ResponseEntity.ok(sessionOpt.get());
    }

    @PostMapping("/signout")
    public ResponseEntity<Void> signout(HttpServletRequest request, HttpServletResponse response) {
        String jwtToken = getJwtTokenFromRequest(request);
        if (jwtToken != null) {
            Optional<SessionResponse> sessionOpt = sessionService.validateSessionByToken(jwtToken);
            sessionOpt.ifPresent(sr -> sessionService.invalidateSession(sr.getSessionId()));
        }
        clearCookie(response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invalidate-all-sessions")
    public ResponseEntity<Void> signoutAll(HttpServletRequest request, HttpServletResponse response) {
        String jwtToken = getJwtTokenFromRequest(request);
        if (jwtToken != null) {
            Optional<SessionResponse> sessionOpt = sessionService.validateSessionByToken(jwtToken);
            sessionOpt.ifPresent(sr -> sessionService.invalidateAllUserSessions(sr.getUserId()));
        }
        clearCookie(response);
        return ResponseEntity.ok().build();
    }

    private void clearCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String getJwtTokenFromRequest(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}

