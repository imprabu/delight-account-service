package com.delight.account.service.cache;

import com.delight.account.model.Product;

import java.util.Optional;

public interface IProductCacheService {
    /**
     * Find product by code with Redis caching and database fallback.
     *
     * @param code the product code (e.g., "CHAT")
     * @return Optional containing the Product if found
     */
    Optional<Product> findByCode(String code);

    /**
     * Invalidate product cache when product is updated.
     *
     * @param code the product code
     */
    void evictCache(String code);
}
