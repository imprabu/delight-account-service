package com.delight.account.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Account event message format matching the integration guide specification.
 * Format: { eventType, eventId, timestamp, accountId, data }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountEventMessage {
    private String eventType; // account.created, account.updated, account.deleted, account.status.changed
    private String eventId;
    private OffsetDateTime timestamp;
    private Long accountId;
    private Map<String, Object> data; // Account information payload
}

