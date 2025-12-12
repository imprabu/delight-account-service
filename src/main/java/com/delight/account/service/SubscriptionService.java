package com.delight.account.service;

import com.delight.account.dto.SubscriptionInfo;
import com.delight.account.model.Subscription;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing subscriptions.
 */
public interface SubscriptionService {
    
    /**
     * Get active subscription for an account
     * @param accountId Account ID
     * @return Optional subscription
     */
    Optional<Subscription> getActiveSubscription(Long accountId);
    
    /**
     * Get active subscription for an account and product
     * @param accountId Account ID
     * @param productCode Product code
     * @return Optional subscription
     */
    Optional<Subscription> getActiveSubscription(Long accountId, String productCode);
    
    /**
     * Get all subscriptions for an account
     * @param accountId Account ID
     * @return List of subscriptions
     */
    List<Subscription> getSubscriptions(Long accountId);
    
    /**
     * Create a new subscription
     * @param accountId Account ID
     * @param productCode Product code
     * @param planCode Plan code
     * @return Created subscription
     */
    Subscription createSubscription(Long accountId, String productCode, String planCode);
    
    /**
     * Update subscription plan
     * @param subscriptionId Subscription ID
     * @param newPlanCode New plan code
     * @return Updated subscription
     */
    Subscription updateSubscription(Long subscriptionId, String newPlanCode);
    
    /**
     * Cancel a subscription
     * @param subscriptionId Subscription ID
     */
    void cancelSubscription(Long subscriptionId);
    
    /**
     * Get subscription info for session payload
     * @param accountId Account ID
     * @return Optional SubscriptionInfo
     */
    Optional<SubscriptionInfo> getSubscriptionInfo(Long accountId);
}



