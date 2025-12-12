package com.delight.account.controller;

import com.delight.account.dto.SigninRequest;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.model.ClientType;
import com.delight.account.model.PlanType;
import com.delight.account.model.Session;
import com.delight.account.model.User;
import com.delight.account.model.UserCredential;
import com.delight.account.model.UserStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.SessionRepository;
import com.delight.account.repository.UserCredentialRepository;
import com.delight.account.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SigninControllerClientTypeTest {

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

    @Autowired
    private SessionRepository sessionRepository;

    private Account activeAccount;
    private User activeUser;

    @BeforeEach
    void setUp() throws Exception {
        // Create active account with localhost domain
        activeAccount = new Account();
        activeAccount.setDomain("localhost");
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
    void signinWithWebClientTypeCreatesWebSession() throws Exception {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("john@example.com");
        request.setPassword("pass");

        String response = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-request-source", "web")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String sessionId = objectMapper.readTree(response).get("sessionId").asText();
        
        // Verify session was created with WEB client type
        Optional<Session> sessionOpt = sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
        assertThat(sessionOpt).isPresent();
        Session session = sessionOpt.get();
        assertThat(session.getClientType()).isEqualTo(ClientType.WEB);
        
        // Verify web session has 24-hour TTL
        OffsetDateTime expectedExpiry = session.getCreatedAt().plusHours(24);
        assertThat(session.getExpiresAt()).isBetween(
            expectedExpiry.minusMinutes(1), 
            expectedExpiry.plusMinutes(1)
        );
    }

    @Test
    void signinWithAndroidClientTypeCreatesAndroidSession() throws Exception {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("john@example.com");
        request.setPassword("pass");

        String response = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-request-source", "android")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String sessionId = objectMapper.readTree(response).get("sessionId").asText();
        
        // Verify session was created with ANDROID client type
        Optional<Session> sessionOpt = sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
        assertThat(sessionOpt).isPresent();
        Session session = sessionOpt.get();
        assertThat(session.getClientType()).isEqualTo(ClientType.ANDROID);
        
        // Verify Android session has 7-day TTL (168 hours)
        OffsetDateTime expectedExpiry = session.getCreatedAt().plusHours(168);
        assertThat(session.getExpiresAt()).isBetween(
            expectedExpiry.minusMinutes(1), 
            expectedExpiry.plusMinutes(1)
        );
    }

    @Test
    void signinWithIosClientTypeCreatesIosSession() throws Exception {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("john@example.com");
        request.setPassword("pass");

        String response = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-request-source", "ios")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String sessionId = objectMapper.readTree(response).get("sessionId").asText();
        
        // Verify session was created with IOS client type
        Optional<Session> sessionOpt = sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
        assertThat(sessionOpt).isPresent();
        Session session = sessionOpt.get();
        assertThat(session.getClientType()).isEqualTo(ClientType.IOS);
        
        // Verify iOS session has 7-day TTL (168 hours)
        OffsetDateTime expectedExpiry = session.getCreatedAt().plusHours(168);
        assertThat(session.getExpiresAt()).isBetween(
            expectedExpiry.minusMinutes(1), 
            expectedExpiry.plusMinutes(1)
        );
    }

    @Test
    void signinWithoutClientTypeHeaderDefaultsToWeb() throws Exception {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("john@example.com");
        request.setPassword("pass");

        String response = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String sessionId = objectMapper.readTree(response).get("sessionId").asText();
        
        // Verify session defaults to WEB client type
        Optional<Session> sessionOpt = sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
        assertThat(sessionOpt).isPresent();
        Session session = sessionOpt.get();
        assertThat(session.getClientType()).isEqualTo(ClientType.WEB);
    }

    @Test
    void signinWithUnknownClientTypeDefaultsToWeb() throws Exception {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("john@example.com");
        request.setPassword("pass");

        String response = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-request-source", "unknown-client")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String sessionId = objectMapper.readTree(response).get("sessionId").asText();
        
        // Verify session defaults to WEB client type for unknown sources
        Optional<Session> sessionOpt = sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
        assertThat(sessionOpt).isPresent();
        Session session = sessionOpt.get();
        assertThat(session.getClientType()).isEqualTo(ClientType.WEB);
    }

    @Test
    void signinWithCaseInsensitiveClientTypeWorks() throws Exception {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("john@example.com");
        request.setPassword("pass");

        String response = mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-request-source", "ANDROID")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String sessionId = objectMapper.readTree(response).get("sessionId").asText();
        
        // Verify case-insensitive parsing works
        Optional<Session> sessionOpt = sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
        assertThat(sessionOpt).isPresent();
        Session session = sessionOpt.get();
        assertThat(session.getClientType()).isEqualTo(ClientType.ANDROID);
    }

    private String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec keySpec = new SecretKeySpec("1234567890123456".getBytes(StandardCharsets.UTF_8), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }
}