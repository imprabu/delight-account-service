package com.delight.account.dto;

import com.delight.account.model.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Account details including organization, product, and current user context")
public class AccountInfoResponse {
    @Schema(description = "Unique account identifier", example = "12345")
    private Long id;
    @Schema(description = "Account domain used for routing and access", example = "acme-corp.delight.com")
    private String domain;
    @Schema(description = "Subscription plan code for the account (fetched from subscription)", example = "ENTERPRISE")
    private String plan;
    @Schema(description = "Current status of the account", example = "ACTIVE")
    private AccountStatus status;
    @Schema(description = "Industry type or business category", example = "Software")
    private String industryType;
    @Schema(description = "Company or organization name", example = "Acme Corp")
    private String companyName;
    @Schema(description = "ID of the currently authenticated user", example = "67890")
    private Long currentUserId;
    @Schema(description = "Role of the current user in this account", example = "ADMIN")
    private String currentUserRole;
    @Schema(description = "ID of the organization that owns this account", example = "54321")
    private Long organizationId;
    @Schema(description = "Name of the organization", example = "Acme Organization")
    private String organizationName;
    @Schema(description = "Primary domain for the organization", example = "acme.com")
    private String organizationPrimaryDomain;
    @Schema(description = "Product code identifier", example = "CHAT")
    private String productCode;
    @Schema(description = "Human-readable product name", example = "Human Resource Management System")
    private String productName;
}
