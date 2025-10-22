package com.delight.account.service.impl;

import com.delight.account.cache.CacheKeyManager;
import com.delight.account.model.Account;
import com.delight.account.model.AccountConfig;
import com.delight.account.repository.AccountConfigRepository;
import com.delight.account.repository.AccountRepository;
import com.delight.account.service.AccountConfigService;
import com.delight.account.service.CacheService;
import com.delight.account.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountConfigServiceImpl implements AccountConfigService {
    
    private final AccountConfigRepository accountConfigRepository;
    private final AccountRepository accountRepository;
    private final CacheService cacheService;
    private final CacheKeyManager cacheKeyManager;
    
    @Value("${cache.account.config.ttl:PT30M}")
    private Duration accountConfigCacheTtl;
    
    public AccountConfigServiceImpl(AccountConfigRepository accountConfigRepository, 
                                  AccountRepository accountRepository,
                                  CacheService cacheService,
                                  CacheKeyManager cacheKeyManager) {
        this.accountConfigRepository = accountConfigRepository;
        this.accountRepository = accountRepository;
        this.cacheService = cacheService;
        this.cacheKeyManager = cacheKeyManager;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getConfig(Long accountId, String configType) {
        String cacheKey = getConfigCacheKey(accountId, configType);
        
        // Try cache first
        Optional<Object> cachedValue = cacheService.get(cacheKey, () -> {
            Optional<AccountConfig> accountConfig = accountConfigRepository
                .findByAccountIdAndConfigTypeAndIsActiveTrue(accountId, configType);
            return accountConfig.map(config -> (Object) config.getConfigData());
        }, accountConfigCacheTtl, Object.class);
        
        // Cast to the expected type
        if (cachedValue.isPresent() && cachedValue.get() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> configMap = (Map<String, Object>) cachedValue.get();
            return Optional.of(configMap);
        }
        
        return Optional.empty();
    }
    
    @Override
    public void setConfig(Long accountId, String configType, Map<String, Object> configData) {
        // Verify account exists
        if (!accountRepository.existsById(accountId)) {
            throw new ApiException("error.account.notfound");
        }
        
        // Deactivate existing configuration
        accountConfigRepository.deactivateByAccountIdAndConfigType(accountId, configType);
        
        // Create new configuration
        AccountConfig accountConfig = new AccountConfig();
        accountConfig.setAccountId(accountId);
        accountConfig.setConfigType(configType);
        accountConfig.setConfigData(new HashMap<>(configData));
        accountConfig.setIsActive(true);
        
        accountConfigRepository.save(accountConfig);
        
        // Cache invalidation is now handled automatically by CacheInvalidationEntityListener
    }
    
    @Override
    public void removeConfig(Long accountId, String configType) {
        accountConfigRepository.deactivateByAccountIdAndConfigType(accountId, configType);
        // Cache invalidation is now handled automatically by CacheInvalidationEntityListener
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasConfig(Long accountId, String configType) {
        return accountConfigRepository.existsByAccountIdAndConfigTypeAndIsActiveTrue(accountId, configType);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllConfigs(Long accountId) {
        String cacheKey = getAllConfigsCacheKey(accountId);
        
        // Try cache first
        Optional<Object> cachedValue = cacheService.get(cacheKey, () -> {
            List<AccountConfig> configs = accountConfigRepository.findByAccountIdAndIsActiveTrue(accountId);
            List<Map<String, Object>> configMaps = configs.stream()
                .map(config -> {
                    Map<String, Object> configMap = new HashMap<>(config.getConfigData());
                    configMap.put("configType", config.getConfigType());
                    configMap.put("id", config.getId());
                    configMap.put("createdAt", config.getCreatedAt());
                    configMap.put("updatedAt", config.getUpdatedAt());
                    return configMap;
                })
                .collect(Collectors.toList());
            return Optional.of((Object) configMaps);
        }, accountConfigCacheTtl, Object.class);
        
        // Cast to the expected type
        if (cachedValue.isPresent() && cachedValue.get() instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> configList = (List<Map<String, Object>>) cachedValue.get();
            return configList;
        }
        
        return List.of();
    }
    
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(Long accountId, String configType, String key, T defaultValue) {
        Optional<Map<String, Object>> config = getConfig(accountId, configType);
        if (config.isPresent()) {
            Object value = config.get().get(key);
            if (value != null && defaultValue != null && defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }
        }
        return defaultValue;
    }
    
    @Override
    public void setConfigValue(Long accountId, String configType, String key, Object value) {
        Optional<Map<String, Object>> existingConfig = getConfig(accountId, configType);
        Map<String, Object> configData = existingConfig.orElse(new HashMap<>());
        configData.put(key, value);
        setConfig(accountId, configType, configData);
    }
    
    @Override
    public void invalidateCache(Long accountId, String configType) {
        if (configType != null) {
            String cacheKey = getConfigCacheKey(accountId, configType);
            cacheService.evict(cacheKey);
        }
        
        // Also invalidate all configs cache for this account
        String allConfigsCacheKey = getAllConfigsCacheKey(accountId);
        cacheService.evict(allConfigsCacheKey);
    }
    
    @Override
    public void invalidateAllCache(Long accountId) {
        String pattern = getAccountConfigPattern(accountId);
        cacheService.evictByPattern(pattern);
    }
    
    private String getConfigCacheKey(Long accountId, String configType) {
        if (cacheKeyManager != null) {
            return String.format("%s:account:config:%d:%s", cacheKeyManager.getVersion(), accountId, configType);
        }
        return String.format("account:config:%d:%s", accountId, configType);
    }
    
    private String getAllConfigsCacheKey(Long accountId) {
        if (cacheKeyManager != null) {
            return String.format("%s:account:config:%d:all", cacheKeyManager.getVersion(), accountId);
        }
        return String.format("account:config:%d:all", accountId);
    }
    
    private String getAccountConfigPattern(Long accountId) {
        if (cacheKeyManager != null) {
            return String.format("%s:account:config:%d:*", cacheKeyManager.getVersion(), accountId);
        }
        return String.format("account:config:%d:*", accountId);
    }
}