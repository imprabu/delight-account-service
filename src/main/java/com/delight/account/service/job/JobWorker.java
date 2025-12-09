package com.delight.account.service.job;

import java.util.Map;

import com.delight.account.domain.BatchJobType;

/**
 * Common interface for all job workers.
 * This allows for a generic job processing system without tight coupling.
 */
public interface JobWorker {
    
    /**
     * Process a job with the given parameters.
     * 
     * @param jobId The unique identifier for the job
     * @param accountId The account ID associated with the job
     * @param metadata Additional metadata for the job
     */
    void processJob(Long jobId, Long accountId, Map<String, Object> metadata);
    
    /**
     * Get the job type this worker handles.
     * 
     * @return The BatchJobType enum value
     */
    BatchJobType getJobType();
}


