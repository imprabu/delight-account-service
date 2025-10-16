package com.delight.account.config;

import com.delight.account.interceptor.ApiAuthenticationInterceptor;
import com.delight.account.interceptor.DomainAccountInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private final DomainAccountInterceptor domainAccountInterceptor;
    private final ApiAuthenticationInterceptor apiAuthenticationInterceptor;

    @Autowired
    public InterceptorConfig(DomainAccountInterceptor domainAccountInterceptor,
                           ApiAuthenticationInterceptor apiAuthenticationInterceptor) {
        this.domainAccountInterceptor = domainAccountInterceptor;
        this.apiAuthenticationInterceptor = apiAuthenticationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add API authentication interceptor first (runs before domain interceptor)
        registry.addInterceptor(apiAuthenticationInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**",
                              "/actuator/health");
        
        // Add domain account interceptor for all paths
        registry.addInterceptor(domainAccountInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/account/signup", "/account/signin", 
                              "/account/activate",
                              "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**",
                              "/actuator/health");
    }
}
