package com.delight.account.controller;

import com.delight.account.dto.SigninRequest;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.model.PlanType;
import com.delight.account.model.User;
import com.delight.account.model.UserCredential;
import com.delight.account.model.UserStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.UserCredentialRepository;
import com.delight.account.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SigninControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository credentialRepository;

    private Account activeAccount;
    private User activeUser;

    @BeforeEach
    void setUp() throws Exception {
        // Create active account with localhost domain
        activeAccount = new Account();
        activeAccount.setDomain("localhost");
        activeAccount.setPlan(PlanType.TRIAL);
        activeAccount.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(activeAccount);

        // Create active user
        activeUser = new User();
        activeUser.setAccount(activeAccount);
        activeUser.setEmailAddress("john@example.com");
        activeUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(activeUser);

        // Create user credential
        UserCredential credential = new UserCredential();
        credential.setAccount(activeAccount);
        credential.setUser(activeUser);
        credential.setPasswordHash(encrypt("pass"));
        credentialRepository.save(credential);
    }

    @Test
    void signinReturnsToken() throws Exception {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("john@example.com");
        request.setPassword("pass");

        String response = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("jwtToken").asText();
        assertThat(token).isNotBlank();
    }

    @Test
    void signinUnknownUserReturnsError() throws Exception {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("missing@example.com");
        request.setPassword("pass");

        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void signinWithInvalidPasswordReturnsError() throws Exception {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("john@example.com");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void signinWithInactiveAccountReturnsError() throws Exception {
        // Create inactive account - needs to be ACTIVE for TenantFilter but we'll test the signin logic
        Account inactiveAccount = new Account();
        inactiveAccount.setDomain("inactive");
        inactiveAccount.setPlan(PlanType.TRIAL);
        inactiveAccount.setStatus(AccountStatus.ACTIVE); // Must be ACTIVE for TenantFilter
        accountRepository.save(inactiveAccount);

        User inactiveUser = new User();
        inactiveUser.setAccount(inactiveAccount);
        inactiveUser.setEmailAddress("inactive@inactive.com");
        inactiveUser.setStatus(UserStatus.DEACTIVATED); // This is what we're testing
        userRepository.save(inactiveUser);

        UserCredential inactiveCredential = new UserCredential();
        inactiveCredential.setAccount(inactiveAccount);
        inactiveCredential.setUser(inactiveUser);
        inactiveCredential.setPasswordHash(encrypt("pass"));
        credentialRepository.save(inactiveCredential);

        SigninRequest request = new SigninRequest();
        request.setEmailAddress("inactive@inactive.com");
        request.setPassword("pass");

        mockMvc.perform(post("/account/signin")
                .servletPath("/account/signin")
                .with(req -> {
                    req.setServerName("inactive.com");
                    return req;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("User is not active"));
    }

    @Test
    void signinWithInactiveUserReturnsError() throws Exception {
        // Create inactive user
        User inactiveUser = new User();
        inactiveUser.setAccount(activeAccount);
        inactiveUser.setEmailAddress("inactive@example.com");
        inactiveUser.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(inactiveUser);

        UserCredential inactiveCredential = new UserCredential();
        inactiveCredential.setAccount(activeAccount);
        inactiveCredential.setUser(inactiveUser);
        inactiveCredential.setPasswordHash(encrypt("pass"));
        credentialRepository.save(inactiveCredential);

        SigninRequest request = new SigninRequest();
        request.setEmailAddress("inactive@example.com");
        request.setPassword("pass");

        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("User is not active"));
    }

    @Test
    void signinWithDifferentDomainReturnsError() throws Exception {
        // Create account with different domain
        Account differentAccount = new Account();
        differentAccount.setDomain("different.com");
        differentAccount.setPlan(PlanType.TRIAL);
        differentAccount.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(differentAccount);

        User differentUser = new User();
        differentUser.setAccount(differentAccount);
        differentUser.setEmailAddress("user@different.com");
        differentUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(differentUser);

        UserCredential differentCredential = new UserCredential();
        differentCredential.setAccount(differentAccount);
        differentCredential.setUser(differentUser);
        differentCredential.setPasswordHash(encrypt("pass"));
        credentialRepository.save(differentCredential);

        SigninRequest request = new SigninRequest();
        request.setEmailAddress("user@different.com");
        request.setPassword("pass");

        // Request to localhost should fail for different.com domain user
        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void signinWithEmptyEmailReturnsError() throws Exception {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("");
        request.setPassword("pass");

        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void signinWithNullEmailReturnsError() throws Exception {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress(null);
        request.setPassword("pass");

        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void signinWithEmptyPasswordReturnsError() throws Exception {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("john@example.com");
        request.setPassword("");

        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void signinWithNullPasswordReturnsError() throws Exception {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("john@example.com");
        request.setPassword(null);

        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void signinWithInvalidJsonReturnsError() throws Exception {
        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void signinWithMissingContentTypeReturnsError() throws Exception {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("john@example.com");
        request.setPassword("pass");

        mockMvc.perform(post("/account/signin")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnsupportedMediaType());
    }

    private String encrypt(String value) throws Exception {
        byte[] keyBytes = "1234567890123456".getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, 0, 16, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }
}

