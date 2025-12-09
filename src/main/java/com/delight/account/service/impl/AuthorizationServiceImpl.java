package com.delight.account.service.impl;

import com.delight.account.config.RedisCasbinAdapter;
import com.delight.account.service.AuthorizationService;
import com.delight.account.service.CasbinPolicyCacheService;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Authorization service using Casbin with Redis-based per-tenant policy caching.
 * Each account has its own enforcer instance with policies cached in Redis.
 */
@Service
public class AuthorizationServiceImpl implements AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationServiceImpl.class);

    private final Model casbinModel;
    private final RedisCasbinAdapter redisCasbinAdapter;
    private final CasbinPolicyCacheService policyCacheService;
    
    // Per-account enforcer cache
    private final ConcurrentHashMap<Long, Enforcer> enforcerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ReentrantLock> enforcerLocks = new ConcurrentHashMap<>();

    @Autowired
    public AuthorizationServiceImpl(Model casbinModel, 
                                   RedisCasbinAdapter redisCasbinAdapter,
                                   CasbinPolicyCacheService policyCacheService) {
        this.casbinModel = casbinModel;
        this.redisCasbinAdapter = redisCasbinAdapter;
        this.policyCacheService = policyCacheService;
        logger.info("AuthorizationService initialized with Redis-based Casbin policy caching");
    }
    
    /**
     * Get or create an enforcer for a specific account.
     * Policies are loaded from Redis cache or database if not cached.
     * If cache is empty, ensures enforcer is recreated with fresh policies.
     */
    private Enforcer getEnforcerForAccount(Long accountId) {
        // Check if policies are cached - if not, remove enforcer from cache to force reload
        List<String> cachedPolicies = policyCacheService.getCachedPolicies(accountId);
        if (cachedPolicies == null || cachedPolicies.isEmpty()) {
            // Cache is empty - remove enforcer from cache to force recreation with fresh policies
            Enforcer removed = enforcerCache.remove(accountId);
            if (removed != null) {
                logger.info("🔄 [AUTH-SERVICE] Cache empty for account {} - removing enforcer from cache to force reload", accountId);
            } else {
                logger.debug("🔄 [AUTH-SERVICE] Cache empty for account {} - no enforcer in cache to remove", accountId);
            }
        }
        
        return enforcerCache.computeIfAbsent(accountId, accId -> {
            logger.info("🔄 [AUTH-SERVICE] Creating new enforcer for account: {}", accId);
            Enforcer accountEnforcer = new Enforcer(casbinModel, redisCasbinAdapter);
            accountEnforcer.enableAutoSave(false); // We manage policies via database
            
            // Load policies for this account (will use Redis cache if available, or load from database)
            try {
                redisCasbinAdapter.loadPolicyForAccount(accountEnforcer.getModel(), accId);
                // Verify policies were loaded
                int policyCount = accountEnforcer.getPolicy().size();
                logger.info("✅ [AUTH-SERVICE] Loaded {} policies for account: {}", policyCount, accId);
                if (policyCount == 0) {
                    logger.error("❌ [AUTH-SERVICE] No policies loaded for account: {} - authorization checks will fail! " +
                        "This may indicate missing memberships, roles, or role-privilege mappings.", accId);
                } else {
                    logger.debug("✅ [AUTH-SERVICE] Enforcer created successfully with {} policies for account: {}", policyCount, accId);
                }
            } catch (Exception e) {
                logger.error("❌ [AUTH-SERVICE] Error loading policies for account {}: {}", accId, e.getMessage(), e);
                // Remove enforcer from cache on error so it can be retried
                enforcerCache.remove(accId);
                throw new RuntimeException("Failed to load policies for account: " + accId, e);
            }
            
            return accountEnforcer;
        });
    }
    
    /**
     * Get lock for an account's enforcer to ensure thread-safe policy reloads.
     */
    private ReentrantLock getLockForAccount(Long accountId) {
        return enforcerLocks.computeIfAbsent(accountId, accId -> new ReentrantLock());
    }

    @Override
    public boolean isChannelRequest() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            if (attr == null) {
                return false;
            }
            HttpServletRequest request = attr.getRequest();
            if (request == null) {
                return false;
            }
            
            // Check if request URI starts with /channel/
            String requestURI = request.getRequestURI();
            if (requestURI != null && requestURI.startsWith("/channel/")) {
                logger.debug("Channel request detected via URI: {}", requestURI);
                return true;
            }
            
            // Check if channelAuth attribute is set (set by ChannelAuthFilter)
            Object channelAuthAttr = request.getAttribute("channelAuth");
            if (Boolean.TRUE.equals(channelAuthAttr)) {
                logger.debug("Channel request detected via channelAuth attribute");
                return true;
            }
            
            return false;
        } catch (Exception e) {
            // If we can't access request context, assume it's not a channel request
            logger.debug("Could not determine if request is channel request: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean checkPermission(Long userId, Long accountId, String resource, String action) {
        // Skip privilege checks for channel-authenticated requests
        if (isChannelRequest()) {
            logger.debug("Skipping privilege check for channel request: user={}, account={}, resource={}, action={}", 
                        userId, accountId, resource, action);
            return true;
        }
        
        if (accountId == null) {
            logger.warn("Authorization check failed: accountId is null");
            return false;
        }
        
        try {
            Enforcer enforcer = getEnforcerForAccount(accountId);
            // Subject format: userId:accountId
            String subject = userId + ":" + accountId;
            boolean allowed = enforcer.enforce(subject, resource, action);
            logger.debug("Authorization check: user={}, account={}, resource={}, action={}, allowed={}", 
                        userId, accountId, resource, action, allowed);
            return allowed;
        } catch (Exception e) {
            logger.error("Error during authorization check for user={}, account={}: {}", 
                        userId, accountId, e.getMessage(), e);
            return false; // Fail closed - deny access on error
        }
    }

    @Override
    public boolean hasPrivilege(Long userId, Long accountId, String privilegeCode) {
        // Skip privilege checks for channel-authenticated requests
        if (isChannelRequest()) {
            logger.debug("Skipping privilege check for channel request: user={}, account={}, privilege={}", 
                        userId, accountId, privilegeCode);
            return true;
        }
        
        // Parse privilege code (e.g., "user.create" -> resource="user", action="create")
        String[] parts = privilegeCode.split("\\.");
        if (parts.length != 2) {
            logger.warn("Invalid privilege code format: {}", privilegeCode);
            return false;
        }
        String resource = parts[0];
        String action = parts[1];
        return checkPermission(userId, accountId, resource, action);
    }
    
    @Override
    public void reloadPolicies() {
        logger.warn("🔄 [AUTH-SERVICE] reloadPolicies() called without accountId - clearing all caches");
        // Clear all account caches and remove enforcers
        policyCacheService.clearAllCaches();
        enforcerCache.clear();
        enforcerLocks.clear();
        logger.info("✅ [AUTH-SERVICE] All Casbin policy caches cleared");
    }
    
    @Override
    public void reloadPolicies(Long accountId) {
        if (accountId == null) {
            logger.warn("⚠️ [AUTH-SERVICE] reloadPolicies called with null accountId");
            return;
        }
        
        ReentrantLock lock = getLockForAccount(accountId);
        lock.lock();
        try {
            logger.debug("🔄 [AUTH-SERVICE] Reloading policies for account: {}", accountId);
            
            // Clear Redis cache for this account
            policyCacheService.clearCache(accountId);
            
            // Remove enforcer from cache so it will be recreated with fresh policies
            Enforcer removed = enforcerCache.remove(accountId);
            if (removed != null) {
                logger.debug("Removed enforcer from cache for account: {}", accountId);
            }
            
            logger.info("✅ [AUTH-SERVICE] Policies reloaded for account: {}", accountId);
        } catch (Exception e) {
            logger.error("❌ [AUTH-SERVICE] Error reloading policies for account {}: {}", 
                        accountId, e.getMessage(), e);
            throw e;
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void clearCache(Long accountId) {
        if (accountId == null) {
            logger.warn("⚠️ [AUTH-SERVICE] clearCache called with null accountId");
            return;
        }
        
        ReentrantLock lock = getLockForAccount(accountId);
        lock.lock();
        try {
            logger.debug("🗑️ [AUTH-SERVICE] Clearing cache for account: {}", accountId);
            
            // Clear Redis cache
            policyCacheService.clearCache(accountId);
            
            // Remove enforcer from cache
            enforcerCache.remove(accountId);
            
            logger.info("✅ [AUTH-SERVICE] Cache cleared for account: {}", accountId);
        } catch (Exception e) {
            logger.error("❌ [AUTH-SERVICE] Error clearing cache for account {}: {}", 
                        accountId, e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }
}

