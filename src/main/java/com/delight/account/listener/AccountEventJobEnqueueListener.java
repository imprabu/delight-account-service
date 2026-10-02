package com.delight.account.listener;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.delight.account.event.AccountCreatedEvent;
import com.delight.account.event.AccountDeletedEvent;
import com.delight.account.event.AccountUpdatedEvent;
import com.delight.account.service.job.JobQueueService;

/**
 * Spring event listener that enqueues account event jobs AFTER transaction commits.
 * This ensures jobs are only enqueued if the transaction completes successfully.
 * 
 * Uses @TransactionalEventListener with AFTER_COMMIT phase to handle events
 * only after the transaction commits successfully.
 */
@Component
public class AccountEventJobEnqueueListener {

    private static final Logger logger = LoggerFactory.getLogger(AccountEventJobEnqueueListener.class);

    @Autowired
    private JobQueueService jobQueueService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAccountCreated(AccountCreatedEvent event) {
        try {
            logger.info("📤 [ACCOUNT-EVENT] Account created (after commit): {}", event.getAccount().getId());
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("payloadType", "account_created");
            
            jobQueueService.enqueueJob(
                com.delight.account.domain.BatchJobType.ACCOUNT_EVENT_PUBLISH,
                event.getAccount().getId(),
                event.getAccount().getId(),
                metadata
            );
        } catch (Exception e) {
            logger.error("❌ [ACCOUNT-EVENT] Failed to enqueue account_created event for account {}: {}", 
                        event.getAccount().getId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAccountUpdated(AccountUpdatedEvent event) {
        try {
            logger.info("📤 [ACCOUNT-EVENT] Account updated (after commit): {}", event.getAccount().getId());
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("payloadType", "account_modified");
            metadata.put("changeSet", event.getChangeSet());
            
            jobQueueService.enqueueJob(
                com.delight.account.domain.BatchJobType.ACCOUNT_EVENT_PUBLISH,
                event.getAccount().getId(),
                event.getAccount().getId(),
                metadata
            );
        } catch (Exception e) {
            logger.error("❌ [ACCOUNT-EVENT] Failed to enqueue account_modified event for account {}: {}", 
                        event.getAccount().getId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAccountDeleted(AccountDeletedEvent event) {
        try {
            logger.info("📤 [ACCOUNT-EVENT] Account deleted (after commit): {}", event.getAccount().getId());
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("payloadType", "account_deleted");
            
            jobQueueService.enqueueJob(
                com.delight.account.domain.BatchJobType.ACCOUNT_EVENT_PUBLISH,
                event.getAccount().getId(),
                event.getAccount().getId(),
                metadata
            );
        } catch (Exception e) {
            logger.error("❌ [ACCOUNT-EVENT] Failed to enqueue account_deleted event for account {}: {}", 
                        event.getAccount().getId(), e.getMessage(), e);
        }
    }
}






