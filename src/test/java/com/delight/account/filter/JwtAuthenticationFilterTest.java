package com.delight.account.filter;

import com.delight.account.dto.ErrorResponse;
import com.delight.account.dto.SessionResponse;
import com.delight.account.model.Membership;
import com.delight.account.model.MembershipStatus;
import com.delight.account.model.Role;
import com.delight.account.model.User;
import com.delight.account.model.Account;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.service.SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String JWT_SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm";

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private SessionService sessionService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PrintWriter writer;

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private ObjectMapper objectMapper;
    private User testUser;
    private Account testAccount;
    private Membership testMembership;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        jwtAuthenticationFilter = new JwtAuthenticationFilter(JWT_SECRET, membershipRepository, sessionService, objectMapper);
        
        // Clear security context before each test
        SecurityContextHolder.clearContext();

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmailAddress("test@example.com");

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setDomain("example.com");

        testMembership = new Membership();
        testMembership.setId(1L);
        testMembership.setUser(testUser);
        testMembership.setAccount(testAccount);
        testMembership.setRole(Role.AGENT);
        testMembership.setStatus(MembershipStatus.ACTIVE);
    }

    @Test
    void shouldNotFilterExcludedPaths() {
        String[] excludedPaths = {
            "/account/signup",
            "/account/activate", 
            "/account/signin",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/v3/api-docs",
            "/v3/api-docs/swagger-config"
        };

        for (String path : excludedPaths) {
            when(request.getRequestURI()).thenReturn(path);
            boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);
            assertThat(shouldNotFilter).isTrue();
        }
    }

    @Test
    void shouldFilterProtectedPaths() {
        String[] protectedPaths = {
            "/account/members",
            "/account/profile",
            "/api/protected"
        };

        for (String path : protectedPaths) {
            when(request.getRequestURI()).thenReturn(path);
            boolean shouldNotFilter = jwtAuthenticationFilter.shouldNotFilter(request);
            assertThat(shouldNotFilter).isFalse();
        }
    }

    @Test
    void doFilterInternalWithValidTokenSucceeds() throws ServletException, IOException {
        String token = createValidToken(1L, 1L, "AGENT", 1L, "TEST_PRODUCT");
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(membershipRepository.findByUserIdAndAccountId(1L, 1L))
                .thenReturn(Optional.of(testMembership));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<MutableHttpServletRequest> requestCaptor = ArgumentCaptor.forClass(MutableHttpServletRequest.class);
        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));

        MutableHttpServletRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getHeader("accountId")).isEqualTo("1");
        assertThat(capturedRequest.getHeader("userId")).isEqualTo("1");
        assertThat(capturedRequest.getHeader("role")).isEqualTo("AGENT");
        assertThat(capturedRequest.getHeader("orgId")).isEqualTo("1");
        assertThat(capturedRequest.getHeader("productCode")).isEqualTo("TEST_PRODUCT");

        assertThat(capturedRequest.getAttribute("accountId")).isEqualTo(1L);
        assertThat(capturedRequest.getAttribute("userId")).isEqualTo(1L);
        assertThat(capturedRequest.getAttribute("role")).isEqualTo("AGENT");
        assertThat(capturedRequest.getAttribute("orgId")).isEqualTo(1L);
        assertThat(capturedRequest.getAttribute("productCode")).isEqualTo("TEST_PRODUCT");

        // Verify Spring Security context is set
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("1");
        assertThat(auth.getAuthorities()).hasSize(1);
        assertThat(auth.getAuthorities().iterator().next()).isEqualTo(new SimpleGrantedAuthority("ROLE_AGENT"));
    }

    @Test
    void doFilterInternalWithMissingAuthorizationHeaderReturnsUnauthorized() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(response.getWriter()).thenReturn(writer);
        when(request.getRequestURI()).thenReturn("/test/path");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(any(), any());
        verify(response).getWriter(); // Verify that the writer was requested
    }

    @Test
    void doFilterInternalWithInvalidAuthorizationHeaderReturnsUnauthorized() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Invalid header");
        when(response.getWriter()).thenReturn(writer);
        when(request.getRequestURI()).thenReturn("/test/path");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternalWithInvalidTokenReturnsUnauthorized() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(response.getWriter()).thenReturn(writer);
        when(request.getRequestURI()).thenReturn("/test/path");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(any(), any());
        verify(response).getWriter(); // Verify that the writer was requested
    }

    @Test
    void doFilterInternalWithExpiredTokenReturnsUnauthorized() throws ServletException, IOException {
        String expiredToken = createExpiredToken(1L, 1L, "AGENT");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + expiredToken);
        when(response.getWriter()).thenReturn(writer);
        when(request.getRequestURI()).thenReturn("/test/path");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternalWithMissingAccountIdReturnsUnauthorized() throws ServletException, IOException {
        String tokenWithoutAccountId = Jwts.builder()
            .claim("userId", 1L)
            .claim("role", "AGENT")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenWithoutAccountId);
        when(response.getWriter()).thenReturn(writer);
        when(request.getRequestURI()).thenReturn("/test/path");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternalWithMissingUserIdReturnsUnauthorized() throws ServletException, IOException {
        String tokenWithoutUserId = createTokenWithMissingClaims(1L, null, "AGENT");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenWithoutUserId);
        when(response.getWriter()).thenReturn(writer);
        when(request.getRequestURI()).thenReturn("/test/path");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternalWithMissingMembershipReturnsUnauthorized() throws ServletException, IOException {
        String validToken = createValidToken(1L, 1L, "AGENT", 1L, "TEST");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(membershipRepository.findByUserIdAndAccountId(1L, 1L)).thenReturn(Optional.empty());
        when(response.getWriter()).thenReturn(writer);
        when(request.getRequestURI()).thenReturn("/test/path");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(any(), any());
        verify(response).getWriter(); // Verify that the writer was requested
    }

    @Test
    void doFilterInternalWithInactiveMembershipReturnsUnauthorized() throws ServletException, IOException {
        String validToken = createValidToken(1L, 1L, "AGENT", 1L, "TEST");
        testMembership.setStatus(MembershipStatus.SUSPENDED);
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(membershipRepository.findByUserIdAndAccountId(1L, 1L)).thenReturn(Optional.of(testMembership));
        when(response.getWriter()).thenReturn(writer);
        when(request.getRequestURI()).thenReturn("/test/path");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternalPrefersDatabaseRoleOverTokenRole() throws ServletException, IOException {
        String token = createValidToken(1L, 1L, "AGENT", 1L, "TEST_PRODUCT");
        
        // Create a fresh membership with ADMIN role for this test
        Membership adminMembership = new Membership();
        adminMembership.setId(1L);
        adminMembership.setUser(testUser);
        adminMembership.setAccount(testAccount);
        adminMembership.setRole(Role.ADMIN); // Database has ADMIN, token has AGENT
        adminMembership.setStatus(MembershipStatus.ACTIVE);
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(membershipRepository.findByUserIdAndAccountId(1L, 1L))
                .thenReturn(Optional.of(adminMembership));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<MutableHttpServletRequest> requestCaptor = ArgumentCaptor.forClass(MutableHttpServletRequest.class);
        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));

        MutableHttpServletRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getHeader("role")).isEqualTo("ADMIN");
        assertThat(capturedRequest.getAttribute("role")).isEqualTo("ADMIN");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities()).hasSize(1);
        assertThat(auth.getAuthorities().iterator().next()).isEqualTo(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test
    void doFilterInternalHandlesTokenWithoutOptionalClaims() throws ServletException, IOException {
        String token = createTokenWithMissingClaims(1L, 1L, null); // No role, orgId, productCode
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(membershipRepository.findByUserIdAndAccountId(1L, 1L))
                .thenReturn(Optional.of(testMembership));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<MutableHttpServletRequest> requestCaptor = ArgumentCaptor.forClass(MutableHttpServletRequest.class);
        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));

        MutableHttpServletRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getHeader("accountId")).isEqualTo("1");
        assertThat(capturedRequest.getHeader("userId")).isEqualTo("1");
        assertThat(capturedRequest.getHeader("role")).isEqualTo("AGENT"); // From database
        assertThat(capturedRequest.getHeader("orgId")).isNull();
        assertThat(capturedRequest.getHeader("productCode")).isNull();
    }

    @Test
    void doFilterInternalWithMutableRequestReusesInstance() throws ServletException, IOException {
        String token = createValidToken(1L, 1L, "AGENT", 1L, "TEST_PRODUCT");
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(membershipRepository.findByUserIdAndAccountId(1L, 1L))
                .thenReturn(Optional.of(testMembership));

        jwtAuthenticationFilter.doFilterInternal(mutableRequest, response, filterChain);

        ArgumentCaptor<MutableHttpServletRequest> requestCaptor = ArgumentCaptor.forClass(MutableHttpServletRequest.class);
        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));

        // Should reuse the same MutableHttpServletRequest instance
        assertThat(requestCaptor.getValue()).isSameAs(mutableRequest);
    }

    @Test
    void doFilterInternalSetsCorrectSecurityContextForDifferentRoles() throws ServletException, IOException {
        Role[] roles = {Role.AGENT, Role.ADMIN, Role.OWNER};
        
        for (Role role : roles) {
            SecurityContextHolder.clearContext();
            testMembership.setRole(role);
            
            String token = createValidToken(1L, 1L, role.name(), 1L, "TEST_PRODUCT");
            
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(membershipRepository.findByUserIdAndAccountId(1L, 1L))
                    .thenReturn(Optional.of(testMembership));

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo("1");
            assertThat(auth.getAuthorities()).hasSize(1);
            assertThat(auth.getAuthorities().iterator().next()).isEqualTo(new SimpleGrantedAuthority("ROLE_" + role.name()));
        }
    }

    private String createValidToken(Long accountId, Long userId, String role, Long orgId, String productCode) {
        return Jwts.builder()
                .claim("accountId", accountId)
                .claim("userId", userId)
                .claim("role", role)
                .claim("orgId", orgId)
                .claim("productCode", productCode)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String createExpiredToken(Long accountId, Long userId, String role) {
        return Jwts.builder()
                .claim("accountId", accountId)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String createTokenWithMissingClaims(Long accountId, Long userId, String role) {
        var builder = Jwts.builder()
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));
        
        if (accountId != null) {
            builder.claim("accountId", accountId);
        }
        if (userId != null) {
            builder.claim("userId", userId);
        }
        if (role != null) {
            builder.claim("role", role);
        }
        
        return builder.signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}