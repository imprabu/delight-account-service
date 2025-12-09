package com.delight.account.config;

import com.delight.account.model.Membership;
import com.delight.account.model.RoleEntity;
import com.delight.account.model.RolePrivilege;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.RolePrivilegeRepository;
import com.delight.account.repository.RoleRepository;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.jcasbin.persist.BatchAdapter;
import org.casbin.jcasbin.persist.Helper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Legacy Casbin adapter that loads all policies from database (no Redis caching).
 * 
 * @deprecated Use RedisCasbinAdapter instead for per-tenant Redis caching.
 * This adapter is kept for backward compatibility but should not be used in new code.
 */
@Deprecated
@Component
public class CasbinAdapter implements Adapter, BatchAdapter {

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RolePrivilegeRepository rolePrivilegeRepository;

    @Override
    @Transactional(readOnly = true)
    public void loadPolicy(org.casbin.jcasbin.model.Model model) {
        // Load user-role mappings (g: user_id:account_id -> role_id)
        // Use query that eagerly fetches role
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

        // Load role-privilege mappings (g2: role_id -> privilege_code) and policies (p: role_id, resource, action)
        // Use query that eagerly fetches privileges
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
                // Parse privilege code (e.g., "user.create" -> resource="user", action="create")
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

    @Override
    public void savePolicy(org.casbin.jcasbin.model.Model model) {
        // Casbin adapter is read-only for now
        // Policies are managed through the database entities (Role, RolePrivilege, Membership)
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
