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

@RestController
@RequestMapping("/account")
@Tag(name = "Account")
public class AccountInfoController {

    private final AccountService accountService;

    public AccountInfoController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/info")
    @Operation(
        summary = "Get account info",
        description = "Returns account, organization and product details with current user role",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Account info",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        value = "{\n  \"id\": 1,\n  \"domain\": \"acmecorp\",\n  \"plan\": \"TRIAL\",\n  \"status\": \"ACTIVE\",\n  \"industryType\": \"Software\",\n  \"email\": \"owner@acme.com\",\n  \"companyName\": \"Acme Corp\",\n  \"currentUserId\": 1,\n  \"currentUserRole\": \"OWNER\",\n  \"organizationId\": 1,\n  \"organizationName\": \"Acme Corp\",\n  \"organizationPrimaryDomain\": \"acmecorp\",\n  \"productCode\": \"HRMS\",\n  \"productName\": \"HRMS\"\n}"
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Not found",
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
    public ResponseEntity<AccountInfoResponse> getAccountInfo(
        @RequestHeader(name = "Authorization") String authorization
    ) {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        Object accountIdAttr = attr.getRequest().getAttribute("accountId");
        Object userIdAttr = attr.getRequest().getAttribute("userId");
        if (accountIdAttr == null) {
            throw new ApiException("error.account.notfound");
        }
        if (userIdAttr == null) {
            throw new ApiException("error.user.notfound");
        }
        Long accountId = (Long) accountIdAttr;
        Long userId = (Long) userIdAttr;

        AccountInfoResponse body = accountService.getAccountInfo(accountId, userId);
        return ResponseEntity.ok(body);
    }
}
