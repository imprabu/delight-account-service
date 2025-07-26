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

    @BeforeEach
    void setUp() throws Exception {
        Account account = new Account();
        account.setDomain("localhost");
        account.setPlan(PlanType.TRIAL);
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);

        User user = new User();
        user.setAccount(account);
        user.setEmailAddress("john@example.com");
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        UserCredential credential = new UserCredential();
        credential.setAccount(account);
        credential.setUser(user);
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

        assertThat(response).isNotBlank();
    }

    @Test
    void signinUnknownUserReturnsError() throws Exception {
        SigninRequest request = new SigninRequest();
        request.setEmailAddress("missing@example.com");
        request.setPassword("pass");

        mockMvc.perform(post("/account/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
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

