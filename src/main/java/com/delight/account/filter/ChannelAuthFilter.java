package com.delight.account.filter;

import com.delight.account.dto.ErrorResponse;
import com.delight.account.dto.ChannelJwtClaims;
import com.delight.account.service.ChannelJwtService;
import com.delight.account.context.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import org.springframework.core.annotation.Order;

@Component
@Order(2)
public class ChannelAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ChannelAuthFilter.class);
    
    private final ChannelJwtService channelJwtService;
    private final ObjectMapper objectMapper;

    public ChannelAuthFilter(ChannelJwtService channelJwtService, ObjectMapper objectMapper) {
        this.channelJwtService = channelJwtService;
        this.objectMapper = objectMapper;
        logger.info("=== ChannelAuthFilter initialized ===");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean shouldFilter = path.startsWith("/channel/");
        logger.info("=== ChannelAuthFilter.shouldNotFilter: path={}, shouldFilter={} ===", path, shouldFilter);
        // Only process /channel/ endpoints
        return !shouldFilter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        logger.info("=== ChannelAuthFilter.doFilterInternal START: path={}, method={} ===", 
                   request.getRequestURI(), request.getMethod());
        
        String header = request.getHeader("Authorization");
        logger.info("=== ChannelAuthFilter: Authorization header present={} ===", header != null);
        
        if (header == null || !header.startsWith("Bearer ")) {
            logger.warn("=== ChannelAuthFilter: Missing or invalid Authorization header ===");
            writeUnauthorized(request, response, "error.unauthorized", "Missing or invalid Authorization header");
            return;
        }

        String token = header.substring(7);
        logger.info("=== ChannelAuthFilter: Token extracted, length={} ===", token.length());
        
        try {
            logger.info("=== ChannelAuthFilter: Calling channelJwtService.validateChannelToken ===");
            ChannelJwtClaims claims = channelJwtService.validateChannelToken(token);
            logger.info("=== ChannelAuthFilter: Token validated successfully, accountId={}, keyVersion={} ===", 
                       claims.getAccountId(), claims.getKeyVersion());
            
            // Set tenant context using account_id from JWT
            TenantContext.setAccountId(claims.getAccountId());
            logger.info("=== ChannelAuthFilter: TenantContext set for accountId={} ===", claims.getAccountId());
            
            MutableHttpServletRequest mutableRequest = request instanceof MutableHttpServletRequest
                ? (MutableHttpServletRequest) request
                : new MutableHttpServletRequest(request);

            // Set channel-specific attributes
            mutableRequest.putHeader("accountId", claims.getAccountId());
            mutableRequest.setAttribute("accountId", Long.valueOf(claims.getAccountId()));
            mutableRequest.putHeader("keyVersion", claims.getKeyVersion());
            mutableRequest.setAttribute("keyVersion", claims.getKeyVersion());
            mutableRequest.setAttribute("channelAuth", true);
            
            logger.info("=== ChannelAuthFilter: Request attributes set ===");

            // Set Spring Security context for channel authentication
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                claims.getAccountId(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CHANNEL"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.info("=== ChannelAuthFilter: Security context set with ROLE_CHANNEL ===");

            logger.info("=== ChannelAuthFilter: Proceeding to next filter ===");
            filterChain.doFilter(mutableRequest, response);
            logger.info("=== ChannelAuthFilter: Filter chain completed ===");
        } catch (Exception ex) {
            logger.error("=== ChannelAuthFilter: Token validation failed: {} ===", ex.getMessage(), ex);
            writeUnauthorized(request, response, "error.unauthorized", "Invalid channel token: " + ex.getMessage());
        }
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String code, String message) throws IOException {
        logger.warn("=== ChannelAuthFilter: Writing 401 Unauthorized response: {} ===", message);
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
        logger.warn("=== ChannelAuthFilter: 401 Unauthorized response written ===");
    }
}