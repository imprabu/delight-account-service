package com.delight.account.filter;

import com.delight.account.dto.ErrorResponse;
import com.delight.account.dto.ChannelJwtClaims;
import com.delight.account.dto.SessionValidationRequest;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.service.AccountService;
import com.delight.account.service.ChannelJwtService;
import com.delight.account.service.SessionService;
import com.delight.account.context.TenantContext;
import com.delight.account.util.DomainHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import org.springframework.core.annotation.Order;

@Component
@Order(2)
public class ChannelAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ChannelAuthFilter.class);
    
    private final ChannelJwtService channelJwtService;
    private final ObjectMapper objectMapper;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final SessionService sessionService;
    private final String jwtSecret;
    private final DomainHelper domainHelper;

    public ChannelAuthFilter(ChannelJwtService channelJwtService, 
                            ObjectMapper objectMapper,
                            AccountRepository accountRepository,
                            AccountService accountService,
                            SessionService sessionService,
                            @Value("${security.jwt.secret}") String jwtSecret,
                            DomainHelper domainHelper) {
        this.channelJwtService = channelJwtService;
        this.objectMapper = objectMapper;
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.sessionService = sessionService;
        this.jwtSecret = jwtSecret;
        this.domainHelper = domainHelper;
        logger.info("=== ChannelAuthFilter initialized with domain name: {} ===", domainHelper.getDomainName());
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

        String channelToken = header.substring(7);
        logger.info("=== ChannelAuthFilter: Token extracted, length={} ===", channelToken.length());
        
        // Wrap request to cache body if needed (for POST requests) - do this early
        HttpServletRequest requestToUse = request;
        if ("POST".equalsIgnoreCase(request.getMethod()) && !(request instanceof CachedBodyHttpServletRequest)) {
            try {
                requestToUse = new CachedBodyHttpServletRequest(request);
                logger.debug("=== ChannelAuthFilter: Request body cached for POST request ===");
            } catch (IOException e) {
                logger.warn("=== ChannelAuthFilter: Could not cache request body: {} ===", e.getMessage());
                // Continue with original request
            }
        }
        
        try {
            logger.info("=== ChannelAuthFilter: Calling channelJwtService.validateChannelToken ===");
            ChannelJwtClaims claims = channelJwtService.validateChannelToken(channelToken);
            logger.info("=== ChannelAuthFilter: Token validated successfully, accountId={}, userId={}, keyVersion={} ===", 
                       claims.getAccountId(), claims.getUserId(), claims.getKeyVersion());
            
            // Determine account context: from JWT or domain lookup
            Long accountId = resolveAccountContext(claims, requestToUse);
            if (accountId == null) {
                logger.error("=== ChannelAuthFilter: Could not resolve account context ===");
                writeUnauthorized(request, response, "error.account.notfound", "Account context could not be resolved");
                return;
            }
            
            // Determine user context: from JWT or request body
            Long userId = resolveUserContext(claims, requestToUse);
            
            // Set tenant context
            TenantContext.setAccountId(accountId.toString());
            logger.info("=== ChannelAuthFilter: TenantContext set for accountId={} ===", accountId);
            
            MutableHttpServletRequest mutableRequest = requestToUse instanceof MutableHttpServletRequest
                ? (MutableHttpServletRequest) requestToUse
                : new MutableHttpServletRequest(requestToUse);

            // Set channel-specific attributes
            mutableRequest.putHeader("accountId", String.valueOf(accountId));
            mutableRequest.setAttribute("accountId", accountId);
            mutableRequest.putHeader("keyVersion", claims.getKeyVersion());
            mutableRequest.setAttribute("keyVersion", claims.getKeyVersion());
            mutableRequest.setAttribute("channelAuth", true);
            
            if (userId != null) {
                mutableRequest.putHeader("userId", String.valueOf(userId));
                mutableRequest.setAttribute("userId", userId);
                logger.info("=== ChannelAuthFilter: User context set for userId={} ===", userId);
            }
            
            logger.info("=== ChannelAuthFilter: Request attributes set ===");

            // Set Spring Security context for channel authentication
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                accountId.toString(),
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
    
    /**
     * Resolve account context from JWT claims, payload JWT, or domain lookup.
     * Priority: account_id from channel JWT > account_id from payload JWT (if base domain) > domain lookup
     */
    private Long resolveAccountContext(ChannelJwtClaims claims, HttpServletRequest request) {
        // If account_id is in channel JWT, use it
        if (claims.getAccountId() != null && !claims.getAccountId().trim().isEmpty()) {
            try {
                Long accountId = Long.valueOf(claims.getAccountId());
                logger.info("=== ChannelAuthFilter: Using account_id from channel JWT: {} ===", accountId);
                return accountId;
            } catch (NumberFormatException e) {
                logger.warn("=== ChannelAuthFilter: Invalid account_id format in channel JWT: {} ===", claims.getAccountId());
            }
        }
        
        // Check if we're using base domain (no subdomain) - if so, try to extract from payload JWT
        String extractedDomain = domainHelper.extractDomainFromRequest(request);
        boolean isBaseDomain = domainHelper.isBaseDomain(request);
        
        if (isBaseDomain && "POST".equalsIgnoreCase(request.getMethod())) {
            // For base domain, extract account_id from payload JWT and validate it
            try {
                String userJwtToken = extractUserJwtFromBody(request);
                if (userJwtToken != null && !userJwtToken.trim().isEmpty()) {
                    // Validate the user JWT token (check if session is active)
                    if (sessionService.validateSessionByToken(userJwtToken).isPresent()) {
                        Long accountId = extractAccountIdFromJwt(userJwtToken);
                        if (accountId != null) {
                            logger.info("=== ChannelAuthFilter: Using account_id from validated payload JWT (base domain): {} ===", accountId);
                            return accountId;
                        }
                    } else {
                        logger.warn("=== ChannelAuthFilter: User JWT token from payload is invalid or expired ===");
                    }
                }
            } catch (Exception e) {
                logger.debug("=== ChannelAuthFilter: Could not extract/validate account_id from payload JWT: {} ===", e.getMessage());
            }
        }
        
        // Fallback to domain lookup (for subdomain-based requests)
        if (extractedDomain != null && !extractedDomain.trim().isEmpty() && !isBaseDomain) {
            logger.info("=== ChannelAuthFilter: Looking up account by domain: {} ===", extractedDomain);
            Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(extractedDomain, AccountStatus.ACTIVE);
            if (accountOpt.isPresent()) {
                Long accountId = accountOpt.get().getId();
                logger.info("=== ChannelAuthFilter: Found account by domain: {} -> accountId={} ===", extractedDomain, accountId);
                return accountId;
            } else {
                logger.warn("=== ChannelAuthFilter: Account not found for domain: {} ===", extractedDomain);
            }
        }
        
        return null;
    }
    
    /**
     * Extract account_id from user JWT token.
     */
    private Long extractAccountIdFromJwt(String jwtToken) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(jwtToken)
                .getPayload();
            
            Long accountId = claims.get("accountId", Long.class);
            return accountId;
        } catch (Exception e) {
            logger.debug("=== ChannelAuthFilter: Could not extract account_id from JWT: {} ===", e.getMessage());
            return null;
        }
    }
    
    /**
     * Resolve user context from JWT claims or request body JWT token.
     * Priority: user_id from channel JWT > user_id from request body JWT token
     */
    private Long resolveUserContext(ChannelJwtClaims claims, HttpServletRequest request) {
        // If user_id is in channel JWT, use it
        if (claims.getUserId() != null && !claims.getUserId().trim().isEmpty()) {
            try {
                Long userId = Long.valueOf(claims.getUserId());
                logger.info("=== ChannelAuthFilter: Using user_id from channel JWT: {} ===", userId);
                return userId;
            } catch (NumberFormatException e) {
                logger.warn("=== ChannelAuthFilter: Invalid user_id format in channel JWT: {} ===", claims.getUserId());
            }
        }
        
        // Fallback to extracting user_id from request body JWT token (for POST requests)
        // For base domain, we should validate the JWT; for subdomain, validation happens later
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            try {
                String userJwtToken = extractUserJwtFromBody(request);
                if (userJwtToken != null && !userJwtToken.trim().isEmpty()) {
                    // Validate the user JWT token if using base domain (for subdomain, validation happens in controller)
                    boolean isBaseDomain = domainHelper.isBaseDomain(request);
                    
                    if (isBaseDomain) {
                        // For base domain, validate the JWT token before extracting user_id
                        if (sessionService.validateSessionByToken(userJwtToken).isEmpty()) {
                            logger.warn("=== ChannelAuthFilter: User JWT token from payload is invalid or expired ===");
                            return null;
                        }
                    }
                    
                    Long userId = extractUserIdFromJwt(userJwtToken);
                    if (userId != null) {
                        logger.info("=== ChannelAuthFilter: Using user_id from request body JWT: {} ===", userId);
                        return userId;
                    }
                }
            } catch (Exception e) {
                logger.debug("=== ChannelAuthFilter: Could not extract user_id from request body: {} ===", e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * Extract user JWT token from request body (for POST requests with JSON body).
     */
    private String extractUserJwtFromBody(HttpServletRequest request) {
        try {
            String body;
            if (request instanceof CachedBodyHttpServletRequest) {
                body = ((CachedBodyHttpServletRequest) request).getCachedBody();
            } else {
                // This should not happen as we cache POST requests, but handle it gracefully
                logger.warn("=== ChannelAuthFilter: Request body not cached, cannot safely read ===");
                return null;
            }
            
            if (body != null && !body.trim().isEmpty()) {
                SessionValidationRequest validationRequest = objectMapper.readValue(body, SessionValidationRequest.class);
                return validationRequest.getJwtToken();
            }
        } catch (Exception e) {
            logger.debug("=== ChannelAuthFilter: Could not parse request body: {} ===", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extract user_id from user JWT token.
     */
    private Long extractUserIdFromJwt(String jwtToken) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(jwtToken)
                .getPayload();
            
            Long userId = claims.get("userId", Long.class);
            return userId;
        } catch (Exception e) {
            logger.debug("=== ChannelAuthFilter: Could not extract user_id from JWT: {} ===", e.getMessage());
            return null;
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