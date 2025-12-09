package com.delight.account.config;

import com.delight.account.interceptor.ApiAuthenticationInterceptor;
import com.delight.account.interceptor.DomainAccountInterceptor;
import com.delight.account.interceptor.RequestTracingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private final DomainAccountInterceptor domainAccountInterceptor;
    private final ApiAuthenticationInterceptor apiAuthenticationInterceptor;
    private final RequestTracingInterceptor requestTracingInterceptor;

    @Autowired
    public InterceptorConfig(DomainAccountInterceptor domainAccountInterceptor,
                           ApiAuthenticationInterceptor apiAuthenticationInterceptor,
                           RequestTracingInterceptor requestTracingInterceptor) {
        this.domainAccountInterceptor = domainAccountInterceptor;
        this.apiAuthenticationInterceptor = apiAuthenticationInterceptor;
        this.requestTracingInterceptor = requestTracingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add request tracing interceptor first to capture all requests
        registry.addInterceptor(requestTracingInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**");
        
        // Add API authentication interceptor (runs after tracing)
        registry.addInterceptor(apiAuthenticationInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**",
                              "/actuator/health");
        
        // Add domain account interceptor for all paths except channel endpoints and frontend routes (runs last)
        registry.addInterceptor(domainAccountInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/accounts/signup", "/accounts/activate", "/accounts/activate/validate-token", "/accounts/signin", 
                              "/signup", "/activate", "/signin", "/channel/**",
                              "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**",
                              "/actuator/health",
                              // Exclude frontend static assets and common frontend routes
                              "/assets/**", "/*.js", "/*.css", "/*.html", "/index.html", "/*.ico", "/*.png", "/*.jpg", 
                              "/*.jpeg", "/*.gif", "/*.svg", "/*.woff", "/*.woff2", "/*.ttf", 
                              "/*.eot", "/*.json", "/*.map",
                              // Exclude frontend SPA routes
                              "/", "/login", "/dashboard", "/settings", "/contacts", "/conversations",
                              "/inbox", "/analytics", "/integrations", "/knowledge-base", "/flows",
                              "/channels", "/billing", "/security");
    }
}
