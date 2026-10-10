package com.delight.account.controller;

import com.delight.account.dto.InviteMemberRequest;
import com.delight.account.dto.MemberResponse;
import com.delight.account.dto.UpdateMemberRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Role;
import com.delight.account.service.MembershipService;
import com.delight.account.util.LoggingUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping({"/accounts/members", "/api/accounts/members", "/channel/accounts/members"})
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
        @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        LoggingUtils.logMethodEntry(logger, "listMembers", "authorization", authorization);
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        LoggingUtils.logBusinessOperation(logger, "GET_REQUEST_ATTRIBUTES", "SUCCESS");
        Object accountIdAttr = attr.getRequest().getAttribute("accountId");
        LoggingUtils.logBusinessOperation(logger, "EXTRACT_REQUEST_ATTRIBUTES", "SUCCESS", 
            "accountIdAttr", accountIdAttr);
        if (accountIdAttr == null) {
            LoggingUtils.logSecurityEvent(logger, "MISSING_ACCOUNT_ID", "FAILED", "reason", "accountId attribute not found");
            throw new ApiException("error.account.notfound");
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
        @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            Object accountIdAttr = attr.getRequest().getAttribute("accountId");
            Object roleAttr = attr.getRequest().getAttribute("role");
            
            if (accountIdAttr == null) {
                logger.error("=== updateMember: accountId attribute is null ===");
                throw new ApiException("error.account.notfound");
            }
            if (roleAttr == null) {
                logger.error("=== updateMember: role attribute is null ===");
                throw new ApiException("error.authorization");
            }
            Long accountId = (Long) accountIdAttr;
            // Role codes now match the enum: OWNER, ADMIN, AGENT
            String roleCode = roleAttr.toString();
            Role actorRole;
            try {
                actorRole = Role.fromCode(roleCode);
                logger.info("=== updateMember: Using role enum: {} ===", actorRole);
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown role code: {}, defaulting to USER", roleCode);
                actorRole = Role.USER;
            }
            //String productCode = productCodeAttr.toString();
            logger.info("=== updateMember: Mapped role: {}, productCode: {} ===", actorRole);
            LoggingUtils.logMethodEntry(logger, "updateMember", 
                "accountId", accountId, "userId", userId, "actorRole", actorRole, "productCode");
            LoggingUtils.logBusinessOperation(logger, "UPDATE_MEMBER_REQUEST", "PROCESSING", 
                "requestRoleId", (request.getRoleId() == null ? null : request.getRoleId().toString()), 
                "requestStatus", (request.getStatus() == null ? null : request.getStatus().name()));
            MemberResponse response = membershipService.updateMember(accountId, userId, request, actorRole);
            LoggingUtils.logMethodExit(logger, "updateMember", response);
            return ResponseEntity
                .ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(response);
        } catch (ApiException e) {
            logger.error("=== updateMember: ApiException: {} ===", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("=== updateMember: Unexpected exception ===", e);
            throw new ApiException("error.internal", "An unexpected error occurred: " + e.getMessage());
        }
    }

    @PostMapping(value = "/invite", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE, produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Invite team member",
        description = "Invites a new user to join the account with the specified role. Creates user if they don't exist. Requires user.invite privilege.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = InviteMemberRequest.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "InviteMemberRequest",
                    value = "{\n  \"emailAddress\": \"newmember@acme.com\",\n  \"roleId\": 1,\n  \"firstName\": \"John\",\n  \"lastName\": \"Doe\"\n}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Member invited successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = MemberResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Bad request - Invalid email or role",
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
                description = "Forbidden - Insufficient permissions to invite member",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Conflict - User is already a member of this account",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @SecurityRequirement(name = "cookieAuth")
    public ResponseEntity<MemberResponse> inviteMember(
        @Valid @RequestBody InviteMemberRequest request,
        @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            Object accountIdAttr = attr.getRequest().getAttribute("accountId");
            if (accountIdAttr == null) {
                throw new ApiException("error.account.notfound");
            }
            Long accountId = (Long) accountIdAttr;
            LoggingUtils.logMethodEntry(logger, "inviteMember", "accountId", accountId, "email", request.getEmailAddress());
            MemberResponse response = membershipService.inviteMember(accountId, request);
            LoggingUtils.logMethodExit(logger, "inviteMember", response);
            return ResponseEntity
                .ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(response);
        } catch (ApiException e) {
            logger.error("=== inviteMember: ApiException: {} ===", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("=== inviteMember: Unexpected exception ===", e);
            throw new ApiException("error.internal", "An unexpected error occurred: " + e.getMessage());
        }
    }

    @DeleteMapping(value = "/{userId}", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Remove team member",
        description = "Removes a member from the account by setting their status to REMOVED. Cannot remove the last ACCOUNT_ADMIN. Requires user.update privilege.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "204",
                description = "Member removed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Bad request - Cannot remove last organization administrator",
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
                description = "Forbidden - Insufficient permissions to remove member",
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
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @SecurityRequirement(name = "cookieAuth")
    public ResponseEntity<Void> removeMember(
        @PathVariable("userId") Long userId,
        @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            Object accountIdAttr = attr.getRequest().getAttribute("accountId");
            if (accountIdAttr == null) {
                throw new ApiException("error.account.notfound");
            }
            Long accountId = (Long) accountIdAttr;
            LoggingUtils.logMethodEntry(logger, "removeMember", "accountId", accountId, "userId", userId);
            membershipService.removeMember(accountId, userId);
            LoggingUtils.logMethodExit(logger, "removeMember");
            return ResponseEntity.noContent().build();
        } catch (ApiException e) {
            logger.error("=== removeMember: ApiException: {} ===", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("=== removeMember: Unexpected exception ===", e);
            throw new ApiException("error.internal", "An unexpected error occurred: " + e.getMessage());
        }
    }
}
