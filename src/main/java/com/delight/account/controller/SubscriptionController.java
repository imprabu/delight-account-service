package com.delight.account.controller;

import com.delight.account.dto.FeatureInfo;
import com.delight.account.dto.SubscriptionInfo;
import com.delight.account.dto.UsageStatisticsResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Subscription;
import com.delight.account.service.FeatureService;
import com.delight.account.service.SubscriptionService;
import com.delight.account.service.UsageTrackingService;
import com.delight.account.util.LoggingUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/accounts")
@Tag(name = "Subscription Management", description = "Subscription and feature management endpoints")
public class SubscriptionController {
    
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);
    
    private final SubscriptionService subscriptionService;
    private final FeatureService featureService;
    private final UsageTrackingService usageTrackingService;
    
    public SubscriptionController(
            SubscriptionService subscriptionService,
            FeatureService featureService,
            UsageTrackingService usageTrackingService) {
        this.subscriptionService = subscriptionService;
        this.featureService = featureService;
        this.usageTrackingService = usageTrackingService;
    }
    
    @GetMapping(value = "/{accountId}/subscription", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get current subscription",
        description = "Returns the active subscription for an account. This endpoint is NOT rate-limited (uses direct path, not /api/*).",
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
                description = "Subscription retrieved successfully",
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
        HttpServletRequest request
    ) {
        // Get accountId from request attributes (set by DomainAccountInterceptor)
        Long requestAccountId = getAccountIdFromRequest(request);
        if (requestAccountId == null || !requestAccountId.equals(accountId)) {
            throw new ApiException("error.authorization", "Access denied");
        }
        
        Optional<SubscriptionInfo> subscriptionOpt = subscriptionService.getSubscriptionInfo(accountId);
        if (subscriptionOpt.isEmpty()) {
            throw new ApiException("error.subscription.not.found", "No active subscription found");
        }
        
        return ResponseEntity.ok(subscriptionOpt.get());
    }
    
    @GetMapping(value = "/{accountId}/subscription/features", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get active features",
        description = "Returns all active features with limits and current usage for an account. This endpoint is NOT rate-limited (uses direct path, not /api/*).",
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
        HttpServletRequest request
    ) {
        // Get accountId from request attributes (set by DomainAccountInterceptor)
        Long requestAccountId = getAccountIdFromRequest(request);
        if (requestAccountId == null || !requestAccountId.equals(accountId)) {
            throw new ApiException("error.authorization", "Access denied");
        }
        
        List<FeatureInfo> features = featureService.getActiveFeatures(accountId);
        return ResponseEntity.ok(features);
    }
    
    @GetMapping(value = "/{accountId}/subscription/usage", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get usage statistics",
        description = "Returns usage statistics for all features. This endpoint is NOT rate-limited (uses direct path, not /api/*).",
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
                description = "Usage statistics retrieved successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UsageStatisticsResponse.class)
                )
            )
        }
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UsageStatisticsResponse> getUsage(
        @PathVariable Long accountId,
        HttpServletRequest request
    ) {
        // Get accountId from request attributes (set by DomainAccountInterceptor)
        Long requestAccountId = getAccountIdFromRequest(request);
        if (requestAccountId == null || !requestAccountId.equals(accountId)) {
            throw new ApiException("error.authorization", "Access denied");
        }
        
        List<FeatureInfo> features = featureService.getActiveFeatures(accountId);
        
        Map<String, FeatureInfo> usageMap = new HashMap<>();
        for (FeatureInfo feature : features) {
            usageMap.put(feature.getCode(), feature);
        }
        
        UsageStatisticsResponse response = UsageStatisticsResponse.builder()
            .accountId(accountId)
            .features(usageMap)
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping(value = "/{accountId}/subscription/upgrade", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Upgrade subscription plan (Stub)",
        description = "Upgrades the subscription to a new plan. This is a stub implementation for development.",
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
            description = "Upgrade request with new plan code",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    type = "object",
                    example = "{\"planCode\": \"PROFESSIONAL\"}"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Subscription upgraded successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                        type = "object",
                        example = "{\"status\": \"success\", \"message\": \"Subscription upgraded to PROFESSIONAL\"}"
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request (missing planCode)",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.delight.account.dto.ErrorResponse.class)
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
    public ResponseEntity<Map<String, String>> upgradeSubscription(
        @PathVariable Long accountId,
        @RequestBody Map<String, String> request,
        HttpServletRequest httpRequest
    ) {
        // Get accountId from request attributes
        Long requestAccountId = getAccountIdFromRequest(httpRequest);
        if (requestAccountId == null || !requestAccountId.equals(accountId)) {
            throw new ApiException("error.authorization", "Access denied");
        }
        
        String newPlanCode = request.get("planCode");
        if (newPlanCode == null || newPlanCode.trim().isEmpty()) {
            throw new ApiException("error.validation", "planCode is required");
        }
        
        Optional<Subscription> subscriptionOpt = subscriptionService.getActiveSubscription(accountId);
        if (subscriptionOpt.isEmpty()) {
            throw new ApiException("error.subscription.not.found", "No active subscription found");
        }
        
        Subscription subscription = subscriptionOpt.get();
        subscriptionService.updateSubscription(subscription.getId(), newPlanCode);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Subscription upgraded to " + newPlanCode);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping(value = "/{accountId}/subscription/cancel", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Cancel subscription (Stub)",
        description = "Cancels the active subscription. This is a stub implementation for development.",
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
                description = "Subscription cancelled successfully",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                        type = "object",
                        example = "{\"status\": \"success\", \"message\": \"Subscription cancelled\"}"
                    )
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
    public ResponseEntity<Map<String, String>> cancelSubscription(
        @PathVariable Long accountId,
        HttpServletRequest httpRequest
    ) {
        // Get accountId from request attributes
        Long requestAccountId = getAccountIdFromRequest(httpRequest);
        if (requestAccountId == null || !requestAccountId.equals(accountId)) {
            throw new ApiException("error.authorization", "Access denied");
        }
        
        Optional<Subscription> subscriptionOpt = subscriptionService.getActiveSubscription(accountId);
        if (subscriptionOpt.isEmpty()) {
            throw new ApiException("error.subscription.not.found", "No active subscription found");
        }
        
        Subscription subscription = subscriptionOpt.get();
        subscriptionService.cancelSubscription(subscription.getId());
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Subscription cancelled");
        
        return ResponseEntity.ok(response);
    }
    
    private Long getAccountIdFromRequest(HttpServletRequest request) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            Object accountIdObj = attributes.getAttribute("accountId", ServletRequestAttributes.SCOPE_REQUEST);
            if (accountIdObj != null) {
                return Long.valueOf(accountIdObj.toString());
            }
        }
        return null;
    }
}

