package com.delight.account.dto;

import com.delight.account.model.MembershipStatus;
import com.delight.account.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "User membership in an account")
public class MemberResponse {
    @Schema(description = "User id")
    private Long userId;
    @Schema(description = "User email")
    private String emailAddress;
    @Schema(description = "User first name")
    private String firstName;
    @Schema(description = "User last name")
    private String lastName;
    @Schema(description = "Membership role")
    private Role role;
    @Schema(description = "Membership status")
    private MembershipStatus status;
}
