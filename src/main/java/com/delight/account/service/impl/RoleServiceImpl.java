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
        LoggingUtils.info(logger,"Initializing PlantPulse default roles for account: {}", accountId);
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        List<Privilege> allPrivileges = privilegeRepository.findAll();
        
        // ACCOUNT_ADMIN — full privileges (creator)
        RoleEntity accountAdminRole = createRole(account, Role.ACCOUNT_ADMIN.getCode(), Role.ACCOUNT_ADMIN.getDisplayName(),
            Role.ACCOUNT_ADMIN.getDescription(), RoleType.SYSTEM, null, 1);
        assignAllPrivileges(accountAdminRole, allPrivileges);
        
        // ADMIN — management on assigned sites
        RoleEntity adminRole = createRole(account, Role.ADMIN.getCode(), Role.ADMIN.getDisplayName(),
            Role.ADMIN.getDescription(), RoleType.SYSTEM, accountAdminRole, 2);
        assignManagementPrivileges(adminRole, allPrivileges);
        
        // USER — read / basic
        RoleEntity userRole = createRole(account, Role.USER.getCode(), Role.USER.getDisplayName(),
            Role.USER.getDescription(), RoleType.SYSTEM, adminRole, 3);
        assignBasicPrivileges(userRole, allPrivileges);

        LoggingUtils.info(logger,"Default roles initialized for account: {} - ACCOUNT_ADMIN, ADMIN, USER", accountId);
        
        if (authorizationService != null) {
            try {
                authorizationService.reloadPolicies(accountId);
            } catch (Exception e) {
                LoggingUtils.error(logger,"Failed to reload Casbin policies after role initialization for account {}: {}",
                    accountId, e.getMessage(), e);
            }
        }
        
        return accountAdminRole;
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
            "account.read", "account.update",
            "role.read", "role.assign",
            "member.invite", "member.manage",
            "site.update", "site.view",
            "device.provision", "device.view",
            "measurement.read", "measurement.export",
            "data.read", "data.write", "data.export"
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
            "site.view",
            "device.view",
            "measurement.read",
            "data.read"
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


