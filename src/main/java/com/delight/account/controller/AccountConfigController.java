package com.delight.account.controller;

import com.delight.account.service.CacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
public class AccountConfigController {

    private final CacheService cacheService;

    public AccountConfigController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/config")
    public ResponseEntity<Void> getConfig(@RequestAttribute("accountId") Long accountId) {
        String key = String.format("account:config:%d", accountId);
        cacheService.put(key, "cached");
        return ResponseEntity.ok().build();
    }

    @PutMapping("/config")
    public ResponseEntity<Void> updateConfig(@RequestAttribute("accountId") Long accountId, @RequestBody String body) {
        String key = String.format("account:config:%d", accountId);
        cacheService.evict(key);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@RequestAttribute("accountId") Long accountId) {
        String pattern = String.format("account:config:%d:*", accountId);
        cacheService.evictByPattern(pattern);
        return ResponseEntity.ok().build();
    }
}

