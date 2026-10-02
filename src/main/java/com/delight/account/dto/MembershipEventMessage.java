package com.delight.account.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Membership event message format matching the integration guide specification.
 * Format: { eventType, eventId, timestamp, userId, accountId, data }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MembershipEventMessage {
    private String eventType; // user.membership.added, user.membership.removed, user.membership.updated
    private String eventId;
    private OffsetDateTime timestamp;
    private Long userId;
    private Long accountId;
    private Map<String, Object> data; // Membership information payload
}





