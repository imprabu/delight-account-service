package com.delight.account.service.job;

import com.delight.account.config.JobRunrConfigurationService;
import com.delight.account.config.JobRunrProperties;
import com.delight.account.domain.BatchJobType;
import com.delight.account.util.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.HashMap;
import java.util.Map;

/**
 * Base abstract class for all job workers that provides common retry logic
 * with exponential backoff using JobRunr re-enqueuing.
 * 
 * Workers extend this class and implement doProcessJob() instead of processJob().
 * Max retries can be overridden per worker by implementing getMaxRetries().
 */
public abstract class BaseWorker implements JobWorker {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Autowired(required = false)
    @Lazy
    private JobQueueService jobQueueService;
    
    @Autowired(required = false)
    @Lazy
    private JobRunrConfigurationService jobRunrConfigurationService;
    
    /**
     * Template method that handles retry logic.
     * Workers should implement doProcessJob() instead of this method.
     */
    @Override
    public final void processJob(Long jobId, Long accountId, Map<String, Object> metadata) {
        try {
            // Get retry attempt count from metadata (default to 0 for first attempt)
            int retryAttempt = metadata != null && metadata.containsKey("retryAttempt") 
                ? ((Number) metadata.get("retryAttempt")).intValue() 
                : 0;
            
            // Get max retries for this worker
            int maxRetries = getMaxRetries();
            
            LoggingUtils.info(logger, "🔄 [{}] Processing job {} for account {} (attempt {}/{})", 
                           getWorkerName(), jobId, accountId, retryAttempt + 1, maxRetries + 1);
            
            // Execute the actual job processing (implemented by subclasses)
            doProcessJob(jobId, accountId, metadata);
            
            LoggingUtils.info(logger, "✅ [{}] Successfully completed job {} for account {}", 
                           getWorkerName(), jobId, accountId);
            
        } catch (Exception e) {
            try {
                handleJobFailure(jobId, accountId, metadata, e);
            } catch (Exception retryException) {
                // If handleJobFailure throws (re-enqueue failed or max retries exceeded),
                // wrap it in RuntimeException since processJob doesn't declare checked exceptions
                throw new RuntimeException("Job processing failed and retry handling failed", retryException);
            }
        }
    }
    
    /**
     * Handle job failure with retry logic.
     * Re-enqueues the job with exponential backoff if retries are available.
     * 
     * @throws RuntimeException If retries are exhausted or re-enqueue fails
     */
    protected void handleJobFailure(Long jobId, Long accountId, Map<String, Object> metadata, Exception e) {
        // Get retry attempt count from metadata
        int retryAttempt = metadata != null && metadata.containsKey("retryAttempt") 
            ? ((Number) metadata.get("retryAttempt")).intValue() 
            : 0;
        
        int maxRetries = getMaxRetries();
        
        LoggingUtils.error(logger, "❌ [{}] Job {} failed for account {}: {}", 
                        getWorkerName(), jobId, accountId, e.getMessage(), e);
        
        // Check if we should retry
        if (retryAttempt < maxRetries && jobQueueService != null) {
            int nextRetryAttempt = retryAttempt + 1;
            int delaySeconds = calculateExponentialBackoff(nextRetryAttempt);
            
            LoggingUtils.warn(logger, "⚠️ [{}] Re-enqueuing job {} with {}s delay (attempt {}/{})", 
                           getWorkerName(), jobId, delaySeconds, nextRetryAttempt + 1, maxRetries + 1);
            
            // Create new metadata with retry attempt count
            Map<String, Object> retryMetadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            retryMetadata.put("retryAttempt", nextRetryAttempt);
            
            // Re-enqueue the job with exponential backoff delay
            try {
                jobQueueService.enqueueJobWithDelay(
                    getJobType(),
                    jobId,
                    accountId,
                    delaySeconds,
                    retryMetadata
                );
            } catch (Exception reEnqueueException) {
                LoggingUtils.error(logger, "❌ [{}] Failed to re-enqueue job {}: {}", 
                                getWorkerName(), jobId, reEnqueueException.getMessage(), reEnqueueException);
                // Re-throw original exception wrapped in RuntimeException if re-enqueue fails
                throw new RuntimeException("Failed to re-enqueue job after failure", e);
            }
        } else {
            LoggingUtils.error(logger, "❌ [{}] Job {} failed after {} attempts - giving up", 
                            getWorkerName(), jobId, maxRetries + 1);
            // Re-throw the exception wrapped in RuntimeException to mark job as failed
            throw new RuntimeException("Job failed after max retries", e);
        }
    }
    
