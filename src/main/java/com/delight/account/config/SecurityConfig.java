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
                .requestMatchers("/account/signup", "/account/activate", "/account/signin").permitAll()
                .requestMatchers("/accounts/session", "/accounts/validate-session", "/accounts/signout", "/accounts/invalidate-all-sessions", "/accounts/logout", "/accounts/logout-all").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/channel/**").hasRole("CHANNEL") // Channel endpoints require ROLE_CHANNEL
                .anyRequest().authenticated())
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(tenantFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(channelAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
