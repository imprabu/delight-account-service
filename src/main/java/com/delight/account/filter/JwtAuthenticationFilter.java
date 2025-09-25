package com.delight.account.filter;

import com.delight.account.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.delight.account.model.Membership;
import com.delight.account.model.MembershipStatus;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.filter.MutableHttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final String jwtSecret;
    private final MembershipRepository membershipRepository;

    public JwtAuthenticationFilter(@Value("${security.jwt.secret}") String jwtSecret,
                                   MembershipRepository membershipRepository) {
        this.jwtSecret = jwtSecret;
        this.membershipRepository = membershipRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/account/signup") || path.equals("/account/activate") || path.equals("/account/signin") ||
               path.equals("/swagger-ui.html") || path.startsWith("/swagger-ui/") || 
               path.equals("/v3/api-docs") || path.startsWith("/v3/api-docs/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        System.out.println("DEBUG: JwtAuthenticationFilter - Authorization header: " + header);
        
        if (header == null || !header.startsWith("Bearer ")) {
            System.out.println("DEBUG: JwtAuthenticationFilter - Missing or invalid Authorization header");
            writeUnauthorized(request, response, "error.unauthorized", "Missing or invalid Authorization header");
            return;
        }

        String token = header.substring(7);
        System.out.println("DEBUG: JwtAuthenticationFilter - Extracted token: " + token.substring(0, Math.min(20, token.length())) + "...");
        
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

            System.out.println("DEBUG: JwtAuthenticationFilter - Parsed claims - accountId: " + accountId + ", userId: " + userId + ", role: " + role);

            if (accountId == null || userId == null) {
                System.out.println("DEBUG: JwtAuthenticationFilter - Invalid token claims - missing accountId or userId");
                writeUnauthorized(request, response, "error.unauthorized", "Invalid token claims");
                return;
            }

            Membership membership = membershipRepository
                .findByUserIdAndAccountId(userId, accountId)
                .orElse(null);
            
            System.out.println("DEBUG: JwtAuthenticationFilter - Membership lookup result: " + (membership != null ? "Found" : "Not found"));
            
            if (membership == null || membership.getStatus() != MembershipStatus.ACTIVE) {
                System.out.println("DEBUG: JwtAuthenticationFilter - Inactive or missing membership");
                writeUnauthorized(request, response, "error.authorization", "Inactive or missing membership");
                return;
            }
            // Prefer DB-sourced role if available
            if (membership != null && membership.getRole() != null) {
                role = membership.getRole().name();
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

            // Set Spring Security Authentication so .authenticated() passes
            List<SimpleGrantedAuthority> authorities = role != null
                ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                : List.of();
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(String.valueOf(userId), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(mutableRequest, response);
        } catch (JwtException ex) {
            writeUnauthorized(request, response, "error.unauthorized", "Invalid token");
        }
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
        new ObjectMapper().writeValue(response.getWriter(), body);
    }
}
