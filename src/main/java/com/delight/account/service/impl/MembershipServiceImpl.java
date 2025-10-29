package com.delight.account.service.impl;

import com.delight.account.dto.MemberResponse;
import com.delight.account.dto.UpdateMemberRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Membership;
import com.delight.account.model.Role;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.AccountRepository;
import com.delight.account.service.MembershipService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipServiceImpl implements MembershipService {

    private final MembershipRepository membershipRepository;
    private final AccountRepository accountRepository;

    public MembershipServiceImpl(MembershipRepository membershipRepository, AccountRepository accountRepository) {
        this.membershipRepository = membershipRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long accountId, String productCode) {
        ensureAccountProduct(accountId, productCode);
        return membershipRepository.findByAccountId(accountId).stream()
            .map(m -> MemberResponse.builder()
                .userId(m.getUser().getId())
                .emailAddress(m.getUser().getEmailAddress())
                .firstName(m.getUser().getFirstName())
                .lastName(m.getUser().getLastName())
                .role(m.getRole())
                .status(m.getStatus())
                .build())
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MemberResponse updateMember(Long accountId, Long targetUserId, UpdateMemberRequest request, Role actorRole, String productCode) {
        if (actorRole == null || (actorRole != Role.ADMIN && actorRole != Role.OWNER)) {
            throw new ApiException("error.authorization");
        }
        ensureAccountProduct(accountId, productCode);

        Membership membership = membershipRepository
            .findByUserIdAndAccountId(targetUserId, accountId)
            .orElseThrow(() -> new ApiException("error.user.notfound"));

        if (request.getRole() != null) {
            membership.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            membership.setStatus(request.getStatus());
        }
        membershipRepository.save(membership);

        // Cache invalidation is now handled automatically by CacheInvalidationEntityListener

        return MemberResponse.builder()
            .userId(membership.getUser().getId())
            .emailAddress(membership.getUser().getEmailAddress())
            .firstName(membership.getUser().getFirstName())
            .lastName(membership.getUser().getLastName())
            .role(membership.getRole())
            .status(membership.getStatus())
            .build();
    }

    private void ensureAccountProduct(Long accountId, String productCode) {
        if (productCode == null) {
            throw new ApiException("error.authorization");
        }
        var account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ApiException("error.account.notfound"));
        var acctProduct = account.getProduct();
        if (acctProduct == null || acctProduct.getCode() == null || !acctProduct.getCode().equals(productCode)) {
            throw new ApiException("error.authorization");
        }
    }
}
