package com.delight.account.repository;

import com.delight.account.model.RolePrivilege;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePrivilegeRepository extends JpaRepository<RolePrivilege, Long> {
    List<RolePrivilege> findByRoleId(Long roleId);
    
    @Query("SELECT rp FROM RolePrivilege rp JOIN FETCH rp.privilege WHERE rp.role.id = :roleId")
    List<RolePrivilege> findByRoleIdWithPrivilege(@Param("roleId") Long roleId);
    
    @Query("SELECT rp FROM RolePrivilege rp JOIN FETCH rp.privilege JOIN FETCH rp.role")
    List<RolePrivilege> findAllWithPrivileges();
    
    /**
     * Find role privileges for roles in a specific account.
     * Used by RedisCasbinAdapter to load policies for a specific account.
     */
    @Query("SELECT rp FROM RolePrivilege rp JOIN FETCH rp.privilege JOIN FETCH rp.role " +
           "WHERE rp.role.id IN :roleIds AND rp.role.account.id = :accountId")
    List<RolePrivilege> findByRoleIdInAndRoleAccountId(@Param("roleIds") java.util.Set<Long> roleIds, 
                                                        @Param("accountId") Long accountId);
    
    void deleteByRoleId(Long roleId);
    
    @Modifying
    @Query("DELETE FROM RolePrivilege rp WHERE rp.role.id = :roleId AND rp.privilege.id = :privilegeId")
    void deleteByRoleIdAndPrivilegeId(@Param("roleId") Long roleId, @Param("privilegeId") Long privilegeId);
    
    boolean existsByRoleIdAndPrivilegeId(Long roleId, Long privilegeId);
}

