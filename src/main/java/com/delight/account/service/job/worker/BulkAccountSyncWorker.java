package com.delight.account.service.job.worker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.delight.account.domain.BatchJobType;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.service.MessagingService;
import com.delight.account.service.job.JobWorker;

/**
 * Worker implementation for bulk account synchronization.
 * Iterates over all active accounts and publishes account_modified events to the messaging service.
 */
@Component
public class BulkAccountSyncWorker implements JobWorker {
    
    private static final Logger logger = LoggerFactory.getLogger(BulkAccountSyncWorker.class);
    
    @Autowired
    private MessagingService messagingService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Override
    @Transactional(readOnly = true)
    public void processJob(Long jobId, Long accountId, Map<String, Object> metadata) {
        logger.info("🔄 [BULK-ACCOUNT-SYNC] Starting bulk account sync job {}", jobId);
        
        try {
            // Fetch all active accounts with relations loaded
            List<Account> activeAccounts = accountRepository.findAllByStatusWithRelations(AccountStatus.ACTIVE);
            
            logger.info("📊 [BULK-ACCOUNT-SYNC] Found {} active accounts to sync", activeAccounts.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            // Iterate over each active account and publish account_modified event
            for (Account account : activeAccounts) {
                try {
                    Map<String, Object> accountInfo = buildAccountInfo(account);
                    
                    // For bulk sync, we don't have a changeset (no actual change occurred)
                    // We just publish the current state as account_modified
                    Map<String, Object> emptyChangeSet = new HashMap<>();
                    
                    String accountIdStr = account.getId().toString();
                    logger.debug("📤 [BULK-ACCOUNT-SYNC] Publishing account_modified event for account {}", accountIdStr);
                    
                    boolean success = messagingService.publishAccountEventWithRetry(
                        "account_modified",
                        accountIdStr,
                        accountInfo,
                        emptyChangeSet
                    );
                    
                    if (success) {
                        successCount++;
                        logger.debug("✅ [BULK-ACCOUNT-SYNC] Successfully published account_modified event for account {}", accountIdStr);
                    } else {
                        failureCount++;
                        logger.error("❌ [BULK-ACCOUNT-SYNC] Failed to publish account_modified event for account {} after retries", accountIdStr);
                    }
                } catch (Exception e) {
                    failureCount++;
                    logger.error("❌ [BULK-ACCOUNT-SYNC] Error processing account {}: {}", 
                                account.getId(), e.getMessage(), e);
                }
            }
            
            logger.info("✅ [BULK-ACCOUNT-SYNC] Bulk account sync job {} completed. Success: {}, Failed: {}, Total: {}", 
                       jobId, successCount, failureCount, activeAccounts.size());
        } catch (Exception e) {
            logger.error("❌ [BULK-ACCOUNT-SYNC] Failed to process bulk account sync job {}: {}", 
                        jobId, e.getMessage(), e);
            throw e;
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
        
        accountInfo.put("id", account.getId());
        accountInfo.put("domain", account.getDomain());
        accountInfo.put("plan", account.getPlan() != null ? account.getPlan().name() : null);
        accountInfo.put("status", account.getStatus() != null ? account.getStatus().name() : null);
        accountInfo.put("industryType", account.getIndustryType());
        accountInfo.put("email", account.getEmail());
        accountInfo.put("companyName", account.getCompanyName());
        
        // Include updated_at timestamp with nanosecond precision (stored as microseconds in DB)
        if (account.getUpdatedAt() != null) {
            accountInfo.put("updatedAt", account.getUpdatedAt().toString());
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
        
        return accountInfo;
    }
    
    @Override
    public BatchJobType getJobType() {
        return BatchJobType.BULK_ACCOUNT_SYNC;
    }
}

