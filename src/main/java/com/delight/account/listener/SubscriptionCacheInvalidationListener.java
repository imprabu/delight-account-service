package com.delight.account.listener;

import com.delight.account.cache.AccountCacheKeyManager;
import com.delight.account.model.Subscription;
import com.delight.account.service.CacheService;
import com.delight.account.service.SessionService;
import com.delight.account.service.SubscriptionEventPublisher;
import com.delight.account.util.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.*;

/**
 * JPA Entity Listener for subscription cache and session invalidation.
 * This listener ensures cache consistency and invalidates all user sessions
 * when subscription changes occur.
 */
@Slf4j
@Component
public class SubscriptionCacheInvalidationListener {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private AccountCacheKeyManager accountCacheKeyManager;

    @Autowired
    private SessionService sessionService;

    @Autowired(required = false)
    private SubscriptionEventPublisher subscriptionEventPublisher;

    @PostPersist
    @PostUpdate
    @PostRemove
    public void invalidateCacheAndSessions(Object entity) {
        if (cacheService == null || accountCacheKeyManager == null || sessionService == null) {
            log.warn("Cache/session invalidation skipped - Services not available");
            return;
        }

        if (!(entity instanceof Subscription)) {
            return;
        }

        Subscription subscription = (Subscription) entity;
        Long accountId = subscription.getAccount() != null ? subscription.getAccount().getId() : null;
        
        if (accountId == null) {
            log.warn("Cannot invalidate cache/sessions - Account ID is null for subscription: {}", subscription.getId());
            return;
        }

        try {
            String operation = getCurrentOperation();
            log.info("Invalidating subscription cache and sessions - Operation: {}, SubscriptionId: {}, AccountId: {}", 
                operation, subscription.getId(), accountId);

            // Invalidate subscription-related cache
            invalidateSubscriptionCache(accountId, subscription.getId(), operation);

            // Invalidate account info cache
            invalidateAccountCache(accountId, operation);

            // Invalidate all user sessions for this account
            sessionService.invalidateAccountSessions(accountId);
            log.info("Invalidated all sessions for account: {}", accountId);

            // Publish Kafka event for subscription change
            if (subscriptionEventPublisher != null) {
                String eventType = getEventType(operation);
                try {
                    subscriptionEventPublisher.publishSubscriptionChanged(subscription.getId(), eventType);
                } catch (Exception e) {
                    log.error("Failed to publish subscription event - SubscriptionId: {}, AccountId: {}, EventType: {}, Error: {}", 
                        subscription.getId(), accountId, eventType, e.getMessage(), e);
                    // Don't throw - event publishing failure shouldn't break cache invalidation
                }
            }

        } catch (Exception e) {
            log.error("Failed to invalidate subscription cache/sessions - SubscriptionId: {}, AccountId: {}, Error: {}", 
                subscription.getId(), accountId, e.getMessage(), e);
        }
    }

    private String getCurrentOperation() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String methodName = element.getMethodName();
            if (methodName.contains("PostPersist")) return "CREATE";
            if (methodName.contains("PostUpdate")) return "UPDATE";
            if (methodName.contains("PostRemove")) return "DELETE";
        }
        return "UNKNOWN";
    }
    
    /**
     * Map operation to Kafka event type.
     */
    private String getEventType(String operation) {
        return switch (operation) {
            case "CREATE" -> "subscription.created";
            case "UPDATE" -> "subscription.updated";
            case "DELETE" -> "subscription.cancelled";
            default -> "subscription.updated"; // Default to updated for unknown operations
        };
    }

    private void invalidateSubscriptionCache(Long accountId, Long subscriptionId, String operation) {
        try {
            // Invalidate subscription cache patterns
            String versionedPattern = String.format("%s:*subscription*%d*", accountCacheKeyManager.getVersion(), subscriptionId);
            String legacyPattern = String.format("subscription:*%d*", subscriptionId);
            
            cacheService.evictByPattern(versionedPattern);
            LoggingUtils.logCacheOperation(log, "EVICT_PATTERN", "SUCCESS", 
                "pattern", versionedPattern, "subscriptionId", subscriptionId, "operation", operation);
            
            cacheService.evictByPattern(legacyPattern);
            LoggingUtils.logCacheOperation(log, "EVICT_PATTERN", "SUCCESS", 
                "pattern", legacyPattern, "subscriptionId", subscriptionId, "operation", operation);

            // Invalidate account-specific subscription cache
            String versionedKey = String.format("%s:account:subscription:%d", accountCacheKeyManager.getVersion(), accountId);
            String legacyKey = String.format("account:subscription:%d", accountId);
            
            cacheService.evict(versionedKey);
            LoggingUtils.logCacheOperation(log, "EVICT", "SUCCESS", 
                "key", versionedKey, "subscriptionId", subscriptionId, "accountId", accountId, "operation", operation);
            
            cacheService.evict(legacyKey);
            LoggingUtils.logCacheOperation(log, "EVICT", "SUCCESS", 
                "key", legacyKey, "subscriptionId", subscriptionId, "accountId", accountId, "operation", operation);
        } catch (Exception e) {
            log.error("Failed to invalidate subscription cache - SubscriptionId: {}, Error: {}", subscriptionId, e.getMessage(), e);
        }
    }

    private void invalidateAccountCache(Long accountId, String operation) {
        try {
            // Invalidate account info cache
            String accountInfoKey = accountCacheKeyManager.getAccountInfoKey(accountId);
            String legacyAccountInfoKey = String.format("account:info:%d", accountId);
            
            cacheService.evict(accountInfoKey);
            LoggingUtils.logCacheOperation(log, "EVICT", "SUCCESS", 
                "key", accountInfoKey, "accountId", accountId, "operation", operation);
            
            cacheService.evict(legacyAccountInfoKey);
            LoggingUtils.logCacheOperation(log, "EVICT", "SUCCESS", 
                "key", legacyAccountInfoKey, "accountId", accountId, "operation", operation);
        } catch (Exception e) {
            log.error("Failed to invalidate account cache - AccountId: {}, Error: {}", accountId, e.getMessage(), e);
        }
    }
}



