package com.delight.account.controller;

import com.delight.account.dto.SessionResponse;
import com.delight.account.dto.SessionValidationRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Role;
import com.delight.account.service.SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
class SessionControllerJwtValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionService sessionService;

    @Autowired
    private ObjectMapper objectMapper;

    private SessionResponse mockSessionResponse;
    private SessionValidationRequest validRequest;

    @BeforeEach
    void setUp() {
        mockSessionResponse = SessionResponse.builder()
                .sessionId("test-session-id")
                .userId(1L)
                .accountId(1L)
                .firstName("Test")
                .lastName("User")
                .emailAddress("test@example.com")
                .roleId(3L)
                .roleCode("AGENT")
                .roleName("Agent")
                .accountDomain("test-domain")
                .companyName("Test Company")
                .lastAccessedAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .isActive(true)
                .token("test-jwt-token")
                .build();

        validRequest = new SessionValidationRequest("valid-jwt-token");
    }

    @Test
    void validateSessionWithToken_ValidToken_ReturnsSessionResponse() throws Exception {
        // Given
        when(sessionService.validateSessionByToken("valid-jwt-token"))
                .thenReturn(Optional.of(mockSessionResponse));

        // When & Then
        mockMvc.perform(post("/accounts/session/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sessionId").value("test-session-id"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.accountDomain").value("test-domain"));

        verify(sessionService).validateSessionByToken("valid-jwt-token");
        verify(sessionService).updateLastAccessed("test-session-id");
    }

    @Test
    void validateSessionWithToken_InvalidToken_ThrowsApiException() throws Exception {
        // Given
        when(sessionService.validateSessionByToken("invalid-jwt-token"))
                .thenReturn(Optional.empty());

        SessionValidationRequest invalidRequest = new SessionValidationRequest("invalid-jwt-token");

        // When & Then
        mockMvc.perform(post("/accounts/session/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isNotFound());

        verify(sessionService).validateSessionByToken("invalid-jwt-token");
        verify(sessionService, never()).updateLastAccessed(anyString());
    }

    @Test
    void validateSessionWithToken_EmptyToken_ThrowsApiException() throws Exception {
        // Given
        SessionValidationRequest emptyTokenRequest = new SessionValidationRequest("");

        // When & Then
        mockMvc.perform(post("/accounts/session/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyTokenRequest)))
                .andExpect(status().isBadRequest());

        verify(sessionService, never()).validateSessionByToken(anyString());
        verify(sessionService, never()).updateLastAccessed(anyString());
    }

    @Test
    void validateSessionWithToken_NullToken_ThrowsApiException() throws Exception {
        // Given
        SessionValidationRequest nullTokenRequest = new SessionValidationRequest(null);

        // When & Then
        mockMvc.perform(post("/accounts/session/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullTokenRequest)))
                .andExpect(status().isBadRequest());

        verify(sessionService, never()).validateSessionByToken(anyString());
        verify(sessionService, never()).updateLastAccessed(anyString());
    }

    @Test
    void validateSessionWithToken_WhitespaceToken_ThrowsApiException() throws Exception {
        // Given
        SessionValidationRequest whitespaceTokenRequest = new SessionValidationRequest("   ");

        // When & Then
        mockMvc.perform(post("/accounts/session/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(whitespaceTokenRequest)))
                .andExpect(status().isBadRequest());

        verify(sessionService, never()).validateSessionByToken(anyString());
        verify(sessionService, never()).updateLastAccessed(anyString());
    }

    @Test
    void validateSessionWithToken_ServiceException_ThrowsApiException() throws Exception {
        // Given
        when(sessionService.validateSessionByToken("valid-jwt-token"))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(post("/accounts/session/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError());

        verify(sessionService).validateSessionByToken("valid-jwt-token");
        verify(sessionService, never()).updateLastAccessed(anyString());
    }

    @Test
    void validateSessionWithToken_MissingRequestBody_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/accounts/session/validate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(sessionService, never()).validateSessionByToken(anyString());
        verify(sessionService, never()).updateLastAccessed(anyString());
    }

    @Test
    void validateSessionWithToken_InvalidJson_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/accounts/session/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json}"))
                .andExpect(status().isBadRequest());

        verify(sessionService, never()).validateSessionByToken(anyString());
        verify(sessionService, never()).updateLastAccessed(anyString());
    }
}