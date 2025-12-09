package com.delight.account.service.job;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.delight.account.config.JobRunrConfigurationService;
import com.delight.account.config.JobRunrProperties;
import com.delight.account.domain.BatchJobType;

/**
 * Generic JobRunr implementation of JobQueueService using worker registry pattern.
 * This design eliminates switch statements by using dynamic property lookup.
 * Adding new job types requires NO modifications to this class.
 */
@Service
public class JobRunrQueueService implements JobQueueService {

    private static final Logger logger = LoggerFactory.getLogger(JobRunrQueueService.class);

    @Autowired
    private JobScheduler jobScheduler;

    @Autowired
    private JobRunrConfigurationService configurationService;

    @Autowired
    private JobWorkerRegistry workerRegistry;

    @Override
    public void enqueueJob(String jobTypeName, Long jobId, Long accountId, Map<String, Object> metadata) {
        if (!isJobTypeEnabled(jobTypeName)) {
            logger.warn("⚠️ Job type '{}' is disabled, skipping job {}", jobTypeName, jobId);
            return;
        }

        JobWorker worker = workerRegistry.getWorker(jobTypeName);
        if (worker == null) {
            logger.error("❌ [JOBRUNR] No worker found for job type: {}", jobTypeName);
            throw new IllegalArgumentException("No worker found for job type: " + jobTypeName);
        }

        try {
            // Ensure metadata is not null and create a final copy for lambda
            Map<String, Object> finalMetadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            
            // Ensure accountId is in metadata
            if (!finalMetadata.containsKey("accountId") && accountId != null) {
                finalMetadata.put("accountId", accountId);
            }
            
            // Create unique job ID with timestamp to prevent duplicates
            String customJobId = jobTypeName + "_" + jobId + "_" + accountId + "_" + System.currentTimeMillis();
            java.util.UUID jobUuid = java.util.UUID.nameUUIDFromBytes(customJobId.getBytes());
            
            // Dynamic job enqueueing with custom job ID - JobRunr prevents duplicates automatically
            jobScheduler.enqueue(jobUuid, 
                               () -> processJobDynamically(jobTypeName, jobId, accountId, finalMetadata));
            
            logger.info("📤 [JOBRUNR] Enqueued {} job for ID: {} using worker {} (account: {})", 
                       jobTypeName, jobId, worker.getJobType().getWorkerClassName(), accountId);
        } catch (Exception e) {
            logger.error("❌ [JOBRUNR] Failed to enqueue {} job for ID: {}: {}", 
                        jobTypeName, jobId, e.getMessage(), e);
            throw new RuntimeException("Failed to enqueue job", e);
        }
    }

    @Override
    public void enqueueJobWithDelay(String jobTypeName, Long jobId, Long accountId, int delayInSeconds, Map<String, Object> metadata) {
        if (!isJobTypeEnabled(jobTypeName)) {
            logger.warn("⚠️ Job type '{}' is disabled, skipping delayed job {}", jobTypeName, jobId);
            return;
        }

        JobWorker worker = workerRegistry.getWorker(jobTypeName);
        if (worker == null) {
            logger.error("❌ [JOBRUNR] No worker found for job type: {}", jobTypeName);
            throw new IllegalArgumentException("No worker found for job type: " + jobTypeName);
        }

        try {
            // Ensure metadata is not null and create a final copy for lambda
            Map<String, Object> finalMetadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            
            // Ensure accountId is in metadata
            if (!finalMetadata.containsKey("accountId") && accountId != null) {
                finalMetadata.put("accountId", accountId);
            }
            
            // Create unique job ID with timestamp to prevent duplicates
            String customJobId = jobTypeName + "_" + jobId + "_" + accountId + "_delayed_" + System.currentTimeMillis();
            java.util.UUID jobUuid = java.util.UUID.nameUUIDFromBytes(customJobId.getBytes());
            
            // Dynamic delayed job scheduling with custom job ID - JobRunr prevents duplicates automatically
            jobScheduler.schedule(jobUuid,
                                java.time.Instant.now().plusSeconds(delayInSeconds), 
                                () -> processJobDynamically(jobTypeName, jobId, accountId, finalMetadata));
            
            logger.info("📤 [JOBRUNR] Scheduled {} job for ID: {} with {}s delay using worker {} (account: {})", 
                       jobTypeName, jobId, delayInSeconds, worker.getJobType().getWorkerClassName(), accountId);
        } catch (Exception e) {
            logger.error("❌ [JOBRUNR] Failed to schedule delayed {} job for ID: {}: {}", 
                        jobTypeName, jobId, e.getMessage(), e);
            throw new RuntimeException("Failed to schedule delayed job", e);
        }
    }

