package com.delight.account.domain;

/**
 * Enum representing all supported batch job types in the system.
 * Each job type has a name, property key, and worker class name for complete configuration.
 * This eliminates the need for switch statements and reflection throughout the codebase.
 */
public enum BatchJobType {
    
    EMAIL_NOTIFICATION("email_notification", "emailNotification", "EmailNotificationJobWorker"),
    ACCOUNT_SYNC("account_sync", "accountSync", "AccountSyncJobWorker"),
    CACHE_WARMUP("cache_warmup", "cacheWarmup", "CacheWarmupJobWorker"),
    DATA_CLEANUP("data_cleanup", "dataCleanup", "DataCleanupJobWorker"),
    ACCOUNT_EVENT_PUBLISH("account_event_publish", "accountEventPublish", "AccountEventPublisherWorker"),
    BULK_ACCOUNT_SYNC("bulk_account_sync", "bulkAccountSync", "BulkAccountSyncWorker"),
    USER_EVENT_PUBLISH("user_event_publish", "userEventPublish", "UserEventPublisherWorker"),
    MEMBERSHIP_EVENT_PUBLISH("membership_event_publish", "membershipEventPublish", "MembershipEventPublisherWorker");
    
    private final String name;
    private final String propertyKey;
    private final String workerClassName;
    
    BatchJobType(String name, String propertyKey, String workerClassName) {
        this.name = name;
        this.propertyKey = propertyKey;
        this.workerClassName = workerClassName;
    }
    
    /**
     * Get the job type name (used as worker identifier)
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the property key for configuration lookup
     */
    public String getPropertyKey() {
        return propertyKey;
    }
    
    /**
     * Get the worker class name for this job type
     */
    public String getWorkerClassName() {
        return workerClassName;
    }
    
    /**
     * Find BatchJobType by name
     */
    public static BatchJobType fromName(String name) {
        for (BatchJobType jobType : values()) {
            if (jobType.name.equals(name)) {
                return jobType;
            }
        }
        throw new IllegalArgumentException("Unknown job type: " + name);
    }
    
    /**
     * Check if a job type exists by name
     */
    public static boolean exists(String name) {
        try {
            fromName(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Get all job type names
     */
    public static String[] getAllNames() {
        return java.util.Arrays.stream(values())
                .map(BatchJobType::getName)
                .toArray(String[]::new);
    }
}

