package com.delight.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to invite a new team member")
public class InviteMemberRequest {
    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Email address of the member to invite", example = "newmember@acme.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String emailAddress;

    @NotNull(message = "Role ID is required")
    @Schema(description = "Role ID to assign to the member", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long roleId;

    @Schema(description = "First name of the member (optional, will be extracted from email if not provided)", example = "John")
    private String firstName;

    @Schema(description = "Last name of the member (optional)", example = "Doe")
    private String lastName;
}

