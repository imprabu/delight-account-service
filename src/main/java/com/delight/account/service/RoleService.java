package com.delight.account.service;

import com.delight.account.dto.RoleResponse;
import com.delight.account.model.RoleEntity;
import java.util.List;

/**
 * Service for managing roles and privileges.
 */
public interface RoleService {
    /**
     * Initialize default roles for an account during signup.
     * Creates OWNER, ADMIN, and AGENT roles with their default privileges.
     * 
     * @param accountId The account ID to create roles for
     * @return The OWNER role entity (for assigning to the account creator)
     */
    RoleEntity initializeDefaultRolesForAccount(Long accountId);
    
    /**
     * List all active roles for an account (system + custom).
     * 
     * @param accountId The account ID
     * @return List of role responses
     */
    List<RoleResponse> listRoles(Long accountId);
}


