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
    @Schema(description = "User id")
    private Long userId;
    @Schema(description = "Role of user in this account")
    private String role;
}