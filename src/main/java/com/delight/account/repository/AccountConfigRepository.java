package com.delight.account.repository;

import com.delight.account.model.AccountConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountConfigRepository extends JpaRepository<AccountConfig, Long> {
    
    /**
     * Find active configuration by account ID and config type
     */
    @Query("SELECT ac FROM AccountConfig ac WHERE ac.accountId = :accountId AND ac.configType = :configType AND ac.isActive = true")
    Optional<AccountConfig> findByAccountIdAndConfigTypeAndIsActiveTrue(@Param("accountId") Long accountId, @Param("configType") String configType);
    
    /**
     * Find all active configurations for an account
     */
    @Query("SELECT ac FROM AccountConfig ac WHERE ac.accountId = :accountId AND ac.isActive = true")
    List<AccountConfig> findByAccountIdAndIsActiveTrue(@Param("accountId") Long accountId);
    
    /**
     * Find all configurations by config type (across all accounts)
     */
    @Query("SELECT ac FROM AccountConfig ac WHERE ac.configType = :configType AND ac.isActive = true")
    List<AccountConfig> findByConfigTypeAndIsActiveTrue(@Param("configType") String configType);
    
    /**
     * Check if a configuration exists for account and type
     */
    @Query("SELECT COUNT(ac) > 0 FROM AccountConfig ac WHERE ac.accountId = :accountId AND ac.configType = :configType AND ac.isActive = true")
    boolean existsByAccountIdAndConfigTypeAndIsActiveTrue(@Param("accountId") Long accountId, @Param("configType") String configType);
    
    /**
     * Deactivate all configurations of a specific type for an account
     */
    @Query("UPDATE AccountConfig ac SET ac.isActive = false WHERE ac.accountId = :accountId AND ac.configType = :configType")
    void deactivateByAccountIdAndConfigType(@Param("accountId") Long accountId, @Param("configType") String configType);
}