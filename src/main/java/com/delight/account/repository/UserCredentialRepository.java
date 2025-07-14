package com.delight.account.repository;

import com.delight.account.model.UserCredential;
import com.delight.account.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    Optional<UserCredential> findByUser(User user);
}
