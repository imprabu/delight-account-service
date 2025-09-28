package com.delight.account.integration;

import com.delight.account.dto.SignupRequest;
import com.delight.account.dto.SigninRequest;
import com.delight.account.model.*;
import com.delight.account.repository.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthenticationFlowIntegrationTest {

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

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SignupRequest signupRequest;
    private SigninRequest signinRequest;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest();
        signupRequest.setCompanyName("IntegrationTestCo");
        signupRequest.setEmailAddress("integration@test.com");
        signupRequest.setIndustryType("Technology");
        signupRequest.setFirstName("Integration");
        signupRequest.setLastName("Test");

        signinRequest = new SigninRequest();
        signinRequest.setEmailAddress("integration@test.com");
        signinRequest.setPassword("TestPassword123");
    }

    @Test
    void completeAuthenticationFlow() throws Exception {
        // Step 1: Signup - Create new account
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk())
            .andExpect(content().string("Signup successful"));

        // Verify account was created
        Optional<Account> accountOpt = accountRepository.findByDomainAndStatus("integrationtestco", AccountStatus.ACTIVE);
        assertThat(accountOpt).isPresent();
        Account account = accountOpt.get();
        assertThat(account.getCompanyName()).isEqualTo("IntegrationTestCo");
        assertThat(account.getEmail()).isEqualTo("integration@test.com");
        assertThat(account.getPlan()).isEqualTo(PlanType.TRIAL);

        // Verify user was created
        Optional<User> userOpt = userRepository.findByAccountIdAndEmailAddress(account.getId(), "integration@test.com");
        assertThat(userOpt).isPresent();
        User user = userOpt.get();
        assertThat(user.getFirstName()).isEqualTo("Integration");
        assertThat(user.getLastName()).isEqualTo("Test");

        // Verify membership was created
        Optional<Membership> membershipOpt = membershipRepository.findByUserIdAndAccountId(user.getId(), account.getId());
        assertThat(membershipOpt).isPresent();
        Membership membership = membershipOpt.get();
        assertThat(membership.getRole()).isEqualTo(Role.OWNER);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);

        // Step 2: Create credentials for signin (simulating activation)
        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setPasswordHash(encrypt("TestPassword123")); // Properly encrypt the password
        credentialRepository.save(credential);

        // Step 3: Signin - Get JWT token
        MvcResult signinResult = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Host", "integrationtestco")
                .content(objectMapper.writeValueAsString(signinRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String jwtToken = signinResult.getResponse().getContentAsString();
        assertThat(jwtToken).isNotEmpty();

        // Verify JWT token contents
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(jwtToken)
            .getPayload();

        assertThat(claims.get("accountId", Long.class)).isEqualTo(account.getId());
        assertThat(claims.get("userId", Long.class)).isEqualTo(user.getId());
        assertThat(claims.get("role", String.class)).isEqualTo("OWNER");

        // Step 4: Use JWT token to access protected endpoint
        mockMvc.perform(get("/account/info")
                .header("Authorization", "Bearer " + jwtToken)
                .header("Host", "integrationtestco"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyName").value("IntegrationTestCo"))
            .andExpect(jsonPath("$.email").value("integration@test.com"))
            .andExpect(jsonPath("$.currentUserRole").value("OWNER"));
    }

    @Test
    void signupWithExistingDomainGeneratesUniqueDomain() throws Exception {
        // First signup
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Second signup with same company name
        SignupRequest duplicateRequest = new SignupRequest();
        duplicateRequest.setCompanyName("IntegrationTestCo");
        duplicateRequest.setEmailAddress("duplicate@test.com");
        duplicateRequest.setIndustryType("Technology");
        duplicateRequest.setFirstName("Duplicate");
        duplicateRequest.setLastName("User");

        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
            .andExpect(status().isOk());

        // Verify two different accounts exist
        var accounts = accountRepository.findAll();
        var integrationTestAccounts = accounts.stream()
            .filter(acc -> acc.getDomain().startsWith("integrationtestco"))
            .toList();
        
        assertThat(integrationTestAccounts).hasSize(2);
        assertThat(integrationTestAccounts.get(0).getDomain()).isNotEqualTo(integrationTestAccounts.get(1).getDomain());
    }

    @Test
    void signinWithInvalidCredentialsReturnsUnauthorized() throws Exception {
        // First create account
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Try to signin with wrong password
        SigninRequest wrongPasswordRequest = new SigninRequest();
        wrongPasswordRequest.setEmailAddress("integration@test.com");
        wrongPasswordRequest.setPassword("WrongPassword");

        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Host", "integrationtestco")
                .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void accessProtectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/account/info"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void accessProtectedEndpointWithInvalidTokenReturnsUnauthorized() throws Exception {
        String invalidToken = "invalid.jwt.token";

        mockMvc.perform(get("/account/info")
                .header("Authorization", "Bearer " + invalidToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void membershipManagementFlow() throws Exception {
        // Step 1: Create account through signup
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        // Get the created account and user
        Account account = accountRepository.findByDomainAndStatus("integrationtestco", AccountStatus.ACTIVE).orElseThrow();
        User user = userRepository.findByAccountIdAndEmailAddress(account.getId(), "integration@test.com").orElseThrow();

        // Create credentials
        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setPasswordHash(encrypt("TestPassword123"));
        credentialRepository.save(credential);

        // Step 2: Signin to get JWT
        MvcResult signinResult = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Host", "integrationtestco")
                .content(objectMapper.writeValueAsString(signinRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String jwtToken = signinResult.getResponse().getContentAsString();

        // Step 3: List members
        mockMvc.perform(get("/account/members")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].emailAddress").value("integration@test.com"))
            .andExpect(jsonPath("$[0].role").value("OWNER"));

        // Step 4: Create another user and membership for testing update
        User secondUser = new User();
        secondUser.setAccount(account);
        secondUser.setUserName("TestUser2");
        secondUser.setEmailAddress("test2@test.com");
        secondUser.setFirstName("Test");
        secondUser.setLastName("User2");
        secondUser.setCompanyName("IntegrationTestCo");
        userRepository.save(secondUser);

        Membership secondMembership = new Membership();
        secondMembership.setAccount(account);
        secondMembership.setUser(secondUser);
        secondMembership.setRole(Role.MEMBER);
        secondMembership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(secondMembership);

        // Step 5: Update member role
        String updateRequest = "{\"role\":\"ADMIN\"}";
        mockMvc.perform(patch("/account/members/" + secondUser.getId())
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
            .andExpect(status().isOk());

        // Verify the update
        Membership updatedMembership = membershipRepository.findByUserIdAndAccountId(secondUser.getId(), account.getId()).orElseThrow();
        assertThat(updatedMembership.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void swaggerEndpointsAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk());
    }

    @Test
    void jwtTokenExpirationHandling() throws Exception {
        // Create account and credentials
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isOk());

        Account account = accountRepository.findByDomainAndStatus("integrationtestco", AccountStatus.ACTIVE).orElseThrow();
        User user = userRepository.findByAccountIdAndEmailAddress(account.getId(), "integration@test.com").orElseThrow();

        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setPasswordHash(encrypt("TestPassword123"));
        credentialRepository.save(credential);

        // Get JWT token
        MvcResult signinResult = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Host", "integrationtestco")
                .content(objectMapper.writeValueAsString(signinRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String jwtToken = signinResult.getResponse().getContentAsString();

        // Verify token is valid
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(jwtToken)
            .getPayload();

        // Check expiration is set (should be 24 hours from now)
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration().getTime()).isGreaterThan(System.currentTimeMillis());
    }

    private String encrypt(String value) {
        try {
            byte[] keyBytes = "1234567890123456".getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, 0, 16, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
}
