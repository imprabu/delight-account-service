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
    @Schema(description = "Unique identifier for the member", example = "12345")
    private Long userId;
    @Schema(description = "Member's email address", example = "john.doe@acme.com")
    private String emailAddress;
    @Schema(description = "Member's first name", example = "John")
    private String firstName;
    @Schema(description = "Member's last name", example = "Doe")
    private String lastName;
    @Schema(description = "Member's role within the organization", example = "ADMIN", allowableValues = {"OWNER", "ADMIN", "USER"})
    private Role role;
    @Schema(description = "Current membership status", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "PENDING"})
    private MembershipStatus status;
}
