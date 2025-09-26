package com.delight.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Standard error response")
public class ErrorResponse {
    @Schema(description = "Timestamp of the error in ISO-8601", example = "2025-09-25T12:34:56.789Z")
    private OffsetDateTime timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "HTTP error phrase", example = "Bad Request")
    private String error;

    @Schema(description = "Human readable message (localized)", example = "Validation failed")
    private String message;

    @Schema(description = "Machine-readable error code/message key", example = "error.validation")
    private String code;

    @Schema(description = "Request path", example = "/account/signup")
    private String path;
}

