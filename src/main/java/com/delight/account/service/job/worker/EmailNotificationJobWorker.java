package com.delight.account.service.job.worker;

import java.util.Map;

import com.delight.account.util.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.delight.account.domain.BatchJobType;
import com.delight.account.service.NotificationService;
import com.delight.account.service.job.BaseWorker;

/**
 * Worker implementation for email notification jobs.
 * Uses SendGrid to send email notifications asynchronously.
 * 
 * Retry logic is handled by BaseWorker with exponential backoff.
 */
@Component
public class EmailNotificationJobWorker extends BaseWorker {
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationJobWorker.class);
    private final NotificationService notificationService;
    
    public EmailNotificationJobWorker(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * Process the email notification job.
     * This method is called by BaseWorker.processJob() after retry logic setup.
     * 
     * Metadata can contain:
     * - "recipient": email address (required)
     * - "subject": email subject (for simple emails)
     * - "body": email content (for simple emails)
     * - "templateId": SendGrid template ID (for templated emails)
     * - "templateData": Map of template data (for templated emails)
     * - "type": "simple" or "templated" (default: "simple")
     */
    @Override
    protected void doProcessJob(Long jobId, Long accountId, Map<String, Object> metadata) throws Exception {
        // Extract metadata
        String recipient = (String) metadata.getOrDefault("recipient", "");
        String type = (String) metadata.getOrDefault("type", "simple");
        
        if (recipient == null || recipient.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email address is required");
        }
        
        LoggingUtils.info(logger,"📧 [EMAIL-WORKER] Processing email job {} for account {} to: {}", jobId, accountId, recipient);
        
        if ("templated".equals(type)) {
            // Send templated email
            String templateId = (String) metadata.get("templateId");
            if (templateId == null || templateId.trim().isEmpty()) {
                throw new IllegalArgumentException("Template ID is required for templated emails");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> templateData = (Map<String, Object>) metadata.get("templateData");
            
            notificationService.sendTemplatedEmail(recipient, templateId, templateData);
        } else {
            // Send simple email
            String subject = (String) metadata.getOrDefault("subject", "");
            String body = (String) metadata.getOrDefault("body", "");
            
            if (subject == null || subject.trim().isEmpty()) {
                throw new IllegalArgumentException("Subject is required for simple emails");
            }
            
            notificationService.sendEmail(recipient, subject, body);
        }
        
        LoggingUtils.info(logger,"📧 [EMAIL-WORKER] Email job {} completed successfully", jobId);
    }
    
    @Override
    public BatchJobType getJobType() {
        return BatchJobType.EMAIL_NOTIFICATION;
    }
}