    @Override
    public void scheduleRecurringJob(String jobTypeName, String jobId, String cronExpression, Long accountId, Map<String, Object> metadata) {
        if (!isJobTypeEnabled(jobTypeName)) {
            logger.warn("⚠️ Job type '{}' is disabled, skipping recurring job {}", jobTypeName, jobId);
            return;
        }

        JobWorker worker = workerRegistry.getWorker(jobTypeName);
        if (worker == null) {
            logger.error("❌ [JOBRUNR] No worker found for job type: {}", jobTypeName);
            throw new IllegalArgumentException("No worker found for job type: " + jobTypeName);
        }

        try {
            // Dynamic recurring job scheduling - no switch statements needed
            jobScheduler.scheduleRecurrently(jobId, cronExpression, 
                                           () -> processJobDynamically(jobTypeName, Long.parseLong(jobId), accountId, metadata));
            
            logger.info("📤 [JOBRUNR] Scheduled recurring {} job '{}' with cron: {} using worker {}", 
                       jobTypeName, jobId, cronExpression, worker.getJobType().getWorkerClassName());
        } catch (Exception e) {
            logger.error("❌ [JOBRUNR] Failed to schedule recurring {} job '{}': {}", 
                        jobTypeName, jobId, e.getMessage(), e);
            throw new RuntimeException("Failed to schedule recurring job", e);
        }
    }

