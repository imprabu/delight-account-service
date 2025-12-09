package com.delight.account.dto;

import com.delight.account.model.MembershipStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
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
    @Schema(description = "Role ID", example = "1")
    private Long roleId;
    @Schema(description = "Role code", example = "ORG_ADMIN")
    private String roleCode;
    @Schema(description = "Role name", example = "Org Admin")
    private String roleName;
    @Schema(description = "Current membership status", example = "ACTIVE", allowableValues = {"ACTIVE", "INVITED", "SUSPENDED", "REMOVED"})
    private MembershipStatus status;
    @Schema(description = "Last active timestamp from session", example = "2024-01-15T10:30:00Z")
    private OffsetDateTime lastActiveAt;
    @Schema(description = "Two-factor authentication enabled status", example = "false")
    private Boolean twoFactorEnabled;
    @Schema(description = "Timestamp when member was invited", example = "2024-01-10T08:00:00Z")
    private OffsetDateTime invitedAt;
    @Schema(description = "User ID of the person who invited this member", example = "123")
    private Long invitedBy;
}
