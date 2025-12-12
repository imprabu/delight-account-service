package com.delight.account.service.impl;

import com.delight.account.dto.Feature;
import com.delight.account.dto.FeatureInfo;
import com.delight.account.model.Subscription;
import com.delight.account.model.SubscriptionFeature;
import com.delight.account.repository.SubscriptionFeatureRepository;
import com.delight.account.repository.SubscriptionRepository;
import com.delight.account.service.FeatureConfigService;
import com.delight.account.service.FeatureService;
import com.delight.account.service.UsageTrackingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class FeatureServiceImpl implements FeatureService {
    
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionFeatureRepository subscriptionFeatureRepository;
    private final FeatureConfigService featureConfigService;
    private final UsageTrackingService usageTrackingService;
    private final ObjectMapper objectMapper;
    
    public FeatureServiceImpl(
            SubscriptionRepository subscriptionRepository,
            SubscriptionFeatureRepository subscriptionFeatureRepository,
            FeatureConfigService featureConfigService,
            UsageTrackingService usageTrackingService,
            ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionFeatureRepository = subscriptionFeatureRepository;
        this.featureConfigService = featureConfigService;
        this.usageTrackingService = usageTrackingService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<FeatureInfo> getActiveFeatures(Long accountId) {
        java.util.List<com.delight.account.model.SubscriptionStatus> activeStatuses = java.util.Arrays.asList(
            com.delight.account.model.SubscriptionStatus.TRIAL, 
            com.delight.account.model.SubscriptionStatus.ACTIVE
        );
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findActiveByAccountId(accountId, activeStatuses);
        if (subscriptionOpt.isEmpty()) {
            return List.of();
        }
        
        Subscription subscription = subscriptionOpt.get();
        List<SubscriptionFeature> subscriptionFeatures = 
            subscriptionFeatureRepository.findBySubscriptionId(subscription.getId());
        
        List<FeatureInfo> featureInfos = new ArrayList<>();
        
        for (SubscriptionFeature subscriptionFeature : subscriptionFeatures) {
            Optional<Feature> featureOpt = featureConfigService.getFeature(subscriptionFeature.getFeatureCode());
            if (featureOpt.isEmpty()) {
                continue;
            }
            
            Feature feature = featureOpt.get();
            FeatureInfo.FeatureInfoBuilder builder = FeatureInfo.builder()
                .code(feature.getCode())
                .name(feature.getName())
                .type(feature.getType())
                .enabled(subscriptionFeature.getEnabled());
            
            // Set limit and current usage for usage_limit features
            if ("usage_limit".equals(feature.getType()) && subscriptionFeature.getLimitValue() != null) {
                builder.limit(subscriptionFeature.getLimitValue());
                
                // Get current usage
                Optional<com.delight.account.model.FeatureUsage> usageOpt = 
                    usageTrackingService.getCurrentUsage(
                        accountId, 
                        subscription.getId(), 
                        subscriptionFeature.getFeatureCode());
                
                Long currentUsage = usageOpt.map(com.delight.account.model.FeatureUsage::getUsageValue)
                    .orElse(0L);
                builder.currentUsage(currentUsage);
            }
            
            // Set rate limit for api.access feature
            if ("api.access".equals(feature.getCode()) && subscriptionFeature.getRateLimitRpm() != null) {
                builder.rateLimitRpm(subscriptionFeature.getRateLimitRpm());
            }
            
            // Set allowed channels for channels.allowed feature
            if ("channels.allowed".equals(feature.getCode()) && subscriptionFeature.getChannelList() != null) {
                try {
                    List<String> channels = objectMapper.readValue(
                        subscriptionFeature.getChannelList(), 
                        new TypeReference<List<String>>() {});
                    builder.allowedChannels(channels);
                } catch (Exception e) {
                    log.error("Error parsing channel list: {}", e.getMessage(), e);
                }
            }
            
            featureInfos.add(builder.build());
        }
        
        return featureInfos;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(Long accountId, String featureCode) {
        java.util.List<com.delight.account.model.SubscriptionStatus> activeStatuses = java.util.Arrays.asList(
            com.delight.account.model.SubscriptionStatus.TRIAL, 
            com.delight.account.model.SubscriptionStatus.ACTIVE
        );
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findActiveByAccountId(accountId, activeStatuses);
        if (subscriptionOpt.isEmpty()) {
            return false;
        }
        
        Subscription subscription = subscriptionOpt.get();
        Optional<SubscriptionFeature> featureOpt = 
            subscriptionFeatureRepository.findBySubscriptionIdAndFeatureCode(
                subscription.getId(), featureCode);
        
        return featureOpt.map(SubscriptionFeature::getEnabled)
            .orElse(false);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isUsageWithinLimit(Long accountId, String featureCode) {
        java.util.List<com.delight.account.model.SubscriptionStatus> activeStatuses = java.util.Arrays.asList(
            com.delight.account.model.SubscriptionStatus.TRIAL, 
            com.delight.account.model.SubscriptionStatus.ACTIVE
        );
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findActiveByAccountId(accountId, activeStatuses);
        if (subscriptionOpt.isEmpty()) {
            return false;
        }
        
        Subscription subscription = subscriptionOpt.get();
        return usageTrackingService.isUsageAllowed(accountId, subscription.getId(), featureCode);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isChannelAllowed(Long accountId, String channelCode) {
        java.util.List<com.delight.account.model.SubscriptionStatus> activeStatuses = java.util.Arrays.asList(
            com.delight.account.model.SubscriptionStatus.TRIAL, 
            com.delight.account.model.SubscriptionStatus.ACTIVE
        );
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findActiveByAccountId(accountId, activeStatuses);
        if (subscriptionOpt.isEmpty()) {
            return false;
        }
        
        Subscription subscription = subscriptionOpt.get();
        Optional<SubscriptionFeature> featureOpt = 
            subscriptionFeatureRepository.findBySubscriptionIdAndFeatureCode(
                subscription.getId(), "channels.allowed");
        
        if (featureOpt.isEmpty() || featureOpt.get().getChannelList() == null) {
            return false;
        }
        
        try {
            List<String> allowedChannels = objectMapper.readValue(
                featureOpt.get().getChannelList(), 
                new TypeReference<List<String>>() {});
            return allowedChannels != null && allowedChannels.contains(channelCode);
        } catch (Exception e) {
            log.error("Error parsing channel list: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Integer getApiRateLimit(Long accountId) {
        java.util.List<com.delight.account.model.SubscriptionStatus> activeStatuses = java.util.Arrays.asList(
            com.delight.account.model.SubscriptionStatus.TRIAL, 
            com.delight.account.model.SubscriptionStatus.ACTIVE
        );
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findActiveByAccountId(accountId, activeStatuses);
        if (subscriptionOpt.isEmpty()) {
            return null;
        }
        
        Subscription subscription = subscriptionOpt.get();
        Optional<SubscriptionFeature> featureOpt = 
            subscriptionFeatureRepository.findBySubscriptionIdAndFeatureCode(
                subscription.getId(), "api.access");
        
        if (featureOpt.isEmpty() || !Boolean.TRUE.equals(featureOpt.get().getEnabled())) {
            return null;
        }
        
        return featureOpt.get().getRateLimitRpm();
    }
}