    @Override
    public Map<String, Object> getJobMetrics(String jobTypeName) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("jobType", jobTypeName);
        metrics.put("enabled", isJobTypeEnabled(jobTypeName));
        metrics.put("workerExists", workerRegistry.hasWorker(jobTypeName));
        metrics.put("timestamp", LocalDateTime.now());
        return metrics;
    }

    @Override
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("framework", "JobRunr");
        health.put("timestamp", LocalDateTime.now());
        health.put("registeredWorkers", workerRegistry.getWorkerInfo());
        health.put("totalWorkers", workerRegistry.getRegisteredJobTypes().size());
        return health;
    }

    @Override
    public boolean isJobTypeEnabled(String jobTypeName) {
        try {
            // Check if it's a known job type
            if (BatchJobType.exists(jobTypeName)) {
                BatchJobType jobType = BatchJobType.fromName(jobTypeName);
                return isJobTypeEnabledByProperty(jobType);
            } else {
                // For unknown job types, check if a worker exists for it
                // This allows new job types to be automatically enabled if a worker exists
                return workerRegistry.hasWorker(jobTypeName);
            }
        } catch (Exception e) {
            logger.warn("⚠️ [JOBRUNR] Error checking if job type '{}' is enabled: {}", jobTypeName, e.getMessage());
            return false;
        }
    }

    /**
     * Check if a job type is enabled by looking up its property dynamically
     */
    private boolean isJobTypeEnabledByProperty(BatchJobType jobType) {
        try {
            JobRunrProperties properties = configurationService.getProperties();
            String propertyKey = jobType.getPropertyKey();
            
            // Use reflection to dynamically call the getter method
            String methodName = "get" + propertyKey.substring(0, 1).toUpperCase() + propertyKey.substring(1);
            Method method = properties.getClass().getMethod(methodName);
            Object queueConfig = method.invoke(properties);
            
            // Call isEnabled() on the queue config
            Method isEnabledMethod = queueConfig.getClass().getMethod("isEnabled");
            return (Boolean) isEnabledMethod.invoke(queueConfig);
            
        } catch (Exception e) {
            logger.warn("⚠️ [JOBRUNR] Error checking property for job type '{}': {}", jobType.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Enqueue a job using BatchJobType enum - completely dynamic approach
     * @param jobType The job type enum
     * @param jobId The job ID
     * @param accountId The account ID
     * @param metadata Additional metadata
     */
    @Override
    public void enqueueJob(BatchJobType jobType, Long jobId, Long accountId, Map<String, Object> metadata) {
        enqueueJob(jobType.getName(), jobId, accountId, metadata);
    }
    
    /**
     * Enqueue a job with delay using BatchJobType enum - completely dynamic approach
     * @param jobType The job type enum
     * @param jobId The job ID
     * @param accountId The account ID
     * @param delayInSeconds Delay in seconds
     * @param metadata Additional metadata
     */
    @Override
    public void enqueueJobWithDelay(BatchJobType jobType, Long jobId, Long accountId, int delayInSeconds, Map<String, Object> metadata) {
        enqueueJobWithDelay(jobType.getName(), jobId, accountId, delayInSeconds, metadata);
    }
    
    /**
     * Schedule a recurring job using BatchJobType enum - completely dynamic approach
     * @param jobType The job type enum
     * @param jobId The job ID
     * @param cronExpression Cron expression
     * @param accountId The account ID
     * @param metadata Additional metadata
     */
    @Override
    public void scheduleRecurringJob(BatchJobType jobType, String jobId, String cronExpression, Long accountId, Map<String, Object> metadata) {
        scheduleRecurringJob(jobType.getName(), jobId, cronExpression, accountId, metadata);
    }
    
    /**
     * Dynamic job processing method - works for ANY job type without modification
     * This is the key to extensibility - no switch statements needed!
     * Ensures accountId and userId are in metadata for logging purposes.
     */
    public void processJobDynamically(String jobTypeName, Long jobId, Long accountId, Map<String, Object> metadata) {
        // Ensure accountId and userId are in metadata for background job logging
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        
        // Ensure accountId is in metadata (use parameter if not already present)
        if (!metadata.containsKey("accountId") && accountId != null) {
            metadata.put("accountId", accountId);
        }
        
        // Use accountId from metadata if parameter is null
        Long effectiveAccountId = accountId;
        if (effectiveAccountId == null && metadata.containsKey("accountId")) {
            Object accountIdObj = metadata.get("accountId");
            if (accountIdObj instanceof Long) {
                effectiveAccountId = (Long) accountIdObj;
            } else if (accountIdObj instanceof Number) {
                effectiveAccountId = ((Number) accountIdObj).longValue();
            }
        }
        
        try {
            JobWorker worker = workerRegistry.getWorker(jobTypeName);
            if (worker != null) {
                logger.info("🔄 [JOBRUNR] Executing {} job {} using worker {}", 
                           jobTypeName, jobId, worker.getJobType().getWorkerClassName());
                worker.processJob(jobId, effectiveAccountId, metadata);
                logger.info("✅ [JOBRUNR] Successfully completed {} job {}", jobTypeName, jobId);
            } else {
                logger.error("❌ [JOBRUNR] No worker found for job type: {} (job: {})", jobTypeName, jobId);
                throw new IllegalStateException("No worker found for job type: " + jobTypeName);
            }
        } catch (Exception e) {
            logger.error("❌ [JOBRUNR] Worker {} failed for job {}: {}", 
                        jobTypeName, jobId, e.getMessage(), e);
            throw e; // Re-throw to let JobRunr handle retries
        }
    }
}


