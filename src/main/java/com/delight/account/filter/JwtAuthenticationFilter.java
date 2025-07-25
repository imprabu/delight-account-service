package com.delight.account.filter;

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
import com.delight.account.model.User;
import com.delight.account.repository.UserRepository;
import com.delight.account.filter.MutableHttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final String jwtSecret;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(@Value("${security.jwt.secret}") String jwtSecret,
                                   UserRepository userRepository) {
        this.jwtSecret = jwtSecret;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/signup") || path.equals("/api/activate") || path.equals("/api/signin");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

            Long accountId = claims.get("accountId", Long.class);
            Long userId = claims.get("userId", Long.class);

            if (accountId == null || userId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getAccount() == null || !accountId.equals(user.getAccount().getId())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            MutableHttpServletRequest mutableRequest = request instanceof MutableHttpServletRequest
                ? (MutableHttpServletRequest) request
                : new MutableHttpServletRequest(request);

            mutableRequest.putHeader("accountId", String.valueOf(accountId));
            mutableRequest.setAttribute("accountId", accountId);
            mutableRequest.putHeader("userId", String.valueOf(userId));
            mutableRequest.setAttribute("userId", userId);

            filterChain.doFilter(mutableRequest, response);
        } catch (JwtException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}

