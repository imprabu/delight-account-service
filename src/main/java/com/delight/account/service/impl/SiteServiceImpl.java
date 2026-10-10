package com.delight.account.service.impl;

import com.delight.account.dto.CreateSiteRequest;
import com.delight.account.dto.SiteResponse;
import com.delight.account.dto.UpdateSiteRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.*;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.MembershipSiteRepository;
import com.delight.account.repository.SiteRepository;
import com.delight.account.service.SiteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SiteServiceImpl implements SiteService {

    private final SiteRepository siteRepository;
    private final AccountRepository accountRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipSiteRepository membershipSiteRepository;

    public SiteServiceImpl(SiteRepository siteRepository,
                           AccountRepository accountRepository,
                           MembershipRepository membershipRepository,
                           MembershipSiteRepository membershipSiteRepository) {
        this.siteRepository = siteRepository;
        this.accountRepository = accountRepository;
        this.membershipRepository = membershipRepository;
        this.membershipSiteRepository = membershipSiteRepository;
    }

    @Override
    @Transactional
    public SiteResponse create(Long accountId, CreateSiteRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException("error.account.notfound", String.valueOf(accountId)));
        String code = request.getCode().trim();
        if (siteRepository.existsByAccountIdAndCode(accountId, code)) {
            throw new ApiException("error.site.duplicate", code);
        }
        Site site = new Site();
        site.setAccount(account);
        site.setCode(code);
        site.setName(request.getName().trim());
        site.setTimezone(request.getTimezone() == null || request.getTimezone().isBlank() ? "UTC" : request.getTimezone().trim());
        site.setStatus(SiteStatus.ACTIVE);
        return toResponse(siteRepository.save(site));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteResponse> list(Long accountId, Long userId, boolean includeArchived) {
        List<Site> sites = includeArchived
                ? siteRepository.findByAccountIdOrderByCodeAsc(accountId)
                : siteRepository.findByAccountIdAndStatusOrderByCodeAsc(accountId, SiteStatus.ACTIVE);

        Membership membership = membershipRepository.findByUserIdAndAccountId(userId, accountId).orElse(null);
        if (membership == null) {
            return List.of();
        }
        if (membership.getSiteScopeMode() == SiteScopeMode.ALL_SITES
                || (membership.getRole() != null && Role.ACCOUNT_ADMIN.getCode().equals(membership.getRole().getCode()))) {
            return sites.stream().map(this::toResponse).collect(Collectors.toList());
        }
        Set<UUID> allowed = membershipSiteRepository.findByMembershipIdWithSite(membership.getId()).stream()
                .map(ms -> ms.getSite().getId())
                .collect(Collectors.toSet());
        return sites.stream().filter(s -> allowed.contains(s.getId())).map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteResponse> listAllForAccount(Long accountId, boolean includeArchived) {
        List<Site> sites = includeArchived
                ? siteRepository.findByAccountIdOrderByCodeAsc(accountId)
                : siteRepository.findByAccountIdAndStatusOrderByCodeAsc(accountId, SiteStatus.ACTIVE);
        return sites.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SiteResponse getById(Long accountId, UUID siteId) {
        Site site = siteRepository.findByIdAndAccountId(siteId, accountId)
                .orElseThrow(() -> new ApiException("error.site.notfound", siteId.toString()));
        return toResponse(site);
    }

    @Override
    @Transactional(readOnly = true)
    public SiteResponse getByCode(Long accountId, String code) {
        Site site = siteRepository.findByAccountIdAndCode(accountId, code)
                .orElseThrow(() -> new ApiException("error.site.notfound", code));
        return toResponse(site);
    }

    @Override
    @Transactional
    public SiteResponse update(Long accountId, UUID siteId, UpdateSiteRequest request) {
        Site site = siteRepository.findByIdAndAccountId(siteId, accountId)
                .orElseThrow(() -> new ApiException("error.site.notfound", siteId.toString()));
        if (request.getName() != null && !request.getName().isBlank()) {
            site.setName(request.getName().trim());
        }
        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            site.setTimezone(request.getTimezone().trim());
        }
        return toResponse(siteRepository.save(site));
    }

    @Override
    @Transactional
    public SiteResponse archive(Long accountId, UUID siteId) {
        Site site = siteRepository.findByIdAndAccountId(siteId, accountId)
                .orElseThrow(() -> new ApiException("error.site.notfound", siteId.toString()));
        site.setStatus(SiteStatus.ARCHIVED);
        return toResponse(siteRepository.save(site));
    }

    private SiteResponse toResponse(Site site) {
        return SiteResponse.builder()
                .id(site.getId().toString())
                .accountId(site.getAccount().getId())
                .code(site.getCode())
                .name(site.getName())
                .timezone(site.getTimezone())
                .status(site.getStatus().name())
                .createdAt(site.getCreatedAt())
                .updatedAt(site.getUpdatedAt())
                .build();
    }
}
