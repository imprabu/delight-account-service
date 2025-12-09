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

import com.delight.account.event.UserCreatedEvent;
import com.delight.account.event.UserDeletedEvent;
import com.delight.account.event.UserUpdatedEvent;
import com.delight.account.model.User;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreUpdate;

/**
 * JPA Entity Listener for publishing user events to Kafka.
 * Publishes events asynchronously when User entities are created, updated, or deleted.
 * 
 * Architecture:
 * - CREATE/DELETE: Only pass userId, worker fetches user from DB
 * - UPDATE: Pass userId + changeset, worker builds payload from changeset
 */
@Component
public class UserEventEntityListener implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(UserEventEntityListener.class);
    
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
    public void onPreUpdate(User user) {
        // Capture old state before update for changeset comparison
        try {
            Map<String, Object> oldUserState = buildUserInfo(user);
            oldState.set(oldUserState);
        } catch (Exception e) {
            logger.warn("⚠️ [USER-EVENT] Failed to capture old state for user {}: {}", 
                       user.getId(), e.getMessage());
            oldState.set(new HashMap<>());
        }
    }

    @PostPersist
    public void onUserCreated(User user) {
        ApplicationEventPublisher eventPublisher = getEventPublisher();
        if (eventPublisher == null) {
            logger.warn("⚠️ [USER-EVENT] ApplicationEventPublisher not available, skipping event publication");
            return;
        }

        try {
            logger.debug("📤 [USER-EVENT] Publishing UserCreatedEvent for user: {}", user.getId());
            // Publish Spring event - will be handled by @TransactionalEventListener AFTER_COMMIT
            eventPublisher.publishEvent(new UserCreatedEvent(user));
        } catch (Exception e) {
            logger.error("❌ [USER-EVENT] Failed to publish UserCreatedEvent for user {}: {}", 
                        user.getId(), e.getMessage(), e);
        }
    }

    @PostUpdate
    public void onUserUpdated(User user) {
        ApplicationEventPublisher eventPublisher = getEventPublisher();
        if (eventPublisher == null) {
            logger.warn("⚠️ [USER-EVENT] ApplicationEventPublisher not available, skipping event publication");
            oldState.remove();
            return;
        }

        try {
            logger.debug("📤 [USER-EVENT] Publishing UserUpdatedEvent for user: {}", user.getId());
            
            // Get old state captured in @PreUpdate
            Map<String, Object> oldUserState = oldState.get();
            Map<String, Object> newUserState = buildUserInfo(user);
            
            // Build changeset by comparing old and new state
            Map<String, Object> changeSet = buildChangeSet(oldUserState, newUserState);
            
            // Publish Spring event - will be handled by @TransactionalEventListener AFTER_COMMIT
            eventPublisher.publishEvent(new UserUpdatedEvent(user, changeSet));
        } catch (Exception e) {
            logger.error("❌ [USER-EVENT] Failed to publish UserUpdatedEvent for user {}: {}", 
                        user.getId(), e.getMessage(), e);
        } finally {
            // Clean up ThreadLocal
            oldState.remove();
        }
    }

    @PostRemove
    public void onUserDeleted(User user) {
        ApplicationEventPublisher eventPublisher = getEventPublisher();
        if (eventPublisher == null) {
            logger.warn("⚠️ [USER-EVENT] ApplicationEventPublisher not available, skipping event publication");
            return;
        }

        try {
            logger.debug("📤 [USER-EVENT] Publishing UserDeletedEvent for user: {}", user.getId());
            // Publish Spring event - will be handled by @TransactionalEventListener AFTER_COMMIT
            eventPublisher.publishEvent(new UserDeletedEvent(user));
        } catch (Exception e) {
            logger.error("❌ [USER-EVENT] Failed to publish UserDeletedEvent for user {}: {}", 
                        user.getId(), e.getMessage(), e);
        }
    }

    /**
     * Build user info map from User entity.
     */
    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        if (user == null) {
            return userInfo;
        }
        
        userInfo.put("id", user.getId());
        userInfo.put("userName", user.getUserName());
        userInfo.put("emailAddress", user.getEmailAddress());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("companyName", user.getCompanyName());
        userInfo.put("phoneNumber", user.getPhoneNumber());
        userInfo.put("status", user.getStatus() != null ? user.getStatus().name() : null);
        
        if (user.getAccount() != null) {
            userInfo.put("accountId", user.getAccount().getId());
        }
        
        // Include updated_at timestamp with nanosecond precision (used as version of the payload)
        if (user.getUpdatedAt() != null) {
            userInfo.put("updated_at", user.getUpdatedAt().toString());
        } else {
            // If updated_at is null, use current timestamp with nanosecond precision
            userInfo.put("updated_at", java.time.OffsetDateTime.now().toString());
        }
        
        return userInfo;
    }

    /**
     * Build changeset by comparing old and new user state.
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

