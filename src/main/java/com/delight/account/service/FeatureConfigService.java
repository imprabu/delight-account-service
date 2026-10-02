package com.delight.account.service;

import com.delight.account.dto.Feature;
import com.delight.account.dto.FeaturePlanConfig;

import java.util.List;
import java.util.Optional;

/**
 * Service for loading and managing feature and plan configurations from YAML files.
 * Supports product-aware configurations and versioned plan definitions.
 */
public interface FeatureConfigService {
    
    /**
     * Load all features from features.yaml
     * @return List of all features
     */
    List<Feature> loadFeatures();
    
    /**
     * Get a feature by code
     * @param featureCode Feature code
     * @return Optional feature
     */
    Optional<Feature> getFeature(String featureCode);
    
    /**
     * Get features for a specific product
     * @param productCode Product code (e.g., CHAT, SUPPORT)
     * @return List of features available for the product
     */
    List<Feature> getFeaturesForProduct(String productCode);
    
    /**
     * Load the latest feature-plan configuration based on current date
     * @param productCode Product code
     * @return Optional FeaturePlanConfig
     */
    Optional<FeaturePlanConfig> loadLatestPlanConfig(String productCode);
    
    /**
     * Load a specific version of feature-plan configuration
     * @param productCode Product code
     * @param version Version in mmyyyy format (e.g., "122024")
     * @return Optional FeaturePlanConfig
     */
    Optional<FeaturePlanConfig> loadPlanConfig(String productCode, String version);
    
    /**
     * Get plan configuration for a specific product and plan
     * @param productCode Product code
     * @param planCode Plan code (e.g., STARTER, PROFESSIONAL, ENTERPRISE)
     * @return Optional PlanConfig
     */
    Optional<FeaturePlanConfig.PlanConfig> getPlanConfig(String productCode, String planCode);
    
    /**
     * Reload configurations (useful for development/hot-reload)
     */
    void reloadConfigurations();
}

