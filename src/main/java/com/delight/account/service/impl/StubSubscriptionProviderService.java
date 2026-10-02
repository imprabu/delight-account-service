package com.delight.account.service.impl;

import com.delight.account.model.Subscription;
import com.delight.account.service.SubscriptionProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub implementation of SubscriptionProviderService for development.
 * Returns mock data without actual provider integration.
 */
@Slf4j
@Service
public class StubSubscriptionProviderService implements SubscriptionProviderService {
    
    @Override
    public Subscription createSubscription(Long accountId, String productCode, String planCode) {
        log.info("Stub: Creating subscription for accountId={}, productCode={}, planCode={}", 
            accountId, productCode, planCode);
        // This is a stub - actual implementation would call Stripe/Chargebee API
        // For now, the subscription will be created by SubscriptionService without provider integration
        return null;
    }
    
    @Override
    public Subscription updateSubscription(Long subscriptionId, String newPlanCode) {
        log.info("Stub: Updating subscription subscriptionId={}, newPlanCode={}", 
            subscriptionId, newPlanCode);
        // This is a stub - actual implementation would call Stripe/Chargebee API
        return null;
    }
    
    @Override
    public void cancelSubscription(Long subscriptionId) {
        log.info("Stub: Cancelling subscription subscriptionId={}", subscriptionId);
        // This is a stub - actual implementation would call Stripe/Chargebee API
    }
    
    @Override
    public Subscription syncFromProvider(String providerSubscriptionId) {
        log.info("Stub: Syncing subscription from provider providerSubscriptionId={}", 
            providerSubscriptionId);
        // This is a stub - actual implementation would call Stripe/Chargebee API
        return null;
    }
}