    /**
     * Calculate exponential backoff delay in seconds.
     * Formula: baseDelay * (2 ^ attemptNumber)
     * Example: attempt 1 = 2s, attempt 2 = 4s, attempt 3 = 8s, attempt 4 = 16s
     * 
     * @param attemptNumber The retry attempt number (1-based)
     * @return Delay in seconds (capped at 5 minutes)
     */
    protected int calculateExponentialBackoff(int attemptNumber) {
        int baseDelaySeconds = 2; // Start with 2 seconds
        int delaySeconds = baseDelaySeconds * (int) Math.pow(2, attemptNumber - 1);
        
        // Cap at 5 minutes (300 seconds) to avoid extremely long delays
        return Math.min(delaySeconds, 300);
    }
    
    /**
     * Get max retries for this worker.
     * Default implementation reads from JobRunr configuration.
     * Can be overridden by subclasses to return a fixed value or custom logic.
     * 
     * @return Maximum number of retry attempts
     */
    protected int getMaxRetries() {
        try {
            if (jobRunrConfigurationService != null) {
                JobRunrProperties properties = jobRunrConfigurationService.getProperties();
                JobRunrProperties.QueueConfig queueConfig = getQueueConfig(properties);
                return queueConfig != null ? queueConfig.getMaxRetries() : 3; // Default to 3
            }
        } catch (Exception e) {
            LoggingUtils.warn(logger, "⚠️ [{}] Failed to get max retries from config, using default: 3", getWorkerName());
        }
        return 3; // Default fallback
    }
    
    /**
     * Get the queue configuration for this worker from JobRunrProperties.
     * Subclasses can override this to map their job type to the correct config.
     * 
     * @param properties JobRunr properties
     * @return Queue configuration for this worker
     */
    protected JobRunrProperties.QueueConfig getQueueConfig(JobRunrProperties properties) {
        // Default implementation - map BatchJobType to property getter
        BatchJobType jobType = getJobType();
        String propertyKey = jobType.getPropertyKey();
        
        try {
            // Use reflection to get the queue config dynamically
            String methodName = "get" + 
                propertyKey.substring(0, 1).toUpperCase() + 
                propertyKey.substring(1);
            java.lang.reflect.Method method = properties.getClass().getMethod(methodName);
            return (JobRunrProperties.QueueConfig) method.invoke(properties);
        } catch (Exception e) {
            LoggingUtils.warn(logger, "⚠️ [{}] Failed to get queue config for property key '{}': {}", 
                           getWorkerName(), propertyKey, e.getMessage());
            return null;
        }
    }
    
    /**
     * Get worker name for logging purposes.
     * 
     * @return Worker class name
     */
    protected String getWorkerName() {
        return getClass().getSimpleName();
    }
    
    /**
     * Abstract method that workers must implement.
     * This is where the actual job processing logic goes.
     * 
     * @param jobId The unique identifier for the job
     * @param accountId The account ID associated with the job
     * @param metadata Additional metadata for the job
     * @throws Exception If job processing fails
     */
    protected abstract void doProcessJob(Long jobId, Long accountId, Map<String, Object> metadata) throws Exception;
    
    /**
     * Get the job type this worker handles.
     * Must be implemented by subclasses.
     * 
     * @return The BatchJobType enum value
     */
    @Override
    public abstract BatchJobType getJobType();
}

