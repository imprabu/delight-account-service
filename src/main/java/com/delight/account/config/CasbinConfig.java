package com.delight.account.config;

import org.casbin.jcasbin.model.Model;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Casbin configuration for Redis-based per-tenant policy caching.
 * Enforcers are created per-account on demand in AuthorizationServiceImpl.
 */
@Configuration
public class CasbinConfig {
    
    @Bean
    @Primary
    public Model casbinModel() {
        // Create model from string definition
        // Request definition: sub (subject/user), obj (object/resource), act (action)
        // Policy definition: role_id, resource, action
        // Role definition: user_id:account_id -> role_id
        // Role-privilege definition: role_id -> privilege_code
        // Policy effect: allow if any policy matches
        // Matcher: check if user has role, resource matches, and action matches
        // g: user_id:account_id -> role_id (user-role assignment)
        // g2: role_id -> privilege_code (role-privilege mapping, for reference only)
        // p: role_id, resource, action (role-permission policy)
        // The matcher checks: user has role (g), resource matches (p.obj), action matches (p.act)
        String modelText = 
            "[request_definition]\n" +
            "r = sub, obj, act\n\n" +
            "[policy_definition]\n" +
            "p = sub, obj, act\n\n" +
            "[role_definition]\n" +
            "g = _, _\n" +
            "g2 = _, _\n\n" +
            "[policy_effect]\n" +
            "e = some(where (p.eft == allow))\n\n" +
            "[matchers]\n" +
            "m = g(r.sub, p.sub) && r.obj == p.obj && r.act == p.act\n";
        
        Model model = new Model();
        model.loadModelFromText(modelText);
        return model;
    }
    
    // Note: We no longer create a global Enforcer bean.
    // Enforcers are created per-account on demand in AuthorizationServiceImpl
    // using RedisCasbinAdapter for Redis-backed policy caching.
}
