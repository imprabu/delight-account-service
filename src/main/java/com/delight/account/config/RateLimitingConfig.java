package com.delight.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import com.delight.account.service.AccountConfigService;
import com.delight.account.service.RateLimitingService;
import com.delight.account.service.impl.RateLimitingServiceImpl;

@Configuration
public class RateLimitingConfig {

    @Bean
    public RateLimitingService rateLimitingService(RedisTemplate<String, Object> redisTemplate, 
                                                   AccountConfigService accountConfigService) {
        return new RateLimitingServiceImpl(redisTemplate, accountConfigService);
    }
}