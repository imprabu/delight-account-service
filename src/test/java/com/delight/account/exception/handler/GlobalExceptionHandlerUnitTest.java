package com.delight.account.exception.handler;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.delight.account.dto.ErrorResponse;
import com.delight.account.exception.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Unit tests for GlobalExceptionHandler focusing on core functionality
 * Tests exception handlers with various scenarios and validates response payloads
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerUnitTest {

    @Mock
    private MessageSource messageSource;

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler(messageSource);
    }

    // ========== ApiException Tests ==========

    @Test
    void handleApiException_WithSigninInvalid_ReturnsBadRequest() {
        // Given
        ApiException ex = new ApiException("error.signin.invalid");
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(request.getRequestURI()).thenReturn("/account/signin");
        when(messageSource.getMessage(eq("error.signin.invalid"), eq(new Object[0]), eq(Locale.ENGLISH)))
            .thenReturn("Invalid credentials");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleApiException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid credentials");
        assertThat(response.getBody().getCode()).isEqualTo("error.signin.invalid");
        assertThat(response.getBody().getPath()).isEqualTo("/account/signin");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleApiException_WithRateLimit_ReturnsTooManyRequests() {
        // Given
        ApiException ex = new ApiException("error.rate.limit.exceeded");
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(request.getRequestURI()).thenReturn("/account/signin");
        when(messageSource.getMessage(eq("error.rate.limit.exceeded"), eq(new Object[0]), eq(Locale.ENGLISH)))
            .thenReturn("Rate limit exceeded");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleApiException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().getStatus()).isEqualTo(429);
        assertThat(response.getBody().getError()).isEqualTo("Too Many Requests");
        assertThat(response.getBody().getMessage()).isEqualTo("Rate limit exceeded");
    }

    @Test
    void handleApiException_WithSessionInvalid_ReturnsUnauthorized() {
        // Given
        ApiException ex = new ApiException("error.session.invalid");
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(request.getRequestURI()).thenReturn("/account/info");
        when(messageSource.getMessage(eq("error.session.invalid"), eq(new Object[0]), eq(Locale.ENGLISH)))
            .thenReturn("Invalid session");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleApiException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
    }

    @Test
    void handleApiException_WithSignupFailed_ReturnsInternalServerError() {
        // Given
        ApiException ex = new ApiException("error.signup.failed");
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(request.getRequestURI()).thenReturn("/account/signup");
        when(messageSource.getMessage(eq("error.signup.failed"), eq(new Object[0]), eq(Locale.ENGLISH)))
            .thenReturn("Signup failed");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleApiException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
    }

    @Test
    void handleApiException_WithArgs_IncludesArgsInLogging() {
        // Given
        Object[] args = {"user@example.com", "invalid"};
        ApiException ex = new ApiException("error.signin.invalid", args);
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(request.getRequestURI()).thenReturn("/account/signin");
        when(messageSource.getMessage("error.signin.invalid", args, Locale.ENGLISH))
            .thenReturn("Invalid credentials for user@example.com");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleApiException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid credentials for user@example.com");
    }

    @Test
    void handleApiException_WithNullArgs_HandlesGracefully() {
        // Given
        ApiException ex = new ApiException("error.signin.invalid", (Object[]) null);
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(request.getRequestURI()).thenReturn("/account/signin");
        when(messageSource.getMessage(eq("error.signin.invalid"), eq(null), eq(Locale.ENGLISH)))
            .thenReturn("Invalid credentials");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleApiException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid credentials");
    }

    // ========== Validation Exception Tests ==========

    @Test
    void handleValidation_WithMethodArgumentNotValidException_ReturnsBadRequest() {
        // Given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(messageSource.getMessage("error.validation", null, Locale.ENGLISH))
            .thenReturn("Validation failed");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
    }

    @Test
    void handleValidation_WithConstraintViolationException_ReturnsBadRequest() {
        // Given
        ConstraintViolationException ex = mock(ConstraintViolationException.class);
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(messageSource.getMessage("error.validation", null, Locale.ENGLISH))
            .thenReturn("Validation failed");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
    }

    // ========== Type Mismatch Exception Tests ==========

    @Test
    void handleTypeMismatch_ReturnsBadRequest() {
        // Given
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(messageSource.getMessage("error.validation", null, Locale.ENGLISH))
            .thenReturn("Invalid parameter type");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleTypeMismatch(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid parameter type");
    }

    // ========== HTTP Message Not Readable Tests ==========

    @Test
    void handleHttpMessageNotReadable_ReturnsBadRequest() {
        // Given
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(messageSource.getMessage("error.validation", null, Locale.ENGLISH))
            .thenReturn("Invalid request body");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleHttpMessageNotReadable(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid request body");
    }

    // ========== Media Type Not Supported Tests ==========

    @Test
    void handleHttpMediaTypeNotSupported_ReturnsUnsupportedMediaType() {
        // Given
        HttpMediaTypeNotSupportedException ex = mock(HttpMediaTypeNotSupportedException.class);
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(messageSource.getMessage("error.validation", null, Locale.ENGLISH))
            .thenReturn("Unsupported media type");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleHttpMediaTypeNotSupported(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody().getMessage()).isEqualTo("Unsupported media type");
    }

    // ========== General Exception Tests ==========

    @Test
    void handleGeneral_WithRuntimeException_ReturnsInternalServerError() {
        // Given
        RuntimeException ex = new RuntimeException("Unexpected error");
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(request.getRequestURI()).thenReturn("/account/signin");
        when(messageSource.getMessage(eq("error.signup.failed"), eq(null), eq(Locale.ENGLISH)))
            .thenReturn("An unexpected error occurred");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGeneral(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getCode()).isEqualTo("error.internal");
        assertThat(response.getBody().getPath()).isEqualTo("/account/signin");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleGeneral_WithNullPointerException_ReturnsInternalServerError() {
        // Given
        NullPointerException ex = new NullPointerException("Null pointer");
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(request.getRequestURI()).thenReturn("/account/signup");
        when(messageSource.getMessage(eq("error.signup.failed"), eq(null), eq(Locale.ENGLISH)))
            .thenReturn("An unexpected error occurred");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGeneral(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getCode()).isEqualTo("error.internal");
    }

    // ========== Error Response Payload Validation Tests ==========

    @Test
    void handleApiException_ErrorResponsePayload_ContainsAllRequiredFields() {
        // Given
        ApiException ex = new ApiException("error.signin.invalid");
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(request.getRequestURI()).thenReturn("/account/signin");
        when(messageSource.getMessage(eq("error.signin.invalid"), eq(new Object[0]), eq(Locale.ENGLISH)))
            .thenReturn("Invalid credentials");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleApiException(ex, request);

        // Then
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTimestamp()).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getError()).isEqualTo("Bad Request");
        assertThat(body.getMessage()).isEqualTo("Invalid credentials");
        assertThat(body.getCode()).isEqualTo("error.signin.invalid");
        assertThat(body.getPath()).isEqualTo("/account/signin");
    }

    @Test
    void handleGeneral_ErrorResponsePayload_ContainsAllRequiredFields() {
        // Given
        RuntimeException ex = new RuntimeException("Test error");
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(request.getRequestURI()).thenReturn("/test");
        when(messageSource.getMessage(eq("error.signup.failed"), eq(null), eq(Locale.ENGLISH)))
            .thenReturn("An unexpected error occurred");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGeneral(ex, request);

        // Then
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTimestamp()).isNotNull();
        assertThat(body.getStatus()).isEqualTo(500);
        assertThat(body.getError()).isEqualTo("Internal Server Error");
        assertThat(body.getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(body.getCode()).isEqualTo("error.internal");
        assertThat(body.getPath()).isEqualTo("/test");
    }

    // ========== Locale Handling Tests ==========

    @Test
    void handleApiException_WithSpanishLocale_ReturnsLocalizedMessage() {
        // Given
        ApiException ex = new ApiException("error.signin.invalid");
        when(request.getLocale()).thenReturn(new Locale("es"));
        when(request.getRequestURI()).thenReturn("/account/signin");
        when(messageSource.getMessage(eq("error.signin.invalid"), eq(new Object[0]), eq(new Locale("es"))))
            .thenReturn("Credenciales inválidas");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleApiException(ex, request);

        // Then
        assertThat(response.getBody().getMessage()).isEqualTo("Credenciales inválidas");
    }

    @Test
    void handleApiException_WithFrenchLocale_ReturnsLocalizedMessage() {
        // Given
        ApiException ex = new ApiException("error.signin.invalid");
        when(request.getLocale()).thenReturn(new Locale("fr"));
        when(request.getRequestURI()).thenReturn("/account/signin");
        when(messageSource.getMessage(eq("error.signin.invalid"), eq(new Object[0]), eq(new Locale("fr"))))
            .thenReturn("Identifiants invalides");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleApiException(ex, request);

        // Then
        assertThat(response.getBody().getMessage()).isEqualTo("Identifiants invalides");
    }

    // ========== Edge Cases Tests ==========

    @Test
    void handleApiException_WithEmptyMessageKey_ReturnsDefaultStatus() {
        // Given
        ApiException ex = new ApiException("");
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(request.getRequestURI()).thenReturn("/test");
        when(messageSource.getMessage(eq(""), eq(new Object[0]), eq(Locale.ENGLISH)))
            .thenReturn("Unknown error");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleApiException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleApiException_WithNullRequestUri_HandlesGracefully() {
        // Given
        ApiException ex = new ApiException("error.signin.invalid");
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        when(request.getRequestURI()).thenReturn(null);
        when(messageSource.getMessage(eq("error.signin.invalid"), eq(new Object[0]), eq(Locale.ENGLISH)))
            .thenReturn("Invalid credentials");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleApiException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getPath()).isNull();
    }
}
