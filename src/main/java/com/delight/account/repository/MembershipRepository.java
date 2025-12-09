package com.delight.account.repository;

import com.delight.account.model.Account;
import com.delight.account.model.Membership;
import com.delight.account.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByUserAndAccount(User user, Account account);
    Optional<Membership> findByUserIdAndAccountId(Long userId, Long accountId);
    
    /**
     * Find membership by userId and accountId with eagerly loaded role, user, and account.
     * This prevents LazyInitializationException when accessing relationships outside of transaction.
     */
    @Query("SELECT m FROM Membership m " +
           "LEFT JOIN FETCH m.role " +
           "LEFT JOIN FETCH m.user " +
           "LEFT JOIN FETCH m.account " +
           "WHERE m.user.id = :userId AND m.account.id = :accountId")
    Optional<Membership> findByUserIdAndAccountIdWithRole(@Param("userId") Long userId, @Param("accountId") Long accountId);
    
    /**
     * Find all memberships with eagerly loaded role, user, and account.
     * Used by CasbinAdapter to load policies without LazyInitializationException.
     */
    @Query("SELECT DISTINCT m FROM Membership m JOIN FETCH m.role JOIN FETCH m.user JOIN FETCH m.account")
    List<Membership> findAllWithRoles();
    
    List<Membership> findByAccountId(Long accountId);
    
    /**
     * Find all active memberships for an account with eagerly loaded role, user, and account.
     * Used by RedisCasbinAdapter to load policies for a specific account.
     */
    @Query("SELECT DISTINCT m FROM Membership m JOIN FETCH m.role JOIN FETCH m.user JOIN FETCH m.account " +
           "WHERE m.account.id = :accountId AND m.status = :status")
    List<Membership> findByAccountIdAndStatus(@Param("accountId") Long accountId, 
                                               @Param("status") com.delight.account.model.MembershipStatus status);
}
