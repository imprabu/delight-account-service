package com.delight.account.repository;

import com.delight.account.model.Account;
import com.delight.account.model.Membership;
import com.delight.account.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByUserAndAccount(User user, Account account);
    Optional<Membership> findByUserIdAndAccountId(Long userId, Long accountId);
    List<Membership> findByAccountId(Long accountId);
}
