package com.delight.account.service.job;

import java.util.Map;

import com.delight.account.domain.BatchJobType;

/**
 * Interface for job queue operations following SOLID principles.
 * Provides a clean abstraction for different job types.
 */
public interface JobQueueService {

    /**
     * Enqueue a job for processing
     * @param jobType The type of job
     * @param jobId Unique identifier for the job
     * @param accountId Account identifier
     * @param metadata Additional job metadata
     */
    void enqueueJob(String jobType, Long jobId, Long accountId, Map<String, Object> metadata);

    /**
     * Enqueue a job with delay
     * @param jobType The type of job
     * @param jobId Unique identifier for the job
     * @param accountId Account identifier
     * @param delayInSeconds Delay in seconds
     * @param metadata Additional job metadata
     */
    void enqueueJobWithDelay(String jobType, Long jobId, Long accountId, int delayInSeconds, Map<String, Object> metadata);

    /**
     * Schedule a recurring job
     * @param jobType The type of job
     * @param jobId Unique identifier for the job
     * @param cronExpression Cron expression for scheduling
     * @param accountId Account identifier
     * @param metadata Additional job metadata
     */
    void scheduleRecurringJob(String jobType, String jobId, String cronExpression, Long accountId, Map<String, Object> metadata);

    /**
     * Get job metrics for a specific job type
     * @param jobType The type of job
     * @return Job metrics
     */
    Map<String, Object> getJobMetrics(String jobType);

    /**
     * Get overall job system health
     * @return Health status
     */
    Map<String, Object> getSystemHealth();

    /**
     * Check if a job type is enabled
     * @param jobType The type of job
     * @return True if enabled
     */
    boolean isJobTypeEnabled(String jobType);

    /**
     * Enqueue a job for processing using BatchJobType enum
     * @param jobType The job type enum
     * @param jobId The job ID
     * @param accountId The account ID
     * @param metadata Additional metadata
     */
    void enqueueJob(BatchJobType jobType, Long jobId, Long accountId, Map<String, Object> metadata);
    
    /**
     * Enqueue a job with delay using BatchJobType enum
     * @param jobType The job type enum
     * @param jobId The job ID
     * @param accountId The account ID
     * @param delayInSeconds Delay in seconds
     * @param metadata Additional metadata
     */
    void enqueueJobWithDelay(BatchJobType jobType, Long jobId, Long accountId, int delayInSeconds, Map<String, Object> metadata);
    
    /**
     * Schedule a recurring job using BatchJobType enum
     * @param jobType The job type enum
     * @param jobId The job ID
     * @param cronExpression Cron expression
     * @param accountId The account ID
     * @param metadata Additional metadata
     */
    void scheduleRecurringJob(BatchJobType jobType, String jobId, String cronExpression, Long accountId, Map<String, Object> metadata);
}


