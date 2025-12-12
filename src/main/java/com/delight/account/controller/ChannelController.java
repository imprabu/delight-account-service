package com.delight.account.controller;

import com.delight.account.dto.AccountInfoResponse;
import com.delight.account.dto.AccountRateLimitResponse;
import com.delight.account.dto.FeatureInfo;
import com.delight.account.dto.RateLimitCheckRequest;
import com.delight.account.dto.RateLimitCheckResponse;
import com.delight.account.dto.SessionResponse;
import com.delight.account.dto.SessionValidationRequest;
import com.delight.account.dto.SubscriptionInfo;
import com.delight.account.dto.UsageCheckResponse;
import com.delight.account.dto.UsageDecrementRequest;
import com.delight.account.dto.UsageIncrementRequest;
import com.delight.account.dto.UsageSetRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.SubscriptionFeature;
import com.delight.account.repository.SubscriptionFeatureRepository;
import com.delight.account.service.AccountService;
import com.delight.account.service.FeatureService;
import com.delight.account.service.RateLimitingService;
import com.delight.account.service.SubscriptionService;
import com.delight.account.service.UsageTrackingService;
import com.delight.account.service.job.JobQueueService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/channel")
@Tag(name = "Channel Management", description = "Channel-authenticated endpoints for external systems")
public class ChannelController {

    private final AccountService accountService;
    private final JobQueueService jobQueueService;
    private final RateLimitingService rateLimitingService;
    private final SubscriptionService subscriptionService;
    private final FeatureService featureService;
    private final UsageTrackingService usageTrackingService;
    private final SubscriptionFeatureRepository subscriptionFeatureRepository;

