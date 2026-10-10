package com.delight.account.repository;

import com.delight.account.model.Site;
import com.delight.account.model.SiteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SiteRepository extends JpaRepository<Site, UUID> {
    List<Site> findByAccountIdAndStatusOrderByCodeAsc(Long accountId, SiteStatus status);
    List<Site> findByAccountIdOrderByCodeAsc(Long accountId);
    Optional<Site> findByAccountIdAndCode(Long accountId, String code);
    Optional<Site> findByIdAndAccountId(UUID id, Long accountId);
    boolean existsByAccountIdAndCode(Long accountId, String code);
}
