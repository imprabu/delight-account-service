package com.delight.account.controller;

import com.delight.account.dto.CreateSiteRequest;
import com.delight.account.dto.SiteResponse;
import com.delight.account.dto.UpdateSiteRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.service.AuthorizationService;
import com.delight.account.service.SiteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/accounts/sites", "/api/accounts/sites", "/channel/accounts/sites"})
public class SiteController {

    private final SiteService siteService;
    private final AuthorizationService authorizationService;

    public SiteController(SiteService siteService, AuthorizationService authorizationService) {
        this.siteService = siteService;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public ResponseEntity<SiteResponse> create(@RequestAttribute("accountId") Long accountId,
                                               @RequestAttribute("userId") Long userId,
                                               @Valid @RequestBody CreateSiteRequest request) {
        require(userId, accountId, "site.create");
        return ResponseEntity.ok(siteService.create(accountId, request));
    }

    @GetMapping
    public ResponseEntity<List<SiteResponse>> list(@RequestAttribute("accountId") Long accountId,
                                                   @RequestAttribute("userId") Long userId,
                                                   @RequestParam(defaultValue = "false") boolean includeArchived,
                                                   HttpServletRequest httpRequest) {
        // Channel list: accountId from channel JWT claim may be set by filter; allow channel without user privilege skip
        if (isChannel(httpRequest)) {
            if (accountId == null) {
                throw new ApiException("error.authorization", "accountId required");
            }
            return ResponseEntity.ok(siteService.listAllForAccount(accountId, includeArchived));
        }
        require(userId, accountId, "site.view");
        return ResponseEntity.ok(siteService.list(accountId, userId, includeArchived));
    }

    @GetMapping("/{siteCode}")
    public ResponseEntity<SiteResponse> getByCode(@RequestAttribute("accountId") Long accountId,
                                                  @RequestAttribute(value = "userId", required = false) Long userId,
                                                  @PathVariable String siteCode,
                                                  HttpServletRequest httpRequest) {
        if (!isChannel(httpRequest)) {
            require(userId, accountId, "site.view");
        }
        // Prefer UUID path if looks like UUID
        try {
            UUID id = UUID.fromString(siteCode);
            return ResponseEntity.ok(siteService.getById(accountId, id));
        } catch (IllegalArgumentException ignored) {
            return ResponseEntity.ok(siteService.getByCode(accountId, siteCode));
        }
    }

    @PatchMapping("/{siteId}")
    public ResponseEntity<SiteResponse> update(@RequestAttribute("accountId") Long accountId,
                                               @RequestAttribute("userId") Long userId,
                                               @PathVariable UUID siteId,
                                               @RequestBody UpdateSiteRequest request) {
        require(userId, accountId, "site.update");
        return ResponseEntity.ok(siteService.update(accountId, siteId, request));
    }

    @PostMapping("/{siteId}/archive")
    public ResponseEntity<SiteResponse> archive(@RequestAttribute("accountId") Long accountId,
                                                @RequestAttribute("userId") Long userId,
                                                @PathVariable UUID siteId) {
        require(userId, accountId, "site.update");
        return ResponseEntity.ok(siteService.archive(accountId, siteId));
    }

    private void require(Long userId, Long accountId, String privilege) {
        if (userId == null || accountId == null || !authorizationService.hasPrivilege(userId, accountId, privilege)) {
            throw new ApiException("error.authorization", "Insufficient permissions");
        }
    }

    private boolean isChannel(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.contains("/channel/");
    }
}
