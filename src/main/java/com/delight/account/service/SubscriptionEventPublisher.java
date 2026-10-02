package com.delight.account.service;

import com.delight.account.dto.AccountInfoResponse;
import com.delight.account.dto.FeatureInfo;
import com.delight.account.model.Subscription;
import com.delight.account.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service to build and publish subscription change events to Kafka.
 * Publishes events to account_activities_v1 topic with subscription, account, and features data.
 */
@Slf4j
@Service
public class SubscriptionEventPublisher {
    
    private final KafkaEventPublisher kafkaEventPublisher;
    private final FeatureService featureService;
    private final AccountService accountService;
    private final SubscriptionRepository subscriptionRepository;
    
    public SubscriptionEventPublisher(
            KafkaEventPublisher kafkaEventPublisher,
            FeatureService featureService,
            AccountService accountService,
            SubscriptionRepository subscriptionRepository) {
        this.kafkaEventPublisher = kafkaEventPublisher;
        this.featureService = featureService;
        this.accountService = accountService;
        this.subscriptionRepository = subscriptionRepository;
    }
    
    /**
     * Publish subscription change event to Kafka.
     * @param subscriptionId Subscription ID
     * @param eventType Event type: subscription.created, subscription.updated, subscription.cancelled
     */
    @Transactional(readOnly = true)
    public void publishSubscriptionChanged(Long subscriptionId, String eventType) {
        try {
            // 1. Load subscription with account
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
            if (subscriptionOpt.isEmpty()) {
                log.warn("Subscription not found for subscriptionId={}, skipping event publication", subscriptionId);
                return;
            }
            
            Subscription subscription = subscriptionOpt.get();
            if (subscription.getAccount() == null || subscription.getAccount().getId() == null) {
                log.warn("Account ID is null for subscriptionId={}, skipping event publication", subscriptionId);
                return;
            }
            
            Long accountId = subscription.getAccount().getId();
            
            // 2. Build subscription data map
            Map<String, Object> subscriptionData = buildSubscriptionData(subscription);
            
            // 3. Build account data map (using accountService.getAccountInfoForChannel)
            Map<String, Object> accountData = buildAccountData(accountId);
            
            // 4. Build features list (using featureService.getActiveFeatures)
            List<Map<String, Object>> featuresData = buildFeaturesData(accountId);
            
            // 5. Build event data map with subscription, account, and features
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("subscription", subscriptionData);
            eventData.put("account", accountData);
            eventData.put("features", featuresData);
            
            // 6. Publish to Kafka using existing account_activities_v1 topic
            kafkaEventPublisher.publishAccountEvent(eventType, accountId, eventData);
            
            log.info("✅ [SUBSCRIPTION-EVENT] Published {} event for subscriptionId={}, accountId={}", 
                    eventType, subscriptionId, accountId);
        } catch (Exception e) {
            log.error("❌ [SUBSCRIPTION-EVENT] Failed to publish {} event for subscriptionId={}: {}", 
                    eventType, subscriptionId, e.getMessage(), e);
            // Don't throw exception - event publishing failure shouldn't break the transaction
        }
    }
    
    /**
     * Build subscription data map from Subscription entity.
     */
    private Map<String, Object> buildSubscriptionData(Subscription subscription) {
        Map<String, Object> subscriptionData = new HashMap<>();
        if (subscription == null) {
            return subscriptionData;
        }
        
        subscriptionData.put("id", subscription.getId());
        subscriptionData.put("productCode", subscription.getProductCode());
        subscriptionData.put("planCode", subscription.getPlanCode());
        subscriptionData.put("planVersion", subscription.getPlanVersion());
        subscriptionData.put("status", subscription.getStatus() != null ? subscription.getStatus().name() : null);
        subscriptionData.put("provider", subscription.getProvider());
        subscriptionData.put("providerSubscriptionId", subscription.getProviderSubscriptionId());
        subscriptionData.put("billingCycleStartDate", subscription.getBillingCycleStartDate());
        subscriptionData.put("billingCycleEndDate", subscription.getBillingCycleEndDate());
        subscriptionData.put("nextBillingDate", subscription.getNextBillingDate());
        subscriptionData.put("createdAt", subscription.getCreatedAt());
        subscriptionData.put("updatedAt", subscription.getUpdatedAt());
        subscriptionData.put("cancelledAt", subscription.getCancelledAt());
        
        return subscriptionData;
    }
    
    /**
     * Build account data map from AccountInfoResponse.
     */
    private Map<String, Object> buildAccountData(Long accountId) {
        Map<String, Object> accountData = new HashMap<>();
        try {
            AccountInfoResponse accountInfo = accountService.getAccountInfoForChannel(accountId);
            if (accountInfo != null) {
                accountData.put("id", accountInfo.getId());
                accountData.put("domain", accountInfo.getDomain());
                accountData.put("plan", accountInfo.getPlan());
                accountData.put("status", accountInfo.getStatus() != null ? accountInfo.getStatus().name() : null);
                accountData.put("industryType", accountInfo.getIndustryType());
                accountData.put("companyName", accountInfo.getCompanyName());
                accountData.put("organizationId", accountInfo.getOrganizationId());
                accountData.put("organizationName", accountInfo.getOrganizationName());
                accountData.put("organizationPrimaryDomain", accountInfo.getOrganizationPrimaryDomain());
                accountData.put("productCode", accountInfo.getProductCode());
                accountData.put("productName", accountInfo.getProductName());
            }
        } catch (Exception e) {
            log.error("Error building account data for accountId={}: {}", accountId, e.getMessage(), e);
        }
        return accountData;
    }
    
    /**
     * Build features data list from FeatureInfo list.
     */
    private List<Map<String, Object>> buildFeaturesData(Long accountId) {
        List<Map<String, Object>> featuresData = new ArrayList<>();
        try {
            List<FeatureInfo> features = featureService.getActiveFeatures(accountId);
            for (FeatureInfo feature : features) {
                Map<String, Object> featureData = new HashMap<>();
                featureData.put("code", feature.getCode());
                featureData.put("name", feature.getName());
                featureData.put("type", feature.getType());
                featureData.put("limit", feature.getLimit());
                featureData.put("currentUsage", feature.getCurrentUsage());
                featureData.put("enabled", feature.getEnabled());
                featureData.put("rateLimitRpm", feature.getRateLimitRpm());
                featureData.put("allowedChannels", feature.getAllowedChannels());
                featuresData.add(featureData);
            }
        } catch (Exception e) {
            log.error("Error building features data for accountId={}: {}", accountId, e.getMessage(), e);
        }
        return featuresData;
    }
}

