package com.delight.account.service.impl;

import com.delight.account.dto.AccountInfoResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.model.Membership;
import com.delight.account.service.AccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final MembershipRepository membershipRepository;

    public AccountServiceImpl(AccountRepository accountRepository, MembershipRepository membershipRepository) {
        this.accountRepository = accountRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountInfoResponse getAccountInfo(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ApiException("error.account.notfound"));
        Membership membership = membershipRepository.findByUserIdAndAccountId(userId, accountId).orElse(null);

        return AccountInfoResponse.builder()
            .id(account.getId())
            .domain(account.getDomain())
            .plan(account.getPlan())
            .status(account.getStatus())
            .industryType(account.getIndustryType())
            .email(account.getEmail())
            .companyName(account.getCompanyName())
            .currentUserId(userId)
            .currentUserRole(membership != null && membership.getRole() != null ? membership.getRole().name() : null)
            .organizationId(account.getOrganization() != null ? account.getOrganization().getId() : null)
            .organizationName(account.getOrganization() != null ? account.getOrganization().getName() : null)
            .organizationPrimaryDomain(account.getOrganization() != null ? account.getOrganization().getPrimaryDomain() : null)
            .productCode(account.getProduct() != null ? account.getProduct().getCode() : null)
            .productName(account.getProduct() != null ? account.getProduct().getName() : null)
            .build();
    }
}
