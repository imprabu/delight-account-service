package com.delight.account.service;

import com.delight.account.dto.CreateSiteRequest;
import com.delight.account.dto.SiteResponse;
import com.delight.account.dto.UpdateSiteRequest;

import java.util.List;
import java.util.UUID;

public interface SiteService {
    SiteResponse create(Long accountId, CreateSiteRequest request);
    List<SiteResponse> list(Long accountId, Long userId, boolean includeArchived);
    List<SiteResponse> listAllForAccount(Long accountId, boolean includeArchived);
    SiteResponse getById(Long accountId, UUID siteId);
    SiteResponse getByCode(Long accountId, String code);
    SiteResponse update(Long accountId, UUID siteId, UpdateSiteRequest request);
    SiteResponse archive(Long accountId, UUID siteId);
}
