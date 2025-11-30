package com.delight.account.controller;

import com.delight.account.dto.AccountInfoResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequestMapping({"/account", "/api/account", "/channel/account"})
@Tag(name = "Account Management", description = "Account information and organization details management")
public class AccountInfoController {

    private final AccountService accountService;

    public AccountInfoController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping(value = "/info", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Tag(name = "Channel APIs", description = "Endpoints accessible via ChannelJWT under '/channel/**'")
    @Operation(
        summary = "Get account information",
        description = "Returns comprehensive account, organization and product details with current user role and permissions. Requires valid authentication.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Account information retrieved successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = AccountInfoResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "AccountInfoResponse",
                        value = "{\n  \"id\": 1,\n  \"domain\": \"acmecorp\",\n  \"plan\": \"TRIAL\",\n  \"status\": \"ACTIVE\",\n  \"industryType\": \"Software\",\n  \"email\": \"owner@acme.com\",\n  \"companyName\": \"Acme Corp\",\n  \"currentUserId\": 1,\n  \"currentUserRole\": \"OWNER\",\n  \"organizationId\": 1,\n  \"organizationName\": \"Acme Corp\",\n  \"organizationPrimaryDomain\": \"acmecorp\",\n  \"productCode\": \"HRMS\",\n  \"productName\": \"HRMS\"\n}"
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Invalid or expired authentication",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Account or user not found",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @SecurityRequirement(name = "cookieAuth")
    public ResponseEntity<AccountInfoResponse> getAccountInfo(
        @RequestHeader(name = "Authorization") String authorization
    ) {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        Object channelAuthAttr = attr.getRequest().getAttribute("channelAuth");
        Object accountIdAttr = attr.getRequest().getAttribute("accountId");

        if (attr.getRequest().getRequestURI().startsWith("/channel/") || Boolean.TRUE.equals(channelAuthAttr)) {
            if (accountIdAttr == null) {
                throw new ApiException("error.account.notfound");
            }
            Long accountId = (Long) accountIdAttr;
            AccountInfoResponse body = accountService.getAccountInfoForChannel(accountId);
            return ResponseEntity.ok(body);
        }

        if (accountIdAttr == null) {
            throw new ApiException("error.account.notfound");
        }
        Object userIdAttr = attr.getRequest().getAttribute("userId");
        if (userIdAttr == null) {
            throw new ApiException("error.user.notfound");
        }
        Long accountId = (Long) accountIdAttr;
        Long userId = (Long) userIdAttr;

        AccountInfoResponse body = accountService.getAccountInfo(accountId, userId);
        return ResponseEntity.ok(body);
    }

    @GetMapping(value = "/allAccount", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Tag(name = "Channel APIs", description = "Endpoints accessible via ChannelJWT under '/channel/**'")
    @Operation(
        summary = "Get all accounts information",
        description = "Returns a list of all accounts with their organization and product details. Does not include user-specific information. Requires valid authentication.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "All accounts information retrieved successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                        type = "array",
                        implementation = AccountInfoResponse.class
                    ),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "AllAccountsResponse",
                        value = "[\n  {\n    \"id\": 1,\n    \"domain\": \"acmecorp\",\n    \"plan\": \"TRIAL\",\n    \"status\": \"ACTIVE\",\n    \"industryType\": \"Software\",\n    \"email\": \"owner@acme.com\",\n    \"companyName\": \"Acme Corp\",\n    \"currentUserId\": null,\n    \"currentUserRole\": null,\n    \"organizationId\": 1,\n    \"organizationName\": \"Acme Corp\",\n    \"organizationPrimaryDomain\": \"acmecorp\",\n    \"productCode\": \"HRMS\",\n    \"productName\": \"HRMS\"\n  },\n  {\n    \"id\": 2,\n    \"domain\": \"techcorp\",\n    \"plan\": \"PREMIUM\",\n    \"status\": \"ACTIVE\",\n    \"industryType\": \"Technology\",\n    \"email\": \"admin@techcorp.com\",\n    \"companyName\": \"Tech Corp\",\n    \"currentUserId\": null,\n    \"currentUserRole\": null,\n    \"organizationId\": 2,\n    \"organizationName\": \"Tech Corp\",\n    \"organizationPrimaryDomain\": \"techcorp\",\n    \"productCode\": \"CRM\",\n    \"productName\": \"CRM\"\n  }\n]"
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Invalid or expired authentication",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @SecurityRequirement(name = "cookieAuth")
    public ResponseEntity<List<AccountInfoResponse>> getAllAccounts(
        @RequestHeader(name = "Authorization") String authorization
    ) {
        List<AccountInfoResponse> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }
}
