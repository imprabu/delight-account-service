package com.delight.account.listener;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.delight.account.event.UserCreatedEvent;
import com.delight.account.event.UserDeletedEvent;
import com.delight.account.event.UserUpdatedEvent;
import com.delight.account.service.job.JobQueueService;

/**
 * Spring event listener that enqueues user event jobs AFTER transaction commits.
 * This ensures jobs are only enqueued if the transaction completes successfully.
 * 
 * Uses @TransactionalEventListener with AFTER_COMMIT phase to handle events
 * only after the transaction commits successfully.
 */
@Component
public class UserEventJobEnqueueListener {

    private static final Logger logger = LoggerFactory.getLogger(UserEventJobEnqueueListener.class);

    @Autowired
    private JobQueueService jobQueueService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserCreated(UserCreatedEvent event) {
        try {
            logger.info("📤 [USER-EVENT] User created (after commit): {}", event.getUser().getId());
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("eventType", "user.created");
            
            Long accountId = event.getUser().getAccount() != null ? event.getUser().getAccount().getId() : null;
            
            jobQueueService.enqueueJob(
                com.delight.account.domain.BatchJobType.USER_EVENT_PUBLISH,
                event.getUser().getId(),
                accountId,
                metadata
            );
        } catch (Exception e) {
            logger.error("❌ [USER-EVENT] Failed to enqueue user.created event for user {}: {}", 
                        event.getUser().getId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserUpdated(UserUpdatedEvent event) {
        try {
            logger.info("📤 [USER-EVENT] User updated (after commit): {}", event.getUser().getId());
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("eventType", "user.updated");
            metadata.put("changeSet", event.getChangeSet());
            
            Long accountId = event.getUser().getAccount() != null ? event.getUser().getAccount().getId() : null;
            
            jobQueueService.enqueueJob(
                com.delight.account.domain.BatchJobType.USER_EVENT_PUBLISH,
                event.getUser().getId(),
                accountId,
                metadata
            );
        } catch (Exception e) {
            logger.error("❌ [USER-EVENT] Failed to enqueue user.updated event for user {}: {}", 
                        event.getUser().getId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserDeleted(UserDeletedEvent event) {
        try {
            logger.info("📤 [USER-EVENT] User deleted (after commit): {}", event.getUser().getId());
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("eventType", "user.deleted");
            
            Long accountId = event.getUser().getAccount() != null ? event.getUser().getAccount().getId() : null;
            
            jobQueueService.enqueueJob(
                com.delight.account.domain.BatchJobType.USER_EVENT_PUBLISH,
                event.getUser().getId(),
                accountId,
                metadata
            );
        } catch (Exception e) {
            logger.error("❌ [USER-EVENT] Failed to enqueue user.deleted event for user {}: {}", 
                        event.getUser().getId(), e.getMessage(), e);
        }
    }
}





