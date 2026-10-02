package com.delight.account.service;

import com.delight.account.model.Subscription;

/**
 * Interface for subscription provider integration (Stripe, Chargebee, etc.)
 * Stub implementation for development.
 */
public interface SubscriptionProviderService {
    
    /**
     * Create a subscription with the provider
     * @param accountId Account ID
     * @param productCode Product code
     * @param planCode Plan code
     * @return Created subscription
     */
    Subscription createSubscription(Long accountId, String productCode, String planCode);
    
    /**
     * Update subscription plan with the provider
     * @param subscriptionId Subscription ID
     * @param newPlanCode New plan code
     * @return Updated subscription
     */
    Subscription updateSubscription(Long subscriptionId, String newPlanCode);
    
    /**
     * Cancel subscription with the provider
     * @param subscriptionId Subscription ID
     */
    void cancelSubscription(Long subscriptionId);
    
    /**
     * Sync subscription from provider
     * @param providerSubscriptionId Provider subscription ID
     * @return Synced subscription
     */
    Subscription syncFromProvider(String providerSubscriptionId);
}



