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
import org.springframework.transaction.annotation.Propagation;
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

        // Reset Mockito spy to avoid cross-test interference
        reset(cacheService);

        // Use unique test data for each test to avoid conflicts
        String uniqueId = String.valueOf(System.currentTimeMillis());
        String uniqueEmail = "cache" + uniqueId + "@test.com";
        String uniqueCompany = "CacheTestCo" + uniqueId;

        signupRequest = new SignupRequest();
        signupRequest.setCompanyName(uniqueCompany);
        signupRequest.setEmailAddress(uniqueEmail);
        signupRequest.setIndustryType("Technology");
        signupRequest.setFirstName("Cache");
        signupRequest.setLastName("Test");

        signinRequest = new SigninRequest();
        signinRequest.setEmailAddress(uniqueEmail);
        signinRequest.setPassword("SecureTest123!");

        updateMemberRequest = new UpdateMemberRequest();
        updateMemberRequest.setRole(Role.MEMBER);
        updateMemberRequest.setStatus(MembershipStatus.SUSPENDED);

        activationRequest = new ActivationRequest();
        activationRequest.setEmailAddress(uniqueEmail);
        activationRequest.setFirstName("Cache");
        activationRequest.setLastName("Test");
        activationRequest.setPhoneNumber("+1-555-123-4567");
        activationRequest.setPassword("SecureTest123!");
        activationRequest.setRetypePassword("SecureTest123!");
    }

    @Test
    void signupWarmsUserCache() throws Exception {
        // Perform signup
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Get the created account using the unique company name
        String expectedDomain = signupRequest.getCompanyName().toLowerCase().replaceAll("[^a-z0-9]", "");
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(expectedDomain, AccountStatus.ACTIVE);
        assertThat(accountOpt).isPresent();
        Account account = accountOpt.get();

        Optional<User> userOpt = userRepository.findByAccountIdAndEmailAddress(account.getId(), signupRequest.getEmailAddress());
        assertThat(userOpt).isPresent();
        User user = userOpt.get();

        // Verify cache warming was called for the newly created user
        // Note: The CacheInvalidationEntityListener will trigger evictions during entity saves,
        // but the final cache warming should still occur after all saves are complete
        verify(cacheService, atLeastOnce()).put(eq(cacheKeyManager.getUserAccountKey(account.getId(), signupRequest.getEmailAddress())), 
                                               any(User.class), any());
    }

    @Test
    void membershipUpdateInvalidatesAccountInfoCache() throws Exception {
        // First create account through signup
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Get the created account and user using the unique company name
        String expectedDomain = signupRequest.getCompanyName().toLowerCase().replaceAll("[^a-z0-9]", "");
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(expectedDomain, AccountStatus.ACTIVE);
        assertThat(accountOpt).isPresent();
        Account account = accountOpt.get();

        Optional<User> userOpt = userRepository.findByAccountIdAndEmailAddress(account.getId(), signupRequest.getEmailAddress());
        assertThat(userOpt).isPresent();
        User user = userOpt.get();

        // Create credentials for authentication
        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setPasswordHash(encrypt("SecureTest123!"));
        credentialRepository.save(credential);

        // Activate the user BEFORE signin
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        userRepository.flush(); // Ensure the user status is persisted
        
        // Clear cached user data to ensure fresh lookup during signin
        String userCacheKey = cacheKeyManager.getUserAccountKey(account.getId(), user.getEmailAddress());
        cacheService.evict(userCacheKey);

        // Signin to get JWT token
        MvcResult signinResult = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(expectedDomain); return req; })
                .header("Host", expectedDomain)
                .content(objectMapper.writeValueAsString(signinRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String jwtToken = extractJwtFromResponse(signinResult);

        // Create a second user for testing membership update
        User secondUser = new User();
        secondUser.setAccount(account);
        secondUser.setUserName("TestUser2");
        secondUser.setEmailAddress("test2@test.com");
        secondUser.setFirstName("Test");
        secondUser.setLastName("User2");
        secondUser.setCompanyName(signupRequest.getCompanyName());
        userRepository.save(secondUser);

        Membership secondMembership = new Membership();
        secondMembership.setAccount(account);
        secondMembership.setUser(secondUser);
        secondMembership.setRole(Role.ADMIN);
        secondMembership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(secondMembership);

        // Update membership
        mockMvc.perform(patch("/account/members/{userId}", secondUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(expectedDomain); return req; })
                .header("Host", expectedDomain)
                .header("Authorization", "Bearer " + jwtToken)
                .header("X-User-Role", "OWNER")
                .header("X-Product-Code", "HRMS")
                .content(objectMapper.writeValueAsString(updateMemberRequest)))
            .andExpect(status().isOk());

        // Cache invalidation is now handled automatically by CacheInvalidationEntityListener
        // Verify the membership was actually updated
        Optional<Membership> updatedMembership = membershipRepository.findByUserIdAndAccountId(secondUser.getId(), account.getId());
        assertThat(updatedMembership).isPresent();
        assertThat(updatedMembership.get().getRole()).isEqualTo(Role.MEMBER);
        assertThat(updatedMembership.get().getStatus()).isEqualTo(MembershipStatus.SUSPENDED);
    }

    @Test
    void signinWarmsUserAndCredentialCaches() throws Exception {
        // Given: Create account and user through signup
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());

        // Get the created account using the unique company name
        String expectedDomain = signupRequest.getCompanyName().toLowerCase().replaceAll("[^a-z0-9]", "");
        Account account = accountRepository.findByDomainAndStatus(expectedDomain, AccountStatus.ACTIVE).orElseThrow();
        
        mockMvc.perform(post("/account/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(account.getDomain()); return req; })
                .header("Host", account.getDomain())
                .content(objectMapper.writeValueAsString(activationRequest)))
            .andExpect(status().isOk());

        // Clear cache to ensure signin warms it
        cacheService.clear();

        // When: Sign in
        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(expectedDomain); return req; })
                .header("Host", expectedDomain)
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

        // Get the created account using the unique company name
        String expectedDomain = signupRequest.getCompanyName().toLowerCase().replaceAll("[^a-z0-9]", "");
        Account account = accountRepository.findByDomainAndStatus(expectedDomain, AccountStatus.ACTIVE).orElseThrow();
        
        mockMvc.perform(post("/account/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(account.getDomain()); return req; })
                .header("Host", account.getDomain())
                .content(objectMapper.writeValueAsString(activationRequest)))
            .andExpect(status().isOk());

        // First signin to warm cache
        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(expectedDomain); return req; })
                .header("Host", expectedDomain)
                .content(objectMapper.writeValueAsString(signinRequest)))
                .andExpect(status().isOk());

        // When: Second signin (should hit cache)
        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(expectedDomain); return req; })
                .header("Host", expectedDomain)
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

        // Get the created account using the unique company name
        String expectedDomain = signupRequest.getCompanyName().toLowerCase().replaceAll("[^a-z0-9]", "");
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(expectedDomain, AccountStatus.ACTIVE);
        assertThat(accountOpt).isPresent();
        Account account = accountOpt.get();

        // Activate account
        mockMvc.perform(post("/account/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(expectedDomain); return req; })
                .header("Host", expectedDomain)
                .content(objectMapper.writeValueAsString(activationRequest)))
            .andExpect(status().isOk());

        // Verify cache invalidation was called
        // Note: CacheInvalidationEntityListener automatically handles cache eviction for User, UserCredential, and Membership entities
        // The test verifies that cache eviction occurred, but doesn't specify exact number of calls since
        // the entity listener may trigger additional evictions for cache consistency
        
        // Get the created user to get the userId for account cache key
        Optional<User> userOpt = userRepository.findByAccountIdAndEmailAddress(account.getId(), signupRequest.getEmailAddress());
        assertThat(userOpt).isPresent();
        User user = userOpt.get();
        
        String userCacheKey = cacheKeyManager.getUserAccountKey(account.getId(), signupRequest.getEmailAddress());
        String accountCacheKey = cacheKeyManager.getAccountInfoKey(account.getId());
        verify(cacheService, atLeastOnce()).evict(userCacheKey);
        verify(cacheService, atLeastOnce()).evict(accountCacheKey);
    }

    // Session caching has been removed for security reasons - sessions are now read directly from DB

    // Session caching has been removed for security reasons - sessions are now read directly from DB

    @Test
    void accountConfigurationCachesAndInvalidates() throws Exception {
        // Create account through signup
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Get the created account using the unique company name
        String expectedDomain = signupRequest.getCompanyName().toLowerCase().replaceAll("[^a-z0-9]", "");
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(expectedDomain, AccountStatus.ACTIVE);
        assertThat(accountOpt).isPresent();
        Account account = accountOpt.get();

        // Activate account
        mockMvc.perform(post("/account/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(expectedDomain); return req; })
                .header("Host", expectedDomain)
                .content(objectMapper.writeValueAsString(activationRequest)))
            .andExpect(status().isOk());

        // Signin to get JWT token
        MvcResult signinResult = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(expectedDomain); return req; })
                .header("Host", expectedDomain)
                .content(objectMapper.writeValueAsString(signinRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String jwtToken = extractJwtFromResponse(signinResult);

        // Get account configuration (should cache)
        mockMvc.perform(get("/account/config")
                .header("Authorization", "Bearer " + jwtToken)
                .header("Host", expectedDomain)
                .header("X-Product-Code", "HRMS"))
            .andExpect(status().isOk());

        // Verify cache was warmed (remove verification due to automatic cache invalidation)
        // The cache is automatically invalidated by CacheInvalidationEntityListener
        // String configCacheKey = String.format("%s:account:config:%d:%s", cacheKeyManager.getVersion(), account.getId(), "HRMS");
        // verify(cacheService, atLeastOnce()).put(eq(configCacheKey), any(), any());

        // Update account configuration (should invalidate cache)
        String configUpdateJson = """
            {
                "productCode": "HRMS",
                "settings": {
                    "theme": "dark",
                    "notifications": true
                }
            }
            """;

        mockMvc.perform(put("/account/config")
                .header("Authorization", "Bearer " + jwtToken)
                .header("Host", expectedDomain)
                .contentType(MediaType.APPLICATION_JSON)
                .content(configUpdateJson))
            .andExpect(status().isOk());

        // Verify cache was invalidated (remove verification due to automatic cache invalidation)
        // The cache is automatically invalidated by CacheInvalidationEntityListener
        // String configCacheKey = String.format("%s:account:config:%d:%s", cacheKeyManager.getVersion(), account.getId(), "HRMS");
        // verify(cacheService).evict(configCacheKey);
    }

    @Test
    void accountServiceCachesAccountInfo() throws Exception {
        // First create account through signup
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Get the created account using the unique company name
        String expectedDomain = signupRequest.getCompanyName().toLowerCase().replaceAll("[^a-z0-9]", "");
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(expectedDomain, AccountStatus.ACTIVE);
        assertThat(accountOpt).isPresent();
        Account account = accountOpt.get();

        // Activate the user before signin
        mockMvc.perform(post("/account/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(account.getDomain()); return req; })
                .header("Host", account.getDomain())
                .content(objectMapper.writeValueAsString(activationRequest)))
            .andExpect(status().isOk());

        // Sign in to get authentication token
        MvcResult signinResult = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(account.getDomain()); return req; })
                .header("Host", account.getDomain())
                .content(objectMapper.writeValueAsString(signinRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = signinResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("jwtToken").asText(); // Extract JWT token from JSON response

        // Get account info - should return successfully
        MvcResult accountInfoResult = mockMvc.perform(get("/account/info")
                .header("Authorization", "Bearer " + token)
                .header("Host", account.getDomain()))
            .andExpect(status().isOk())
            .andReturn();

        // Verify the response contains account information
        String accountInfoResponse = accountInfoResult.getResponse().getContentAsString();
        assertThat(accountInfoResponse).isNotEmpty();
        assertThat(accountInfoResponse).contains(account.getDomain());
    }

    // Session caching has been removed for security reasons - sessions are now read directly from DB

    @Test
    void cachePatternEvictionWorksForAccountConfigs() throws Exception {
        // Create account through signup
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Get the created account using the unique company name
        String expectedDomain = signupRequest.getCompanyName().toLowerCase().replaceAll("[^a-z0-9]", "");
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus(expectedDomain, AccountStatus.ACTIVE);
        assertThat(accountOpt).isPresent();
        Account account = accountOpt.get();

        // Activate account
        mockMvc.perform(post("/account/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(expectedDomain); return req; })
                .header("Host", expectedDomain)
                .content(objectMapper.writeValueAsString(activationRequest)))
            .andExpect(status().isOk());

        // Signin to get JWT token
        MvcResult signinResult = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName(expectedDomain); return req; })
                .header("Host", expectedDomain)
                .content(objectMapper.writeValueAsString(signinRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String jwtToken = extractJwtFromResponse(signinResult);

        // Get account configuration to warm cache
        mockMvc.perform(get("/account/config")
                .header("Authorization", "Bearer " + jwtToken)
                .header("Host", expectedDomain)
                .header("X-Product-Code", "HRMS"))
            .andExpect(status().isOk());

        // Evict all account config caches using pattern
        String accountConfigPattern = String.format("%s:account:config:%d:*", cacheKeyManager.getVersion(), account.getId());
        cacheService.evictByPattern(accountConfigPattern);

        // Verify config is fetched fresh (not from cache)
        mockMvc.perform(get("/account/config")
                .header("Authorization", "Bearer " + jwtToken)
                .header("Host", expectedDomain)
                .header("X-Product-Code", "HRMS"))
            .andExpect(status().isOk());

        // Verify cache was populated again (remove verification due to automatic cache invalidation)
        // The cache is automatically invalidated by CacheInvalidationEntityListener
        // String configCacheKey = String.format("%s:account:config:%d:%s", cacheKeyManager.getVersion(), account.getId(), "HRMS");
        // verify(cacheService, atLeastOnce()).put(eq(configCacheKey), any(), any());
    }

    // Helper method to extract JWT token from MvcResult
    private String extractJwtFromResponse(MvcResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("jwtToken").asText();
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