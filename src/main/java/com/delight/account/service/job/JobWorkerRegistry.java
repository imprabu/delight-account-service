package com.delight.account.service.job;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.delight.account.domain.BatchJobType;
import jakarta.annotation.PostConstruct;

/**
 * Registry for job workers that allows dynamic worker registration.
 * This decouples the job queue service from specific worker implementations.
 */
@Component
public class JobWorkerRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(JobWorkerRegistry.class);
    
    private final Map<BatchJobType, JobWorker> workers = new HashMap<>();
    private final Map<String, JobWorker> workersByName = new HashMap<>();
    
    @Autowired
    private List<JobWorker> availableWorkers;
    
    @PostConstruct
    public void initializeWorkers() {
        logger.info("🔧 [REGISTRY] Initializing job worker registry...");
        
        for (JobWorker worker : availableWorkers) {
            BatchJobType jobType = worker.getJobType();
            workers.put(jobType, worker);
            workersByName.put(jobType.getName(), worker);
            logger.info("✅ [REGISTRY] Registered worker '{}' for job type '{}'", 
                       jobType.getWorkerClassName(), jobType.getName());
        }
        
        logger.info("🎯 [REGISTRY] Worker registry initialized with {} workers: {}", 
                   workers.size(), workers.keySet());
    }
    
    /**
     * Get a worker for the specified job type enum.
     * 
     * @param jobType The job type enum
     * @return The worker instance, or null if not found
     */
    public JobWorker getWorker(BatchJobType jobType) {
        JobWorker worker = workers.get(jobType);
        if (worker == null) {
            logger.warn("⚠️ [REGISTRY] No worker found for job type: {}", jobType);
        }
        return worker;
    }
    
    /**
     * Get a worker for the specified job type name.
     * 
     * @param jobTypeName The job type name
     * @return The worker instance, or null if not found
     */
    public JobWorker getWorker(String jobTypeName) {
        JobWorker worker = workersByName.get(jobTypeName);
        if (worker == null) {
            logger.warn("⚠️ [REGISTRY] No worker found for job type name: {}", jobTypeName);
        }
        return worker;
    }
    
    /**
     * Check if a worker exists for the specified job type enum.
     * 
     * @param jobType The job type enum
     * @return true if worker exists, false otherwise
     */
    public boolean hasWorker(BatchJobType jobType) {
        return workers.containsKey(jobType);
    }
    
    /**
     * Check if a worker exists for the specified job type name.
     * 
     * @param jobTypeName The job type name
     * @return true if worker exists, false otherwise
     */
    public boolean hasWorker(String jobTypeName) {
        return workersByName.containsKey(jobTypeName);
    }
    
    /**
     * Get all registered job types.
     * 
     * @return Set of job type enums
     */
    public java.util.Set<BatchJobType> getRegisteredJobTypes() {
        return workers.keySet();
    }
    
    /**
     * Get all registered job type names.
     * 
     * @return Set of job type names
     */
    public java.util.Set<String> getRegisteredJobTypeNames() {
        return workersByName.keySet();
    }
    
    /**
     * Get worker information for all registered workers.
     * 
     * @return Map of job type name to worker class name
     */
    public Map<String, String> getWorkerInfo() {
        Map<String, String> info = new HashMap<>();
        workers.forEach((jobType, worker) -> 
            info.put(jobType.getName(), jobType.getWorkerClassName()));
        return info;
    }
}


