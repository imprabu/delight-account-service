package com.delight.account.cache.impl;

import com.delight.account.cache.CacheKeyManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Implementation of CacheKeyManager that provides versioned Redis cache keys.
 * The version can be configured via application properties to enable cache invalidation
 * across deployments without manual key cleanup.
 */
@Component
public class VersionedCacheKeyManager implements CacheKeyManager {

    private final String version;

    public VersionedCacheKeyManager(@Value("${cache.key.version:v1}") String version) {
        this.version = version;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getUserAccountKey(Long accountId, String email) {
        return String.format("%s:user:account:%d:email:%s", version, accountId, email);
    }

    @Override
    public String getAccountInfoKey(Long accountId, Long userId) {
        return String.format("%s:account:info:%d:%d", version, accountId, userId);
    }

    @Override
    public String getAccountInfoKey(Long accountId) {
        return String.format("%s:account:info:%d", version, accountId);
    }

    @Override
    public String getSessionKey(String sessionId) {
        return String.format("%s:session:id:%s", version, sessionId);
    }

    @Override
    public String getSessionTokenKey(String tokenHash) {
        return String.format("%s:session:token:%s", version, tokenHash);
    }

    @Override
    public String getUserCredentialKey(Long userId) {
        return String.format("%s:user:credential:%d", version, userId);
    }

    @Override
    public String getSessionPattern() {
        return String.format("%s:session:*", version);
    }

    @Override
    public String getAccountInfoPattern() {
        return String.format("%s:account:info:*", version);
    }

    @Override
    public String getUserAccountPattern() {
        return String.format("%s:user:account:*", version);
    }

    @Override
    public String getKeyPattern(String keyType) {
        return String.format("%s:%s:*", version, keyType);
    }

    @Override
    public String getAllKeysPattern() {
        return String.format("%s:*", version);
    }
}