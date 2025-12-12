package com.delight.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for rate limit check operations.
 * Used by microservices to check and increment rate limit counters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitCheckRequest {
    
    /**
     * Endpoint identifier (e.g., "api", "chat", "upload")
     */
    @NotBlank(message = "Endpoint is required")
    private String endpoint;
    
    /**
     * Client identifier (e.g., IP address, user ID)
     */
    @NotBlank(message = "Client key is required")
    private String clientKey;
}

