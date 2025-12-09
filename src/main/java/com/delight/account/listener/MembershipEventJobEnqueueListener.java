package com.delight.account.listener;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.delight.account.event.MembershipAddedEvent;
import com.delight.account.event.MembershipRemovedEvent;
import com.delight.account.event.MembershipUpdatedEvent;
import com.delight.account.model.Membership;
import com.delight.account.service.KafkaEventPublisher;
import com.delight.account.service.job.JobQueueService;

/**
 * Spring event listener that enqueues membership event jobs AFTER transaction commits.
 * This ensures jobs are only enqueued if the transaction completes successfully.
 * 
 * Uses @TransactionalEventListener with AFTER_COMMIT phase to handle events
 * only after the transaction commits successfully.
 */
@Component
public class MembershipEventJobEnqueueListener {

    private static final Logger logger = LoggerFactory.getLogger(MembershipEventJobEnqueueListener.class);

    @Autowired
    private JobQueueService jobQueueService;
    
    @Autowired
    private KafkaEventPublisher kafkaEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMembershipAdded(MembershipAddedEvent event) {
        try {
            logger.info("📤 [MEMBERSHIP-EVENT] Membership added (after commit): {}", event.getMembership().getId());
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("eventType", "user.membership.added");
            
            Long userId = event.getMembership().getUser() != null ? event.getMembership().getUser().getId() : null;
            Long accountId = event.getMembership().getAccount() != null ? event.getMembership().getAccount().getId() : null;
            
            jobQueueService.enqueueJob(
                com.delight.account.domain.BatchJobType.MEMBERSHIP_EVENT_PUBLISH,
                userId != null ? userId : event.getMembership().getId(),
                accountId,
                metadata
            );
        } catch (Exception e) {
            logger.error("❌ [MEMBERSHIP-EVENT] Failed to enqueue user.membership.added event for membership {}: {}", 
                        event.getMembership().getId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMembershipUpdated(MembershipUpdatedEvent event) {
        try {
            logger.info("📤 [MEMBERSHIP-EVENT] Membership updated (after commit): {}", event.getMembership().getId());
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("eventType", "user.membership.updated");
            metadata.put("changeSet", event.getChangeSet());
            
            Long userId = event.getMembership().getUser() != null ? event.getMembership().getUser().getId() : null;
            Long accountId = event.getMembership().getAccount() != null ? event.getMembership().getAccount().getId() : null;
            
            // Check if role changed - if so, publish to user_role_activities_v1 topic
            Map<String, Object> changeSet = event.getChangeSet();
            if (changeSet != null && changeSet.containsKey("roleId")) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> roleChange = (Map<String, Object>) changeSet.get("roleId");
                    Object oldRoleIdObj = roleChange != null ? roleChange.get("old") : null;
                    Object newRoleIdObj = roleChange != null ? roleChange.get("new") : null;
                    
                    // Also check roleName change
                    Object oldRoleNameObj = changeSet.containsKey("roleName") && changeSet.get("roleName") instanceof Map ? 
                            ((Map<String, Object>) changeSet.get("roleName")).get("old") : null;
                    Object newRoleNameObj = changeSet.containsKey("roleName") && changeSet.get("roleName") instanceof Map ? 
                            ((Map<String, Object>) changeSet.get("roleName")).get("new") : null;
                    
                    Long oldRoleId = oldRoleIdObj != null ? Long.valueOf(oldRoleIdObj.toString()) : null;
                    Long newRoleId = newRoleIdObj != null ? Long.valueOf(newRoleIdObj.toString()) : null;
                    String oldRoleName = oldRoleNameObj != null ? oldRoleNameObj.toString() : null;
                    String newRoleName = newRoleNameObj != null ? newRoleNameObj.toString() : null;
                    
                    // If role actually changed, publish to role activities topic
                    if (oldRoleId != null && newRoleId != null && !oldRoleId.equals(newRoleId)) {
                        logger.info("📤 [ROLE-CHANGE] Role changed for membership {}: {} -> {}", 
                                event.getMembership().getId(), oldRoleName, newRoleName);
                        
                        // Build additional data with membership info
                        Map<String, Object> additionalData = new HashMap<>();
                        Membership membership = event.getMembership();
                        if (membership.getRole() != null) {
                            additionalData.put("roleId", membership.getRole().getId());
                            additionalData.put("roleName", membership.getRole().getName());
                        }
                        additionalData.put("membershipId", membership.getId());
                        additionalData.put("status", membership.getStatus() != null ? membership.getStatus().name() : null);
                        // Include updated_at timestamp with nanosecond precision (used as version of the payload)
                        if (membership.getUpdatedAt() != null) {
                            additionalData.put("updated_at", membership.getUpdatedAt().toString());
                        } else {
                            additionalData.put("updated_at", java.time.OffsetDateTime.now().toString());
                        }
                        
                        kafkaEventPublisher.publishUserRoleChangeEvent(
                                userId, 
                                accountId, 
                                oldRoleId, 
                                oldRoleName, 
                                newRoleId, 
                                newRoleName,
                                additionalData
                        );
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ [ROLE-CHANGE] Failed to publish role change event: {}", e.getMessage());
                }
            }
            
            jobQueueService.enqueueJob(
                com.delight.account.domain.BatchJobType.MEMBERSHIP_EVENT_PUBLISH,
                userId != null ? userId : event.getMembership().getId(),
                accountId,
                metadata
            );
        } catch (Exception e) {
            logger.error("❌ [MEMBERSHIP-EVENT] Failed to enqueue user.membership.updated event for membership {}: {}", 
                        event.getMembership().getId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMembershipRemoved(MembershipRemovedEvent event) {
        try {
            logger.info("📤 [MEMBERSHIP-EVENT] Membership removed (after commit): {}", event.getMembership().getId());
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("eventType", "user.membership.removed");
            
            Long userId = event.getMembership().getUser() != null ? event.getMembership().getUser().getId() : null;
            Long accountId = event.getMembership().getAccount() != null ? event.getMembership().getAccount().getId() : null;
            
            jobQueueService.enqueueJob(
                com.delight.account.domain.BatchJobType.MEMBERSHIP_EVENT_PUBLISH,
                userId != null ? userId : event.getMembership().getId(),
                accountId,
                metadata
            );
        } catch (Exception e) {
            logger.error("❌ [MEMBERSHIP-EVENT] Failed to enqueue user.membership.removed event for membership {}: {}", 
                        event.getMembership().getId(), e.getMessage(), e);
        }
    }
}

