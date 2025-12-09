package com.delight.account.config;

import com.delight.account.filter.ChannelAuthFilter;
import com.delight.account.filter.JwtAuthenticationFilter;
import com.delight.account.filter.TenantFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, 
                                                 JwtAuthenticationFilter jwtAuthenticationFilter,
                                                 ChannelAuthFilter channelAuthFilter,
                                                 TenantFilter tenantFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/accounts/signup", "/accounts/activate", "/accounts/activate/validate-token", "/accounts/signin", "/signup", "/activate", "/signin").permitAll()
                .requestMatchers("/accounts/session", "/accounts/validate-session", "/accounts/signout", "/accounts/invalidate-all-sessions", "/accounts/logout", "/accounts/logout-all").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/test/**").permitAll() // Test endpoints for development
                .requestMatchers("/channel/**").hasRole("CHANNEL") // Channel endpoints require ROLE_CHANNEL
                // Allow static resources (CSS, JS, images, etc.) from frontend build
                .requestMatchers("/assets/**", "/*.js", "/*.css", "/*.html", "/index.html", "/*.ico", "/*.png", "/*.jpg", "/*.jpeg", 
                                "/*.gif", "/*.svg", "/*.woff", "/*.woff2", "/*.ttf", "/*.eot", 
                                "/*.json", "/*.map").permitAll()
                // Allow frontend routes (SPA routing) - these will be served by FrontendController
                // Note: FrontendController will check if it's an API path and return 404 if so
                .requestMatchers("/", "/login", "/signup", "/activate", "/dashboard", "/settings", "/contacts",
                                "/conversations", "/inbox", "/analytics", "/integrations", 
                                "/knowledge-base", "/flows", "/channels", "/billing", "/security").permitAll()
                .anyRequest().authenticated())
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(tenantFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(channelAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
