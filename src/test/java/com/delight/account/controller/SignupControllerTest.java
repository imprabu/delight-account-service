package com.delight.account.controller;

import com.delight.account.dto.SignupRequest;
import com.delight.account.exception.AccountCreationException;
import com.delight.account.service.SignupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SignupController.class)
class SignupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SignupService signupService;

    @MockBean
    private MessageSource messageSource;

    @Autowired
    private ObjectMapper objectMapper;

    private SignupRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new SignupRequest();
        validRequest.setCompanyName("TestCo");
        validRequest.setEmailAddress("test@example.com");
        validRequest.setIndustryType("IT");
        validRequest.setFirstName("John");
        validRequest.setLastName("Doe");
    }

    @Test
    void signupWithValidRequestReturnsSuccess() throws Exception {
        when(messageSource.getMessage("signup.success", null, Locale.ENGLISH))
            .thenReturn("Signup successful");

        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isOk())
            .andExpect(content().string("Signup successful"));

        verify(signupService).signup(any(SignupRequest.class));
        verify(messageSource).getMessage("signup.success", null, Locale.ENGLISH);
    }

    @Test
    void signupWithAcceptLanguageHeaderUsesCorrectLocale() throws Exception {
        Locale frenchLocale = Locale.FRENCH;
        when(messageSource.getMessage("signup.success", null, frenchLocale))
            .thenReturn("Inscription réussie");

        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Accept-Language", "fr")
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isOk())
            .andExpect(content().string("Inscription réussie"));

        verify(messageSource).getMessage("signup.success", null, frenchLocale);
    }

    @Test
    void signupWithMissingCompanyNameReturnsBadRequest() throws Exception {
        validRequest.setCompanyName("");

        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isBadRequest());

        verify(signupService, never()).signup(any());
    }

    @Test
    void signupWithInvalidEmailReturnsBadRequest() throws Exception {
        validRequest.setEmailAddress("not-an-email");

        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isBadRequest());

        verify(signupService, never()).signup(any());
    }

    @Test
    void signupWithMissingFirstNameReturnsBadRequest() throws Exception {
        validRequest.setFirstName("");

        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isBadRequest());

        verify(signupService, never()).signup(any());
    }

    @Test
    void signupWithMissingLastNameReturnsBadRequest() throws Exception {
        validRequest.setLastName("");

        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isBadRequest());

        verify(signupService, never()).signup(any());
    }

    @Test
    void signupWithMissingIndustryTypeReturnsBadRequest() throws Exception {
        validRequest.setIndustryType("");

        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isBadRequest());

        verify(signupService, never()).signup(any());
    }

    @Test
    void signupWithTooLongFieldsReturnsBadRequest() throws Exception {
        String longString = "a".repeat(256); // Exceeds @Size(max = 255)
        validRequest.setCompanyName(longString);

        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isBadRequest());

        verify(signupService, never()).signup(any());
    }

    @Test
    void signupWithNullRequestBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        verify(signupService, never()).signup(any());
    }

    @Test
    void signupWithInvalidJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
            .andExpect(status().isBadRequest());

        verify(signupService, never()).signup(any());
    }

    @Test
    void signupWithMissingContentTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/account/signup")
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isUnsupportedMediaType());

        verify(signupService, never()).signup(any());
    }

    @Test
    void signupServiceExceptionReturnsInternalServerError() throws Exception {
        doThrow(new AccountCreationException()).when(signupService).signup(any());

        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isInternalServerError());

        verify(signupService).signup(any(SignupRequest.class));
    }

    @Test
    void signupWithNullFieldsReturnsBadRequest() throws Exception {
        SignupRequest nullFieldsRequest = new SignupRequest();
        // All fields are null by default

        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nullFieldsRequest)))
            .andExpect(status().isBadRequest());

        verify(signupService, never()).signup(any());
    }

    @Test
    void signupWithWhitespaceOnlyFieldsReturnsBadRequest() throws Exception {
        validRequest.setCompanyName("   ");
        validRequest.setFirstName("   ");

        mockMvc.perform(post("/account/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isBadRequest());

        verify(signupService, never()).signup(any());
    }
}
