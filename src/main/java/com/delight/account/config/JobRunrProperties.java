package com.delight.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Configuration properties for JobRunr job processing.
 * Follows SOLID principles for extensibility and maintainability.
 */
@Component
@ConfigurationProperties(prefix = "app.jobrunr")
public class JobRunrProperties {

    private QueueConfig emailNotification = new QueueConfig();
    private QueueConfig accountSync = new QueueConfig();
    private QueueConfig cacheWarmup = new QueueConfig();
    private QueueConfig dataCleanup = new QueueConfig();
    private QueueConfig accountEventPublish = new QueueConfig();
    private QueueConfig bulkAccountSync = new QueueConfig();
    private RedisConfig redis = new RedisConfig();
    private DashboardConfig dashboard = new DashboardConfig();

    public QueueConfig getEmailNotification() {
        return emailNotification;
    }

    public void setEmailNotification(QueueConfig emailNotification) {
        this.emailNotification = emailNotification;
    }

    public QueueConfig getAccountSync() {
        return accountSync;
    }

    public void setAccountSync(QueueConfig accountSync) {
        this.accountSync = accountSync;
    }

    public QueueConfig getCacheWarmup() {
        return cacheWarmup;
    }

    public void setCacheWarmup(QueueConfig cacheWarmup) {
        this.cacheWarmup = cacheWarmup;
    }

    public QueueConfig getDataCleanup() {
        return dataCleanup;
    }

    public void setDataCleanup(QueueConfig dataCleanup) {
        this.dataCleanup = dataCleanup;
    }

    public QueueConfig getAccountEventPublish() {
        return accountEventPublish;
    }

    public void setAccountEventPublish(QueueConfig accountEventPublish) {
        this.accountEventPublish = accountEventPublish;
    }

    public QueueConfig getBulkAccountSync() {
        return bulkAccountSync;
    }

    public void setBulkAccountSync(QueueConfig bulkAccountSync) {
        this.bulkAccountSync = bulkAccountSync;
    }

    public RedisConfig getRedis() {
        return redis;
    }

    public void setRedis(RedisConfig redis) {
        this.redis = redis;
    }

    public DashboardConfig getDashboard() {
        return dashboard;
    }

    public void setDashboard(DashboardConfig dashboard) {
        this.dashboard = dashboard;
    }

    /**
     * Queue configuration for different job types
     */
    public static class QueueConfig {
        private boolean enabled = true;
        private int workerCount = 5;
        private int pollIntervalSeconds = 15;
        private int deleteSucceededJobsAfterHours = 24;
        private int permanentlyDeleteJobsAfterHours = 72;
        private int maxRetries = 3;
        private int retryDelaySeconds = 60;
        private Map<String, Object> metadata = Map.of();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getWorkerCount() {
            return workerCount;
        }

        public void setWorkerCount(int workerCount) {
            this.workerCount = workerCount;
        }

        public int getPollIntervalSeconds() {
            return pollIntervalSeconds;
        }

        public void setPollIntervalSeconds(int pollIntervalSeconds) {
            this.pollIntervalSeconds = pollIntervalSeconds;
        }

        public int getDeleteSucceededJobsAfterHours() {
            return deleteSucceededJobsAfterHours;
        }

        public void setDeleteSucceededJobsAfterHours(int deleteSucceededJobsAfterHours) {
            this.deleteSucceededJobsAfterHours = deleteSucceededJobsAfterHours;
        }

        public int getPermanentlyDeleteJobsAfterHours() {
            return permanentlyDeleteJobsAfterHours;
        }

        public void setPermanentlyDeleteJobsAfterHours(int permanentlyDeleteJobsAfterHours) {
            this.permanentlyDeleteJobsAfterHours = permanentlyDeleteJobsAfterHours;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public int getRetryDelaySeconds() {
            return retryDelaySeconds;
        }

        public void setRetryDelaySeconds(int retryDelaySeconds) {
            this.retryDelaySeconds = retryDelaySeconds;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }

    /**
     * Redis configuration
     */
    public static class RedisConfig {
        private String host = "localhost";
        private int port = 6379;
        private String password = "";
        private int database = 3; // Default to database 3 for account-service JobRunr
        private int connectionTimeout = 5000;
        private int commandTimeout = 3000;
        private int maxConnections = 8;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }

        public int getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public int getCommandTimeout() {
            return commandTimeout;
        }

        public void setCommandTimeout(int commandTimeout) {
            this.commandTimeout = commandTimeout;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }
    }

    /**
     * Dashboard configuration
     */
    public static class DashboardConfig {
        private boolean enabled = true;
        private int port = 8000;
        private String username = "";
        private String password = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}

