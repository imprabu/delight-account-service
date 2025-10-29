package com.delight.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "User-specific membership data for an account")
public class UserMembershipCacheResponse {
    @Schema(description = "Unique identifier for the user", example = "67890")
    private Long userId;
    @Schema(description = "User's role within the account", example = "ADMIN", allowableValues = {"OWNER", "ADMIN", "USER"})
    private String role;
}