package com.delight.account.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing account-specific configurations with caching support
 */
public interface AccountConfigService {
    
    /**
     * Get configuration data for an account and config type
     * @param accountId the account ID
     * @param configType the configuration type
     * @return configuration data map, or empty if not found
     */
    Optional<Map<String, Object>> getConfig(Long accountId, String configType);
    
    /**
     * Set configuration data for an account and config type
     * @param accountId the account ID
     * @param configType the configuration type
     * @param configData the configuration data
     */
    void setConfig(Long accountId, String configType, Map<String, Object> configData);
    
    /**
     * Remove configuration for an account and config type
     * @param accountId the account ID
     * @param configType the configuration type
     */
    void removeConfig(Long accountId, String configType);
    
    /**
     * Check if account has configuration of a specific type
     * @param accountId the account ID
     * @param configType the configuration type
     * @return true if configuration exists
     */
    boolean hasConfig(Long accountId, String configType);
    
    /**
     * Get all active configurations for an account
     * @param accountId the account ID
     * @return list of all configurations for the account
     */
    List<Map<String, Object>> getAllConfigs(Long accountId);
    
    /**
     * Get a specific value from account configuration
     * @param accountId the account ID
     * @param configType the configuration type
     * @param key the key within the configuration
     * @param defaultValue the default value if not found
     * @param <T> the type of the value
     * @return the configuration value or default value
     */
    <T> T getConfigValue(Long accountId, String configType, String key, T defaultValue);
    
    /**
     * Set a specific value in account configuration
     * @param accountId the account ID
     * @param configType the configuration type
     * @param key the key within the configuration
     * @param value the value to set
     */
    void setConfigValue(Long accountId, String configType, String key, Object value);
    
    /**
     * Invalidate cache for account configuration
     * @param accountId the account ID
     * @param configType the configuration type (optional, if null invalidates all configs for account)
     */
    void invalidateCache(Long accountId, String configType);
    
    /**
     * Invalidate all cache for an account
     * @param accountId the account ID
     */
    void invalidateAllCache(Long accountId);
}