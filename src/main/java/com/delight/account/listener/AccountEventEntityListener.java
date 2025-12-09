package com.delight.account.listener;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.delight.account.event.AccountCreatedEvent;
import com.delight.account.event.AccountDeletedEvent;
import com.delight.account.event.AccountUpdatedEvent;
import com.delight.account.model.Account;
import com.delight.account.repository.AccountRepository;

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
    
    private static ApplicationEventPublisher getEventPublisher() {
        if (applicationContext == null) {
            return null;
        }
        try {
            // ApplicationEventPublisher is typically the ApplicationContext itself
            if (applicationContext instanceof ApplicationEventPublisher) {
                return (ApplicationEventPublisher) applicationContext;
            }
            // Fallback: try to get it as a bean
            return applicationContext.getBean(ApplicationEventPublisher.class);
        } catch (BeansException | IllegalStateException e) {
            logger.warn("⚠️ [ACCOUNT-EVENT] Failed to get ApplicationEventPublisher: {}", e.getMessage());
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
        ApplicationEventPublisher eventPublisher = getEventPublisher();
        if (eventPublisher == null) {
            logger.warn("⚠️ [ACCOUNT-EVENT] ApplicationEventPublisher not available, skipping event publication");
            return;
        }

        try {
            logger.debug("📤 [ACCOUNT-EVENT] Publishing AccountCreatedEvent for account: {}", account.getId());
            // Publish Spring event - will be handled by @TransactionalEventListener AFTER_COMMIT
            eventPublisher.publishEvent(new AccountCreatedEvent(account));
        } catch (Exception e) {
            logger.error("❌ [ACCOUNT-EVENT] Failed to publish AccountCreatedEvent for account {}: {}", 
                        account.getId(), e.getMessage(), e);
        }
    }

    @PostUpdate
    public void onAccountUpdated(Account account) {
        ApplicationEventPublisher eventPublisher = getEventPublisher();
        if (eventPublisher == null) {
            logger.warn("⚠️ [ACCOUNT-EVENT] ApplicationEventPublisher not available, skipping event publication");
            oldState.remove();
            return;
        }

        try {
            logger.debug("📤 [ACCOUNT-EVENT] Publishing AccountUpdatedEvent for account: {}", account.getId());
            
            // Get old state captured in @PreUpdate
            Map<String, Object> oldAccountState = oldState.get();
            Map<String, Object> newAccountState = buildAccountInfo(account);
            
            // Build changeset by comparing old and new state
            Map<String, Object> changeSet = buildChangeSet(oldAccountState, newAccountState);
            
            // Publish Spring event - will be handled by @TransactionalEventListener AFTER_COMMIT
            eventPublisher.publishEvent(new AccountUpdatedEvent(account, changeSet));
        } catch (Exception e) {
            logger.error("❌ [ACCOUNT-EVENT] Failed to publish AccountUpdatedEvent for account {}: {}", 
                        account.getId(), e.getMessage(), e);
        } finally {
            // Clean up ThreadLocal
            oldState.remove();
        }
    }

    @PostRemove
    public void onAccountDeleted(Account account) {
        ApplicationEventPublisher eventPublisher = getEventPublisher();
        if (eventPublisher == null) {
            logger.warn("⚠️ [ACCOUNT-EVENT] ApplicationEventPublisher not available, skipping event publication");
            return;
        }

        try {
            logger.debug("📤 [ACCOUNT-EVENT] Publishing AccountDeletedEvent for account: {}", account.getId());
            // Publish Spring event - will be handled by @TransactionalEventListener AFTER_COMMIT
            eventPublisher.publishEvent(new AccountDeletedEvent(account));
        } catch (Exception e) {
            logger.error("❌ [ACCOUNT-EVENT] Failed to publish AccountDeletedEvent for account {}: {}", 
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
        accountInfo.put("companyName", account.getCompanyName());
        accountInfo.put("region", account.getRegion());
        
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

