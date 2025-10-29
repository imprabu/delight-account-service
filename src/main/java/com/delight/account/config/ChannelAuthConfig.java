package com.delight.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "channel.auth")
public class ChannelAuthConfig {
    
    private Map<String, String> keys = new HashMap<>();
    
    public Map<String, String> getKeys() {
        return keys;
    }
    
    public void setKeys(Map<String, String> keys) {
        this.keys = keys;
    }
}