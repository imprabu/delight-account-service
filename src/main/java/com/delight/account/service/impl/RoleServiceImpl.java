package com.delight.account.service.impl;

import com.delight.account.dto.RoleResponse;
import com.delight.account.model.*;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.PrivilegeRepository;
import com.delight.account.repository.RolePrivilegeRepository;
import com.delight.account.repository.RoleRepository;
import com.delight.account.service.AuthorizationService;
import com.delight.account.service.RoleService;
import com.delight.account.util.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PrivilegeRepository privilegeRepository;

    @Autowired
    private RolePrivilegeRepository rolePrivilegeRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired(required = false)
    private AuthorizationService authorizationService;

    @Override
    @Transactional
    public RoleEntity initializeDefaultRolesForAccount(Long accountId) {
        LoggingUtils.info(logger,"Initializing default roles for account: {}", accountId);
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        // Get all privileges (they are global, not account-spcific)
        List<Privilege> allPrivileges = privilegeRepository.findAll();
        
        // Create ORG_ADMIN role (highest level - all privileges)
        RoleEntity orgAdminRole = createRole(account, Role.ORG_ADMIN.getCode(), Role.ORG_ADMIN.getDisplayName(),
            Role.ORG_ADMIN.getDescription(), RoleType.SYSTEM, null, 1);
        assignAllPrivileges(orgAdminRole, allPrivileges);
        
        // Create ACCOUNT_ADMIN role (management privileges)
        RoleEntity accountAdminRole = createRole(account, Role.ACCOUNT_ADMIN.getCode(), Role.ACCOUNT_ADMIN.getDisplayName(),
            Role.ACCOUNT_ADMIN.getDescription(), RoleType.SYSTEM, orgAdminRole, 2);
        assignManagementPrivileges(accountAdminRole, allPrivileges);
        
        // Create AGENT role (basic privileges)
        RoleEntity agentRole = createRole(account, Role.AGENT.getCode(), Role.AGENT.getDisplayName(),
            Role.AGENT.getDescription(), RoleType.SYSTEM, accountAdminRole, 3);
        assignBasicPrivileges(agentRole, allPrivileges);

        LoggingUtils.info(logger,"✅ [ROLE-SERVICE] Default roles initialized for account: {} - ORG_ADMIN, ACCOUNT_ADMIN, AGENT", accountId);
        
        // Reload Casbin policies once after all roles and privileges are created
        // This is more efficient than reloading on each RolePrivilege @PostPersist
        // Clear Redis cache for this account and reload policies
        if (authorizationService != null) {
            LoggingUtils.debug(logger,"🔄 [ROLE-SERVICE] About to reload Casbin policies after role initialization for account: {}", accountId);
            try {
                authorizationService.reloadPolicies(accountId);
                LoggingUtils.info(logger,"✅ [ROLE-SERVICE] Casbin policies reloaded (Redis cache cleared) after default role initialization for account: {}", accountId);
            } catch (Exception e) {
                LoggingUtils.error(logger,"❌ [ROLE-SERVICE] Failed to reload Casbin policies after role initialization for account {}: {}",
                    accountId, e.getMessage(), e);
                // Don't fail the transaction if policy reload fails - but log the error
            }
        } else {
            LoggingUtils.warn(logger,"⚠️ [ROLE-SERVICE] AuthorizationService is null - cannot reload policies");
        }
        
        return orgAdminRole;
    }

    private RoleEntity createRole(Account account, String code, String name, String description,
                                 RoleType roleType, RoleEntity baseRole, int hierarchyLevel) {
        RoleEntity role = new RoleEntity();
        role.setAccount(account);
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setRoleType(roleType);
        role.setBaseRole(baseRole);
        role.setHierarchyLevel(hierarchyLevel);
        role.setIsActive(true);
        return roleRepository.save(role);
    }

    private void assignAllPrivileges(RoleEntity role, List<Privilege> privileges) {
        for (Privilege privilege : privileges) {
            RolePrivilege rolePrivilege = new RolePrivilege();
            rolePrivilege.setRole(role);
            rolePrivilege.setPrivilege(privilege);
            rolePrivilegeRepository.save(rolePrivilege);
        }
        LoggingUtils.debug(logger,"Assigned all {} privileges to role: {}", privileges.size(), role.getCode());
    }

    private void assignManagementPrivileges(RoleEntity role, List<Privilege> privileges) {
        // Management privileges: user management, account management, role management, data access
        // Excludes: user.delete, account.delete, org.delete, system privileges
        String[] allowedCodes = {
            "user.create", "user.read", "user.update", "user.invite", "user.activate", "user.suspend",
            "account.read", "account.update", "account.billing", "account.settings",
            "role.create", "role.read", "role.update", "role.delete", "role.assign",
            "org.read",
            "data.read", "data.write", "data.delete", "data.export"
        };
        
        for (Privilege privilege : privileges) {
            for (String code : allowedCodes) {
                if (code.equals(privilege.getCode())) {
                    RolePrivilege rolePrivilege = new RolePrivilege();
                    rolePrivilege.setRole(role);
                    rolePrivilege.setPrivilege(privilege);
                    rolePrivilegeRepository.save(rolePrivilege);
                    break;
                }
            }
        }
        LoggingUtils.debug(logger,"Assigned management privileges to role: {}", role.getCode());
    }

    private void assignBasicPrivileges(RoleEntity role, List<Privilege> privileges) {
        // Basic privileges: read access and basic write
        String[] allowedCodes = {
            "user.read",
            "account.read",
            "role.read",
            "org.read",
            "data.read", "data.write"
        };
        
        for (Privilege privilege : privileges) {
            for (String code : allowedCodes) {
                if (code.equals(privilege.getCode())) {
                    RolePrivilege rolePrivilege = new RolePrivilege();
                    rolePrivilege.setRole(role);
                    rolePrivilege.setPrivilege(privilege);
                    rolePrivilegeRepository.save(rolePrivilege);
                    break;
                }
            }
        }
        LoggingUtils.debug(logger,"Assigned basic privileges to role: {}", role.getCode());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles(Long accountId) {
        List<RoleEntity> roles = roleRepository.findByAccountIdAndIsActive(accountId, true);
        
        return roles.stream()
            .sorted((r1, r2) -> Integer.compare(r1.getHierarchyLevel(), r2.getHierarchyLevel()))
            .map(role -> RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .roleType(role.getRoleType())
                .isCustom(role.getRoleType() == RoleType.CUSTOM)
                .hierarchyLevel(role.getHierarchyLevel())
                .build())
            .collect(Collectors.toList());
    }
}


