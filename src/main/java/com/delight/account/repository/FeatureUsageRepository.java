package com.delight.account.repository;

import com.delight.account.model.FeatureUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeatureUsageRepository extends JpaRepository<FeatureUsage, Long> {
    
    @Query("SELECT fu FROM FeatureUsage fu " +
           "JOIN FETCH fu.account " +
           "JOIN FETCH fu.subscription " +
           "WHERE fu.account.id = :accountId " +
           "AND fu.subscription.id = :subscriptionId " +
           "AND fu.featureCode = :featureCode " +
           "AND fu.billingCycleStartDate = :billingCycleStartDate")
    Optional<FeatureUsage> findByAccountIdAndSubscriptionIdAndFeatureCodeAndBillingCycleStartDate(
        @Param("accountId") Long accountId,
        @Param("subscriptionId") Long subscriptionId,
        @Param("featureCode") String featureCode,
        @Param("billingCycleStartDate") LocalDate billingCycleStartDate);
    
    @Query("SELECT fu FROM FeatureUsage fu " +
           "JOIN FETCH fu.account " +
           "JOIN FETCH fu.subscription " +
           "WHERE fu.account.id = :accountId " +
           "AND fu.subscription.id = :subscriptionId " +
           "AND fu.featureCode = :featureCode " +
           "AND fu.billingCycleStartDate <= :currentDate " +
           "AND fu.billingCycleEndDate >= :currentDate")
    Optional<FeatureUsage> findCurrentUsageByAccountIdAndSubscriptionIdAndFeatureCode(
        @Param("accountId") Long accountId,
        @Param("subscriptionId") Long subscriptionId,
        @Param("featureCode") String featureCode,
        @Param("currentDate") LocalDate currentDate);
    
    List<FeatureUsage> findByAccountId(Long accountId);
    
    List<FeatureUsage> findBySubscriptionId(Long subscriptionId);
    
    @Modifying
    @Query("UPDATE FeatureUsage fu SET fu.usageValue = fu.usageValue + :increment " +
           "WHERE fu.id = :id")
    void incrementUsageValue(@Param("id") Long id, @Param("increment") Long increment);
    
    @Modifying
    @Query("UPDATE FeatureUsage fu SET fu.usageValue = fu.usageValue - :decrement " +
           "WHERE fu.id = :id AND fu.usageValue >= :decrement")
    int decrementUsageValue(@Param("id") Long id, @Param("decrement") Long decrement);
}



