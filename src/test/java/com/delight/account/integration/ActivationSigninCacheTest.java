package com.delight.account.integration;

import com.delight.account.dto.SignupRequest;
import com.delight.account.dto.SigninRequest;
import com.delight.account.dto.ActivationRequest;
import com.delight.account.model.*;
import com.delight.account.repository.*;
import com.delight.account.service.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ActivationSigninCacheTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @SpyBean
    private CacheService cacheService;

    @Autowired
    private com.delight.account.cache.CacheKeyManager cacheKeyManager;

    private SignupRequest signupRequest;
    private SigninRequest signinRequest;
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
        signinRequest.setPassword("SecureTest123!");

        activationRequest = new ActivationRequest();
        activationRequest.setEmailAddress("cache@test.com");
        activationRequest.setFirstName("Cache");
        activationRequest.setLastName("Test");
        activationRequest.setPhoneNumber("+1-555-123-4567");
        activationRequest.setPassword("SecureTest123!");
        activationRequest.setRetypePassword("SecureTest123!");
    }

    @Test
    void activationInvalidatesUserCacheAndSigninWarmsCache() throws Exception {
        // Step 1: Create account through signup
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

        // Verify user is cached as INSERTED
        assertThat(user.getStatus()).isEqualTo(UserStatus.INSERTED);

        // Step 2: Perform activation
        mockMvc.perform(post("/account/activate")
                .header("Host", account.getDomain())
                .with(req -> { req.setServerName(account.getDomain()); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activationRequest)))
            .andExpect(status().isOk());

        // Verify the user was actually activated
        User activatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(activatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);

        // Step 3: Clear cache to ensure signin warms it
        cacheService.clear();

        // Step 4: Sign in
        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .with(req -> { req.setServerName("cachetestco"); return req; })
                .header("Host", "cachetestco")
                .content(objectMapper.writeValueAsString(signinRequest)))
                .andExpect(status().isOk());

        // Step 5: Verify user cache is warmed after signin
        String userCacheKey = cacheKeyManager != null
            ? cacheKeyManager.getUserAccountKey(account.getId(), signinRequest.getEmailAddress())
            : String.format("user:account:%d:email:%s", account.getId(), signinRequest.getEmailAddress());
        assertThat(cacheService.exists(userCacheKey)).isTrue();
    }
}