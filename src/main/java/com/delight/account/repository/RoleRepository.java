package com.delight.account.repository;

import com.delight.account.model.RoleEntity;
import com.delight.account.model.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByCode(String code);
    
    Optional<RoleEntity> findByCodeAndRoleType(String code, RoleType roleType);
    
    List<RoleEntity> findByAccountId(Long accountId);
    
    List<RoleEntity> findByRoleType(RoleType roleType);
    
    List<RoleEntity> findByAccountIdAndRoleType(Long accountId, RoleType roleType);
    
    List<RoleEntity> findByAccountIdAndIsActive(Long accountId, Boolean isActive);
    
    Optional<RoleEntity> findByAccountIdAndCode(Long accountId, String code);
    
    Optional<RoleEntity> findByAccountIdAndCodeAndRoleType(Long accountId, String code, RoleType roleType);
}

