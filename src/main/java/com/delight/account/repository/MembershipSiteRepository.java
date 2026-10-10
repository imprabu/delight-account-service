package com.delight.account.repository;

import com.delight.account.model.MembershipSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MembershipSiteRepository extends JpaRepository<MembershipSite, Long> {
    List<MembershipSite> findByMembershipId(Long membershipId);

    @Query("select ms from MembershipSite ms join fetch ms.site where ms.membership.id = :membershipId")
    List<MembershipSite> findByMembershipIdWithSite(@Param("membershipId") Long membershipId);

    void deleteByMembershipId(Long membershipId);
}
