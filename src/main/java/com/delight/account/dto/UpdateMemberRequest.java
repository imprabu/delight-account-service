package com.delight.account.dto;

import com.delight.account.model.MembershipStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Update member role and/or status")
public class UpdateMemberRequest {
    @Schema(description = "Role ID to assign to the member (optional)", example = "1")
    private Long roleId;                // optional
    @Schema(description = "New status to set for the member (optional)", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "PENDING"})
    private MembershipStatus status;  // optional
}
