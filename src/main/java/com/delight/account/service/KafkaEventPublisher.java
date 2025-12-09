package com.delight.account.service;

import com.delight.account.dto.AccountEventMessage;
import com.delight.account.dto.MembershipEventMessage;
import com.delight.account.dto.UserEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing events directly to Kafka.
 * Replaces the HTTP-based MessagingService.
 */
@Service
public class KafkaEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String accountEventsTopic;
    private final String userEventsTopic;
    private final String userRoleEventsTopic;

    public KafkaEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topic.account-events}") String accountEventsTopic,
            @Value("${kafka.topic.user-events}") String userEventsTopic,
            @Value("${kafka.topic.user-role-events}") String userRoleEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.accountEventsTopic = accountEventsTopic;
        this.userEventsTopic = userEventsTopic;
        this.userRoleEventsTopic = userRoleEventsTopic;
    }

    /**
     * Publish an account event to Kafka.
     */
    public void publishAccountEvent(String eventType, Long accountId, Map<String, Object> accountData) {
        try {
            AccountEventMessage message = AccountEventMessage.builder()
                    .eventType(eventType)
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(OffsetDateTime.now())
                    .accountId(accountId)
                    .data(accountData)
                    .build();

            String key = accountId != null ? accountId.toString() : "unknown";
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(accountEventsTopic, key, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("✅ [KAFKA] Successfully published account event: type={}, accountId={}, offset={}",
                            eventType, accountId, result.getRecordMetadata().offset());
                } else {
                    logger.error("❌ [KAFKA] Failed to publish account event: type={}, accountId={}, error={}",
                            eventType, accountId, ex.getMessage(), ex);
                }
            });

            logger.debug("📤 [KAFKA] Publishing account event: type={}, accountId={}", eventType, accountId);
        } catch (Exception e) {
            logger.error("❌ [KAFKA] Exception publishing account event: type={}, accountId={}, error={}",
                    eventType, accountId, e.getMessage(), e);
            throw new RuntimeException("Failed to publish account event", e);
        }
    }

    /**
     * Publish a user event to Kafka.
     */
    public void publishUserEvent(String eventType, Long userId, Long accountId, Map<String, Object> userData) {
        try {
            UserEventMessage message = UserEventMessage.builder()
                    .eventType(eventType)
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(OffsetDateTime.now())
                    .userId(userId)
                    .accountId(accountId)
                    .data(userData)
                    .build();

            String key = userId != null ? userId.toString() : "unknown";
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(userEventsTopic, key, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("✅ [KAFKA] Successfully published user event: type={}, userId={}, accountId={}, offset={}",
                            eventType, userId, accountId, result.getRecordMetadata().offset());
                } else {
                    logger.error("❌ [KAFKA] Failed to publish user event: type={}, userId={}, accountId={}, error={}",
                            eventType, userId, accountId, ex.getMessage(), ex);
                }
            });

            logger.debug("📤 [KAFKA] Publishing user event: type={}, userId={}, accountId={}", eventType, userId, accountId);
        } catch (Exception e) {
            logger.error("❌ [KAFKA] Exception publishing user event: type={}, userId={}, accountId={}, error={}",
                    eventType, userId, accountId, e.getMessage(), e);
            throw new RuntimeException("Failed to publish user event", e);
        }
    }

    /**
     * Publish a membership event to Kafka.
     * Membership events go to the user_role_activities_v1 topic.
     */
    public void publishMembershipEvent(String eventType, Long userId, Long accountId, Map<String, Object> membershipData) {
        try {
            MembershipEventMessage message = MembershipEventMessage.builder()
                    .eventType(eventType)
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(OffsetDateTime.now())
                    .userId(userId)
                    .accountId(accountId)
                    .data(membershipData)
                    .build();

            // Membership events go to user_role_activities_v1 topic
            String key = userId != null ? userId.toString() : "unknown";
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(userRoleEventsTopic, key, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("✅ [KAFKA] Successfully published membership event: type={}, userId={}, accountId={}, offset={}",
                            eventType, userId, accountId, result.getRecordMetadata().offset());
                } else {
                    logger.error("❌ [KAFKA] Failed to publish membership event: type={}, userId={}, accountId={}, error={}",
                            eventType, userId, accountId, ex.getMessage(), ex);
                }
            });

            logger.debug("📤 [KAFKA] Publishing membership event: type={}, userId={}, accountId={}", eventType, userId, accountId);
        } catch (Exception e) {
            logger.error("❌ [KAFKA] Exception publishing membership event: type={}, userId={}, accountId={}, error={}",
                    eventType, userId, accountId, e.getMessage(), e);
            throw new RuntimeException("Failed to publish membership event", e);
        }
    }

    /**
     * Publish a user role change event to Kafka.
     * Role change events are published to the user_role_activities_v1 topic.
     */
    public void publishUserRoleChangeEvent(Long userId, Long accountId, Long oldRoleId, String oldRoleName, 
                                          Long newRoleId, String newRoleName, Map<String, Object> additionalData) {
        try {
            Map<String, Object> roleChangeData = new HashMap<>();
            if (additionalData != null) {
                roleChangeData.putAll(additionalData);
            }
            
            // Add role change specific data
            roleChangeData.put("oldRoleId", oldRoleId);
            roleChangeData.put("oldRoleName", oldRoleName);
            roleChangeData.put("newRoleId", newRoleId);
            roleChangeData.put("newRoleName", newRoleName);
            
            MembershipEventMessage message = MembershipEventMessage.builder()
                    .eventType("user.role.changed")
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(OffsetDateTime.now())
                    .userId(userId)
                    .accountId(accountId)
                    .data(roleChangeData)
                    .build();

            String key = userId != null ? userId.toString() : "unknown";
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(userRoleEventsTopic, key, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("✅ [KAFKA] Successfully published role change event: userId={}, accountId={}, oldRole={}, newRole={}, offset={}",
                            userId, accountId, oldRoleName, newRoleName, result.getRecordMetadata().offset());
                } else {
                    logger.error("❌ [KAFKA] Failed to publish role change event: userId={}, accountId={}, error={}",
                            userId, accountId, ex.getMessage(), ex);
                }
            });

            logger.debug("📤 [KAFKA] Publishing role change event: userId={}, accountId={}, oldRole={}, newRole={}", 
                    userId, accountId, oldRoleName, newRoleName);
        } catch (Exception e) {
            logger.error("❌ [KAFKA] Exception publishing role change event: userId={}, accountId={}, error={}",
                    userId, accountId, e.getMessage(), e);
            throw new RuntimeException("Failed to publish role change event", e);
        }
    }
}

