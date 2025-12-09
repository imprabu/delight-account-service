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
import com.delight.account.model.User;
import com.delight.account.repository.UserRepository;
import com.delight.account.service.KafkaEventPublisher;
import com.delight.account.service.job.BaseWorker;

/**
 * Worker implementation for publishing user events to Kafka.
 * 
 * Architecture:
 * - CREATE: Fetches user from DB and builds payload
 * - UPDATE: Fetches user from DB and uses changeset to build payload
 * - DELETE: Uses userInfo from metadata (captured before deletion)
 * 
 * Retry logic is handled by BaseWorker with exponential backoff.
 */
@Component
public class UserEventPublisherWorker extends BaseWorker {

    private static final Logger logger = LoggerFactory.getLogger(UserEventPublisherWorker.class);
    
    @Autowired
    private KafkaEventPublisher kafkaEventPublisher;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Process the user event job.
     * This method is called by BaseWorker.processJob() after retry logic setup.
     */
    @Override
    @Transactional(readOnly = true)
    protected void doProcessJob(Long jobId, Long userId, Map<String, Object> metadata) throws Exception {
        String eventType = (String) metadata.get("eventType");
        
        if (eventType == null) {
            LoggingUtils.error(logger, "❌ [USER-EVENT-WORKER] Missing required metadata: eventType");
            throw new IllegalArgumentException("Missing required metadata: eventType");
        }
        
        if (userId == null) {
            LoggingUtils.error(logger, "❌ [USER-EVENT-WORKER] User ID is missing");
            throw new IllegalArgumentException("User ID is missing");
        }
        
        // accountId will be extracted from userInfo if available
        Long accountId = null;
        
        Map<String, Object> userInfo;
        
        // Build payload based on event type
        if ("user.deleted".equals(eventType)) {
            // For DELETE, try to fetch user, but if not found, create minimal payload
            userInfo = fetchAndBuildUserInfo(userId);
            if (userInfo == null) {
                LoggingUtils.warn(logger, "⚠️ [USER-EVENT-WORKER] User {} not found in DB for DELETE event, creating minimal payload", userId);
                userInfo = new HashMap<>();
                userInfo.put("id", userId);
                // For deleted users, accountId might not be available
            }
        } else {
            // For CREATE and UPDATE, fetch user from DB
            userInfo = fetchAndBuildUserInfo(userId);
            if (userInfo == null) {
                LoggingUtils.error(logger, "❌ [USER-EVENT-WORKER] Failed to fetch user {} from DB", userId);
                throw new IllegalStateException("Failed to fetch user " + userId + " from DB");
            }
            
            // Use accountId from userInfo if available
            if (userInfo.containsKey("accountId")) {
                accountId = Long.valueOf(userInfo.get("accountId").toString());
            }
        }
        
        // Ensure accountId is set for deleted users if available in userInfo
        if (accountId == null && userInfo != null && userInfo.containsKey("accountId")) {
            accountId = Long.valueOf(userInfo.get("accountId").toString());
        }

        LoggingUtils.info(logger, "📤 [USER-EVENT-WORKER] Publishing {} event for user {}", eventType, userId);
        
        // Publish to Kafka (no retry logic here - handled by BaseWorker)
        try {
            kafkaEventPublisher.publishUserEvent(eventType, userId, accountId, userInfo);
        } catch (Exception e) {
            LoggingUtils.error(logger, "❌ [USER-EVENT-WORKER] Failed to publish {} event for user {}: {}", 
                    eventType, userId, e.getMessage(), e);
            throw new RuntimeException("Failed to publish " + eventType + " event for user " + userId, e);
        }
    }
    
    /**
     * Fetch user from DB and build user info map.
     * Uses findByIdWithAccount to eagerly load the account relationship.
     */
    private Map<String, Object> fetchAndBuildUserInfo(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findByIdWithAccount(userId);
            if (userOpt.isEmpty()) {
                LoggingUtils.warn(logger, "⚠️ [USER-EVENT-WORKER] User {} not found in database", userId);
                return null;
            }
            
            User user = userOpt.get();
            
            // Log user details for debugging
            LoggingUtils.debug(logger, "📋 [USER-EVENT-WORKER] Fetched user: id={}, accountId={}", 
                    user.getId(), 
                    user.getAccount() != null ? user.getAccount().getId() : "null");
            
            return buildUserInfo(user);
        } catch (Exception e) {
            LoggingUtils.error(logger, "❌ [USER-EVENT-WORKER] Failed to fetch user {} from DB: {}",
                        userId, e.getMessage(), e);
            return null;
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
        
        // User ID (primary key)
        Long userId = user.getId();
        userInfo.put("id", userId);
        userInfo.put("userName", user.getUserName());
        userInfo.put("emailAddress", user.getEmailAddress());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("companyName", user.getCompanyName());
        userInfo.put("phoneNumber", user.getPhoneNumber());
        userInfo.put("status", user.getStatus() != null ? user.getStatus().name() : null);
        
        // Account ID (from account_id foreign key)
        if (user.getAccount() != null) {
            Long accountId = user.getAccount().getId();
            userInfo.put("accountId", accountId);
            
            // Log warning if user ID equals account ID (unusual but possible)
            if (userId != null && userId.equals(accountId)) {
                LoggingUtils.warn(logger, "⚠️ [USER-EVENT-WORKER] User ID {} equals Account ID {} - this is unusual", 
                        userId, accountId);
            }
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
    
    @Override
    public BatchJobType getJobType() {
        return BatchJobType.USER_EVENT_PUBLISH;
    }
}

