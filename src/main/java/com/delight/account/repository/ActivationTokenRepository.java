package com.delight.account.repository;

import com.delight.account.model.ActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {
    
    /**
     * Find a valid (unused and not expired) activation token
     */
    @Query("SELECT at FROM ActivationToken at WHERE at.token = :token AND at.used = false AND at.expiresAt > :now")
    Optional<ActivationToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);
    
    /**
     * Find the latest activation token for an email and account
     */
    Optional<ActivationToken> findTopByEmailAddressAndAccountIdOrderByCreatedAtDesc(String emailAddress, Long accountId);
    
    /**
     * Delete expired tokens (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM ActivationToken at WHERE at.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * Count unused tokens for an email and account within a time period
     */
    @Query("SELECT COUNT(at) FROM ActivationToken at WHERE at.emailAddress = :emailAddress AND at.accountId = :accountId AND at.used = false AND at.createdAt > :since")
    long countUnusedTokensSince(@Param("emailAddress") String emailAddress, @Param("accountId") Long accountId, @Param("since") LocalDateTime since);
}