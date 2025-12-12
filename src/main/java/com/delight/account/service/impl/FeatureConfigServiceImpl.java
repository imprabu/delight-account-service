package com.delight.account.service.impl;

import com.delight.account.dto.Feature;
import com.delight.account.dto.FeaturePlanConfig;
import com.delight.account.service.FeatureConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FeatureConfigServiceImpl implements FeatureConfigService {
    
    private static final String FEATURES_FILE = "config/features.yaml";
    private static final String PLAN_CONFIG_PREFIX = "config/feature-plan-";
    private static final String PLAN_CONFIG_SUFFIX = ".yaml";
    
    private List<Feature> features = new ArrayList<>();
    private Map<String, FeaturePlanConfig> planConfigsByProduct = new HashMap<>();
    private final Yaml yaml = new Yaml();
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    
    @PostConstruct
    public void init() {
        loadFeatures();
        loadPlanConfigs();
    }
    
    @Override
    public List<Feature> loadFeatures() {
        try {
            ClassPathResource resource = new ClassPathResource(FEATURES_FILE);
            if (!resource.exists()) {
                log.warn("Features file not found: {}", FEATURES_FILE);
                return Collections.emptyList();
            }
            
            try (InputStream inputStream = resource.getInputStream()) {
                Map<String, Object> data = yaml.load(inputStream);
                if (data == null || !data.containsKey("features")) {
                    log.warn("Invalid features file format: {}", FEATURES_FILE);
                    return Collections.emptyList();
                }
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> featuresList = (List<Map<String, Object>>) data.get("features");
                
                features = featuresList.stream()
                    .map(this::mapToFeature)
                    .collect(Collectors.toList());
                
                log.info("Loaded {} features from {}", features.size(), FEATURES_FILE);
                return features;
            }
        } catch (Exception e) {
            log.error("Error loading features from {}: {}", FEATURES_FILE, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public Optional<Feature> getFeature(String featureCode) {
        return features.stream()
            .filter(f -> f.getCode().equals(featureCode))
            .findFirst();
    }
    
    @Override
    public List<Feature> getFeaturesForProduct(String productCode) {
        return features.stream()
            .filter(f -> f.getProducts() != null && f.getProducts().contains(productCode))
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<FeaturePlanConfig> loadLatestPlanConfig(String productCode) {
        // Find the latest available plan config based on effectiveDate
        // Scan all available plan config files and find the one with the latest effectiveDate <= today
        LocalDate today = LocalDate.now();
        Optional<FeaturePlanConfig> latestConfig = Optional.empty();
        LocalDate latestEffectiveDate = null;
        
        try {
            // Find all plan config files
            String pattern = "classpath:" + PLAN_CONFIG_PREFIX + "*" + PLAN_CONFIG_SUFFIX;
            Resource[] resources = resourcePatternResolver.getResources(pattern);
            
            for (Resource resource : resources) {
                if (!resource.exists()) {
                    continue;
                }
                
                try (InputStream inputStream = resource.getInputStream()) {
                    Map<String, Object> data = yaml.load(inputStream);
                    if (data == null) {
                        continue;
                    }
                    
                    FeaturePlanConfig config = mapToFeaturePlanConfig(data);
                    
                    // Check if this config is for the requested product
                    if (config.getProducts() == null) {
                        continue;
                    }
                    
                    boolean hasProduct = config.getProducts().stream()
                        .anyMatch(p -> p.getCode().equals(productCode));
                    
                    if (!hasProduct) {
                        continue;
                    }
                    
                    // Check if effectiveDate is valid and <= today
                    LocalDate effectiveDate = config.getEffectiveDate();
                    if (effectiveDate == null) {
                        continue;
                    }
                    
                    if (effectiveDate.isAfter(today)) {
                        // Skip future-dated configs
                        continue;
                    }
                    
                    // Check if this is the latest config so far
                    if (latestEffectiveDate == null || effectiveDate.isAfter(latestEffectiveDate)) {
                        latestEffectiveDate = effectiveDate;
                        latestConfig = Optional.of(config);
                    }
                } catch (Exception e) {
                    log.warn("Error loading plan config from {}: {}", resource.getFilename(), e.getMessage());
                }
            }
            
            if (latestConfig.isPresent()) {
                // Extract product-specific config
                FeaturePlanConfig config = latestConfig.get();
                Optional<FeaturePlanConfig.ProductConfig> productConfig = config.getProducts().stream()
                    .filter(p -> p.getCode().equals(productCode))
                    .findFirst();
                
                if (productConfig.isPresent()) {
                    // Create a new config with only this product
                    FeaturePlanConfig productSpecificConfig = FeaturePlanConfig.builder()
                        .version(config.getVersion())
                        .effectiveDate(config.getEffectiveDate())
                        .products(List.of(productConfig.get()))
                        .build();
                    
                    // Cache it
                    String cacheKey = productCode + ":" + config.getVersion();
                    planConfigsByProduct.put(cacheKey, productSpecificConfig);
                    
                    log.debug("Loaded latest plan config for product {}: version {}, effectiveDate {}", 
                        productCode, config.getVersion(), config.getEffectiveDate());
                    return Optional.of(productSpecificConfig);
                }
            }
            
            if (latestEffectiveDate == null) {
                log.warn("No plan config found for product {} with effectiveDate <= {}", productCode, today);
            } else {
                log.warn("No plan config found for product {} (latest effectiveDate found: {})", 
                    productCode, latestEffectiveDate);
            }
            
        } catch (Exception e) {
            log.error("Error finding latest plan config for product {}: {}", productCode, e.getMessage(), e);
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<FeaturePlanConfig> loadPlanConfig(String productCode, String version) {
        String cacheKey = productCode + ":" + version;
        
        // Check cache first
        if (planConfigsByProduct.containsKey(cacheKey)) {
            return Optional.of(planConfigsByProduct.get(cacheKey));
        }
        
        // Load from file
        String fileName = PLAN_CONFIG_PREFIX + version + PLAN_CONFIG_SUFFIX;
        try {
            ClassPathResource resource = new ClassPathResource(fileName);
            if (!resource.exists()) {
                log.warn("Plan config file not found: {}", fileName);
                return Optional.empty();
            }
            
            try (InputStream inputStream = resource.getInputStream()) {
                Map<String, Object> data = yaml.load(inputStream);
                if (data == null) {
                    log.warn("Invalid plan config file format: {}", fileName);
                    return Optional.empty();
                }
                
                FeaturePlanConfig config = mapToFeaturePlanConfig(data);
                
                // Extract product-specific config
                if (config.getProducts() != null) {
                    Optional<FeaturePlanConfig.ProductConfig> productConfig = config.getProducts().stream()
                        .filter(p -> p.getCode().equals(productCode))
                        .findFirst();
                    
                    if (productConfig.isPresent()) {
                        // Create a new config with only this product
                        FeaturePlanConfig productSpecificConfig = FeaturePlanConfig.builder()
                            .version(config.getVersion())
                            .effectiveDate(config.getEffectiveDate())
                            .products(List.of(productConfig.get()))
                            .build();
                        
                        // Cache it
                        planConfigsByProduct.put(cacheKey, productSpecificConfig);
                        return Optional.of(productSpecificConfig);
                    }
                }
                
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error loading plan config from {}: {}", fileName, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<FeaturePlanConfig.PlanConfig> getPlanConfig(String productCode, String planCode) {
        Optional<FeaturePlanConfig> configOpt = loadLatestPlanConfig(productCode);
        if (configOpt.isEmpty()) {
            return Optional.empty();
        }
        
        FeaturePlanConfig config = configOpt.get();
        if (config.getProducts() == null || config.getProducts().isEmpty()) {
            return Optional.empty();
        }
        
        return config.getProducts().stream()
            .filter(p -> p.getCode().equals(productCode))
            .findFirst()
            .flatMap(product -> product.getPlans().stream()
                .filter(plan -> plan.getCode().equals(planCode))
                .findFirst());
    }
    
    @Override
    public void reloadConfigurations() {
        log.info("Reloading feature and plan configurations...");
        features.clear();
        planConfigsByProduct.clear();
        loadFeatures();
        loadPlanConfigs();
        log.info("Configuration reload complete");
    }
    
    private void loadPlanConfigs() {
        // Load all available plan config files
        // For now, we'll load them on-demand when requested
        // This method can be enhanced to preload all versions if needed
        log.debug("Plan configs will be loaded on-demand");
    }
    
    private Feature mapToFeature(Map<String, Object> map) {
        @SuppressWarnings("unchecked")
        List<String> products = map.get("products") != null ? 
            (List<String>) map.get("products") : Collections.emptyList();
        
        Integer defaultRateLimit = map.get("defaultRateLimit") != null ? 
            (Integer) map.get("defaultRateLimit") : null;
        
        return Feature.builder()
            .code((String) map.get("code"))
            .name((String) map.get("name"))
            .type((String) map.get("type"))
            .unit((String) map.get("unit"))
            .description((String) map.get("description"))
            .products(products)
            .defaultRateLimit(defaultRateLimit)
            .build();
    }
    
    @SuppressWarnings("unchecked")
    private FeaturePlanConfig mapToFeaturePlanConfig(Map<String, Object> map) {
        String version = (String) map.get("version");
        String effectiveDateStr = (String) map.get("effectiveDate");
        LocalDate effectiveDate = effectiveDateStr != null ? 
            LocalDate.parse(effectiveDateStr) : null;
        
        List<Map<String, Object>> productsList = (List<Map<String, Object>>) map.get("products");
        List<FeaturePlanConfig.ProductConfig> products = productsList != null ?
            productsList.stream()
                .map(this::mapToProductConfig)
                .collect(Collectors.toList()) : Collections.emptyList();
        
        return FeaturePlanConfig.builder()
            .version(version)
            .effectiveDate(effectiveDate)
            .products(products)
            .build();
    }
    
    @SuppressWarnings("unchecked")
    private FeaturePlanConfig.ProductConfig mapToProductConfig(Map<String, Object> map) {
        String code = (String) map.get("code");
        String name = (String) map.get("name");
        List<Map<String, Object>> plansList = (List<Map<String, Object>>) map.get("plans");
        
        List<FeaturePlanConfig.PlanConfig> plans = plansList != null ?
            plansList.stream()
                .map(this::mapToPlanConfig)
                .collect(Collectors.toList()) : Collections.emptyList();
        
        return FeaturePlanConfig.ProductConfig.builder()
            .code(code)
            .name(name)
            .plans(plans)
            .build();
    }
    
    @SuppressWarnings("unchecked")
    private FeaturePlanConfig.PlanConfig mapToPlanConfig(Map<String, Object> map) {
        String code = (String) map.get("code");
        String name = (String) map.get("name");
        List<Map<String, Object>> featuresList = (List<Map<String, Object>>) map.get("features");
        
        List<FeaturePlanConfig.FeatureConfig> features = featuresList != null ?
            featuresList.stream()
                .map(this::mapToFeatureConfig)
                .collect(Collectors.toList()) : Collections.emptyList();
        
        return FeaturePlanConfig.PlanConfig.builder()
            .code(code)
            .name(name)
            .features(features)
            .build();
    }
    
    @SuppressWarnings("unchecked")
    private FeaturePlanConfig.FeatureConfig mapToFeatureConfig(Map<String, Object> map) {
        String code = (String) map.get("code");
        Long limit = map.get("limit") != null ? 
            ((Number) map.get("limit")).longValue() : null;
        Boolean enabled = map.get("enabled") != null ? 
            (Boolean) map.get("enabled") : null;
        Integer rateLimitRpm = map.get("rateLimitRpm") != null ? 
            (Integer) map.get("rateLimitRpm") : null;
        List<String> channels = map.get("channels") != null ? 
            (List<String>) map.get("channels") : null;
        
        return FeaturePlanConfig.FeatureConfig.builder()
            .code(code)
            .limit(limit)
            .enabled(enabled)
            .rateLimitRpm(rateLimitRpm)
            .channels(channels)
            .build();
    }
}

