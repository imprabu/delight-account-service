package com.delight.account.controller;

import com.delight.account.dto.PrivilegeCheckRequest;
import com.delight.account.dto.PrivilegeCheckResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.service.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for authorization/privilege check endpoints.
 * These endpoints are accessible via Channel JWT authentication under '/channel/**' context.
 */
@RestController
@RequestMapping("/channel/accounts/authorize")
@Tag(name = "Channel APIs", description = "Endpoints accessible via ChannelJWT under '/channel/**'")
public class AuthorizationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationController.class);
    
    private final AuthorizationService authorizationService;
    
    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }
    
    @PostMapping("/check")
    @Operation(
        summary = "Check user privilege",
        description = "Checks if a user has a specific privilege in an account. Channel authentication required. This endpoint is used by microservices to perform authorization checks.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Privilege check request with userId, accountId, and privilegeCode",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = PrivilegeCheckRequest.class)
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Privilege check completed successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = PrivilegeCheckResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Bad request - Invalid input",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Invalid or expired channel token",
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
    public ResponseEntity<PrivilegeCheckResponse> checkPrivilege(
            @RequestBody @Valid PrivilegeCheckRequest request,
            HttpServletRequest httpRequest) {
        
        // Verify this is a channel-authenticated request
        String requestURI = httpRequest.getRequestURI();
        Object channelAuthAttr = httpRequest.getAttribute("channelAuth");
        
        if (!requestURI.contains("/channel/")) {
            throw new ApiException("error.authorization", 
                    "This endpoint requires channel authentication. Access via /channel/accounts/authorize/check with Channel JWT token.");
        }
        
        if (channelAuthAttr == null || !Boolean.TRUE.equals(channelAuthAttr)) {
            throw new ApiException("error.authorization", 
                    "Invalid channel authentication. This endpoint only accepts Channel JWT tokens, not regular user JWT tokens.");
        }
        
        logger.debug("🔐 [AUTHORIZATION] Checking privilege: userId={}, accountId={}, privilegeCode={}", 
                request.getUserId(), request.getAccountId(), request.getPrivilegeCode());
        
        // Check privilege using AuthorizationService
        // Note: AuthorizationService.isChannelRequest() will return true for channel requests,
        // and hasPrivilege() will skip the check and return true automatically
        boolean authorized = authorizationService.hasPrivilege(
                request.getUserId(),
                request.getAccountId(),
                request.getPrivilegeCode()
        );
        
        PrivilegeCheckResponse response = PrivilegeCheckResponse.builder()
                .authorized(authorized)
                .userId(request.getUserId())
                .accountId(request.getAccountId())
                .privilegeCode(request.getPrivilegeCode())
                .message(authorized ? "User has the requested privilege" : "User does not have the requested privilege")
                .build();
        
        logger.info("✅ [AUTHORIZATION] Privilege check result: userId={}, accountId={}, privilegeCode={}, authorized={}", 
                request.getUserId(), request.getAccountId(), request.getPrivilegeCode(), authorized);
        
        return ResponseEntity.ok(response);
    }
}

