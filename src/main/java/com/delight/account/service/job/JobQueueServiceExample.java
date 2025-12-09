package com.delight.account.service.job;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.delight.account.domain.BatchJobType;

/**
 * Example service demonstrating how to use JobQueueService to enqueue async jobs.
 * 
 * Usage examples:
 * 
 * 1. Enqueue a job immediately:
 *    jobQueueService.enqueueJob(BatchJobType.EMAIL_NOTIFICATION, userId, accountId, metadata);
 * 
 * 2. Enqueue a job with delay:
 *    jobQueueService.enqueueJobWithDelay(BatchJobType.EMAIL_NOTIFICATION, userId, accountId, 60, metadata);
 * 
 * 3. Schedule a recurring job:
 *    jobQueueService.scheduleRecurringJob(BatchJobType.DATA_CLEANUP, "cleanup-job-1", "0 0 2 * * ?", accountId, metadata);
 * 
 * 4. Using string job type:
 *    jobQueueService.enqueueJob("email_notification", userId, accountId, metadata);
 * 
 * This class is for documentation purposes only and can be removed.
 */
@Service
public class JobQueueServiceExample {

    @Autowired
    private JobQueueService jobQueueService;

    /**
     * Example: Enqueue an email notification job after user signup
     */
    public void sendWelcomeEmailAsync(Long userId, Long accountId, String emailAddress) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("recipient", emailAddress);
        metadata.put("subject", "Welcome to Delight!");
        metadata.put("body", "Thank you for signing up!");
        metadata.put("template", "welcome-email");
        
        // Enqueue the job - it will be processed asynchronously
        jobQueueService.enqueueJob(BatchJobType.EMAIL_NOTIFICATION, userId, accountId, metadata);
    }

    /**
     * Example: Enqueue a delayed job (e.g., send reminder after 24 hours)
     */
    public void sendReminderEmailAsync(Long userId, Long accountId, String emailAddress, int delayInHours) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("recipient", emailAddress);
        metadata.put("subject", "Don't forget to complete your profile!");
        metadata.put("body", "Complete your profile to get started.");
        
        int delayInSeconds = delayInHours * 3600;
        jobQueueService.enqueueJobWithDelay(BatchJobType.EMAIL_NOTIFICATION, userId, accountId, delayInSeconds, metadata);
    }

    /**
     * Example: Schedule a recurring cleanup job
     */
    public void scheduleDailyCleanup(Long accountId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("cleanupType", "expired_sessions");
        
        // Schedule to run daily at 2 AM
        jobQueueService.scheduleRecurringJob(
            BatchJobType.DATA_CLEANUP, 
            "daily-cleanup-" + accountId, 
            "0 0 2 * * ?", 
            accountId, 
            metadata
        );
    }
}


