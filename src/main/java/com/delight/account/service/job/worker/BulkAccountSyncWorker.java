package com.delight.account.service.job.worker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.delight.account.util.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.delight.account.domain.BatchJobType;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.service.KafkaEventPublisher;
import com.delight.account.service.job.BaseWorker;

/**
 * Worker implementation for bulk account synchronization.
 * Iterates over all active accounts and publishes account_modified events to the messaging service.
 * 
 * Retry logic is handled by BaseWorker with exponential backoff.
 * Note: Individual account sync failures are logged but don't fail the entire job.
 */
@Component
public class BulkAccountSyncWorker extends BaseWorker {
    private static final Logger logger = LoggerFactory.getLogger(BulkAccountSyncWorker.class);
    @Autowired
    private KafkaEventPublisher kafkaEventPublisher;
    
    @Autowired
    private AccountRepository accountRepository;
    
    /**
     * Process the bulk account sync job.
     * This method is called by BaseWorker.processJob() after retry logic setup.
     */
    @Override
    @Transactional(readOnly = true)
    protected void doProcessJob(Long jobId, Long accountIdParam, Map<String, Object> metadata) throws Exception {
        LoggingUtils.info(logger, "🔄 [BULK-ACCOUNT-SYNC] Starting bulk account sync job {}", jobId);
        
        // Fetch all active accounts with relations loaded
        List<Account> activeAccounts = accountRepository.findAllByStatusWithRelations(AccountStatus.ACTIVE);
        
        LoggingUtils.info(logger, "📊 [BULK-ACCOUNT-SYNC] Found {} active accounts to sync", activeAccounts.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        // Iterate over each active account and publish account_modified event
        for (Account account : activeAccounts) {
            try {
                Map<String, Object> accountInfo = buildAccountInfo(account);
                Long accountId = account.getId();
                String accountIdStr = accountId != null ? accountId.toString() : "unknown";
                LoggingUtils.debug(logger, "📤 [BULK-ACCOUNT-SYNC] Publishing account.updated event for account {}", accountIdStr);
                
                // Publish to Kafka (no retry - handled by BaseWorker if this job fails)
                try {
                    kafkaEventPublisher.publishAccountEvent("account.updated", accountId, accountInfo);
                    successCount++;
                    LoggingUtils.debug(logger, "✅ [BULK-ACCOUNT-SYNC] Successfully published account.updated event for account {}", accountIdStr);
                } catch (Exception e) {
                    failureCount++;
                    LoggingUtils.error(logger, "❌ [BULK-ACCOUNT-SYNC] Failed to publish account.updated event for account {}: {}", 
                            accountIdStr, e.getMessage(), e);
                }
            } catch (Exception e) {
                failureCount++;
                LoggingUtils.error(logger, "❌ [BULK-ACCOUNT-SYNC] Error processing account {}: {}", 
                                account.getId(), e.getMessage(), e);
                // Continue with next account - don't fail entire job for individual account failures
            }
        }
        
        LoggingUtils.info(logger, "✅ [BULK-ACCOUNT-SYNC] Bulk account sync job {} completed. Success: {}, Failed: {}, Total: {}", 
                       jobId, successCount, failureCount, activeAccounts.size());
        
        // Only throw exception if all accounts failed (or if there's a critical error)
        if (successCount == 0 && activeAccounts.size() > 0) {
            throw new RuntimeException("All account syncs failed for bulk sync job " + jobId);
        }
    }
    
    /**
     * Build account info map from Account entity.
     * Reuses the same logic as AccountEventPublisherWorker.
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
        accountInfo.put("plan", account.getPlan() != null ? account.getPlan().name() : null);
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
        accountInfo.put("region", account.getRegion());
        
        return accountInfo;
    }
    
    @Override
    public BatchJobType getJobType() {
        return BatchJobType.BULK_ACCOUNT_SYNC;
    }
}

