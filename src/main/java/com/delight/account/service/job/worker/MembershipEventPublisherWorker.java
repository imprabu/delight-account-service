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
import com.delight.account.model.Membership;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.service.KafkaEventPublisher;
import com.delight.account.service.job.BaseWorker;

/**
 * Worker implementation for publishing membership events to Kafka.
 * 
 * Architecture:
 * - CREATE: Fetches membership from DB and builds payload
 * - UPDATE: Fetches membership from DB and uses changeset to build payload
 * - DELETE: Uses membershipInfo from metadata (captured before deletion)
 * 
 * Retry logic is handled by BaseWorker with exponential backoff.
 */
@Component
public class MembershipEventPublisherWorker extends BaseWorker {

    private static final Logger logger = LoggerFactory.getLogger(MembershipEventPublisherWorker.class);
    
    @Autowired
    private KafkaEventPublisher kafkaEventPublisher;
    
    @Autowired
    private MembershipRepository membershipRepository;
    
    /**
     * Process the membership event job.
     * This method is called by BaseWorker.processJob() after retry logic setup.
     */
    @Override
    @Transactional(readOnly = true)
    protected void doProcessJob(Long jobId, Long userId, Map<String, Object> metadata) throws Exception {
        String eventType = (String) metadata.get("eventType");
        
        if (eventType == null) {
            LoggingUtils.error(logger, "❌ [MEMBERSHIP-EVENT-WORKER] Missing required metadata: eventType");
            throw new IllegalArgumentException("Missing required metadata: eventType");
        }
        
        if (userId == null) {
            LoggingUtils.error(logger, "❌ [MEMBERSHIP-EVENT-WORKER] User ID is missing");
            throw new IllegalArgumentException("User ID is missing");
        }
        
        Long accountId = metadata.get("accountId") != null ? 
                Long.valueOf(metadata.get("accountId").toString()) : null;
        
        // For membership events, we need to find the membership by userId and accountId
        // Since we don't have membershipId in metadata, we'll need to fetch it
        // However, for REMOVED events, the membership might already be deleted
        Map<String, Object> membershipInfo;
        
        if ("user.membership.removed".equals(eventType)) {
            // For REMOVED, try to fetch membership, but if not found, create minimal payload
            membershipInfo = fetchAndBuildMembershipInfo(userId, accountId);
            if (membershipInfo == null) {
                LoggingUtils.warn(logger, "⚠️ [MEMBERSHIP-EVENT-WORKER] Membership not found for userId={}, accountId={} for REMOVED event, creating minimal payload", 
                        userId, accountId);
                membershipInfo = new HashMap<>();
                membershipInfo.put("userId", userId);
                if (accountId != null) {
                    membershipInfo.put("accountId", accountId);
                }
            }
        } else {
            // For ADDED and UPDATED, fetch membership from DB
            membershipInfo = fetchAndBuildMembershipInfo(userId, accountId);
            if (membershipInfo == null) {
                LoggingUtils.error(logger, "❌ [MEMBERSHIP-EVENT-WORKER] Failed to fetch membership for userId={}, accountId={}", userId, accountId);
                throw new IllegalStateException("Failed to fetch membership for userId=" + userId + ", accountId=" + accountId);
            }
            
            // Use accountId from membershipInfo if available
            if (accountId == null && membershipInfo.containsKey("accountId")) {
                accountId = Long.valueOf(membershipInfo.get("accountId").toString());
            }
        }

        LoggingUtils.info(logger, "📤 [MEMBERSHIP-EVENT-WORKER] Publishing {} event for userId={}, accountId={}", 
                eventType, userId, accountId);
        
        // Publish to Kafka (no retry logic here - handled by BaseWorker)
        try {
            kafkaEventPublisher.publishMembershipEvent(eventType, userId, accountId, membershipInfo);
        } catch (Exception e) {
            LoggingUtils.error(logger, "❌ [MEMBERSHIP-EVENT-WORKER] Failed to publish {} event for userId={}, accountId={}: {}", 
                    eventType, userId, accountId, e.getMessage(), e);
            throw new RuntimeException("Failed to publish " + eventType + " event for userId=" + userId + ", accountId=" + accountId, e);
        }
    }
    
    /**
     * Fetch membership from DB and build membership info map.
     */
    private Map<String, Object> fetchAndBuildMembershipInfo(Long userId, Long accountId) {
        try {
            if (accountId == null) {
                LoggingUtils.warn(logger, "⚠️ [MEMBERSHIP-EVENT-WORKER] AccountId is null, cannot fetch membership");
                return null;
            }
            
            Optional<Membership> membershipOpt = membershipRepository.findByUserIdAndAccountIdWithRole(userId, accountId);
            if (membershipOpt.isEmpty()) {
                LoggingUtils.warn(logger, "⚠️ [MEMBERSHIP-EVENT-WORKER] Membership not found for userId={}, accountId={}", userId, accountId);
                return null;
            }
            
            Membership membership = membershipOpt.get();
            
            // Log membership details for debugging
            LoggingUtils.debug(logger, "📋 [MEMBERSHIP-EVENT-WORKER] Fetched membership: id={}, userId={}, accountId={}", 
                    membership.getId(), 
                    membership.getUser() != null ? membership.getUser().getId() : "null",
                    membership.getAccount() != null ? membership.getAccount().getId() : "null");
            
            return buildMembershipInfo(membership);
        } catch (Exception e) {
            LoggingUtils.error(logger, "❌ [MEMBERSHIP-EVENT-WORKER] Failed to fetch membership for userId={}, accountId={}: {}",
                        userId, accountId, e.getMessage(), e);
            return null;
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
        
        // Membership ID (primary key of memberships table)
        Long membershipId = membership.getId();
        membershipInfo.put("id", membershipId);
        membershipInfo.put("status", membership.getStatus() != null ? membership.getStatus().name() : null);
        
        // User ID (from user_id foreign key)
        if (membership.getUser() != null) {
            Long userId = membership.getUser().getId();
            membershipInfo.put("userId", userId);
            
            // Log warning if membership ID equals user ID (unusual but possible)
            if (membershipId != null && membershipId.equals(userId)) {
                LoggingUtils.warn(logger, "⚠️ [MEMBERSHIP-EVENT-WORKER] Membership ID {} equals User ID {} - this is unusual", 
                        membershipId, userId);
            }
        }
        
        // Account ID (from account_id foreign key)
        if (membership.getAccount() != null) {
            Long accountId = membership.getAccount().getId();
            membershipInfo.put("accountId", accountId);
            
            // Log warning if membership ID equals account ID (unusual but possible)
            if (membershipId != null && membershipId.equals(accountId)) {
                LoggingUtils.warn(logger, "⚠️ [MEMBERSHIP-EVENT-WORKER] Membership ID {} equals Account ID {} - this is unusual", 
                        membershipId, accountId);
            }
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
    
    @Override
    public BatchJobType getJobType() {
        return BatchJobType.MEMBERSHIP_EVENT_PUBLISH;
    }
}