    public ChannelController(AccountService accountService, JobQueueService jobQueueService,
                             RateLimitingService rateLimitingService,
                             SubscriptionService subscriptionService,
                             FeatureService featureService,
                             UsageTrackingService usageTrackingService,
                             SubscriptionFeatureRepository subscriptionFeatureRepository) {
        this.accountService = accountService;
        this.jobQueueService = jobQueueService;
        this.rateLimitingService = rateLimitingService;
        this.subscriptionService = subscriptionService;
        this.featureService = featureService;
        this.usageTrackingService = usageTrackingService;
        this.subscriptionFeatureRepository = subscriptionFeatureRepository;
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
    
    @GetMapping(value = "/accounts/{accountId}/rateLimit", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get account rate limiting configuration (Channel)",
        description = "Returns rate limiting configuration for a specific account, including default settings and any account-specific overrides. This endpoint is the source of truth for rate limiting configuration used by microservices. Channel authentication required.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Account rate limiting configuration retrieved successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                        implementation = AccountRateLimitResponse.class
                    ),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "AccountRateLimitResponse",
                        value = "{\n  \"accountId\": 123456789,\n  \"defaultConfig\": {\n    \"maxRequests\": 600,\n    \"windowSeconds\": 60\n  },\n  \"endpointOverrides\": {\n    \"api\": {\n      \"maxRequests\": 1000,\n      \"windowSeconds\": 60\n    },\n    \"upload\": {\n      \"maxRequests\": 100,\n      \"windowSeconds\": 300\n    }\n  }\n}"
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid account ID",
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
                responseCode = "404",
                description = "Not Found - Account not found",
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
    public ResponseEntity<AccountRateLimitResponse> getAccountRateLimit(
        @PathVariable Long accountId,
        @RequestHeader(name = "Authorization") String authorization,
        HttpServletRequest request
    ) {
        // Ensure this endpoint is only accessible via channel authentication
        String requestURI = request.getRequestURI();
        Object channelAuthAttr = request.getAttribute("channelAuth");
        
        // Reject if not accessed via /channel/ path
        if (!requestURI.contains("/channel/")) {
            throw new ApiException("error.authorization", "This endpoint requires channel authentication. Access via /channel/accounts/{accountId}/rateLimit with Channel JWT token.");
        }
        
        // Verify channel authentication attribute is set (set by ChannelAuthFilter)
        if (channelAuthAttr == null || !Boolean.TRUE.equals(channelAuthAttr)) {
            throw new ApiException("error.authorization", "Invalid channel authentication. This endpoint only accepts Channel JWT tokens, not regular user JWT tokens.");
        }
        
        // Validate accountId
        if (accountId == null || accountId <= 0) {
            throw new ApiException("error.validation", "Invalid account ID");
        }
        
        // Verify account exists
        try {
            accountService.getAccountInfoForChannel(accountId);
        } catch (Exception e) {
            throw new ApiException("error.account.not.found", "Account not found: " + accountId);
        }
        
        // Get rate limiting configuration
        AccountRateLimitResponse response = rateLimitingService.getAccountRateLimitConfig(accountId);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping(value = "/accounts/rateLimit/default", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get default account rate limiting configuration (Channel)",
        description = "Returns the default per-account rate limiting configuration that applies when no account-specific overrides are set. Channel authentication required.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Default rate limiting configuration retrieved successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                        implementation = AccountRateLimitResponse.RateLimitConfig.class
                    ),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "DefaultRateLimitConfig",
                        value = "{\n  \"maxRequests\": 600,\n  \"windowSeconds\": 60\n}"
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
    public ResponseEntity<AccountRateLimitResponse.RateLimitConfig> getDefaultRateLimit(
        @RequestHeader(name = "Authorization") String authorization,
        HttpServletRequest request
    ) {
        // Ensure this endpoint is only accessible via channel authentication
        String requestURI = request.getRequestURI();
        Object channelAuthAttr = request.getAttribute("channelAuth");
        
        // Reject if not accessed via /channel/ path
        if (!requestURI.contains("/channel/")) {
            throw new ApiException("error.authorization", "This endpoint requires channel authentication. Access via /channel/accounts/rateLimit/default with Channel JWT token.");
        }
        
        // Verify channel authentication attribute is set (set by ChannelAuthFilter)
        if (channelAuthAttr == null || !Boolean.TRUE.equals(channelAuthAttr)) {
            throw new ApiException("error.authorization", "Invalid channel authentication. This endpoint only accepts Channel JWT tokens, not regular user JWT tokens.");
        }
        
        // Get default rate limiting configuration
        AccountRateLimitResponse.RateLimitConfig response = rateLimitingService.getDefaultAccountRateLimitConfig();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping(value = "/accounts/{accountId}/rateLimit/check", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Check and increment account rate limit (Channel)",
        description = "Atomically checks if a request is allowed based on rate limiting rules and increments the counter if allowed. This endpoint ensures centralized rate limit management across all microservices. Channel authentication required.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Rate limit check completed successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                        implementation = RateLimitCheckResponse.class
                    ),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "RateLimitCheckResponse",
                        value = "{\n  \"allowed\": true,\n  \"remainingRequests\": 599,\n  \"currentCount\": 1,\n  \"maxRequests\": 600,\n  \"windowSeconds\": 60,\n  \"resetAt\": 1702123456\n}"
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid request parameters",
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
                responseCode = "404",
                description = "Not Found - Account not found",
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
    public ResponseEntity<RateLimitCheckResponse> checkAndIncrementRateLimit(
        @PathVariable Long accountId,
        @RequestBody @Valid RateLimitCheckRequest request,
        @RequestHeader(name = "Authorization") String authorization,
        HttpServletRequest httpRequest
    ) {
        // Ensure this endpoint is only accessible via channel authentication
        String requestURI = httpRequest.getRequestURI();
        Object channelAuthAttr = httpRequest.getAttribute("channelAuth");
        
        // Reject if not accessed via /channel/ path
        if (!requestURI.contains("/channel/")) {
            throw new ApiException("error.authorization", "This endpoint requires channel authentication. Access via /channel/accounts/{accountId}/rateLimit/check with Channel JWT token.");
        }
        
        // Verify channel authentication attribute is set (set by ChannelAuthFilter)
        if (channelAuthAttr == null || !Boolean.TRUE.equals(channelAuthAttr)) {
            throw new ApiException("error.authorization", "Invalid channel authentication. This endpoint only accepts Channel JWT tokens, not regular user JWT tokens.");
        }
        
        // Validate accountId
        if (accountId == null || accountId <= 0) {
            throw new ApiException("error.validation", "Invalid account ID");
        }
        
        // Validate request
        if (request == null || request.getEndpoint() == null || request.getEndpoint().trim().isEmpty()) {
            throw new ApiException("error.validation", "Endpoint is required");
        }
        if (request.getClientKey() == null || request.getClientKey().trim().isEmpty()) {
            throw new ApiException("error.validation", "Client key is required");
        }
        
        // Verify account exists
        try {
            accountService.getAccountInfoForChannel(accountId);
        } catch (Exception e) {
            throw new ApiException("error.account.not.found", "Account not found: " + accountId);
        }
        
        // Check and increment rate limit
        RateLimitCheckResponse response = rateLimitingService.checkAndIncrementAccountRateLimit(
            accountId, 
            request.getEndpoint(), 
            request.getClientKey()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping(value = "/accounts/{accountId}/rateLimit/status", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get account rate limit status (Channel)",
        description = "Returns the current rate limit status for an account/endpoint/client combination without incrementing the counter. This is useful for checking remaining requests without consuming a rate limit slot. Channel authentication required.",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Rate limit status retrieved successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                        implementation = RateLimitCheckResponse.class
                    ),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "RateLimitStatusResponse",
                        value = "{\n  \"allowed\": true,\n  \"remainingRequests\": 599,\n  \"currentCount\": 1,\n  \"maxRequests\": 600,\n  \"windowSeconds\": 60,\n  \"resetAt\": 1702123456\n}"
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid request parameters",
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
                responseCode = "404",
                description = "Not Found - Account not found",
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
    public ResponseEntity<RateLimitCheckResponse> getRateLimitStatus(
        @PathVariable Long accountId,
        @RequestParam(name = "endpoint") String endpoint,
        @RequestParam(name = "clientKey") String clientKey,
        @RequestHeader(name = "Authorization") String authorization,
        HttpServletRequest httpRequest
    ) {
        // Ensure this endpoint is only accessible via channel authentication
        String requestURI = httpRequest.getRequestURI();
        Object channelAuthAttr = httpRequest.getAttribute("channelAuth");
        
        // Reject if not accessed via /channel/ path
        if (!requestURI.contains("/channel/")) {
            throw new ApiException("error.authorization", "This endpoint requires channel authentication. Access via /channel/accounts/{accountId}/rateLimit/status with Channel JWT token.");
        }
        
        // Verify channel authentication attribute is set (set by ChannelAuthFilter)
        if (channelAuthAttr == null || !Boolean.TRUE.equals(channelAuthAttr)) {
            throw new ApiException("error.authorization", "Invalid channel authentication. This endpoint only accepts Channel JWT tokens, not regular user JWT tokens.");
        }
        
        // Validate accountId
        if (accountId == null || accountId <= 0) {
            throw new ApiException("error.validation", "Invalid account ID");
        }
        
        // Validate query parameters
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new ApiException("error.validation", "Endpoint query parameter is required");
        }
        if (clientKey == null || clientKey.trim().isEmpty()) {
            throw new ApiException("error.validation", "Client key query parameter is required");
        }
        
        // Verify account exists
        try {
            accountService.getAccountInfoForChannel(accountId);
        } catch (Exception e) {
            throw new ApiException("error.account.not.found", "Account not found: " + accountId);
        }
        
        // Get rate limit status without incrementing
        RateLimitCheckResponse response = rateLimitingService.getAccountRateLimitStatus(
            accountId, 
            endpoint, 
            clientKey
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping(value = "/accounts/{accountId}/subscription", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get subscription details (Channel)",
        description = "Returns subscription details for an account. Channel authentication required.",
        parameters = {
            @io.swagger.v3.oas.annotations.Parameter(
                name = "accountId",
                description = "Account ID",
                required = true,
                in = io.swagger.v3.oas.annotations.enums.ParameterIn.PATH,
                schema = @io.swagger.v3.oas.annotations.media.Schema(type = "integer", format = "int64", example = "1")
            )
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Subscription details retrieved successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SubscriptionInfo.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No active subscription found",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<SubscriptionInfo> getSubscription(
        @PathVariable Long accountId,
        @RequestHeader(name = "Authorization") String authorization,
        HttpServletRequest request
    ) {
        // Ensure this endpoint is only accessible via channel authentication
        String requestURI = request.getRequestURI();
        Object channelAuthAttr = request.getAttribute("channelAuth");
        
        if (!requestURI.contains("/channel/")) {
            throw new ApiException("error.authorization", "This endpoint requires channel authentication.");
        }
        
        if (channelAuthAttr == null || !Boolean.TRUE.equals(channelAuthAttr)) {
            throw new ApiException("error.authorization", "Invalid channel authentication.");
        }
        
        if (accountId == null || accountId <= 0) {
            throw new ApiException("error.validation", "Invalid account ID");
        }
        
        try {
            accountService.getAccountInfoForChannel(accountId);
        } catch (Exception e) {
            throw new ApiException("error.account.not.found", "Account not found: " + accountId);
        }
        
        Optional<SubscriptionInfo> subscriptionOpt = subscriptionService.getSubscriptionInfo(accountId);
        if (subscriptionOpt.isEmpty()) {
            throw new ApiException("error.subscription.not.found", "No active subscription found");
        }
        
        return ResponseEntity.ok(subscriptionOpt.get());
    }
    
    @GetMapping(value = "/accounts/{accountId}/features", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get active features and limits (Channel)",
        description = "Returns all active features with limits for an account. Channel authentication required.",
        parameters = {
            @io.swagger.v3.oas.annotations.Parameter(
                name = "accountId",
                description = "Account ID",
                required = true,
                in = io.swagger.v3.oas.annotations.enums.ParameterIn.PATH,
                schema = @io.swagger.v3.oas.annotations.media.Schema(type = "integer", format = "int64", example = "1")
            )
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Features retrieved successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                        type = "array",
                        implementation = FeatureInfo.class
                    )
                )
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<FeatureInfo>> getFeatures(
        @PathVariable Long accountId,
        @RequestHeader(name = "Authorization") String authorization,
        HttpServletRequest request
    ) {
        // Ensure this endpoint is only accessible via channel authentication
        String requestURI = request.getRequestURI();
        Object channelAuthAttr = request.getAttribute("channelAuth");
        
        if (!requestURI.contains("/channel/")) {
            throw new ApiException("error.authorization", "This endpoint requires channel authentication.");
        }
        
        if (channelAuthAttr == null || !Boolean.TRUE.equals(channelAuthAttr)) {
            throw new ApiException("error.authorization", "Invalid channel authentication.");
        }
        
        if (accountId == null || accountId <= 0) {
            throw new ApiException("error.validation", "Invalid account ID");
        }
        
        try {
            accountService.getAccountInfoForChannel(accountId);
        } catch (Exception e) {
            throw new ApiException("error.account.not.found", "Account not found: " + accountId);
        }
        
        List<FeatureInfo> features = featureService.getActiveFeatures(accountId);
        return ResponseEntity.ok(features);
    }
    
    @PostMapping(value = "/accounts/{accountId}/usage/increment", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Increment usage counter (Channel)",
        description = "Increments the usage counter for a feature. Channel authentication required.",
        parameters = {
            @io.swagger.v3.oas.annotations.Parameter(
                name = "accountId",
                description = "Account ID",
                required = true,
                in = io.swagger.v3.oas.annotations.enums.ParameterIn.PATH,
                schema = @io.swagger.v3.oas.annotations.media.Schema(type = "integer", format = "int64", example = "1")
            )
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Usage increment request with feature code and increment value",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UsageIncrementRequest.class)
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Usage incremented successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                        type = "object",
                        example = "{\"status\": \"success\", \"usageValue\": 1251, \"featureCode\": \"conversations.monthly\"}"
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Account or subscription not found",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> incrementUsage(
        @PathVariable Long accountId,
        @RequestBody @Valid UsageIncrementRequest request,
        @RequestHeader(name = "Authorization") String authorization,
        HttpServletRequest httpRequest
    ) {
        // Ensure this endpoint is only accessible via channel authentication
        String requestURI = httpRequest.getRequestURI();
        Object channelAuthAttr = httpRequest.getAttribute("channelAuth");
        
        if (!requestURI.contains("/channel/")) {
            throw new ApiException("error.authorization", "This endpoint requires channel authentication.");
        }
        
        if (channelAuthAttr == null || !Boolean.TRUE.equals(channelAuthAttr)) {
            throw new ApiException("error.authorization", "Invalid channel authentication.");
        }
        
        if (accountId == null || accountId <= 0) {
            throw new ApiException("error.validation", "Invalid account ID");
        }
        
        try {
            accountService.getAccountInfoForChannel(accountId);
        } catch (Exception e) {
            throw new ApiException("error.account.not.found", "Account not found: " + accountId);
        }
        
        Optional<com.delight.account.model.Subscription> subscriptionOpt = 
            subscriptionService.getActiveSubscription(accountId);
        if (subscriptionOpt.isEmpty()) {
            throw new ApiException("error.subscription.not.found", "No active subscription found");
        }
        
        com.delight.account.model.Subscription subscription = subscriptionOpt.get();
        com.delight.account.model.FeatureUsage usage = usageTrackingService.incrementUsage(
            accountId, subscription.getId(), request.getFeatureCode(), request.getIncrement());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("usageValue", usage.getUsageValue());
        response.put("featureCode", request.getFeatureCode());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping(value = "/accounts/{accountId}/usage/decrement", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Decrement usage counter (Channel)",
        description = "Decrements the usage counter for a feature. Channel authentication required.",
        parameters = {
            @io.swagger.v3.oas.annotations.Parameter(
                name = "accountId",
                description = "Account ID",
                required = true,
                in = io.swagger.v3.oas.annotations.enums.ParameterIn.PATH,
                schema = @io.swagger.v3.oas.annotations.media.Schema(type = "integer", format = "int64", example = "1")
            )
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Usage decrement request with feature code and decrement value",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UsageDecrementRequest.class)
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Usage decremented successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                        type = "object",
                        example = "{\"status\": \"success\", \"usageValue\": 1249, \"featureCode\": \"conversations.monthly\"}"
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request or insufficient usage value",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Account or subscription not found",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> decrementUsage(
        @PathVariable Long accountId,
        @RequestBody @Valid UsageDecrementRequest request,
        @RequestHeader(name = "Authorization") String authorization,
        HttpServletRequest httpRequest
    ) {
        // Ensure this endpoint is only accessible via channel authentication
        String requestURI = httpRequest.getRequestURI();
        Object channelAuthAttr = httpRequest.getAttribute("channelAuth");
        
        if (!requestURI.contains("/channel/")) {
            throw new ApiException("error.authorization", "This endpoint requires channel authentication.");
        }
        
        if (channelAuthAttr == null || !Boolean.TRUE.equals(channelAuthAttr)) {
            throw new ApiException("error.authorization", "Invalid channel authentication.");
        }
        
        if (accountId == null || accountId <= 0) {
            throw new ApiException("error.validation", "Invalid account ID");
        }
        
        try {
            accountService.getAccountInfoForChannel(accountId);
        } catch (Exception e) {
            throw new ApiException("error.account.not.found", "Account not found: " + accountId);
        }
        
        Optional<com.delight.account.model.Subscription> subscriptionOpt = 
            subscriptionService.getActiveSubscription(accountId);
        if (subscriptionOpt.isEmpty()) {
            throw new ApiException("error.subscription.not.found", "No active subscription found");
        }
        
        com.delight.account.model.Subscription subscription = subscriptionOpt.get();
        com.delight.account.model.FeatureUsage usage = usageTrackingService.decrementUsage(
            accountId, subscription.getId(), request.getFeatureCode(), request.getDecrement());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("usageValue", usage.getUsageValue());
        response.put("featureCode", request.getFeatureCode());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping(value = "/accounts/{accountId}/usage/set", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Set usage value (Channel)",
        description = "Sets the usage value for a feature. Channel authentication required.",
        parameters = {
            @io.swagger.v3.oas.annotations.Parameter(
                name = "accountId",
                description = "Account ID",
                required = true,
                in = io.swagger.v3.oas.annotations.enums.ParameterIn.PATH,
                schema = @io.swagger.v3.oas.annotations.media.Schema(type = "integer", format = "int64", example = "1")
            )
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Usage set request with feature code and value",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UsageSetRequest.class)
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Usage set successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                        type = "object",
                        example = "{\"status\": \"success\", \"usageValue\": 100, \"featureCode\": \"conversations.monthly\"}"
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Account or subscription not found",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> setUsage(
        @PathVariable Long accountId,
        @RequestBody @Valid UsageSetRequest request,
        @RequestHeader(name = "Authorization") String authorization,
        HttpServletRequest httpRequest
    ) {
        // Ensure this endpoint is only accessible via channel authentication
        String requestURI = httpRequest.getRequestURI();
        Object channelAuthAttr = httpRequest.getAttribute("channelAuth");
        
        if (!requestURI.contains("/channel/")) {
            throw new ApiException("error.authorization", "This endpoint requires channel authentication.");
        }
        
        if (channelAuthAttr == null || !Boolean.TRUE.equals(channelAuthAttr)) {
            throw new ApiException("error.authorization", "Invalid channel authentication.");
        }
        
        if (accountId == null || accountId <= 0) {
            throw new ApiException("error.validation", "Invalid account ID");
        }
        
        try {
            accountService.getAccountInfoForChannel(accountId);
        } catch (Exception e) {
            throw new ApiException("error.account.not.found", "Account not found: " + accountId);
        }
        
        Optional<com.delight.account.model.Subscription> subscriptionOpt = 
            subscriptionService.getActiveSubscription(accountId);
        if (subscriptionOpt.isEmpty()) {
            throw new ApiException("error.subscription.not.found", "No active subscription found");
        }
        
        com.delight.account.model.Subscription subscription = subscriptionOpt.get();
        com.delight.account.model.FeatureUsage usage = usageTrackingService.setUsage(
            accountId, subscription.getId(), request.getFeatureCode(), request.getValue());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("usageValue", usage.getUsageValue());
        response.put("featureCode", request.getFeatureCode());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping(value = "/accounts/{accountId}/usage/check", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Check if feature usage is allowed (Channel)",
        description = "Checks if usage is within limit for a feature. Channel authentication required.",
        parameters = {
            @io.swagger.v3.oas.annotations.Parameter(
                name = "accountId",
                description = "Account ID",
                required = true,
                in = io.swagger.v3.oas.annotations.enums.ParameterIn.PATH,
                schema = @io.swagger.v3.oas.annotations.media.Schema(type = "integer", format = "int64", example = "1")
            ),
            @io.swagger.v3.oas.annotations.Parameter(
                name = "featureCode",
                description = "Feature code to check (e.g., conversations.monthly)",
                required = true,
                in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY,
                schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", example = "conversations.monthly")
            )
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Usage check completed",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UsageCheckResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request (missing featureCode)",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Account or subscription not found",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
                )
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UsageCheckResponse> checkUsage(
        @PathVariable Long accountId,
        @RequestParam(name = "featureCode") String featureCode,
        @RequestHeader(name = "Authorization") String authorization,
        HttpServletRequest httpRequest
    ) {
        // Ensure this endpoint is only accessible via channel authentication
        String requestURI = httpRequest.getRequestURI();
        Object channelAuthAttr = httpRequest.getAttribute("channelAuth");
        
        if (!requestURI.contains("/channel/")) {
            throw new ApiException("error.authorization", "This endpoint requires channel authentication.");
        }
        
        if (channelAuthAttr == null || !Boolean.TRUE.equals(channelAuthAttr)) {
            throw new ApiException("error.authorization", "Invalid channel authentication.");
        }
        
        if (accountId == null || accountId <= 0) {
            throw new ApiException("error.validation", "Invalid account ID");
        }
        
        if (featureCode == null || featureCode.trim().isEmpty()) {
            throw new ApiException("error.validation", "Feature code is required");
        }
        
        try {
            accountService.getAccountInfoForChannel(accountId);
        } catch (Exception e) {
            throw new ApiException("error.account.not.found", "Account not found: " + accountId);
        }
        
        Optional<com.delight.account.model.Subscription> subscriptionOpt = 
            subscriptionService.getActiveSubscription(accountId);
        if (subscriptionOpt.isEmpty()) {
            throw new ApiException("error.subscription.not.found", "No active subscription found");
        }
        
        com.delight.account.model.Subscription subscription = subscriptionOpt.get();
        boolean allowed = usageTrackingService.isUsageAllowed(
            accountId, subscription.getId(), featureCode);
        
        Long currentUsage = usageTrackingService.getCurrentUsage(
            accountId, subscription.getId(), featureCode)
            .map(com.delight.account.model.FeatureUsage::getUsageValue)
            .orElse(0L);
        
        // Get limit from subscription feature
        Optional<SubscriptionFeature> featureOpt = 
            subscriptionFeatureRepository.findBySubscriptionIdAndFeatureCode(
                subscription.getId(), featureCode);
        
        Long limit = featureOpt.map(SubscriptionFeature::getLimitValue)
            .orElse(null);
        
        Long remaining = limit != null && limit != -1 ? Math.max(0, limit - currentUsage) : null;
        
        String message = allowed ? "Usage is within limit" : "Usage limit exceeded";
        
        UsageCheckResponse response = UsageCheckResponse.builder()
            .allowed(allowed)
            .currentUsage(currentUsage)
            .limit(limit)
            .remaining(remaining)
            .message(message)
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