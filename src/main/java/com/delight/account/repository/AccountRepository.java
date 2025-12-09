package com.delight.account.repository;

import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByDomain(String domain);

    Optional<Account> findByDomainAndStatus(String domain, AccountStatus status);
    
    /**
     * Find account by ID with eagerly loaded Organization and Product relationships.
     * This is used by the AccountEventPublisherWorker to avoid LazyInitializationException.
     */
    @Query("SELECT a FROM Account a " +
           "LEFT JOIN FETCH a.organization " +
           "LEFT JOIN FETCH a.product " +
           "WHERE a.id = :id")
    Optional<Account> findByIdWithRelations(@Param("id") Long id);
    
    /**
     * Find all active accounts with eagerly loaded Organization and Product relationships.
     * This is used by the BulkAccountSyncWorker to publish account_modified events for all active accounts.
     */
    @Query("SELECT a FROM Account a " +
           "LEFT JOIN FETCH a.organization " +
           "LEFT JOIN FETCH a.product " +
           "WHERE a.status = :status")
    java.util.List<Account> findAllByStatusWithRelations(@Param("status") AccountStatus status);
}
