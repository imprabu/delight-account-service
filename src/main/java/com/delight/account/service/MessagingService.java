package com.delight.account.service;

import com.delight.account.config.MessagingProperties;
import com.delight.account.dto.MessagingEventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service for publishing events to the messaging service.
 */
@Service
public class MessagingService {

    private static final Logger logger = LoggerFactory.getLogger(MessagingService.class);

    private final MessagingProperties messagingProperties;
    private final RestTemplate restTemplate;

    public MessagingService(MessagingProperties messagingProperties, RestTemplate restTemplate) {
        this.messagingProperties = messagingProperties;
        this.restTemplate = restTemplate;
    }

    /**
     * Publish an account event to the messaging service.
     * 
     * @param payloadType The type of event (account_created, account_modified, account_deleted)
     * @param accountId The account ID
     * @param accountInfo The account information payload
     * @param changeSet Optional change set for modified events
     * @return true if successful, false otherwise
     */
    public boolean publishAccountEvent(String payloadType, String accountId, 
                                       Map<String, Object> accountInfo, 
                                       Map<String, Object> changeSet) {
        MessagingProperties.TopicsConfig.AccountActivitiesConfig config = 
            messagingProperties.getTopics().getAccountActivities();

        // Check if payload type is enabled
        if (!config.isPayloadTypeEnabled(payloadType)) {
            logger.info("📤 [MESSAGING] Payload type '{}' is disabled, skipping event publication", payloadType);
            return true; // Return true as it's intentionally skipped
        }

        try {
            String url = messagingProperties.getService().getBaseUrl() + 
                        messagingProperties.getService().getEndpoint();

            MessagingEventRequest request = MessagingEventRequest.builder()
                .serviceName(config.getServiceName())
                .topic(config.getTopicName())
                .payloadType(payloadType)
                .accountId(accountId)
                .payload(accountInfo)
                .changeSet(changeSet)
                .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(messagingProperties.getService().getJwtToken());

            HttpEntity<MessagingEventRequest> entity = new HttpEntity<>(request, headers);

            logger.info("📤 [MESSAGING] Publishing {} event for account {} to {}", 
                       payloadType, accountId, url);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("✅ [MESSAGING] Successfully published {} event for account {}", 
                           payloadType, accountId);
                return true;
            } else {
                logger.warn("⚠️ [MESSAGING] Received non-2xx response for {} event: {}", 
                           payloadType, response.getStatusCode());
                return false;
            }
        } catch (RestClientException e) {
            logger.error("❌ [MESSAGING] Failed to publish {} event for account {}: {}", 
                        payloadType, accountId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Publish an account event with retry logic.
     */
    public boolean publishAccountEventWithRetry(String payloadType, String accountId, 
                                                Map<String, Object> accountInfo, 
                                                Map<String, Object> changeSet) {
        MessagingProperties.ServiceConfig.RetryConfig retryConfig = 
            messagingProperties.getService().getRetry();
        
        int maxAttempts = retryConfig.getMaxAttempts();
        long delayMs = retryConfig.getDelayMs();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            boolean success = publishAccountEvent(payloadType, accountId, accountInfo, changeSet);
            if (success) {
                return true;
            }

            if (attempt < maxAttempts) {
                logger.info("🔄 [MESSAGING] Retrying {} event for account {} (attempt {}/{})", 
                           payloadType, accountId, attempt + 1, maxAttempts);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("❌ [MESSAGING] Retry interrupted for {} event", payloadType);
                    return false;
                }
            }
        }

        logger.error("❌ [MESSAGING] Failed to publish {} event after {} attempts", 
                    payloadType, maxAttempts);
        return false;
    }
}


