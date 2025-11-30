package com.delight.account.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for messaging service event ingestion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessagingEventRequest {
    private String serviceName;
    private String topic;
    private String payloadType;
    private String accountId;
    private Map<String, Object> payload;
    private Map<String, Object> changeSet;
}


