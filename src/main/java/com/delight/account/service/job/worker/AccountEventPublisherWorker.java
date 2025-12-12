package com.delight.account.service.job.worker;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.delight.account.util.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.delight.account.domain.BatchJobType;
import com.delight.account.model.Account;
import com.delight.account.model.SubscriptionStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.SubscriptionRepository;
import com.delight.account.service.KafkaEventPublisher;
import com.delight.account.service.job.BaseWorker;

/**
 * Worker implementation for publishing account events to messaging service.
 * 
 * Architecture:
 * - CREATE: Fetches account from DB and builds payload
 * - UPDATE: Fetches account from DB and uses changeset to build payload
 * - DELETE: Uses accountInfo from metadata (captured before deletion)
 * 
 * Retry logic is handled by BaseWorker with exponential backoff.
 */
@Component
public class AccountEventPublisherWorker extends BaseWorker {

    private static final Logger logger = LoggerFactory.getLogger(AccountEventPublisherWorker.class);
    @Autowired
    private KafkaEventPublisher kafkaEventPublisher;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    
    /**
     * Process the account event job.
     * This method is called by BaseWorker.processJob() after retry logic setup.
     */
    @Override
    @Transactional(readOnly = true)
    protected void doProcessJob(Long jobId, Long accountId, Map<String, Object> metadata) throws Exception {
        String payloadType = (String) metadata.get("payloadType");
        
        if (payloadType == null) {
            LoggingUtils.error(logger, "❌ [ACCOUNT-EVENT-WORKER] Missing required metadata: payloadType");
            throw new IllegalArgumentException("Missing required metadata: payloadType");
        }
        
        if (accountId == null) {
            LoggingUtils.error(logger, "❌ [ACCOUNT-EVENT-WORKER] Account ID is missing");
            throw new IllegalArgumentException("Account ID is missing");
        }
        
        String accountIdStr = accountId.toString();
        Map<String, Object> accountInfo;
        Map<String, Object> changeSet;
        
        // Build payload based on event type
        var result = switch (payloadType) {
            case "account_created" -> {
                // Fetch account from DB and build payload for CREATE
                yield new PayloadResult(fetchAndBuildAccountInfo(accountId), null);
            }
            case "account_modified" -> {
                // Fetch account from DB and build payload with changeset for UPDATE
                Map<String, Object> cs = (Map<String, Object>) metadata.get("changeSet");
                yield new PayloadResult(fetchAndBuildAccountInfo(accountId), cs);
            }
            case "account_deleted" -> {
                // Fetch account from DB and build payload for DELETE
                // Note: Account may already be deleted, so this might return null
                Map<String, Object> info = fetchAndBuildAccountInfo(accountId);
                if (info == null) {
                    // If account not found (already deleted), create minimal payload with just ID
                    LoggingUtils.warn(logger,"⚠️ [ACCOUNT-EVENT-WORKER] Account {} not found in DB for DELETE event, creating minimal payload", accountId);
                    info = new HashMap<>();
                    info.put("id", accountId);
                }
                yield new PayloadResult(info, null);
            }
            default -> {
                LoggingUtils.error(logger, "❌ [ACCOUNT-EVENT-WORKER] Unknown payload type: {}", payloadType);
                throw new IllegalArgumentException("Unknown payload type: " + payloadType);
            }
        };
        
        accountInfo = result.accountInfo;
        changeSet = result.changeSet;
        
        if (accountInfo == null) {
            LoggingUtils.error(logger,"❌ [ACCOUNT-EVENT-WORKER] Failed to build account info for account {}", accountIdStr);
            throw new IllegalStateException("Failed to build account info for account " + accountIdStr);
        }

        LoggingUtils.info(logger, "📤 [ACCOUNT-EVENT-WORKER] Publishing {} event for account {}", payloadType, accountIdStr);
        
        // Map payloadType to eventType (account_created -> account.created, account_modified -> account.updated, account_deleted -> account.deleted)
        String eventType = switch (payloadType) {
            case "account_created" -> "account.created";
            case "account_modified" -> "account.updated";
            case "account_deleted" -> "account.deleted";
            default -> {
                LoggingUtils.error(logger, "❌ [ACCOUNT-EVENT-WORKER] Unknown payload type: {}", payloadType);
                throw new IllegalArgumentException("Unknown payload type: " + payloadType);
            }
        };
        
        // Publish to Kafka (no retry logic here - handled by BaseWorker)
        try {
            kafkaEventPublisher.publishAccountEvent(eventType, accountId, accountInfo);
        } catch (Exception e) {
            LoggingUtils.error(logger, "❌ [ACCOUNT-EVENT-WORKER] Failed to publish {} event for account {}: {}", 
                    eventType, accountIdStr, e.getMessage(), e);
            throw new RuntimeException("Failed to publish " + eventType + " event for account " + accountIdStr, e);
        }
    }
    
