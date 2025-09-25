package com.delight.account.dto;

import com.delight.account.model.MembershipStatus;
import com.delight.account.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Update member role and/or status")
public class UpdateMemberRequest {
    @Schema(description = "New role to set", example = "ADMIN")
    private Role role;                // optional
    @Schema(description = "New status to set", example = "ACTIVE")
    private MembershipStatus status;  // optional
}
