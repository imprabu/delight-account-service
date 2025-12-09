package com.delight.account.filter;

import com.delight.account.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.delight.account.model.Membership;
import com.delight.account.model.MembershipStatus;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.filter.MutableHttpServletRequest;
import com.delight.account.service.SessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import org.springframework.core.annotation.Order;

@Component
@Order(3)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final String jwtSecret;
    private final MembershipRepository membershipRepository;
    private final SessionService sessionService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(@Value("${security.jwt.secret}") String jwtSecret,
                                   MembershipRepository membershipRepository,
                                   SessionService sessionService,
                                   ObjectMapper objectMapper) {
        this.jwtSecret = jwtSecret;
        this.membershipRepository = membershipRepository;
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
        logger.info("=== JwtAuthenticationFilter initialized ===");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Exclude authentication endpoints
        boolean shouldSkip = path.equals("/accounts/signup") || path.equals("/accounts/activate") || 
               path.equals("/accounts/activate/validate-token") || path.equals("/accounts/signin") ||
               path.equals("/accounts/session") || path.equals("/accounts/validate-session") ||
               path.equals("/accounts/signout") || path.equals("/accounts/invalidate-all-sessions") ||
               path.equals("/accounts/logout") || path.equals("/accounts/logout-all") ||
               path.equals("/signup") || path.equals("/activate") || path.equals("/signin") || // Handle normalized paths
               path.equals("/swagger-ui.html") || path.startsWith("/swagger-ui/") || 
               path.equals("/v3/api-docs") || path.startsWith("/v3/api-docs/") ||
               path.equals("/actuator/health") ||
               path.startsWith("/channel/") || // Exclude /channel endpoints - handled by ChannelAuthFilter
               path.startsWith("/test/"); // Exclude /test endpoints for development
        
        // Exclude static assets and frontend files
        if (!shouldSkip) {
            // Static assets directory
            if (path.startsWith("/assets/")) {
                shouldSkip = true;
            }
            // Static file extensions (JS, CSS, images, fonts, etc.)
            else if (isStaticAsset(path)) {
                shouldSkip = true;
            }
            // Frontend HTML files
            else if (path.equals("/index.html") || path.equals("/") || path.endsWith(".html")) {
                shouldSkip = true;
            }
            // Frontend SPA routes - exclude common frontend routes that are not API paths
            else if (isFrontendRoute(path)) {
                shouldSkip = true;
            }
        }
        
        logger.debug("=== JwtAuthenticationFilter.shouldNotFilter: path={}, shouldSkip={} ===", path, shouldSkip);
        return shouldSkip;
    }
    
    /**
     * Check if the path is a static asset (has a file extension).
     */
    private boolean isStaticAsset(String path) {
        return path.contains(".") && 
               (path.endsWith(".js") || path.endsWith(".css") || 
                path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                path.endsWith(".gif") || path.endsWith(".svg") || path.endsWith(".ico") ||
                path.endsWith(".woff") || path.endsWith(".woff2") || path.endsWith(".ttf") ||
                path.endsWith(".eot") || path.endsWith(".json") || path.endsWith(".map") ||
                path.endsWith(".webp") || path.endsWith(".avif"));
    }
    
    /**
     * Check if the path is a frontend route (SPA routing).
     */
    private boolean isFrontendRoute(String path) {
        // Common frontend routes that should not require authentication
        return path.equals("/") ||
               path.equals("/login") ||
               path.equals("/signup") ||
               path.equals("/dashboard") ||
               path.equals("/settings") ||
               path.equals("/contacts") ||
               path.equals("/conversations") ||
               path.equals("/inbox") ||
               path.equals("/analytics") ||
               path.equals("/integrations") ||
               path.equals("/knowledge-base") ||
               path.equals("/flows") ||
               path.equals("/channels") ||
               path.equals("/billing") ||
               path.equals("/security");
    }
    
    /**
     * Check if the path is an API endpoint.
     */
    private boolean isApiPath(String path) {
        return path.startsWith("/api/") ||
               path.startsWith("/accounts/") ||
               path.startsWith("/channel/") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/actuator") ||
               path.startsWith("/test/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        logger.info("=== JwtAuthenticationFilter.doFilterInternal START: path={}, method={} ===", 
                   path, request.getMethod());
        
        // Double-check that we should process this request (shouldNotFilter should have caught this, but be defensive)
        if (shouldNotFilter(request)) {
            logger.info("=== JwtAuthenticationFilter: Request should be skipped, passing to next filter ===");
            filterChain.doFilter(request, response);
            return;
        }
        
        // Handle regular JWT authentication for non-channel endpoints
        // Channel endpoints are excluded in shouldNotFilter() and handled by ChannelAuthFilter
        handleRegularAuthentication(request, response, filterChain);
    }
    
    private void handleRegularAuthentication(HttpServletRequest request, HttpServletResponse response,
                                           FilterChain filterChain) throws ServletException, IOException {
        logger.info("=== JwtAuthenticationFilter.handleRegularAuthentication START ===");
        
        // Try to get token from Authorization header first, then fallback to cookie
        String token = null;
        String header = request.getHeader("Authorization");
        logger.info("=== JwtAuthenticationFilter: Authorization header present={} ===", header != null);
        
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
            logger.info("=== JwtAuthenticationFilter: Using token from Authorization header ===");
        } else {
            // Fallback to cookie-based authentication
            token = getJwtTokenFromCookie(request);
            if (token != null) {
                logger.info("=== JwtAuthenticationFilter: Using token from cookie ===");
            } else {
                logger.warn("=== JwtAuthenticationFilter: Missing or invalid Authorization header and no cookie found ===");
                writeUnauthorized(request, response, "error.unauthorized", "Missing or invalid Authorization header or cookie");
                return;
            }
        }
        try {
            Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

            Long accountId = claims.get("accountId", Long.class);
            Long userId = claims.get("userId", Long.class);
            String role = claims.get("role", String.class);
            Long orgId = claims.get("orgId", Long.class);
            String productCode = claims.get("productCode", String.class);
            String sessionId = claims.get("sessionId", String.class);

            if (accountId == null || userId == null) {
                writeUnauthorized(request, response, "error.unauthorized", "Invalid token claims");
                return;
            }

            // Validate session if sessionId is present in the token
            if (sessionId != null) {
                if (sessionService.validateSessionByToken(token).isEmpty()) {
                    writeUnauthorized(request, response, "error.unauthorized", "Session is invalid or expired");
                    return;
                }
            }

            Membership membership = membershipRepository
                .findByUserIdAndAccountIdWithRole(userId, accountId)
                .orElse(null);
            if (membership == null || membership.getStatus() != MembershipStatus.ACTIVE) {
                writeUnauthorized(request, response, "error.authorization", "Inactive or missing membership");
                return;
            }
            // Prefer DB-sourced role if available
            if (membership != null && membership.getRole() != null) {
                role = membership.getRole().getCode();
            }

            MutableHttpServletRequest mutableRequest = request instanceof MutableHttpServletRequest
                ? (MutableHttpServletRequest) request
                : new MutableHttpServletRequest(request);

            mutableRequest.putHeader("accountId", String.valueOf(accountId));
            mutableRequest.setAttribute("accountId", accountId);
            mutableRequest.putHeader("userId", String.valueOf(userId));
            mutableRequest.setAttribute("userId", userId);
            if (orgId != null) {
                mutableRequest.putHeader("orgId", String.valueOf(orgId));
                mutableRequest.setAttribute("orgId", orgId);
            }
            if (productCode != null) {
                mutableRequest.putHeader("productCode", productCode);
                mutableRequest.setAttribute("productCode", productCode);
            }
            if (role != null) {
                mutableRequest.putHeader("role", role);
                mutableRequest.setAttribute("role", role);
            }

            // Set Spring Security context
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                String.valueOf(userId),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(mutableRequest, response);
        } catch (JwtException ex) {
            logger.error("=== JwtAuthenticationFilter: JWT validation failed: {} ===", ex.getMessage(), ex);
            writeUnauthorized(request, response, "error.unauthorized", "Invalid token: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("=== JwtAuthenticationFilter: Unexpected error during authentication: {} ===", ex.getMessage(), ex);
            writeUnauthorized(request, response, "error.unauthorized", "Authentication failed: " + ex.getMessage());
        }
    }

    /**
     * Extract JWT token from cookie (hrsession).
     */
    private String getJwtTokenFromCookie(HttpServletRequest request) {
        try {
            Cookie[] cookies = request.getCookies();
            logger.info("=== JwtAuthenticationFilter: Checking cookies, count={} ===", cookies != null ? cookies.length : 0);
            
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    logger.debug("=== JwtAuthenticationFilter: Cookie name={}, value length={} ===", 
                                cookie.getName(), cookie.getValue() != null ? cookie.getValue().length() : 0);
                    if ("hrsession".equals(cookie.getName())) {
                        String token = cookie.getValue();
                        if (token != null && !token.trim().isEmpty()) {
                            logger.info("=== JwtAuthenticationFilter: Found hrsession cookie with token length={} ===", token.length());
                            return token;
                        }
                    }
                }
            }
            logger.debug("=== JwtAuthenticationFilter: No hrsession cookie found ===");
        } catch (Exception e) {
            logger.error("=== JwtAuthenticationFilter: Error reading cookies: {} ===", e.getMessage(), e);
        }
        return null;
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        ErrorResponse body = ErrorResponse.builder()
            .timestamp(java.time.OffsetDateTime.now())
            .status(HttpServletResponse.SC_UNAUTHORIZED)
            .error("Unauthorized")
            .message(message)
            .code(code)
            .path(request.getRequestURI())
            .build();
        objectMapper.writeValue(response.getWriter(), body);
    }
}
