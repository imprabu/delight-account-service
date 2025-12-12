package com.delight.account.service.impl;

import com.delight.account.dto.FeaturePlanConfig;
import com.delight.account.dto.SubscriptionInfo;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.model.Subscription;
import com.delight.account.model.SubscriptionFeature;
import com.delight.account.model.SubscriptionStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.SubscriptionFeatureRepository;
import com.delight.account.repository.SubscriptionRepository;
import com.delight.account.service.FeatureConfigService;
import com.delight.account.service.SubscriptionProviderService;
import com.delight.account.service.SubscriptionService;
import com.delight.account.util.LoggingUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SubscriptionServiceImpl implements SubscriptionService {
    
    private static final DateTimeFormatter VERSION_FORMATTER = DateTimeFormatter.ofPattern("MMyyyy");
    
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionFeatureRepository subscriptionFeatureRepository;
    private final AccountRepository accountRepository;
    private final FeatureConfigService featureConfigService;
    private final SubscriptionProviderService subscriptionProviderService;
    private final ObjectMapper objectMapper;
    
    public SubscriptionServiceImpl(
            SubscriptionRepository subscriptionRepository,
            SubscriptionFeatureRepository subscriptionFeatureRepository,
            AccountRepository accountRepository,
            FeatureConfigService featureConfigService,
            SubscriptionProviderService subscriptionProviderService,
            ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionFeatureRepository = subscriptionFeatureRepository;
        this.accountRepository = accountRepository;
        this.featureConfigService = featureConfigService;
        this.subscriptionProviderService = subscriptionProviderService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Subscription> getActiveSubscription(Long accountId) {
        java.util.List<SubscriptionStatus> activeStatuses = java.util.Arrays.asList(
            SubscriptionStatus.TRIAL, 
            SubscriptionStatus.ACTIVE
        );
        return subscriptionRepository.findActiveByAccountId(accountId, activeStatuses);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Subscription> getActiveSubscription(Long accountId, String productCode) {
        java.util.List<SubscriptionStatus> activeStatuses = java.util.Arrays.asList(
            SubscriptionStatus.TRIAL, 
            SubscriptionStatus.ACTIVE
        );
        return subscriptionRepository.findActiveByAccountIdAndProductCode(accountId, productCode, activeStatuses);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptions(Long accountId) {
        return subscriptionRepository.findByAccountId(accountId);
    }
    
    @Override
    @Transactional
    public Subscription createSubscription(Long accountId, String productCode, String planCode) {
        LoggingUtils.logMethodEntry(log, "createSubscription", 
            "accountId", accountId, "productCode", productCode, "planCode", planCode);
        
        // Verify account exists
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        // Cancel any existing active subscription for this product
        Optional<Subscription> existingSubscription = getActiveSubscription(accountId, productCode);
        if (existingSubscription.isPresent()) {
            log.info("Cancelling existing subscription for accountId={}, productCode={}", accountId, productCode);
            cancelSubscription(existingSubscription.get().getId());
        }
        
        // Get plan configuration
        Optional<FeaturePlanConfig.PlanConfig> planConfigOpt = 
            featureConfigService.getPlanConfig(productCode, planCode);
        if (planConfigOpt.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Plan not found: productCode=%s, planCode=%s", productCode, planCode));
        }
        
        FeaturePlanConfig.PlanConfig planConfig = planConfigOpt.get();
        
        // Get the plan version from the loaded config
        Optional<FeaturePlanConfig> latestConfigOpt = 
            featureConfigService.loadLatestPlanConfig(productCode);
        String planVersion = latestConfigOpt.map(FeaturePlanConfig::getVersion)
            .orElse(LocalDate.now().format(VERSION_FORMATTER)); // Fallback to current date format
        
        // Set subscription status to TRIAL by default
        SubscriptionStatus subscriptionStatus = SubscriptionStatus.TRIAL;
        
        // For TRIAL status: billing columns are null
        // For ACTIVE status: calculate billing cycle dates
        LocalDate billingCycleStart = null;
        LocalDate billingCycleEnd = null;
        LocalDate nextBillingDate = null;
        
        if (subscriptionStatus == SubscriptionStatus.ACTIVE) {
            billingCycleStart = LocalDate.now();
            billingCycleEnd = billingCycleStart.plusMonths(1).minusDays(1);
            nextBillingDate = billingCycleStart.plusMonths(1);
        }
        
        // Create subscription
        Subscription subscription = new Subscription();
        subscription.setAccount(account);
        subscription.setProductCode(productCode);
        subscription.setPlanCode(planCode);
        subscription.setPlanVersion(planVersion);
        subscription.setStatus(subscriptionStatus);
        subscription.setBillingCycleStartDate(billingCycleStart);
        subscription.setBillingCycleEndDate(billingCycleEnd);
        subscription.setNextBillingDate(nextBillingDate);
        
        // Call provider service (stub for now)
        subscriptionProviderService.createSubscription(accountId, productCode, planCode);
        
        subscription = subscriptionRepository.save(subscription);
        
        // Sync Account.status based on subscription status
        // If subscription is TRIAL or ACTIVE, account status is ACTIVE
        // Otherwise, account status is BLOCKED
        if (subscriptionStatus == SubscriptionStatus.TRIAL || subscriptionStatus == SubscriptionStatus.ACTIVE) {
            account.setStatus(AccountStatus.ACTIVE);
        } else {
            account.setStatus(AccountStatus.BLOCKED);
        }
        accountRepository.save(account);
        
        // Create subscription features
        createSubscriptionFeatures(subscription, planConfig);
        
        LoggingUtils.logMethodExit(log, "createSubscription", 
            String.format("subscriptionId=%d", subscription.getId()));
        
        return subscription;
    }
    
    @Override
    @Transactional
    public Subscription updateSubscription(Long subscriptionId, String newPlanCode) {
        LoggingUtils.logMethodEntry(log, "updateSubscription", 
            "subscriptionId", subscriptionId, "newPlanCode", newPlanCode);
        
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));
        
        String productCode = subscription.getProductCode();
        
        // Get new plan configuration
        Optional<FeaturePlanConfig.PlanConfig> planConfigOpt = 
            featureConfigService.getPlanConfig(productCode, newPlanCode);
        if (planConfigOpt.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Plan not found: productCode=%s, planCode=%s", productCode, newPlanCode));
        }
        
        FeaturePlanConfig.PlanConfig planConfig = planConfigOpt.get();
        
        // Get the plan version from the loaded config
        Optional<FeaturePlanConfig> latestConfigOpt = 
            featureConfigService.loadLatestPlanConfig(productCode);
        String planVersion = latestConfigOpt.map(FeaturePlanConfig::getVersion)
            .orElse(LocalDate.now().format(VERSION_FORMATTER)); // Fallback to current date format
        
        // Update subscription
        subscription.setPlanCode(newPlanCode);
        subscription.setPlanVersion(planVersion);
        
        // Update billing columns based on subscription status
        SubscriptionStatus currentStatus = subscription.getStatus();
        if (currentStatus == SubscriptionStatus.TRIAL) {
            subscription.setBillingCycleStartDate(null);
            subscription.setBillingCycleEndDate(null);
            subscription.setNextBillingDate(null);
        } else if (currentStatus == SubscriptionStatus.ACTIVE) {
            // Only set billing dates if they are null (don't recalculate if already set)
            if (subscription.getBillingCycleStartDate() == null) {
                LocalDate billingCycleStart = LocalDate.now();
                subscription.setBillingCycleStartDate(billingCycleStart);
                subscription.setBillingCycleEndDate(billingCycleStart.plusMonths(1).minusDays(1));
                subscription.setNextBillingDate(billingCycleStart.plusMonths(1));
            }
        }
        
        // Call provider service (stub for now)
        subscriptionProviderService.updateSubscription(subscriptionId, newPlanCode);
        
        subscription = subscriptionRepository.save(subscription);
        
        // Sync Account.status based on subscription status
        Account account = subscription.getAccount();
        if (currentStatus == SubscriptionStatus.TRIAL || currentStatus == SubscriptionStatus.ACTIVE) {
            account.setStatus(AccountStatus.ACTIVE);
        } else {
            account.setStatus(AccountStatus.BLOCKED);
        }
        accountRepository.save(account);
        
        // Update subscription features
        updateSubscriptionFeatures(subscription, planConfig);
        
        LoggingUtils.logMethodExit(log, "updateSubscription", 
            String.format("subscriptionId=%d", subscription.getId()));
        
        return subscription;
    }
    
    @Override
    @Transactional
    public void cancelSubscription(Long subscriptionId) {
        LoggingUtils.logMethodEntry(log, "cancelSubscription", "subscriptionId", subscriptionId);
        
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));
        
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(java.time.OffsetDateTime.now());
        
        // Call provider service (stub for now)
        subscriptionProviderService.cancelSubscription(subscriptionId);
        
        subscriptionRepository.save(subscription);
        
        // Sync Account.status to BLOCKED (since subscription is CANCELLED)
        Account account = subscription.getAccount();
        account.setStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);
        
        LoggingUtils.logMethodExit(log, "cancelSubscription", 
            String.format("subscriptionId=%d", subscriptionId));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionInfo> getSubscriptionInfo(Long accountId) {
        Optional<Subscription> subscriptionOpt = getActiveSubscription(accountId);
        if (subscriptionOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Subscription subscription = subscriptionOpt.get();
        
        // Get plan name from config
        String planName = featureConfigService.getPlanConfig(
            subscription.getProductCode(), 
            subscription.getPlanCode())
            .map(FeaturePlanConfig.PlanConfig::getName)
            .orElse(subscription.getPlanCode());
        
        SubscriptionInfo info = SubscriptionInfo.builder()
            .planCode(subscription.getPlanCode())
            .planName(planName)
            .status(subscription.getStatus() != null ? subscription.getStatus().name() : null)
            .billingCycleEndDate(subscription.getBillingCycleEndDate())
            .productCode(subscription.getProductCode())
            .billingCycleStartDate(subscription.getBillingCycleStartDate())
            .nextBillingDate(subscription.getNextBillingDate())
            .build();
        
        return Optional.of(info);
    }
    
    private void createSubscriptionFeatures(Subscription subscription, FeaturePlanConfig.PlanConfig planConfig) {
        List<SubscriptionFeature> features = new ArrayList<>();
        
        for (FeaturePlanConfig.FeatureConfig featureConfig : planConfig.getFeatures()) {
            SubscriptionFeature feature = new SubscriptionFeature();
            feature.setSubscription(subscription);
            feature.setFeatureCode(featureConfig.getCode());
            
            if (featureConfig.getLimit() != null) {
                feature.setLimitValue(featureConfig.getLimit());
            }
            
            if (featureConfig.getEnabled() != null) {
                feature.setEnabled(featureConfig.getEnabled());
            }
            
            if (featureConfig.getRateLimitRpm() != null) {
                feature.setRateLimitRpm(featureConfig.getRateLimitRpm());
            }
            
            if (featureConfig.getChannels() != null && !featureConfig.getChannels().isEmpty()) {
                try {
                    String channelListJson = objectMapper.writeValueAsString(featureConfig.getChannels());
                    feature.setChannelList(channelListJson);
                } catch (Exception e) {
                    log.error("Error serializing channel list: {}", e.getMessage(), e);
                }
            }
            
            features.add(feature);
        }
        
        subscriptionFeatureRepository.saveAll(features);
        log.info("Created {} subscription features for subscriptionId={}", 
            features.size(), subscription.getId());
    }
    
    private void updateSubscriptionFeatures(Subscription subscription, FeaturePlanConfig.PlanConfig planConfig) {
        // Delete existing features
        List<SubscriptionFeature> existingFeatures = 
            subscriptionFeatureRepository.findBySubscriptionId(subscription.getId());
        subscriptionFeatureRepository.deleteAll(existingFeatures);
        
        // Create new features
        createSubscriptionFeatures(subscription, planConfig);
    }
}

