package com.delight.account.controller;

import com.delight.account.dto.RoleResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.service.RoleService;
import com.delight.account.util.LoggingUtils;
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping({"/accounts/roles", "/api/accounts/roles", "/channel/accounts/roles"})
@Tag(name = "Role Management", description = "Role management including listing available roles")
public class RoleController {

    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
        LoggingUtils.logMethodEntry(logger, "RoleController");
    }

    @GetMapping(produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "List account roles",
        description = "Retrieves a list of all active roles (system + custom) for the current account. Used for role selection in invite member and member management.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Roles retrieved successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                        type = "array",
                        implementation = RoleResponse.class
                    ),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "RolesList",
                        value = "[{\n  \"id\": 1,\n  \"code\": \"ORG_ADMIN\",\n  \"name\": \"Org Admin\",\n  \"description\": \"Organization Administrator with full privileges\",\n  \"roleType\": \"SYSTEM\",\n  \"isCustom\": false,\n  \"hierarchyLevel\": 1\n}]"
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
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    @SecurityRequirement(name = "cookieAuth")
    public ResponseEntity<List<RoleResponse>> listRoles(
        @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            Object accountIdAttr = attr.getRequest().getAttribute("accountId");
            if (accountIdAttr == null) {
                throw new ApiException("error.account.notfound");
            }
            Long accountId = (Long) accountIdAttr;
            LoggingUtils.logMethodEntry(logger, "listRoles", "accountId", accountId);
            List<RoleResponse> roles = roleService.listRoles(accountId);
            LoggingUtils.logMethodExit(logger, "listRoles", roles);
            return ResponseEntity
                .ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(roles);
        } catch (ApiException e) {
            logger.error("=== listRoles: ApiException: {} ===", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("=== listRoles: Unexpected exception ===", e);
            throw new ApiException("error.internal", "An unexpected error occurred: " + e.getMessage());
        }
    }
}


