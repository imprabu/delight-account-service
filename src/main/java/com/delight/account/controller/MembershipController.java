package com.delight.account.controller;

import com.delight.account.dto.MemberResponse;
import com.delight.account.dto.UpdateMemberRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Role;
import com.delight.account.service.MembershipService;
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

@RestController
@RequestMapping("/account/members")
@Tag(name = "Memberships")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping
    @Operation(
        summary = "List members",
        description = "Lists user memberships for the current account",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Members",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        value = "[{\n  \"userId\": 1,\n  \"emailAddress\": \"owner@acme.com\",\n  \"firstName\": \"Alice\",\n  \"lastName\": \"Smith\",\n  \"role\": \"OWNER\",\n  \"status\": \"ACTIVE\"\n}]"
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
                responseCode = "403",
                description = "Forbidden",
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
    public ResponseEntity<List<MemberResponse>> listMembers(
        @RequestHeader(name = "Authorization") String authorization
    ) {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        Object accountIdAttr = attr.getRequest().getAttribute("accountId");
        Object productCodeAttr = attr.getRequest().getAttribute("productCode");
        if (accountIdAttr == null) {
            throw new ApiException("error.account.notfound");
        }
        if (productCodeAttr == null) {
            throw new ApiException("error.authorization");
        }
        Long accountId = (Long) accountIdAttr;
        String productCode = productCodeAttr.toString();
        List<MemberResponse> members = membershipService.listMembers(accountId, productCode);
        return ResponseEntity.ok(members);
    }

    @PatchMapping("/{userId}")
    @Operation(
        summary = "Update member",
        description = "Updates a user's role and/or status in the current account. Requires ADMIN or OWNER.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\n  \"role\": \"ADMIN\",\n  \"status\": \"ACTIVE\"\n}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Updated member",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        value = "{\n  \"userId\": 2,\n  \"emailAddress\": \"user@acme.com\",\n  \"firstName\": \"Bob\",\n  \"lastName\": \"Lee\",\n  \"role\": \"ADMIN\",\n  \"status\": \"ACTIVE\"\n}"
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Bad request",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
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
                responseCode = "403",
                description = "Forbidden",
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
        MemberResponse response = membershipService.updateMember(accountId, userId, request, actorRole, productCode);
        return ResponseEntity.ok(response);
    }
}
