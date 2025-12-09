package com.delight.account.listener;

import com.delight.account.model.Product;
import com.delight.account.service.cache.IProductCacheService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NonTenancyEntityListener {

    @Autowired
    private IProductCacheService productCacheService;

    @PostPersist
    @PostUpdate
    @PostRemove
    public void invalidateCache(Object entity){
        if(entity instanceof Product product){
            productCacheService.evictCache(product.getCode());
        }
    }
}
