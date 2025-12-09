package com.delight.account.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for privilege check endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivilegeCheckRequest {
    @NotNull(message = "userId is required")
    private Long userId;
    
    @NotNull(message = "accountId is required")
    private Long accountId;
    
    @NotNull(message = "privilegeCode is required")
    private String privilegeCode;
}

