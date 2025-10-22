package com.delight.account.dto;

import com.delight.account.model.AccountStatus;
import com.delight.account.model.PlanType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Account-level data that can be cached independently of user context")
public class AccountInfoCacheResponse {
    @Schema(description = "Account id")
    private Long id;
    @Schema(description = "Account domain (used for routing)")
    private String domain;
    @Schema(description = "Plan type for the account")
    private PlanType plan;
    @Schema(description = "Account status")
    private AccountStatus status;
    @Schema(description = "Industry type for the organization/account")
    private String industryType;
    @Schema(description = "Contact email for the account")
    private String email;
    @Schema(description = "Company name")
    private String companyName;
    @Schema(description = "Organization id owning the account")
    private Long organizationId;
    @Schema(description = "Organization name")
    private String organizationName;
    @Schema(description = "Primary domain for the organization")
    private String organizationPrimaryDomain;
    @Schema(description = "Product code for this account, e.g., HRMS")
    private String productCode;
    @Schema(description = "Product display name")
    private String productName;
}