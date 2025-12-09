package com.delight.account.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * User event message format matching the integration guide specification.
 * Format: { eventType, eventId, timestamp, userId, accountId, data }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserEventMessage {
    private String eventType; // user.created, user.updated, user.deleted, user.status.changed
    private String eventId;
    private OffsetDateTime timestamp;
    private Long userId;
    private Long accountId;
    private Map<String, Object> data; // User information payload
}

