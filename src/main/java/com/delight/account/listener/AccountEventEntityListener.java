package com.delight.account.listener;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.delight.account.model.Account;
import com.delight.account.repository.AccountRepository;
import com.delight.account.service.job.JobQueueService;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreUpdate;

/**
 * JPA Entity Listener for publishing account events to messaging service.
 * Publishes events asynchronously when Account entities are created, updated, or deleted.
 * 
 * Architecture:
 * - CREATE/DELETE: Only pass accountId, worker fetches account from DB
 * - UPDATE: Pass accountId + changeset, worker builds payload from changeset
 */
@Component
public class AccountEventEntityListener implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(AccountEventEntityListener.class);

    private static ApplicationContext applicationContext;
    
    // ThreadLocal to store old state for UPDATE events
    private static final ThreadLocal<Map<String, Object>> oldState = new ThreadLocal<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        AccountEventEntityListener.applicationContext = applicationContext;
    }

    private static JobQueueService getJobQueueService() {
        if (applicationContext == null) {
            return null;
        }
        try {
            return applicationContext.getBean(JobQueueService.class);
        } catch (BeansException | IllegalStateException e) {
            logger.warn("⚠️ [ACCOUNT-EVENT] Failed to get JobQueueService bean: {}", e.getMessage());
            return null;
        }
    }

    private static AccountRepository getAccountRepository() {
        if (applicationContext == null) {
            return null;
        }
        try {
            return applicationContext.getBean(AccountRepository.class);
        } catch (BeansException | IllegalStateException e) {
            logger.warn("⚠️ [ACCOUNT-EVENT] Failed to get AccountRepository bean: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Capture old state before update to build changeset.
     * Fetches the old state from the database since the entity already has new values.
     */
    @PreUpdate
    public void onPreUpdate(Account account) {
        try {
            // Fetch old state from database since entity already has new values
            AccountRepository accountRepository = getAccountRepository();
            if (accountRepository == null || account.getId() == null) {
                logger.warn("⚠️ [ACCOUNT-EVENT] Cannot fetch old state - repository or account ID is null");
                oldState.remove();
                return;
            }
            
            accountRepository.findById(account.getId())
                .ifPresentOrElse(
                    oldAccount -> {
                        Map<String, Object> oldAccountState = buildAccountInfo(oldAccount);
                        oldState.set(oldAccountState);
                        logger.debug("📝 [ACCOUNT-EVENT] Captured old state for account {} before update", account.getId());
                    },
                    () -> {
                        logger.warn("⚠️ [ACCOUNT-EVENT] Account {} not found in database for old state capture", account.getId());
                        oldState.remove();
                    }
                );
        } catch (Exception e) {
            logger.warn("⚠️ [ACCOUNT-EVENT] Failed to capture old state for account {}: {}", 
                       account.getId(), e.getMessage());
            oldState.remove();
        }
    }

    @PostPersist
    public void onAccountCreated(Account account) {
        JobQueueService jobQueueService = getJobQueueService();
        if (jobQueueService == null) {
            logger.warn("⚠️ [ACCOUNT-EVENT] JobQueueService not available, skipping event publication");
            return;
        }

        try {
            logger.info("📤 [ACCOUNT-EVENT] Account created: {}", account.getId());
            
            // Only pass accountId - worker will fetch account from DB
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("payloadType", "account_created");
            
            jobQueueService.enqueueJob(
                com.delight.account.domain.BatchJobType.ACCOUNT_EVENT_PUBLISH,
                account.getId(),
                account.getId(),
                metadata
            );
        } catch (Exception e) {
            logger.error("❌ [ACCOUNT-EVENT] Failed to enqueue account_created event for account {}: {}", 
                        account.getId(), e.getMessage(), e);
        }
    }

    @PostUpdate
    public void onAccountUpdated(Account account) {
        JobQueueService jobQueueService = getJobQueueService();
        if (jobQueueService == null) {
            logger.warn("⚠️ [ACCOUNT-EVENT] JobQueueService not available, skipping event publication");
            oldState.remove();
            return;
        }

        try {
            logger.info("📤 [ACCOUNT-EVENT] Account updated: {}", account.getId());
            
            // Get old state captured in @PreUpdate
            Map<String, Object> oldAccountState = oldState.get();
            Map<String, Object> newAccountState = buildAccountInfo(account);
            
            // Build changeset by comparing old and new state
            Map<String, Object> changeSet = buildChangeSet(oldAccountState, newAccountState);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("payloadType", "account_modified");
            metadata.put("changeSet", changeSet);
            
            jobQueueService.enqueueJob(
                com.delight.account.domain.BatchJobType.ACCOUNT_EVENT_PUBLISH,
                account.getId(),
                account.getId(),
                metadata
            );
        } catch (Exception e) {
            logger.error("❌ [ACCOUNT-EVENT] Failed to enqueue account_modified event for account {}: {}", 
                        account.getId(), e.getMessage(), e);
        } finally {
            // Clean up ThreadLocal
            oldState.remove();
        }
    }

    @PostRemove
    public void onAccountDeleted(Account account) {
        JobQueueService jobQueueService = getJobQueueService();
        if (jobQueueService == null) {
            logger.warn("⚠️ [ACCOUNT-EVENT] JobQueueService not available, skipping event publication");
            return;
        }

        try {
            logger.info("📤 [ACCOUNT-EVENT] Account deleted: {}", account.getId());
            
            // Only pass accountId - worker will fetch account from DB
            // Note: Account may already be deleted from DB, but worker will try to fetch it
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("payloadType", "account_deleted");
            
            jobQueueService.enqueueJob(
                com.delight.account.domain.BatchJobType.ACCOUNT_EVENT_PUBLISH,
                account.getId(),
                account.getId(),
                metadata
            );
        } catch (Exception e) {
            logger.error("❌ [ACCOUNT-EVENT] Failed to enqueue account_deleted event for account {}: {}", 
                        account.getId(), e.getMessage(), e);
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

    /**
     * Build changeset by comparing old and new account state.
     * Only includes fields that actually changed.
     */
    private Map<String, Object> buildChangeSet(Map<String, Object> oldState, Map<String, Object> newState) {
        Map<String, Object> changeSet = new HashMap<>();
        
        if (oldState == null || newState == null) {
            logger.warn("⚠️ [ACCOUNT-EVENT] Cannot build changeset - oldState or newState is null");
            return changeSet;
        }
        
        // Compare each field and add to changeset if changed
        for (String key : newState.keySet()) {
            Object oldValue = oldState.get(key);
            Object newValue = newState.get(key);
            
            // Check if value changed (handling null cases)
            if (oldValue == null && newValue != null) {
                changeSet.put(key, Map.of("old", null, "new", newValue));
            } else if (oldValue != null && newValue == null) {
                changeSet.put(key, Map.of("old", oldValue, "new", null));
            } else if (oldValue != null && !oldValue.equals(newValue)) {
                changeSet.put(key, Map.of("old", oldValue, "new", newValue));
            }
        }
        
        return changeSet;
    }
}

