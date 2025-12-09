package com.delight.account.config;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.redis.LettuceRedisStorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.lettuce.core.RedisClient;

/**
 * Centralized JobRunr configuration service following SOLID principles.
 * Handles all JobRunr-related configuration and bean creation.
 */
@Configuration
public class JobRunrConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(JobRunrConfigurationService.class);

    @Autowired
    private JobRunrProperties properties;

    /**
     * Configure Redis client for JobRunr
     */
    @Bean
    public RedisClient jobRunrRedisClient() {
        try {
            String redisUrl = String.format("redis://%s:%d/%d", 
                properties.getRedis().getHost(), 
                properties.getRedis().getPort(), 
                properties.getRedis().getDatabase());
            
            if (properties.getRedis().getPassword() != null && !properties.getRedis().getPassword().isEmpty()) {
                redisUrl = String.format("redis://:%s@%s:%d/%d", 
                    properties.getRedis().getPassword(),
                    properties.getRedis().getHost(), 
                    properties.getRedis().getPort(), 
                    properties.getRedis().getDatabase());
            }

            logger.info("🔧 Configuring JobRunr Redis client: {}", redisUrl.replaceAll("redis://.*@", "redis://***@"));
            return RedisClient.create(redisUrl);
        } catch (Exception e) {
            logger.error("❌ Failed to create JobRunr Redis client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create JobRunr Redis client", e);
        }
    }

    /**
     * Configure JobRunr storage provider
     */
    @Bean
    @Primary
    public StorageProvider jobRunrStorageProvider(JobMapper jobMapper, RedisClient jobRunrRedisClient) {
        try {
            logger.info("🔧 Configuring JobRunr storage provider with Redis");
            LettuceRedisStorageProvider storageProvider = new LettuceRedisStorageProvider(jobRunrRedisClient);
            storageProvider.setJobMapper(jobMapper);
            logger.info("✅ JobRunr storage provider configured successfully");
            return storageProvider;
        } catch (Exception e) {
            logger.error("❌ Failed to configure JobRunr storage provider: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to configure JobRunr storage provider", e);
        }
    }

    /**
     * Get JobRunr properties
     */
    public JobRunrProperties getProperties() {
        return properties;
    }
}

