package com.delight.account.listener;

import com.delight.account.cache.AccountCacheKeyManager;
import com.delight.account.cache.UserCacheKeyManager;
import com.delight.account.model.*;
import com.delight.account.service.CacheService;
import com.delight.account.util.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.*;

/**
 * JPA Entity Listener for automatic cache invalidation.
 * This listener ensures cache consistency by invalidating relevant cache entries
 * immediately after entity operations (persist, update, remove).
 */
@Slf4j
@Component
public class CacheInvalidationEntityListener {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private AccountCacheKeyManager accountCacheKeyManager;

    @Autowired
    private UserCacheKeyManager userCacheKeyManager;

    @PostPersist
    @PostUpdate
    @PostRemove
    public void invalidateCache(Object entity) {
        if (cacheService == null || accountCacheKeyManager == null || userCacheKeyManager == null) {
            log.warn("Cache invalidation skipped - CacheService or CacheKeyManagers not available");
            return;
        }

        try {
            String operation = getCurrentOperation();
            log.debug("Starting cache invalidation for {} operation on entity: {}", operation, entity.getClass().getSimpleName());
            
            if (entity instanceof User) {
                handleUserCacheInvalidation((User) entity, operation);
            } else if (entity instanceof Membership) {
                handleMembershipCacheInvalidation((Membership) entity, operation);
            } else if (entity instanceof AccountConfig) {
                handleAccountConfigCacheInvalidation((AccountConfig) entity, operation);
            } else if (entity instanceof UserCredential) {
                handleUserCredentialCacheInvalidation((UserCredential) entity, operation);
            } else {
                log.debug("No cache invalidation handler for entity type: {}", entity.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("Failed to invalidate cache for entity: {} - Error: {}", 
                entity.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    private String getCurrentOperation() {
        // This is a simple way to determine the operation type
        // In a more sophisticated implementation, you might use ThreadLocal or other mechanisms
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String methodName = element.getMethodName();
            if (methodName.contains("PostPersist")) return "CREATE";
            if (methodName.contains("PostUpdate")) return "UPDATE";
            if (methodName.contains("PostRemove")) return "DELETE";
        }
        return "UNKNOWN";
    }

    private void handleUserCacheInvalidation(User user, String operation) {
        Long userId = user.getId();
        String emailAddress = user.getEmailAddress();
        Long accountId = user.getAccount() != null ? user.getAccount().getId() : null;
        
        log.info("Invalidating User cache - Operation: {}, UserId: {}, Email: {}", 
            operation, userId, emailAddress);

        try {
            // Invalidate user-related cache patterns
            String pattern = String.format("*user*%d*", userId);
            String versionedPattern = String.format("%s:*user*%d*", userCacheKeyManager.getVersion(), userId);
            String legacyPattern = String.format("user:*%d*", userId);
            
            cacheService.evictByPattern(versionedPattern);
            LoggingUtils.logCacheOperation(log, "EVICT_PATTERN", "SUCCESS", 
                "pattern", versionedPattern, "userId", userId, "operation", operation);
            
            cacheService.evictByPattern(legacyPattern);
            LoggingUtils.logCacheOperation(log, "EVICT_PATTERN", "SUCCESS", 
                "pattern", legacyPattern, "userId", userId, "operation", operation);

            // Invalidate specific user account keys if email and accountId are available
            if (emailAddress != null && !emailAddress.isEmpty() && accountId != null) {
                // Use the correct cache key format that matches CacheKeyManager.getUserAccountKey()
                String versionedKey = userCacheKeyManager.getUserAccountKey(accountId, emailAddress);
                String legacyKey = String.format("user:account:%d:email:%s", accountId, emailAddress);
                
                cacheService.evict(versionedKey);
                LoggingUtils.logCacheOperation(log, "EVICT", "SUCCESS", 
                    "key", versionedKey, "userId", userId, "email", emailAddress, "accountId", accountId, "operation", operation);
                
                cacheService.evict(legacyKey);
                LoggingUtils.logCacheOperation(log, "EVICT", "SUCCESS", 
                    "key", legacyKey, "userId", userId, "email", emailAddress, "accountId", accountId, "operation", operation);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate User cache - UserId: {}, Error: {}", userId, e.getMessage(), e);
        }
    }

    private void handleMembershipCacheInvalidation(Membership membership, String operation) {
        Long membershipId = membership.getId();
        Long userId = membership.getUser() != null ? membership.getUser().getId() : null;
        Long accountId = membership.getAccount() != null ? membership.getAccount().getId() : null;
        
        log.info("Invalidating Membership cache - Operation: {}, MembershipId: {}, UserId: {}, AccountId: {}", 
            operation, membershipId, userId, accountId);

        try {
            // Invalidate membership-related cache patterns
            String pattern = String.format("*membership*%d*", membershipId);
            String versionedPattern = String.format("%s:*membership*%d*", userCacheKeyManager.getVersion(), membershipId);
            String legacyPattern = String.format("membership:*%d*", membershipId);
            
            cacheService.evictByPattern(versionedPattern);
            LoggingUtils.logCacheOperation(log, "EVICT_PATTERN", "SUCCESS", 
                "pattern", versionedPattern, "membershipId", membershipId, "operation", operation);
            
            cacheService.evictByPattern(legacyPattern);
            LoggingUtils.logCacheOperation(log, "EVICT_PATTERN", "SUCCESS", 
                "pattern", legacyPattern, "membershipId", membershipId, "operation", operation);

            // Invalidate account info cache and user membership cache if both userId and accountId are available
            if (userId != null && accountId != null) {
                // Invalidate account-level cache (no userId)
                String accountInfoKey = accountCacheKeyManager.getAccountInfoKey(accountId);
                String legacyAccountInfoKey = String.format("account:info:%d", accountId);
                
                cacheService.evict(accountInfoKey);
                LoggingUtils.logCacheOperation(log, "EVICT", "SUCCESS", 
                    "key", accountInfoKey, "membershipId", membershipId, "accountId", accountId, "operation", operation);
                
                cacheService.evict(legacyAccountInfoKey);
                LoggingUtils.logCacheOperation(log, "EVICT", "SUCCESS", 
                    "key", legacyAccountInfoKey, "membershipId", membershipId, "accountId", accountId, "operation", operation);
                
                // Invalidate user membership cache
                String userMembershipKey = userCacheKeyManager.getUserMembershipKey(accountId, userId);
                
                cacheService.evict(userMembershipKey);
                LoggingUtils.logCacheOperation(log, "EVICT", "SUCCESS", 
                    "key", userMembershipKey, "membershipId", membershipId, "userId", userId, "accountId", accountId, "operation", operation);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate Membership cache - MembershipId: {}, Error: {}", membershipId, e.getMessage(), e);
        }
    }

    private void handleAccountConfigCacheInvalidation(AccountConfig accountConfig, String operation) {
        Long configId = accountConfig.getId();
        Long accountId = accountConfig.getAccountId(); // Use getAccountId() instead of getAccount().getId()
        String configType = accountConfig.getConfigType();
        
        log.info("Invalidating AccountConfig cache - Operation: {}, ConfigId: {}, AccountId: {}, ConfigType: {}", 
            operation, configId, accountId, configType);

        try {
            if (accountId != null) {
                // Invalidate specific config cache
                if (configType != null) {
                    String versionedKey = String.format("%s:account:config:%d:%s", accountCacheKeyManager.getVersion(), accountId, configType);
                    String legacyKey = String.format("account:config:%d:%s", accountId, configType);
                    
                    cacheService.evict(versionedKey);
                    LoggingUtils.logCacheOperation(log, "EVICT", "SUCCESS", 
                        "key", versionedKey, "configId", configId, "accountId", accountId, "configType", configType, "operation", operation);
                    
                    cacheService.evict(legacyKey);
                    LoggingUtils.logCacheOperation(log, "EVICT", "SUCCESS", 
                        "key", legacyKey, "configId", configId, "accountId", accountId, "configType", configType, "operation", operation);
                }

                // Invalidate all configs cache for the account
                String versionedAllKey = String.format("%s:account:configs:%d", accountCacheKeyManager.getVersion(), accountId);
                String legacyAllKey = String.format("account:configs:%d", accountId);
                
                cacheService.evict(versionedAllKey);
                LoggingUtils.logCacheOperation(log, "EVICT", "SUCCESS", 
                    "key", versionedAllKey, "configId", configId, "accountId", accountId, "operation", operation);
                
                cacheService.evict(legacyAllKey);
                LoggingUtils.logCacheOperation(log, "EVICT", "SUCCESS", 
                    "key", legacyAllKey, "configId", configId, "accountId", accountId, "operation", operation);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate AccountConfig cache - ConfigId: {}, Error: {}", configId, e.getMessage(), e);
        }
    }

    private void handleUserCredentialCacheInvalidation(UserCredential userCredential, String operation) {
        Long credentialId = userCredential.getId();
        Long userId = userCredential.getUser() != null ? userCredential.getUser().getId() : null;
        String emailAddress = userCredential.getUser() != null ? userCredential.getUser().getEmailAddress() : null;
        
        log.info("Invalidating UserCredential cache - Operation: {}, CredentialId: {}, UserId: {}, Email: {}", 
            operation, credentialId, userId, emailAddress);

        try {
            // Invalidate credential-related cache patterns
            if (userId != null) {
                String versionedPattern = String.format("%s:*credential*%d*", userCacheKeyManager.getVersion(), userId);
                String legacyPattern = String.format("credential:*%d*", userId);
                
                cacheService.evictByPattern(versionedPattern);
                LoggingUtils.logCacheOperation(log, "EVICT_PATTERN", "SUCCESS", 
                    "pattern", versionedPattern, "credentialId", credentialId, "userId", userId, "operation", operation);
                
                cacheService.evictByPattern(legacyPattern);
                LoggingUtils.logCacheOperation(log, "EVICT_PATTERN", "SUCCESS", 
                    "pattern", legacyPattern, "credentialId", credentialId, "userId", userId, "operation", operation);
            }

            // Invalidate user account cache if email is available
            if (emailAddress != null && !emailAddress.isEmpty()) {
                String versionedKey = String.format("%s:user:account:email:%s", userCacheKeyManager.getVersion(), emailAddress);
                String legacyKey = String.format("user:account:email:%s", emailAddress);
                
                cacheService.evict(versionedKey);
                LoggingUtils.logCacheOperation(log, "EVICT", "SUCCESS", 
                    "key", versionedKey, "credentialId", credentialId, "userId", userId, "email", emailAddress, "operation", operation);
                
                cacheService.evict(legacyKey);
                LoggingUtils.logCacheOperation(log, "EVICT", "SUCCESS", 
                    "key", legacyKey, "credentialId", credentialId, "userId", userId, "email", emailAddress, "operation", operation);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate UserCredential cache - CredentialId: {}, Error: {}", credentialId, e.getMessage(), e);
        }
    }
}