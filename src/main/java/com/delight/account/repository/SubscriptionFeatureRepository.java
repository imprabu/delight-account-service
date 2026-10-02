package com.delight.account.repository;

import com.delight.account.model.SubscriptionFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionFeatureRepository extends JpaRepository<SubscriptionFeature, Long> {
    
    List<SubscriptionFeature> findBySubscriptionId(Long subscriptionId);
    
    @Query("SELECT sf FROM SubscriptionFeature sf " +
           "JOIN FETCH sf.subscription s " +
           "WHERE s.account.id = :accountId AND s.status = 'ACTIVE'")
    List<SubscriptionFeature> findByAccountId(@Param("accountId") Long accountId);
    
    Optional<SubscriptionFeature> findBySubscriptionIdAndFeatureCode(
        Long subscriptionId, 
        String featureCode);
}



