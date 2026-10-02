package com.delight.account.service;

import com.delight.account.model.FeatureUsage;

import java.util.Optional;

/**
 * Service for tracking and updating feature usage.
 */
public interface UsageTrackingService {
    
    /**
     * Get current usage for a feature
     * @param accountId Account ID
     * @param subscriptionId Subscription ID
     * @param featureCode Feature code
     * @return Optional FeatureUsage
     */
    Optional<FeatureUsage> getCurrentUsage(Long accountId, Long subscriptionId, String featureCode);
    
    /**
     * Increment usage for a feature
     * @param accountId Account ID
     * @param subscriptionId Subscription ID
     * @param featureCode Feature code
     * @param increment Amount to increment
     * @return Updated FeatureUsage
     */
    FeatureUsage incrementUsage(Long accountId, Long subscriptionId, String featureCode, Long increment);
    
    /**
     * Decrement usage for a feature
     * @param accountId Account ID
     * @param subscriptionId Subscription ID
     * @param featureCode Feature code
     * @param decrement Amount to decrement
     * @return Updated FeatureUsage
     */
    FeatureUsage decrementUsage(Long accountId, Long subscriptionId, String featureCode, Long decrement);
    
    /**
     * Set usage value for a feature
     * @param accountId Account ID
     * @param subscriptionId Subscription ID
     * @param featureCode Feature code
     * @param value New usage value
     * @return Updated FeatureUsage
     */
    FeatureUsage setUsage(Long accountId, Long subscriptionId, String featureCode, Long value);
    
    /**
     * Check if usage is allowed (within limit)
     * @param accountId Account ID
     * @param subscriptionId Subscription ID
     * @param featureCode Feature code
     * @return true if usage is allowed
     */
    boolean isUsageAllowed(Long accountId, Long subscriptionId, String featureCode);
}