    /**
     * Fetch account from DB and build account info map.
     * Uses findByIdWithRelations to load Organization and Product in a single query.
     */
    private Map<String, Object> fetchAndBuildAccountInfo(Long accountId) {
        try {
            Optional<Account> accountOpt = accountRepository.findByIdWithRelations(accountId);
            if (accountOpt.isEmpty()) {
                LoggingUtils.warn(logger,"⚠️ [ACCOUNT-EVENT-WORKER] Account {} not found in database", accountId);
                return null;
            }
            
            Account account = accountOpt.get();
            
            // Log account details for debugging
            LoggingUtils.debug(logger, "📋 [ACCOUNT-EVENT-WORKER] Fetched account: id={}, domain={}, organizationId={}, productCode={}", 
                    account.getId(), 
                    account.getDomain(),
                    account.getOrganization() != null ? account.getOrganization().getId() : "null",
                    account.getProduct() != null ? account.getProduct().getCode() : "null");
            
            return buildAccountInfo(account);
        } catch (Exception e) {
            LoggingUtils.error(logger, "❌ [ACCOUNT-EVENT-WORKER] Failed to fetch account {} from DB: {}",
                        accountId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Build account info map from Account entity.
     */
    private Map<String, Object> buildAccountInfo(Account account) {
        Map<String, Object> accountInfo = new HashMap<>();
        if (account == null) {
            return accountInfo;
        }
        
        // Account ID (primary key)
        Long accountId = account.getId();
        accountInfo.put("id", accountId);
        accountInfo.put("domain", account.getDomain());
        
        // Fetch planCode from subscription
        String planCode = null;
        try {
            java.util.List<SubscriptionStatus> activeStatuses = java.util.Arrays.asList(
                SubscriptionStatus.TRIAL, 
                SubscriptionStatus.ACTIVE
            );
            planCode = subscriptionRepository.findPlanCodeByAccountId(accountId, activeStatuses)
                .orElse(null);
        } catch (Exception e) {
            LoggingUtils.warn(logger, "⚠️ [ACCOUNT-EVENT-WORKER] Failed to fetch planCode for account {}: {}", 
                accountId, e.getMessage());
        }
        accountInfo.put("plan", planCode);
        
        accountInfo.put("status", account.getStatus() != null ? account.getStatus().name() : null);
        accountInfo.put("industryType", account.getIndustryType());
        accountInfo.put("companyName", account.getCompanyName());
        
        // Include updated_at timestamp with nanosecond precision (used as version of the payload)
        if (account.getUpdatedAt() != null) {
            accountInfo.put("updated_at", account.getUpdatedAt().toString());
        } else {
            // If updated_at is null, use current timestamp with nanosecond precision
            accountInfo.put("updated_at", java.time.OffsetDateTime.now().toString());
        }
        
        if (account.getOrganization() != null) {
            accountInfo.put("organizationId", account.getOrganization().getId());
            accountInfo.put("organizationName", account.getOrganization().getName());
            accountInfo.put("organizationPrimaryDomain", account.getOrganization().getPrimaryDomain());
        }
        
        if (account.getProduct() != null) {
            accountInfo.put("productCode", account.getProduct().getCode());
            accountInfo.put("productName", account.getProduct().getName());
        }
        accountInfo.put("region" , account.getRegion());
        return accountInfo;
    }
    
    @Override
    public BatchJobType getJobType() {
        return BatchJobType.ACCOUNT_EVENT_PUBLISH;
    }
    
    /**
     * Helper class to hold payload result.
     */
    private static class PayloadResult {
        final Map<String, Object> accountInfo;
        final Map<String, Object> changeSet;
        
        PayloadResult(Map<String, Object> accountInfo, Map<String, Object> changeSet) {
            this.accountInfo = accountInfo;
            this.changeSet = changeSet;
        }
    }
}

