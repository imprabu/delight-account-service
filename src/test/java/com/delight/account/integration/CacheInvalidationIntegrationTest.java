package com.delight.account.integration;

import com.delight.account.dto.SignupRequest;
import com.delight.account.dto.SigninRequest;
import com.delight.account.dto.UpdateMemberRequest;
import com.delight.account.dto.ActivationRequest;
import com.delight.account.model.*;
import com.delight.account.repository.*;
import com.delight.account.service.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CacheInvalidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserCredentialRepository credentialRepository;

  @SpyBean
  private CacheService cacheService;

  @Autowired
  private com.delight.account.cache.CacheKeyManager cacheKeyManager;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SignupRequest signupRequest;
    private SigninRequest signinRequest;
    private UpdateMemberRequest updateMemberRequest;
    private ActivationRequest activationRequest;

    @BeforeEach
    void setUp() {
        // Clear Redis/cache to avoid cross-test rate limit interference
        try {
            cacheService.clear();
        } catch (Exception ignored) {
            // In test environments without Redis, ignore clearing failures
        }

        signupRequest = new SignupRequest();
        signupRequest.setCompanyName("CacheTestCo");
        signupRequest.setEmailAddress("cache@test.com");
        signupRequest.setIndustryType("Technology");
        signupRequest.setFirstName("Cache");
        signupRequest.setLastName("Test");

        signinRequest = new SigninRequest();
        signinRequest.setEmailAddress("cache@test.com");
        signinRequest.setPassword("TestPassword123");

        updateMemberRequest = new UpdateMemberRequest();
        updateMemberRequest.setRole(Role.MEMBER);
        updateMemberRequest.setStatus(MembershipStatus.SUSPENDED);

        activationRequest = new ActivationRequest();
        activationRequest.setEmailAddress("cache@test.com");
        activationRequest.setFirstName("Cache");
        activationRequest.setLastName("Test");
        activationRequest.setPhoneNumber("+1-555-123-4567");
        activationRequest.setPassword("TestPassword123");
        activationRequest.setRetypePassword("TestPassword123");
    }

    @Test
    void signupWarmsUserCache() throws Exception {
        // Perform signup
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Verify account and user were created
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus("cachetestco", AccountStatus.ACTIVE);
        assertThat(accountOpt).isPresent();
        Account account = accountOpt.get();

        Optional<User> userOpt = userRepository.findByAccountIdAndEmailAddress(account.getId(), "cache@test.com");
        assertThat(userOpt).isPresent();
        User user = userOpt.get();

        // Verify cache warming was called for the newly created user
        verify(cacheService).put(eq(String.format("user:account:%d:email:%s", account.getId(), "cache@test.com")), 
                                 eq(user), any());
    }

    @Test
    void membershipUpdateInvalidatesAccountInfoCache() throws Exception {
        // First create account through signup
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Get the created account and user
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus("cachetestco", AccountStatus.ACTIVE);
        assertThat(accountOpt).isPresent();
        Account account = accountOpt.get();

        Optional<User> userOpt = userRepository.findByAccountIdAndEmailAddress(account.getId(), "cache@test.com");
        assertThat(userOpt).isPresent();
        User user = userOpt.get();

        // Create credentials for authentication
        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setPasswordHash(encrypt("TestPassword123"));
        credentialRepository.save(credential);

        // Signin to get JWT token
        MvcResult signinResult = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName("cachetestco"); return req; })
                .header("Host", "cachetestco")
                .content(objectMapper.writeValueAsString(signinRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = signinResult.getResponse().getContentAsString();
        String jwtToken = objectMapper.readTree(responseBody).get("token").asText();

        // Create a second user for testing membership update
        User secondUser = new User();
        secondUser.setAccount(account);
        secondUser.setUserName("TestUser2");
        secondUser.setEmailAddress("test2@test.com");
        secondUser.setFirstName("Test");
        secondUser.setLastName("User2");
        secondUser.setCompanyName("CacheTestCo");
        userRepository.save(secondUser);

        Membership secondMembership = new Membership();
        secondMembership.setAccount(account);
        secondMembership.setUser(secondUser);
        secondMembership.setRole(Role.ADMIN);
        secondMembership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(secondMembership);

        // Reset the spy to clear previous interactions
        reset(cacheService);

        // Update membership
        mockMvc.perform(patch("/account/members/{userId}", secondUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + jwtToken)
                .header("X-User-Role", "OWNER")
                .header("X-Product-Code", "HRMS")
                .content(objectMapper.writeValueAsString(updateMemberRequest)))
            .andExpect(status().isOk());

        // Verify cache invalidation was called for account info
        verify(cacheService).evict(String.format("account:info:%d:%d", account.getId(), secondUser.getId()));
    }

    @Test
    void signinWarmsUserAndCredentialCaches() throws Exception {
        // Given: Create account and user through signup
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());

        // Activate the user
        Account account = accountRepository.findByDomainAndStatus("cachetestco", AccountStatus.ACTIVE).orElseThrow();
        mockMvc.perform(post("/account/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(account.getDomain()); return req; })
                .requestAttr("accountId", account.getId())
                .header("Host", account.getDomain())
                .content(objectMapper.writeValueAsString(activationRequest)))
            .andExpect(status().isOk());

        // Clear cache to ensure signin warms it
        cacheService.clear();

        // When: Sign in
        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName("cachetestco"); return req; })
                .header("Host", "cachetestco")
                .content(objectMapper.writeValueAsString(signinRequest)))
                .andExpect(status().isOk());

        // Then: Verify user cache is warmed
        String userCacheKey = cacheKeyManager != null
            ? cacheKeyManager.getUserAccountKey(account.getId(), signinRequest.getEmailAddress())
            : String.format("user:account:%d:email:%s", account.getId(), signinRequest.getEmailAddress());
        assertThat(cacheService.exists(userCacheKey)).isTrue();

        // And: Verify credential cache is warmed
        User user = userRepository.findByAccountIdAndEmailAddress(account.getId(), signinRequest.getEmailAddress()).orElseThrow();
        String credentialCacheKey = cacheKeyManager != null
            ? cacheKeyManager.getUserCredentialKey(user.getId())
            : String.format("user:credential:%d", user.getId());
        assertThat(cacheService.exists(credentialCacheKey)).isTrue();
    }

    @Test
    void signinHitsCacheOnSecondAttempt() throws Exception {
        // Given: Create account and user through signup
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());

        // Activate the user
        Account account = accountRepository.findByDomainAndStatus("cachetestco", AccountStatus.ACTIVE).orElseThrow();
        mockMvc.perform(post("/account/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(account.getDomain()); return req; })
                .requestAttr("accountId", account.getId())
                .header("Host", account.getDomain())
                .content(objectMapper.writeValueAsString(activationRequest)))
            .andExpect(status().isOk());

        // First signin to warm cache
        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName("cachetestco"); return req; })
                .header("Host", "cachetestco")
                .content(objectMapper.writeValueAsString(signinRequest)))
                .andExpect(status().isOk());

        // When: Second signin (should hit cache)
        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName("cachetestco"); return req; })
                .header("Host", "cachetestco")
                .content(objectMapper.writeValueAsString(signinRequest)))
                .andExpect(status().isOk());

        // Then: Cache should still contain user and credential data
        String userCacheKey = cacheKeyManager != null
            ? cacheKeyManager.getUserAccountKey(account.getId(), signinRequest.getEmailAddress())
            : String.format("user:account:%d:email:%s", account.getId(), signinRequest.getEmailAddress());
        assertThat(cacheService.exists(userCacheKey)).isTrue();

        User user = userRepository.findByAccountIdAndEmailAddress(account.getId(), signinRequest.getEmailAddress()).orElseThrow();
        String credentialCacheKey = cacheKeyManager != null
            ? cacheKeyManager.getUserCredentialKey(user.getId())
            : String.format("user:credential:%d", user.getId());
        assertThat(cacheService.exists(credentialCacheKey)).isTrue();
    }

    @Test
    void activationInvalidatesUserAndAccountCaches() throws Exception {
        // First create account through signup
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Get the created account
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus("cachetestco", AccountStatus.ACTIVE);
        assertThat(accountOpt).isPresent();
        Account account = accountOpt.get();

        // Reset the spy to clear previous interactions
        reset(cacheService);

        // Perform activation
        mockMvc.perform(post("/account/activate")
                .header("Host", account.getDomain())
                .with(req -> { req.setServerName(account.getDomain()); return req; })
                .requestAttr("accountId", account.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activationRequest)))
            .andExpect(status().isOk());

        // Verify cache invalidation was called for user:account and account:info
        verify(cacheService).evict(String.format("user:account:%d:email:%s", account.getId(), "cache@test.com"));
        verify(cacheService).evict(String.format("account:info:%d", account.getId()));
    }

    @Test
    void sessionValidationCachesSession() throws Exception {
        // First create account and signin to get session
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Activate the user before signin
        Account accountForActivation1 = accountRepository.findByDomainAndStatus("cachetestco", AccountStatus.ACTIVE).orElseThrow();
        mockMvc.perform(post("/account/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(accountForActivation1.getDomain()); return req; })
                .header("Host", accountForActivation1.getDomain())
                .requestAttr("accountId", accountForActivation1.getId())
                .content(objectMapper.writeValueAsString(activationRequest)))
            .andExpect(status().isOk());

        MvcResult signinResult = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName("cachetestco"); return req; })
                .header("Host", "cachetestco")
                .content(objectMapper.writeValueAsString(signinRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = signinResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("token").asText();

        // Decode JWT to get session ID
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        String sessionId = claims.get("sessionId", String.class);
        String tokenHash = String.valueOf(token.hashCode());

        // Reset spy to clear previous interactions
        reset(cacheService);

        // Validate session - should use cached session if present
        mockMvc.perform(get("/account/validate-session")
                .header("Authorization", "Bearer " + token)
                .header("Host", "cachetestco"))
            .andExpect(status().isOk());

        // Verify session cache was accessed via versioned token-based key
        verify(cacheService, atLeastOnce()).get(
            argThat(key -> key.contains(String.format("session:token:%s", tokenHash))),
            any(),
            any(),
            eq(Session.class)
        );
    }

    @Test
    void sessionInvalidationEvictsCache() throws Exception {
        // First create account and signin to get session
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Activate the user before signin
        Account accountForActivation2 = accountRepository.findByDomainAndStatus("cachetestco", AccountStatus.ACTIVE).orElseThrow();
        mockMvc.perform(post("/account/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(accountForActivation2.getDomain()); return req; })
                .header("Host", accountForActivation2.getDomain())
                .requestAttr("accountId", accountForActivation2.getId())
                .content(objectMapper.writeValueAsString(activationRequest)))
            .andExpect(status().isOk());

        MvcResult signinResult = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName("cachetestco"); return req; })
                .header("Host", "cachetestco")
                .content(objectMapper.writeValueAsString(signinRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = signinResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("token").asText();

        // Decode JWT to get session ID
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        String sessionId = claims.get("sessionId", String.class);
        String sessionCacheKey = String.format("session:id:%s", sessionId);

        // Reset spy to clear previous interactions
        reset(cacheService);

        // Invalidate session - should evict from cache
        mockMvc.perform(post("/account/signout")
                .header("Authorization", "Bearer " + token)
                .header("Host", "cachetestco"))
            .andExpect(status().isOk());

        // Verify session was evicted from cache (supports versioned keys)
        verify(cacheService, atLeastOnce()).evict(argThat(key -> key.contains(sessionCacheKey)));
    }

    @Test
    void accountConfigurationCachesAndInvalidates() throws Exception {
        // First create account through signup
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Get the created account
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus("cachetestco", AccountStatus.ACTIVE);
        assertThat(accountOpt).isPresent();
        Account account = accountOpt.get();

        // Get the created user
        Optional<User> userOpt = userRepository.findByAccountIdAndEmailAddress(account.getId(), "cache@test.com");
        assertThat(userOpt).isPresent();
        User user = userOpt.get();

        // Create credentials for authentication
        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setPasswordHash(encrypt("TestPassword123"));
        credentialRepository.save(credential);

        // Sign in to get authentication token
        MvcResult signinResult = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Host", "cachetestco")
                .content(objectMapper.writeValueAsString(signinRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = signinResult.getResponse().getContentAsString();
        String jwtToken = objectMapper.readTree(responseBody).get("token").asText();

        String configCacheKey = String.format("account:config:%d", account.getId());

        // Reset spy to clear previous interactions
        reset(cacheService);

        // Get account configuration - should cache it
        mockMvc.perform(get("/account/config")
                .header("Authorization", "Bearer " + jwtToken)
                .header("Host", account.getDomain()))
            .andExpect(status().isOk());

        // Verify configuration was cached
        verify(cacheService, atLeastOnce()).put(eq(configCacheKey), any());

        // Reset spy again
        reset(cacheService);

        // Update account configuration - should invalidate cache
        String configUpdateJson = "{\"maxUsers\": 100}";
        mockMvc.perform(put("/account/config")
                .header("Authorization", "Bearer " + jwtToken)
                .header("Host", account.getDomain())
                .contentType(MediaType.APPLICATION_JSON)
                .content(configUpdateJson))
            .andExpect(status().isOk());

        // Verify configuration cache was invalidated
        verify(cacheService, atLeastOnce()).evict(configCacheKey);
    }

    @Test
    void accountServiceCachesAccountInfo() throws Exception {
        // First create account through signup
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Get the created account
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus("cachetestco", AccountStatus.ACTIVE);
        assertThat(accountOpt).isPresent();
        Account account = accountOpt.get();

        // Activate the user before signin
        mockMvc.perform(post("/account/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(account.getDomain()); return req; })
                .header("Host", account.getDomain())
                .requestAttr("accountId", account.getId())
                .content(objectMapper.writeValueAsString(activationRequest)))
            .andExpect(status().isOk());

        // Sign in to get authentication token
        MvcResult signinResult = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName("cachetestco"); return req; })
                .header("Host", "cachetestco")
                .content(objectMapper.writeValueAsString(signinRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = signinResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("token").asText();

        String accountInfoCacheKey = String.format("account:info:%d", account.getId());

        // Reset spy to clear previous interactions
        reset(cacheService);

        // Get account info - should cache it
        mockMvc.perform(get("/account/info")
                .header("Authorization", "Bearer " + token)
                .header("Host", account.getDomain()))
            .andExpect(status().isOk());

        // Verify account info was cached (supports versioned keys and user-specific keys)
        verify(cacheService, atLeastOnce()).put(
            argThat(key -> key.contains(String.format("account:info:%d", account.getId()))),
            any(),
            any()
        );
    }

    @Test
    void cachePatternEvictionWorksForUserSessions() throws Exception {
        // First create account and signin to get session
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Get the created account
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus("cachetestco", AccountStatus.ACTIVE);
        assertThat(accountOpt).isPresent();
        Account account = accountOpt.get();

        // Get the created user
        Optional<User> userOpt = userRepository.findByAccountIdAndEmailAddress(account.getId(), "cache@test.com");
        assertThat(userOpt).isPresent();
        User user = userOpt.get();

        // Activate the user before signin
        mockMvc.perform(post("/account/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Host", account.getDomain())
                .requestAttr("accountId", account.getId())
                .content(objectMapper.writeValueAsString(activationRequest)))
            .andExpect(status().isOk());

        MvcResult signinResult = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Host", "cachetestco")
                .content(objectMapper.writeValueAsString(signinRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = signinResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("token").asText();

        String userSessionPattern = "session:*";

        // Reset spy to clear previous interactions
        reset(cacheService);

        // Invalidate all user sessions - should use pattern eviction
        mockMvc.perform(post("/account/invalidate-all-sessions")
                .header("Authorization", "Bearer " + token)
                .header("Host", "cachetestco"))
            .andExpect(status().isOk());

        // Verify pattern eviction was called for user sessions (supports versioned keys)
        verify(cacheService, atLeastOnce()).evictByPattern(argThat(p -> p.contains(userSessionPattern)));
    }

    @Test
    void cachePatternEvictionWorksForAccountConfigs() throws Exception {
        // First create account through signup
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Get the created account
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus("cachetestco", AccountStatus.ACTIVE);
        assertThat(accountOpt).isPresent();
        Account account = accountOpt.get();

        // Activate the user before signin
        mockMvc.perform(post("/account/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Host", account.getDomain())
                .requestAttr("accountId", account.getId())
                .content(objectMapper.writeValueAsString(activationRequest)))
            .andExpect(status().isOk());

        String accountConfigPattern = String.format("account:config:%d:*", account.getId());

        // Sign in to get authentication token for account deletion
        MvcResult signinResult = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Host", "cachetestco")
                .content(objectMapper.writeValueAsString(signinRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = signinResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("token").asText();

        // Reset spy to clear previous interactions
        reset(cacheService);

        // Delete account - should use pattern eviction for all account configs
        mockMvc.perform(delete("/account")
                .header("Authorization", "Bearer " + token)
                .header("Host", account.getDomain()))
            .andExpect(status().isOk());

        // Verify pattern eviction was called for account configs
        verify(cacheService, atLeastOnce()).evictByPattern(accountConfigPattern);
    }

    private String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec("1234567890123456".getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}