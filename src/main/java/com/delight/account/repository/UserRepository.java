package com.delight.account.repository;

import com.delight.account.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByAccountId(Long accountId);

    Optional<User> findByAccountIdAndEmailAddress(Long accountId, String emailAddress);
    
    /**
     * Find user by ID with eagerly loaded account relationship.
     * This prevents LazyInitializationException when accessing account outside of transaction.
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.account WHERE u.id = :id")
    Optional<User> findByIdWithAccount(@Param("id") Long id);
}
