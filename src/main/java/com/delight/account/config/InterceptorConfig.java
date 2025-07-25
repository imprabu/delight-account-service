package com.delight.account.config;

import com.delight.account.interceptor.DomainAccountInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private final DomainAccountInterceptor domainAccountInterceptor;

    @Autowired
    public InterceptorConfig(DomainAccountInterceptor domainAccountInterceptor) {
        this.domainAccountInterceptor = domainAccountInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(domainAccountInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/account/signup");
    }
}
