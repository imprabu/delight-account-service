package com.delight.account.controller;

import com.delight.account.dto.MemberResponse;
import com.delight.account.dto.UpdateMemberRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Role;
import com.delight.account.service.MembershipService;
import com.delight.account.util.LoggingUtils;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@RequestMapping({"/account/members", "/api/account/members", "/channel/account/members"})
@Tag(name = "User Management", description = "User membership management including role assignments and status updates")
public class MembershipController {

    private static final Logger logger = LoggerFactory.getLogger(MembershipController.class);
    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
        LoggingUtils.logMethodEntry(logger, "MembershipController");
    }

    @GetMapping(produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Tag(name = "Channel APIs", description = "Endpoints accessible via ChannelJWT under '/channel/**'")
    @Operation(
        summary = "List organization members",
        description = "Retrieves a list of all user memberships for the current account including their roles and status. Requires valid authentication.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Organization members retrieved successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                        type = "array",
                        implementation = MemberResponse.class
                    ),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "MembersList",
                        value = "[{\n  \"userId\": 1,\n  \"emailAddress\": \"owner@acme.com\",\n  \"firstName\": \"Alice\",\n  \"lastName\": \"Smith\",\n  \"role\": \"OWNER\",\n  \"status\": \"ACTIVE\"\n}]"
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
                responseCode = "403",
                description = "Forbidden - Insufficient permissions",
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
    @SecurityRequirement(name = "cookieAuth")
    public ResponseEntity<List<MemberResponse>> listMembers(
        @RequestHeader(name = "Authorization") String authorization
    ) {
        LoggingUtils.logMethodEntry(logger, "listMembers", "authorization", authorization);
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        LoggingUtils.logBusinessOperation(logger, "GET_REQUEST_ATTRIBUTES", "SUCCESS");
        Object accountIdAttr = attr.getRequest().getAttribute("accountId");
        Object productCodeAttr = attr.getRequest().getAttribute("productCode");
        Object channelAuthAttr = attr.getRequest().getAttribute("channelAuth");
        LoggingUtils.logBusinessOperation(logger, "EXTRACT_REQUEST_ATTRIBUTES", "SUCCESS", 
            "accountIdAttr", accountIdAttr, "productCodeAttr", productCodeAttr, "channelAuth", channelAuthAttr);
        if (accountIdAttr == null) {
            LoggingUtils.logSecurityEvent(logger, "MISSING_ACCOUNT_ID", "FAILED", "reason", "accountId attribute not found");
            throw new ApiException("error.account.notfound");
        }
        // For channel-authenticated requests, productCode is not required
        if (!(attr.getRequest().getRequestURI().startsWith("/channel/") || Boolean.TRUE.equals(channelAuthAttr))) {
            if (productCodeAttr == null) {
                LoggingUtils.logSecurityEvent(logger, "MISSING_PRODUCT_CODE", "FAILED", "reason", "productCode attribute not found");
                throw new ApiException("error.authorization");
            }
        }
        Long accountId = (Long) accountIdAttr;
        LoggingUtils.logBusinessOperation(logger, "LIST_MEMBERS_REQUEST", "PROCESSING", 
            "accountId", accountId);
        List<MemberResponse> members = membershipService.listMembers(accountId);
        LoggingUtils.logMethodExit(logger, "listMembers", members);
        return ResponseEntity
            .ok()
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(members);
    }

    @PatchMapping(value = "/{userId}", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE, produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Update organization member",
        description = "Updates a user's role and/or status in the current account. Requires ADMIN or OWNER permissions.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UpdateMemberRequest.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "UpdateMemberRequest",
                    value = "{\n  \"role\": \"ADMIN\",\n  \"status\": \"ACTIVE\"\n}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Member updated successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = MemberResponse.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "UpdatedMemberResponse",
                        value = "{\n  \"userId\": 2,\n  \"emailAddress\": \"user@acme.com\",\n  \"firstName\": \"Bob\",\n  \"lastName\": \"Lee\",\n  \"role\": \"ADMIN\",\n  \"status\": \"ACTIVE\"\n}"
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Bad request - Invalid role or status values",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
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
                responseCode = "403",
                description = "Forbidden - Insufficient permissions to update member",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Member not found",
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
    public ResponseEntity<MemberResponse> updateMember(
        @PathVariable("userId") Long userId,
        @RequestBody UpdateMemberRequest request,
        @RequestHeader(name = "Authorization") String authorization
    ) {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        Object accountIdAttr = attr.getRequest().getAttribute("accountId");
        Object roleAttr = attr.getRequest().getAttribute("role");
        Object productCodeAttr = attr.getRequest().getAttribute("productCode");
        if (accountIdAttr == null) {
            throw new ApiException("error.account.notfound");
        }
        if (roleAttr == null) {
            throw new ApiException("error.authorization");
        }
        if (productCodeAttr == null) {
            throw new ApiException("error.authorization");
        }
        Long accountId = (Long) accountIdAttr;
        Role actorRole = Role.valueOf(roleAttr.toString());
        String productCode = productCodeAttr.toString();
        LoggingUtils.logMethodEntry(logger, "updateMember", 
            "accountId", accountId, "userId", userId, "actorRole", actorRole, "productCode", productCode);
        LoggingUtils.logBusinessOperation(logger, "UPDATE_MEMBER_REQUEST", "PROCESSING", 
            "requestRole", (request.getRole() == null ? null : request.getRole().name()), 
            "requestStatus", (request.getStatus() == null ? null : request.getStatus().name()));
        MemberResponse response = membershipService.updateMember(accountId, userId, request, actorRole, productCode);
        LoggingUtils.logMethodExit(logger, "updateMember", response);
        return ResponseEntity
            .ok()
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(response);
    }
}
