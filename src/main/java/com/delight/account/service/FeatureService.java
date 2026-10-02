package com.delight.account.service;

import com.delight.account.dto.FeatureInfo;

import java.util.List;

/**
 * Service for managing features and checking feature limits.
 */
public interface FeatureService {
    
    /**
     * Get all active features for an account
     * @param accountId Account ID
     * @return List of FeatureInfo
     */
    List<FeatureInfo> getActiveFeatures(Long accountId);
    
    /**
     * Check if a feature is enabled for an account
     * @param accountId Account ID
     * @param featureCode Feature code
     * @return true if enabled
     */
    boolean isFeatureEnabled(Long accountId, String featureCode);
    
    /**
     * Check if usage is within limit for a feature
     * @param accountId Account ID
     * @param featureCode Feature code
     * @return true if within limit
     */
    boolean isUsageWithinLimit(Long accountId, String featureCode);
    
    /**
     * Check if a channel is allowed for an account
     * @param accountId Account ID
     * @param channelCode Channel code (e.g., INSTAGRAM, WHATSAPP)
     * @return true if allowed
     */
    boolean isChannelAllowed(Long accountId, String channelCode);
    
    /**
     * Get API rate limit (RPM) for an account
     * @param accountId Account ID
     * @return Rate limit in RPM, or null if not enabled
     */
    Integer getApiRateLimit(Long accountId);
}



