package com.delight.account.service;

import java.util.Map;

/**
 * Client interface for calling RAG service to retrieve usage data.
 */
public interface RagServiceClient {
    /**
     * Get current usage for RAG features from RAG service
     * @param accountId Account ID
     * @return Map of feature code to current usage value
     */
    Map<String, Long> getRagUsage(Long accountId);
    
    /**
     * Get specific feature usage from RAG service
     * @param accountId Account ID
     * @param featureCode Feature code (e.g., "rag.storage.gb")
     * @return Current usage value, or null if not available
     */
    Long getFeatureUsage(Long accountId, String featureCode);
}

