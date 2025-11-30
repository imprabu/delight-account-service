package com.delight.account.service.job.worker;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.delight.account.domain.BatchJobType;
import com.delight.account.service.job.JobWorker;

/**
 * Worker implementation for email notification jobs.
 * This is a sample worker that can be extended to send actual email notifications.
 */
@Component
public class EmailNotificationJobWorker implements JobWorker {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationJobWorker.class);
    
    @Override
    public void processJob(Long jobId, Long accountId, Map<String, Object> metadata) {
        logger.info("🔄 [EMAIL-WORKER] Processing email notification job {} for account {}", jobId, accountId);
        
        try {
            // Extract metadata
            String recipient = (String) metadata.getOrDefault("recipient", "");
            String subject = (String) metadata.getOrDefault("subject", "");
            String body = (String) metadata.getOrDefault("body", "");
            
            logger.info("📧 [EMAIL-WORKER] Sending email to: {}, subject: {}", recipient, subject);
            
            // TODO: Implement actual email sending logic here
            // For example:
            // emailService.sendEmail(recipient, subject, body);
            
            logger.info("✅ [EMAIL-WORKER] Successfully processed email notification job {} for account {}", 
                       jobId, accountId);
        } catch (Exception e) {
            logger.error("❌ [EMAIL-WORKER] Failed to process email notification job {} for account {}: {}", 
                        jobId, accountId, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public BatchJobType getJobType() {
        return BatchJobType.EMAIL_NOTIFICATION;
    }
}


