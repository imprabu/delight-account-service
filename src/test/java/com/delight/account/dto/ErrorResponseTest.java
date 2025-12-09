package com.delight.account.dto;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Test suite for ErrorResponse DTO
 * Validates the structure and serialization of error response payloads
 */
class ErrorResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Test
    void errorResponse_Builder_CreatesValidResponse() {
        // Given
        OffsetDateTime timestamp = OffsetDateTime.now();
        int status = 400;
        String error = "Bad Request";
        String message = "Invalid credentials";
        String code = "error.signin.invalid";
        String path = "/account/signin";

        // When
        ErrorResponse response = ErrorResponse.builder()
            .timestamp(timestamp)
            .status(status)
            .error(error)
            .message(message)
            .code(code)
            .path(path)
            .build();

        // Then
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getError()).isEqualTo(error);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getCode()).isEqualTo(code);
        assertThat(response.getPath()).isEqualTo(path);
    }

    @Test
    void errorResponse_Serialization_ProducesValidJson() throws Exception {
        // Given
        OffsetDateTime timestamp = OffsetDateTime.parse("2023-10-25T10:30:00Z");
        ErrorResponse response = ErrorResponse.builder()
            .timestamp(timestamp)
            .status(400)
            .error("Bad Request")
            .message("Invalid credentials")
            .code("error.signin.invalid")
            .path("/account/signin")
            .build();

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then
        assertThat(json).contains("\"timestamp\":\"2023-10-25T10:30:00.000Z\"");
        assertThat(json).contains("\"status\":400");
        assertThat(json).contains("\"error\":\"Bad Request\"");
        assertThat(json).contains("\"message\":\"Invalid credentials\"");
        assertThat(json).contains("\"code\":\"error.signin.invalid\"");
        assertThat(json).contains("\"path\":\"/account/signin\"");
    }

    @Test
    void errorResponse_Deserialization_FromJson_WorksCorrectly() throws Exception {
        // Given
        String json = """
            {
                "timestamp": "2023-10-25T10:30:00.000Z",
                "status": 500,
                "error": "Internal Server Error",
                "message": "An unexpected error occurred",
                "code": "error.internal",
                "path": "/account/signup"
            }
            """;

        // When
        ErrorResponse response = objectMapper.readValue(json, ErrorResponse.class);

        // Then
        assertThat(response.getTimestamp()).isEqualTo(OffsetDateTime.parse("2023-10-25T10:30:00.000Z"));
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getError()).isEqualTo("Internal Server Error");
        assertThat(response.getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getCode()).isEqualTo("error.internal");
        assertThat(response.getPath()).isEqualTo("/account/signup");
    }

    @Test
    void errorResponse_WithNullValues_HandlesGracefully() {
        // Given & When
        ErrorResponse response = ErrorResponse.builder()
            .timestamp(null)
            .status(400)
            .error(null)
            .message(null)
            .code(null)
            .path(null)
            .build();

        // Then
        assertThat(response.getTimestamp()).isNull();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getError()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getCode()).isNull();
        assertThat(response.getPath()).isNull();
    }

    @Test
    void errorResponse_ToString_ContainsAllFields() {
        // Given
        ErrorResponse response = ErrorResponse.builder()
            .timestamp(OffsetDateTime.parse("2023-10-25T10:30:00Z"))
            .status(400)
            .error("Bad Request")
            .message("Invalid credentials")
            .code("error.signin.invalid")
            .path("/account/signin")
            .build();

        // When
        String toString = response.toString();

        // Then
        assertThat(toString).contains("timestamp=2023-10-25T10:30Z");
        assertThat(toString).contains("status=400");
        assertThat(toString).contains("error=Bad Request");
        assertThat(toString).contains("message=Invalid credentials");
        assertThat(toString).contains("code=error.signin.invalid");
        assertThat(toString).contains("path=/account/signin");
    }

    @Test
    void errorResponse_EqualsAndHashCode_WorkCorrectly() {
        // Given
        OffsetDateTime timestamp = OffsetDateTime.now();
        ErrorResponse response1 = ErrorResponse.builder()
            .timestamp(timestamp)
            .status(400)
            .error("Bad Request")
            .message("Invalid credentials")
            .code("error.signin.invalid")
            .path("/account/signin")
            .build();

        ErrorResponse response2 = ErrorResponse.builder()
            .timestamp(timestamp)
            .status(400)
            .error("Bad Request")
            .message("Invalid credentials")
            .code("error.signin.invalid")
            .path("/account/signin")
            .build();

        ErrorResponse response3 = ErrorResponse.builder()
            .timestamp(timestamp)
            .status(500)
            .error("Internal Server Error")
            .message("Unexpected error")
            .code("error.internal")
            .path("/account/signup")
            .build();

        // When & Then
        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        assertThat(response1).isNotEqualTo(response3);
        assertThat(response1.hashCode()).isNotEqualTo(response3.hashCode());
    }

    @Test
    void errorResponse_JsonStructure_MatchesExpectedFormat() throws Exception {
        // Given
        ErrorResponse response = ErrorResponse.builder()
            .timestamp(OffsetDateTime.parse("2023-10-25T10:30:00Z"))
            .status(400)
            .error("Bad Request")
            .message("Invalid credentials")
            .code("error.signin.invalid")
            .path("/account/signin")
            .build();

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then
        // Validate JSON structure
        assertThat(json).startsWith("{");
        assertThat(json).endsWith("}");
        assertThat(json).contains("\"timestamp\":");
        assertThat(json).contains("\"status\":");
        assertThat(json).contains("\"error\":");
        assertThat(json).contains("\"message\":");
        assertThat(json).contains("\"code\":");
        assertThat(json).contains("\"path\":");

        // Validate field order and formatting
        assertThat(json).matches(".*\"timestamp\":\"[^\"]+\".*");
        assertThat(json).matches(".*\"status\":\\d+.*");
        assertThat(json).matches(".*\"error\":\"[^\"]+\".*");
        assertThat(json).matches(".*\"message\":\"[^\"]+\".*");
        assertThat(json).matches(".*\"code\":\"[^\"]+\".*");
        assertThat(json).matches(".*\"path\":\"[^\"]*\".*");
    }
}

