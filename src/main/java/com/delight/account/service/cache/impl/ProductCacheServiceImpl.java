package com.delight.account.service.cache.impl;

import com.delight.account.cache.CacheKeyManager;
import com.delight.account.model.Product;
import com.delight.account.repository.ProductRepository;
import com.delight.account.service.CacheService;
import com.delight.account.service.cache.IProductCacheService;
import com.delight.account.util.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class ProductCacheServiceImpl implements IProductCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ProductCacheServiceImpl.class);
    private static final String CACHE_KEY_PREFIX = "product:code:";
    private static final Duration PRODUCT_CACHE_TTL = Duration.ofHours(24); // Cache products for 24 hours

    private final ProductRepository productRepository;
    private final CacheService cacheService;
    private final CacheKeyManager cacheKeyManager;

    @Autowired
    public ProductCacheServiceImpl(
            ProductRepository productRepository,
            CacheService cacheService,
            @Autowired(required = false) CacheKeyManager cacheKeyManager) {
        this.productRepository = productRepository;
        this.cacheService = cacheService;
        this.cacheKeyManager = cacheKeyManager;
    }

    @Override
    public Optional<Product> findByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            LoggingUtils.warn(logger,"Product code is null or empty");
            return Optional.empty();
        }
        String cacheKey = getCacheKey(code);
        LoggingUtils.debug(logger, "Looking up product by code: {}", code);
        // Use cache service with database fallback
        Optional<Product> product = cacheService.get(
                cacheKey,
                () -> {
                    LoggingUtils.debug(logger, "Cache miss for product code: {}, querying database", code);
                    return productRepository.findByCode(code);
                },
                PRODUCT_CACHE_TTL,
                Product.class
        );

        if (product.isPresent()) {
            LoggingUtils.debug(logger, "Product found: code={}, name={}, active={}",
                    code, product.get().getName(), product.get().getActive());
        } else {
            LoggingUtils.debug(logger, "Product not found for code: {}", code);
        }
        return product;
    }

    @Override
    public void evictCache(String code) {
        if (code == null || code.trim().isEmpty()) {
            return;
        }
        String cacheKey = getCacheKey(code);
        cacheService.evict(cacheKey);
        LoggingUtils.info(logger, "Evicted product cache for code: {}", code);
    }

    private String getCacheKey(String code) {
        // Use CacheKeyManager if available, otherwise use simple prefix
        if (cacheKeyManager != null) {
            // Pattern: v{version}:product:code:{code}
            return cacheKeyManager.getVersion() + ":" + CACHE_KEY_PREFIX + code.toLowerCase();
        }
        return CACHE_KEY_PREFIX + code.toLowerCase();
    }
}
