package com.delight.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for privilege check endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivilegeCheckResponse {
    private boolean authorized;
    private Long userId;
    private Long accountId;
    private String privilegeCode;
    private String message;
}





