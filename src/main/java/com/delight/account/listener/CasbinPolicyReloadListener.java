package com.delight.account.listener;

import com.delight.account.model.Membership;
import com.delight.account.model.RoleEntity;
import com.delight.account.model.RolePrivilege;
import com.delight.account.service.AuthorizationService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * JPA Entity Listener for automatic Casbin policy reloading.
 * This listener ensures Casbin policies are refreshed when role-privilege mappings,
 * memberships, or roles are modified.
 */
@Slf4j
@Component
public class CasbinPolicyReloadListener implements ApplicationContextAware {

    private static ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CasbinPolicyReloadListener.applicationContext = applicationContext;
    }
    
    private static AuthorizationService getAuthorizationService() {
        if (applicationContext == null) {
            return null;
        }
        try {
            return applicationContext.getBean(AuthorizationService.class);
        } catch (BeansException | IllegalStateException e) {
            log.warn("Failed to get AuthorizationService bean: {}", e.getMessage());
            return null;
        }
    }

    @PostPersist
    public void onPostPersist(Object entity) {
        handleEntityChange(entity, "CREATE");
    }

    @PostUpdate
    public void onPostUpdate(Object entity) {
        handleEntityChange(entity, "UPDATE");
    }

    @PostRemove
    public void onPostRemove(Object entity) {
        handleEntityChange(entity, "DELETE");
    }

    private void handleEntityChange(Object entity, String operation) {
        String entityType = entity.getClass().getSimpleName();
        
        log.debug("🔔 [CASBIN-LISTENER] Entity listener triggered - Operation: {}, Entity: {}", operation, entityType);
        
        // Handle RolePrivilege changes - need to get accountId from the role
        if (entity instanceof RolePrivilege) {
            handleRolePrivilegeChange((RolePrivilege) entity, operation);
            return;
        }
        
        // Skip @PostPersist for Membership during signup - only reload on updates/deletes
        // This prevents issues when new memberships are being created in the same transaction
        // The policy reload will happen after transaction commits via AccountEventJobEnqueueListener
        if (entity instanceof Membership && "CREATE".equals(operation)) {
            Membership membership = (Membership) entity;
            Long membershipId = membership.getId();
            log.debug("⏭️ [CASBIN-LISTENER] Skipping policy reload for Membership @PostPersist - MembershipId: {} (will reload after transaction commits or on update/delete)", 
                membershipId);
            return;
        }
        
        // Skip @PostPersist for RoleEntity during initialization - reload happens after all roles are created
        if (entity instanceof RoleEntity && "CREATE".equals(operation)) {
            RoleEntity role = (RoleEntity) entity;
            Long roleId = role.getId();
            log.debug("⏭️ [CASBIN-LISTENER] Skipping policy reload for RoleEntity @PostPersist - RoleId: {} (will reload after transaction commits)", 
                roleId);
            return;
        }
        
        // Skip if entity doesn't have an ID (for PostPersist, ID should be set, but check to be safe)
        if (entity instanceof Membership) {
            Membership membership = (Membership) entity;
            if (membership.getId() == null) {
                log.debug("⏭️ [CASBIN-LISTENER] Skipping policy reload - Membership entity has no ID yet");
                return;
            }
        } else if (entity instanceof RoleEntity) {
            RoleEntity role = (RoleEntity) entity;
            if (role.getId() == null) {
                log.debug("⏭️ [CASBIN-LISTENER] Skipping policy reload - RoleEntity has no ID yet");
                return;
            }
        }
        
        AuthorizationService authorizationService = getAuthorizationService();
        if (authorizationService == null) {
            log.warn("⚠️ [CASBIN-LISTENER] Policy reload skipped - AuthorizationService not available");
            return;
        }

        try {
            log.debug("🔄 [CASBIN-LISTENER] Processing policy reload for {} operation on entity: {}", 
                operation, entityType);
            
            if (entity instanceof Membership) {
                handleMembershipChange((Membership) entity, operation);
            } else if (entity instanceof RoleEntity) {
                handleRoleChange((RoleEntity) entity, operation);
            } else if (entity instanceof RolePrivilege) {
                // Already handled above, but keep for completeness
                handleRolePrivilegeChange((RolePrivilege) entity, operation);
            } else {
                log.debug("ℹ️ [CASBIN-LISTENER] No policy reload needed for entity type: {}", entityType);
            }
        } catch (Exception e) {
            // Log error but don't fail the transaction
            // Suppress exception to prevent transaction rollback
            log.error("❌ [CASBIN-LISTENER] Failed to reload Casbin policies for entity: {} - Error: {}", 
                entityType, e.getMessage(), e);
        }
    }

    private void handleMembershipChange(Membership membership, String operation) {
        Long membershipId = membership.getId();
        log.debug("🔍 [CASBIN-LISTENER] Handling Membership change - Operation: {}, MembershipId: {}", operation, membershipId);
        
        // Skip if membership doesn't have required IDs
        if (membershipId == null) {
            log.debug("⏭️ [CASBIN-LISTENER] Skipping policy reload - Membership has no ID");
            return;
        }
        
        Long userId = membership.getUser() != null ? membership.getUser().getId() : null;
        Long accountId = membership.getAccount() != null ? membership.getAccount().getId() : null;
        Long roleId = membership.getRole() != null ? membership.getRole().getId() : null;
        
        log.debug("🔍 [CASBIN-LISTENER] Membership details - UserId: {}, AccountId: {}, RoleId: {}", 
            userId, accountId, roleId);
        
        // Only reload if we have accountId (required for per-tenant cache clearing)
        if (accountId == null) {
            log.debug("⏭️ [CASBIN-LISTENER] Skipping policy reload - Membership missing accountId");
            return;
        }
        
        log.info("🔄 [CASBIN-LISTENER] Clearing Redis cache and reloading policies for account: {} - Membership {} - UserId: {}, RoleId: {}", 
            accountId, operation, userId, roleId);
        
        reloadPoliciesForAccount(accountId);
    }

    private void handleRoleChange(RoleEntity role, String operation) {
        Long roleId = role.getId();
        Long accountId = role.getAccount() != null ? role.getAccount().getId() : null;
        Boolean isActive = role.getIsActive();
        
        log.debug("🔍 [CASBIN-LISTENER] Handling Role change - Operation: {}, RoleId: {}, AccountId: {}, IsActive: {}", 
            operation, roleId, accountId, isActive);
        
        // Need accountId to clear the correct Redis cache
        if (accountId == null) {
            log.warn("⚠️ [CASBIN-LISTENER] Cannot reload policies - Role missing accountId: RoleId: {}", roleId);
            return;
        }
        
        // Reload if role status changed (activated/deactivated) or if it's a delete
        // CREATE is already handled in handleEntityChange, so this should only be UPDATE or DELETE
        if ("DELETE".equals(operation) || (isActive != null && !isActive)) {
            log.info("🔄 [CASBIN-LISTENER] Clearing Redis cache and reloading policies for account: {} - Role {} - RoleId: {}, IsActive: {}", 
                accountId, operation, roleId, isActive);
            reloadPoliciesForAccount(accountId);
        } else {
            log.debug("⏭️ [CASBIN-LISTENER] Skipping policy reload for role update - RoleId: {}, IsActive: {} (no policy impact)", 
                roleId, isActive);
        }
    }

    private void handleRolePrivilegeChange(RolePrivilege rolePrivilege, String operation) {
        Long rolePrivilegeId = rolePrivilege.getId();
        RoleEntity role = rolePrivilege.getRole();
        Long accountId = role != null && role.getAccount() != null ? role.getAccount().getId() : null;
        Long roleId = role != null ? role.getId() : null;
        
        log.debug("🔍 [CASBIN-LISTENER] Handling RolePrivilege change - Operation: {}, RolePrivilegeId: {}, RoleId: {}, AccountId: {}", 
            operation, rolePrivilegeId, roleId, accountId);
        
        // Need accountId to clear the correct Redis cache
        if (accountId == null) {
            log.warn("⚠️ [CASBIN-LISTENER] Cannot reload policies - RolePrivilege missing accountId: RolePrivilegeId: {}", rolePrivilegeId);
            return;
        }
        
        log.info("🔄 [CASBIN-LISTENER] Clearing Redis cache and reloading policies for account: {} - RolePrivilege {} - RolePrivilegeId: {}, RoleId: {}", 
            accountId, operation, rolePrivilegeId, roleId);
        
        reloadPoliciesForAccount(accountId);
    }

    /**
     * Reload policies for a specific account (clears Redis cache and reloads from database).
     */
    private void reloadPoliciesForAccount(Long accountId) {
        AuthorizationService authorizationService = getAuthorizationService();
        if (authorizationService == null) {
            log.warn("⚠️ [CASBIN-LISTENER] Cannot reload policies - AuthorizationService not available");
            return;
        }
        
        try {
            log.debug("🔄 [CASBIN-LISTENER] Calling authorizationService.reloadPolicies(accountId: {})...", accountId);
            authorizationService.reloadPolicies(accountId);
            log.info("✅ [CASBIN-LISTENER] Casbin policies reloaded for account: {}", accountId);
        } catch (Exception e) {
            log.error("❌ [CASBIN-LISTENER] Failed to reload Casbin policies for account {}: {}", 
                     accountId, e.getMessage(), e);
            // Don't throw - we don't want to fail the database transaction
        }
    }
    
    /**
     * Legacy method for backward compatibility (reloads all policies).
     * Should not be used in normal operation - use reloadPoliciesForAccount instead.
     */
    private void reloadPoliciesSafely() {
        AuthorizationService authorizationService = getAuthorizationService();
        if (authorizationService == null) {
            log.warn("⚠️ [CASBIN-LISTENER] Cannot reload policies - AuthorizationService not available");
            return;
        }
        
        try {
            log.warn("⚠️ [CASBIN-LISTENER] Calling authorizationService.reloadPolicies() without accountId - clearing all caches");
            authorizationService.reloadPolicies();
            log.info("✅ [CASBIN-LISTENER] All Casbin policies reloaded");
        } catch (Exception e) {
            log.error("❌ [CASBIN-LISTENER] Failed to reload Casbin policies: {}", e.getMessage(), e);
            // Don't throw - we don't want to fail the database transaction
        }
    }
}

