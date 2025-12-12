package com.delight.account.service.impl;

import com.delight.account.model.Account;
import com.delight.account.model.FeatureUsage;
import com.delight.account.model.Subscription;
import com.delight.account.model.SubscriptionFeature;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.FeatureUsageRepository;
import com.delight.account.repository.SubscriptionFeatureRepository;
import com.delight.account.repository.SubscriptionRepository;
import com.delight.account.service.UsageTrackingService;
import com.delight.account.util.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
public class UsageTrackingServiceImpl implements UsageTrackingService {
    
    private final FeatureUsageRepository featureUsageRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionFeatureRepository subscriptionFeatureRepository;
    private final AccountRepository accountRepository;
    
    public UsageTrackingServiceImpl(
            FeatureUsageRepository featureUsageRepository,
            SubscriptionRepository subscriptionRepository,
            SubscriptionFeatureRepository subscriptionFeatureRepository,
            AccountRepository accountRepository) {
        this.featureUsageRepository = featureUsageRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionFeatureRepository = subscriptionFeatureRepository;
        this.accountRepository = accountRepository;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<FeatureUsage> getCurrentUsage(Long accountId, Long subscriptionId, String featureCode) {
        LocalDate currentDate = LocalDate.now();
        
        return featureUsageRepository.findCurrentUsageByAccountIdAndSubscriptionIdAndFeatureCode(
            accountId, subscriptionId, featureCode, currentDate);
    }
    
    @Override
    @Transactional
    public FeatureUsage incrementUsage(Long accountId, Long subscriptionId, String featureCode, Long increment) {
        LoggingUtils.logMethodEntry(log, "incrementUsage", 
            "accountId", accountId, "subscriptionId", subscriptionId, 
            "featureCode", featureCode, "increment", increment);
        
        FeatureUsage usage = getOrCreateUsage(accountId, subscriptionId, featureCode);
        
        // Use atomic increment
        featureUsageRepository.incrementUsageValue(usage.getId(), increment);
        
        // Reload to get updated value
        usage = featureUsageRepository.findById(usage.getId())
            .orElseThrow(() -> new IllegalStateException("Usage not found after increment"));
        
        LoggingUtils.logMethodExit(log, "incrementUsage", 
            String.format("usageId=%d, newUsageValue=%d", usage.getId(), usage.getUsageValue()));
        
        return usage;
    }
    
    @Override
    @Transactional
    public FeatureUsage decrementUsage(Long accountId, Long subscriptionId, String featureCode, Long decrement) {
        LoggingUtils.logMethodEntry(log, "decrementUsage", 
            "accountId", accountId, "subscriptionId", subscriptionId, 
            "featureCode", featureCode, "decrement", decrement);
        
        FeatureUsage usage = getOrCreateUsage(accountId, subscriptionId, featureCode);
        
        // Use atomic decrement (only if sufficient value)
        int updated = featureUsageRepository.decrementUsageValue(usage.getId(), decrement);
        if (updated == 0) {
            log.warn("Cannot decrement usage - insufficient value. UsageId={}, CurrentValue={}, Decrement={}", 
                usage.getId(), usage.getUsageValue(), decrement);
            return usage;
        }
        
        // Reload to get updated value
        usage = featureUsageRepository.findById(usage.getId())
            .orElseThrow(() -> new IllegalStateException("Usage not found after decrement"));
        
        LoggingUtils.logMethodExit(log, "decrementUsage", 
            String.format("usageId=%d, newUsageValue=%d", usage.getId(), usage.getUsageValue()));
        
        return usage;
    }
    
    @Override
    @Transactional
    public FeatureUsage setUsage(Long accountId, Long subscriptionId, String featureCode, Long value) {
        LoggingUtils.logMethodEntry(log, "setUsage", 
            "accountId", accountId, "subscriptionId", subscriptionId, 
            "featureCode", featureCode, "value", value);
        
        FeatureUsage usage = getOrCreateUsage(accountId, subscriptionId, featureCode);
        usage.setUsageValue(value);
        usage = featureUsageRepository.save(usage);
        
        LoggingUtils.logMethodExit(log, "setUsage", 
            String.format("usageId=%d, newUsageValue=%d", usage.getId(), usage.getUsageValue()));
        
        return usage;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isUsageAllowed(Long accountId, Long subscriptionId, String featureCode) {
        Optional<SubscriptionFeature> featureOpt = 
            subscriptionFeatureRepository.findBySubscriptionIdAndFeatureCode(subscriptionId, featureCode);
        
        if (featureOpt.isEmpty()) {
            return false;
        }
        
        SubscriptionFeature feature = featureOpt.get();
        
        // For boolean features, check if enabled
        if (feature.getLimitValue() == null) {
            return Boolean.TRUE.equals(feature.getEnabled());
        }
        
        // For unlimited features (-1)
        if (feature.getLimitValue() == -1) {
            return true;
        }
        
        // For usage_limit features, check current usage
        Optional<FeatureUsage> usageOpt = getCurrentUsage(accountId, subscriptionId, featureCode);
        Long currentUsage = usageOpt.map(FeatureUsage::getUsageValue).orElse(0L);
        
        return currentUsage < feature.getLimitValue();
    }
    
    private FeatureUsage getOrCreateUsage(Long accountId, Long subscriptionId, String featureCode) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
        if (subscriptionOpt.isEmpty()) {
            throw new IllegalArgumentException("Subscription not found: " + subscriptionId);
        }
        
        Subscription subscription = subscriptionOpt.get();
        LocalDate billingCycleStart = subscription.getBillingCycleStartDate();
        LocalDate billingCycleEnd = subscription.getBillingCycleEndDate();
        
        // Check if usage exists for current billing cycle
        Optional<FeatureUsage> usageOpt = featureUsageRepository
            .findByAccountIdAndSubscriptionIdAndFeatureCodeAndBillingCycleStartDate(
                accountId, subscriptionId, featureCode, billingCycleStart);
        
        if (usageOpt.isPresent()) {
            return usageOpt.get();
        }
        
        // Create new usage record
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        FeatureUsage usage = new FeatureUsage();
        usage.setAccount(account);
        usage.setSubscription(subscription);
        usage.setFeatureCode(featureCode);
        usage.setUsageValue(0L);
        usage.setBillingCycleStartDate(billingCycleStart);
        usage.setBillingCycleEndDate(billingCycleEnd);
        usage.setLastResetAt(java.time.OffsetDateTime.now());
        
        return featureUsageRepository.save(usage);
    }
}

