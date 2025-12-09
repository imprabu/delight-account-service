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

import com.delight.account.event.MembershipAddedEvent;
import com.delight.account.event.MembershipRemovedEvent;
import com.delight.account.event.MembershipUpdatedEvent;
import com.delight.account.model.Membership;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreUpdate;

/**
 * JPA Entity Listener for publishing membership events to Kafka.
 * Publishes events asynchronously when Membership entities are created, updated, or deleted.
 * 
 * Architecture:
 * - CREATE/DELETE: Only pass membershipId, worker fetches membership from DB
 * - UPDATE: Pass membershipId + changeset, worker builds payload from changeset
 */
@Component
public class MembershipEventEntityListener implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(MembershipEventEntityListener.class);
    
    private static final ThreadLocal<Map<String, Object>> oldState = new ThreadLocal<>();
    
    private ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    private ApplicationEventPublisher getEventPublisher() {
        if (applicationContext == null) {
            return null;
        }
        // ApplicationContext extends ApplicationEventPublisher, so we can cast it directly
        return applicationContext;
    }

    @PreUpdate
    public void onPreUpdate(Membership membership) {
        // Capture old state before update for changeset comparison
        try {
            Map<String, Object> oldMembershipState = buildMembershipInfo(membership);
            oldState.set(oldMembershipState);
        } catch (Exception e) {
            logger.warn("⚠️ [MEMBERSHIP-EVENT] Failed to capture old state for membership {}: {}", 
                       membership.getId(), e.getMessage());
            oldState.set(new HashMap<>());
        }
    }

    @PostPersist
    public void onMembershipAdded(Membership membership) {
        ApplicationEventPublisher eventPublisher = getEventPublisher();
        if (eventPublisher == null) {
            logger.warn("⚠️ [MEMBERSHIP-EVENT] ApplicationEventPublisher not available, skipping event publication");
            return;
        }

        try {
            logger.debug("📤 [MEMBERSHIP-EVENT] Publishing MembershipAddedEvent for membership: {}", membership.getId());
            // Publish Spring event - will be handled by @TransactionalEventListener AFTER_COMMIT
            eventPublisher.publishEvent(new MembershipAddedEvent(membership));
        } catch (Exception e) {
            logger.error("❌ [MEMBERSHIP-EVENT] Failed to publish MembershipAddedEvent for membership {}: {}", 
                        membership.getId(), e.getMessage(), e);
        }
    }

    @PostUpdate
    public void onMembershipUpdated(Membership membership) {
        ApplicationEventPublisher eventPublisher = getEventPublisher();
        if (eventPublisher == null) {
            logger.warn("⚠️ [MEMBERSHIP-EVENT] ApplicationEventPublisher not available, skipping event publication");
            oldState.remove();
            return;
        }

        try {
            logger.debug("📤 [MEMBERSHIP-EVENT] Publishing MembershipUpdatedEvent for membership: {}", membership.getId());
            
            // Get old state captured in @PreUpdate
            Map<String, Object> oldMembershipState = oldState.get();
            Map<String, Object> newMembershipState = buildMembershipInfo(membership);
            
            // Build changeset by comparing old and new state
            Map<String, Object> changeSet = buildChangeSet(oldMembershipState, newMembershipState);
            
            // Publish Spring event - will be handled by @TransactionalEventListener AFTER_COMMIT
            eventPublisher.publishEvent(new MembershipUpdatedEvent(membership, changeSet));
        } catch (Exception e) {
            logger.error("❌ [MEMBERSHIP-EVENT] Failed to publish MembershipUpdatedEvent for membership {}: {}", 
                        membership.getId(), e.getMessage(), e);
        } finally {
            // Clean up ThreadLocal
            oldState.remove();
        }
    }

    @PostRemove
    public void onMembershipRemoved(Membership membership) {
        ApplicationEventPublisher eventPublisher = getEventPublisher();
        if (eventPublisher == null) {
            logger.warn("⚠️ [MEMBERSHIP-EVENT] ApplicationEventPublisher not available, skipping event publication");
            return;
        }

        try {
            logger.debug("📤 [MEMBERSHIP-EVENT] Publishing MembershipRemovedEvent for membership: {}", membership.getId());
            // Publish Spring event - will be handled by @TransactionalEventListener AFTER_COMMIT
            eventPublisher.publishEvent(new MembershipRemovedEvent(membership));
        } catch (Exception e) {
            logger.error("❌ [MEMBERSHIP-EVENT] Failed to publish MembershipRemovedEvent for membership {}: {}", 
                        membership.getId(), e.getMessage(), e);
        }
    }

    /**
     * Build membership info map from Membership entity.
     */
    private Map<String, Object> buildMembershipInfo(Membership membership) {
        Map<String, Object> membershipInfo = new HashMap<>();
        if (membership == null) {
            return membershipInfo;
        }
        
        membershipInfo.put("id", membership.getId());
        membershipInfo.put("status", membership.getStatus() != null ? membership.getStatus().name() : null);
        
        if (membership.getUser() != null) {
            membershipInfo.put("userId", membership.getUser().getId());
        }
        
        if (membership.getAccount() != null) {
            membershipInfo.put("accountId", membership.getAccount().getId());
        }
        
        if (membership.getRole() != null) {
            membershipInfo.put("roleId", membership.getRole().getId());
            membershipInfo.put("roleName", membership.getRole().getName());
        }
        
        if (membership.getInvitedBy() != null) {
            membershipInfo.put("invitedBy", membership.getInvitedBy().getId());
        }
        
        // Include timestamps
        if (membership.getCreatedAt() != null) {
            membershipInfo.put("created_at", membership.getCreatedAt().toString());
        }
        // Include updated_at timestamp with nanosecond precision (used as version of the payload)
        if (membership.getUpdatedAt() != null) {
            membershipInfo.put("updated_at", membership.getUpdatedAt().toString());
        } else {
            // If updated_at is null, use current timestamp with nanosecond precision
            membershipInfo.put("updated_at", java.time.OffsetDateTime.now().toString());
        }
        
        return membershipInfo;
    }

    /**
     * Build changeset by comparing old and new membership state.
     * Only includes fields that actually changed.
     */
    private Map<String, Object> buildChangeSet(Map<String, Object> oldState, Map<String, Object> newState) {
        Map<String, Object> changeSet = new HashMap<>();
        
        if (oldState == null || oldState.isEmpty()) {
            // If no old state, return empty changeset
            return changeSet;
        }
        
        // Compare each field and add to changeset if changed
        for (String key : newState.keySet()) {
            Object oldValue = oldState.get(key);
            Object newValue = newState.get(key);
            
            if (oldValue == null && newValue != null) {
                changeSet.put(key, Map.of("old", null, "new", newValue));
            } else if (oldValue != null && !oldValue.equals(newValue)) {
                changeSet.put(key, Map.of("old", oldValue, "new", newValue));
            }
        }
        
        return changeSet;
    }
}

