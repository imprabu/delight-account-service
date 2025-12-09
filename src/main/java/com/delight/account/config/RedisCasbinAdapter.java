package com.delight.account.config;

import com.delight.account.model.Membership;
import com.delight.account.model.RoleEntity;
import com.delight.account.model.RolePrivilege;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.RolePrivilegeRepository;
import com.delight.account.service.CasbinPolicyCacheService;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.jcasbin.persist.BatchAdapter;
import org.casbin.jcasbin.persist.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis-backed Casbin adapter that loads policies per account from database or cache.
 * Policies are cached in Redis per tenant (account) for better performance.
 * 
 * This is the primary adapter used by AuthorizationServiceImpl for per-tenant policy management.
 */
@Component
@org.springframework.context.annotation.Primary
public class RedisCasbinAdapter implements Adapter, BatchAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RedisCasbinAdapter.class);

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private RolePrivilegeRepository rolePrivilegeRepository;

    @Autowired
    private CasbinPolicyCacheService policyCacheService;

    /**
     * Load policies for a specific account.
     * First checks Redis cache, then loads from database if not cached.
     * 
     * @param model The Casbin model
     * @param accountId The account ID to load policies for
     */
    public void loadPolicyForAccount(org.casbin.jcasbin.model.Model model, Long accountId) {
        logger.debug("Loading Casbin policies for account: {}", accountId);
        
        // Try to get from cache first
        List<String> cachedPolicies = policyCacheService.getCachedPolicies(accountId);
        if (cachedPolicies != null && !cachedPolicies.isEmpty()) {
            logger.debug("Loading {} policies from Redis cache for account: {}", cachedPolicies.size(), accountId);
            for (String policyLine : cachedPolicies) {
                Helper.loadPolicyLine(policyLine, model);
            }
            return;
        }
        
        // Cache miss - load from database
        logger.debug("Cache miss - loading policies from database for account: {}", accountId);
        List<String> policies = loadPoliciesFromDatabase(accountId);
        
        // Cache the policies
        if (!policies.isEmpty()) {
            policyCacheService.cachePolicies(accountId, policies);
        }
        
        // Load policies into model
        for (String policyLine : policies) {
            Helper.loadPolicyLine(policyLine, model);
        }
    }

    /**
     * Load all policies from database (for backward compatibility and global operations).
     */
    @Override
    @Transactional(readOnly = true)
    public void loadPolicy(org.casbin.jcasbin.model.Model model) {
        logger.debug("Loading all Casbin policies from database (no account filter)");
        
        // Load user-role mappings (g: user_id:account_id -> role_id)
        List<Membership> memberships = membershipRepository.findAllWithRoles();
        for (Membership membership : memberships) {
            if (membership.getRole() != null && membership.getStatus() == com.delight.account.model.MembershipStatus.ACTIVE) {
                String line = String.format("g, %d:%d, %d", 
                    membership.getUser().getId(), 
                    membership.getAccount().getId(),
                    membership.getRole().getId());
                Helper.loadPolicyLine(line, model);
            }
        }

        // Load role-privilege mappings and policies
        List<RolePrivilege> allRolePrivileges = rolePrivilegeRepository.findAllWithPrivileges();
        for (RolePrivilege rp : allRolePrivileges) {
            RoleEntity role = rp.getRole();
            if (role != null && role.getIsActive() != null && role.getIsActive()) {
                // Load role-privilege mapping (g2)
                String g2Line = String.format("g2, %d, %s", 
                    role.getId(),
                    rp.getPrivilege().getCode());
                Helper.loadPolicyLine(g2Line, model);
                
                // Load policy (p: role_id, resource, action)
                String privilegeCode = rp.getPrivilege().getCode();
                String[] parts = privilegeCode.split("\\.");
                if (parts.length == 2) {
                    String resource = parts[0];
                    String action = parts[1];
                    String pLine = String.format("p, %d, %s, %s", 
                        role.getId(),
                        resource,
                        action);
                    Helper.loadPolicyLine(pLine, model);
                }
            }
        }
    }

    /**
     * Load policies from database for a specific account.
     * 
     * @param accountId The account ID
     * @return List of policy lines
     */
    @Transactional(readOnly = true)
    private List<String> loadPoliciesFromDatabase(Long accountId) {
        List<String> policies = new ArrayList<>();
        
        // Load user-role mappings for this account (g: user_id:account_id -> role_id)
        List<Membership> memberships = membershipRepository.findByAccountIdAndStatus(
            accountId, 
            com.delight.account.model.MembershipStatus.ACTIVE
        );
        
        for (Membership membership : memberships) {
            if (membership.getRole() != null) {
                String line = String.format("g, %d:%d, %d", 
                    membership.getUser().getId(), 
                    membership.getAccount().getId(),
                    membership.getRole().getId());
                policies.add(line);
            }
        }

        // Load role-privilege mappings for roles in this account
        // Get all role IDs for this account
        Set<Long> roleIds = memberships.stream()
            .map(m -> m.getRole() != null ? m.getRole().getId() : null)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
        
        if (!roleIds.isEmpty()) {
            List<RolePrivilege> rolePrivileges = rolePrivilegeRepository.findByRoleIdInAndRoleAccountId(roleIds, accountId);
            
            for (RolePrivilege rp : rolePrivileges) {
                RoleEntity role = rp.getRole();
                if (role != null && role.getIsActive() != null && role.getIsActive()) {
                    // Load role-privilege mapping (g2)
                    String g2Line = String.format("g2, %d, %s", 
                        role.getId(),
                        rp.getPrivilege().getCode());
                    policies.add(g2Line);
                    
                    // Load policy (p: role_id, resource, action)
                    String privilegeCode = rp.getPrivilege().getCode();
                    String[] parts = privilegeCode.split("\\.");
                    if (parts.length == 2) {
                        String resource = parts[0];
                        String action = parts[1];
                        String pLine = String.format("p, %d, %s, %s", 
                            role.getId(),
                            resource,
                            action);
                        policies.add(pLine);
                    }
                }
            }
        }
        
        logger.debug("Loaded {} policies from database for account: {}", policies.size(), accountId);
        return policies;
    }

    @Override
    public void savePolicy(org.casbin.jcasbin.model.Model model) {
        throw new UnsupportedOperationException("savePolicy is not supported. Use database entities to manage policies.");
    }

    @Override
    public void addPolicy(String sec, String ptype, List<String> rule) {
        throw new UnsupportedOperationException("addPolicy is not supported. Use database entities to manage policies.");
    }

    @Override
    public void removePolicy(String sec, String ptype, List<String> rule) {
        throw new UnsupportedOperationException("removePolicy is not supported. Use database entities to manage policies.");
    }

    @Override
    public void removeFilteredPolicy(String sec, String ptype, int fieldIndex, String... fieldValues) {
        throw new UnsupportedOperationException("removeFilteredPolicy is not supported. Use database entities to manage policies.");
    }

    @Override
    public void addPolicies(String sec, String ptype, List<List<String>> rules) {
        throw new UnsupportedOperationException("addPolicies is not supported. Use database entities to manage policies.");
    }

    @Override
    public void removePolicies(String sec, String ptype, List<List<String>> rules) {
        throw new UnsupportedOperationException("removePolicies is not supported. Use database entities to manage policies.");
    }
}

