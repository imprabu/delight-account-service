package com.delight.account.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonDeserialize(builder = ErrorResponse.ErrorResponseBuilder.class)
@Schema(description = "Standard error response")
public class ErrorResponse {
    @Schema(description = "Timestamp of the error in ISO-8601", example = "2025-09-25T12:34:56.789Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime timestamp;

    @Schema(description = "HTTP status code of the error", example = "400")
    private int status;

    @Schema(description = "HTTP error phrase", example = "Bad Request")
    private String error;

    @Schema(description = "Brief error message describing what went wrong", example = "Invalid request parameters")
    private String message;

    @Schema(description = "Machine-readable error code/message key", example = "error.validation")
    private String code;

    @Schema(description = "Request path", example = "/account/signup")
    private String path;

    @JsonPOJOBuilder(withPrefix = "")
    public static class ErrorResponseBuilder {
    }
}

