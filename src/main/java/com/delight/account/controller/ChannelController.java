package com.delight.account.controller;

import com.delight.account.dto.AccountInfoResponse;
import com.delight.account.dto.SessionResponse;
import com.delight.account.dto.SessionValidationRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.service.AccountService;
import com.delight.account.service.job.JobQueueService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/channel")
@Tag(name = "Channel Management", description = "Channel-authenticated endpoints for external systems")
public class ChannelController {

    private final AccountService accountService;
    private final JobQueueService jobQueueService;

    public ChannelController(AccountService accountService, JobQueueService jobQueueService) {
        this.accountService = accountService;
        this.jobQueueService = jobQueueService;
    }

    @GetMapping(value = {"/allAccounts", "/allAccount"}, produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get all accounts information (Channel)",
        description = "Returns a list of all accounts with their organization and product details. Channel authentication required. Does not include user-specific information. Supports both /channel/allAccounts and /channel/allAccount paths.",
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
                        value = "[\n  {\n    \"id\": 1,\n    \"domain\": \"acmecorp\",\n    \"plan\": \"TRIAL\",\n    \"status\": \"ACTIVE\",\n    \"industryType\": \"Software\",\n    \"email\": \"owner@acme.com\",\n    \"companyName\": \"Acme Corp\",\n    \"currentUserId\": null,\n    \"currentUserRole\": null,\n    \"organizationId\": 1,\n    \"organizationName\": \"Acme Corp\",\n    \"organizationPrimaryDomain\": \"acmecorp\",\n    \"productCode\": \"CHAT\",\n    \"productName\": \"CHAT\"\n  }\n]"
                    )
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
    public ResponseEntity<List<AccountInfoResponse>> getAllAccounts(
        @RequestHeader(name = "Authorization") String authorization,
        HttpServletRequest request
    ) {
        // Ensure this endpoint is only accessible via channel authentication
        String requestURI = request.getRequestURI();
        Object channelAuthAttr = request.getAttribute("channelAuth");
        
        // Reject if not accessed via /channel/ path
        if (!requestURI.contains("/channel/")) {
            throw new ApiException("error.authorization", "This endpoint requires channel authentication. Access via /channel/allAccounts or /channel/allAccount with Channel JWT token.");
        }
        
        // Verify channel authentication attribute is set (set by ChannelAuthFilter)
        if (channelAuthAttr == null || !Boolean.TRUE.equals(channelAuthAttr)) {
            throw new ApiException("error.authorization", "Invalid channel authentication. This endpoint only accepts Channel JWT tokens, not regular user JWT tokens.");
        }
        
        List<AccountInfoResponse> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    @RequestMapping(value = "/validateSession", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST}, produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Validate channel session",
        description = "Validates the current channel session using the channel JWT processed by the ChannelAuthFilter. Supports both GET and POST methods.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            description = "Optional request body for POST requests; ignored for channel validation.",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SessionValidationRequest.class)
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Channel session validation successful",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SessionResponse.class)
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
    public ResponseEntity<SessionResponse> validateChannelSession(
        HttpServletRequest request,
        @RequestBody(required = false) @Valid SessionValidationRequest validationRequest
    ) {
        Object accountIdObj = request.getAttribute("accountId");
        String accountId = accountIdObj != null ? String.valueOf(accountIdObj) : null;

        if (accountId == null) {
            throw new ApiException("error.session.invalid");
        }

        SessionResponse response = SessionResponse.builder()
                .accountId(Long.valueOf(accountId))
                .isActive(true)
                .sessionId("channel-" + accountId + "-" + System.currentTimeMillis())
                .privileges(java.util.Collections.emptyList()) // Channel sessions don't have user privileges
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/sync/accounts", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Trigger bulk account sync",
        description = "Triggers a batch job that iterates over all active accounts and publishes account_modified events to the messaging service. Channel authentication required.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Bulk account sync job triggered successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                        implementation = BulkAccountSyncResponse.class
                    )
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
    public ResponseEntity<BulkAccountSyncResponse> triggerBulkAccountSync(
        @RequestHeader(name = "Authorization") String authorization
    ) {
        // Trigger the bulk account sync job
        // Use a unique job ID based on timestamp
        Long jobId = System.currentTimeMillis();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("triggeredBy", "channel_api");
        metadata.put("triggeredAt", java.time.OffsetDateTime.now().toString());
        
        // Use 0L as placeholder accountId since bulk sync processes all accounts
        // The worker doesn't use accountId for bulk operations
        jobQueueService.enqueueJob(
            com.delight.account.domain.BatchJobType.BULK_ACCOUNT_SYNC,
            jobId,
            0L, // Placeholder - bulk sync doesn't target a specific account
            metadata
        );
        
        BulkAccountSyncResponse response = BulkAccountSyncResponse.builder()
            .jobId(jobId)
            .status("triggered")
            .message("Bulk account sync job has been enqueued. It will process all active accounts and publish account_modified events to the messaging service.")
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Response DTO for bulk account sync operation.
     */
    @lombok.Builder
    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BulkAccountSyncResponse {
        private Long jobId;
        private String status;
        private String message;
    }
}