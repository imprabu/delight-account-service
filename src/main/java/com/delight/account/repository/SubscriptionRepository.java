package com.delight.account.repository;

import com.delight.account.model.Subscription;
import com.delight.account.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    @Query("SELECT s FROM Subscription s " +
           "JOIN FETCH s.account " +
           "WHERE s.account.id = :accountId AND s.status IN (:statuses)")
    Optional<Subscription> findActiveByAccountId(
        @Param("accountId") Long accountId,
        @Param("statuses") List<SubscriptionStatus> statuses);
    
    @Query("SELECT s FROM Subscription s " +
           "JOIN FETCH s.account " +
           "WHERE s.account.id = :accountId AND s.productCode = :productCode AND s.status IN (:statuses)")
    Optional<Subscription> findActiveByAccountIdAndProductCode(
        @Param("accountId") Long accountId, 
        @Param("productCode") String productCode,
        @Param("statuses") List<SubscriptionStatus> statuses);
    
    List<Subscription> findByAccountId(Long accountId);
    
    List<Subscription> findByAccountIdAndStatus(Long accountId, SubscriptionStatus status);
    
    @Query("SELECT s.planCode FROM Subscription s " +
           "WHERE s.account.id = :accountId AND s.status IN (:statuses) " +
           "ORDER BY s.createdAt DESC")
    Optional<String> findPlanCodeByAccountId(
        @Param("accountId") Long accountId,
        @Param("statuses") List<SubscriptionStatus> statuses);
}

