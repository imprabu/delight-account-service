package com.delight.account.service.impl;

import com.delight.account.service.RagServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of RagServiceClient that calls RAG service REST API to retrieve usage data.
 */
@Slf4j
@Service
public class RagServiceClientImpl implements RagServiceClient {
    
    private final RestTemplate restTemplate;
    private final String ragServiceBaseUrl;
    
    public RagServiceClientImpl(
            RestTemplate restTemplate,
            @Value("${rag.service.base-url:http://localhost:8081}") String ragServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.ragServiceBaseUrl = ragServiceBaseUrl;
    }
    
    @Override
    public Map<String, Long> getRagUsage(Long accountId) {
        try {
            String url = ragServiceBaseUrl + "/rag/channel/app/" + accountId + "/usage";
            log.debug("Calling RAG service for usage data: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> body = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && body != null) {
                Map<String, Long> usageMap = new HashMap<>();
                
                // Extract usage values from response
                // Expected format: { "rag.storage.gb": 45, "rag.documents.max": 10, ... }
                for (Map.Entry<String, Object> entry : body.entrySet()) {
                    if (entry.getValue() != null && entry.getValue() instanceof Number) {
                        usageMap.put(entry.getKey(), ((Number) entry.getValue()).longValue());
                    }
                }
                
                log.debug("Retrieved RAG usage for account {}: {}", accountId, usageMap);
                return usageMap;
            }
            
            log.warn("RAG service returned non-2xx status or empty body for account {}", accountId);
            return new HashMap<>();
        } catch (RestClientException e) {
            log.error("Error calling RAG service for account {}: {}", accountId, e.getMessage(), e);
            return new HashMap<>();
        } catch (Exception e) {
            log.error("Unexpected error calling RAG service for account {}: {}", accountId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    @Override
    public Long getFeatureUsage(Long accountId, String featureCode) {
        Map<String, Long> usageMap = getRagUsage(accountId);
        return usageMap.getOrDefault(featureCode, null);
    }
}

