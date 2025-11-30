package com.delight.account.service.job.worker;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.delight.account.domain.BatchJobType;
import com.delight.account.model.Account;
import com.delight.account.repository.AccountRepository;
import com.delight.account.service.MessagingService;
import com.delight.account.service.job.JobWorker;

/**
 * Worker implementation for publishing account events to messaging service.
 * 
 * Architecture:
 * - CREATE: Fetches account from DB and builds payload
 * - UPDATE: Fetches account from DB and uses changeset to build payload
 * - DELETE: Uses accountInfo from metadata (captured before deletion)
 */
@Component
public class AccountEventPublisherWorker implements JobWorker {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountEventPublisherWorker.class);
    
    @Autowired
    private MessagingService messagingService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Override
    @Transactional(readOnly = true)
    public void processJob(Long jobId, Long accountId, Map<String, Object> metadata) {
        logger.info("🔄 [ACCOUNT-EVENT-WORKER] Processing account event job {} for account {}", jobId, accountId);
        
        try {
            String payloadType = (String) metadata.get("payloadType");
            
            if (payloadType == null) {
                logger.error("❌ [ACCOUNT-EVENT-WORKER] Missing required metadata: payloadType");
                return;
            }
            
            if (accountId == null) {
                logger.error("❌ [ACCOUNT-EVENT-WORKER] Account ID is missing");
                return;
            }
            
            String accountIdStr = accountId.toString();
            Map<String, Object> accountInfo;
            Map<String, Object> changeSet;
            
            // Build payload based on event type
            var result = switch (payloadType) {
                case "account_created" -> {
                    // Fetch account from DB and build payload for CREATE
                    yield new PayloadResult(fetchAndBuildAccountInfo(accountId), null);
                }
                case "account_modified" -> {
                    // Fetch account from DB and build payload with changeset for UPDATE
                    Map<String, Object> cs = (Map<String, Object>) metadata.get("changeSet");
                    yield new PayloadResult(fetchAndBuildAccountInfo(accountId), cs);
                }
                case "account_deleted" -> {
                    // Fetch account from DB and build payload for DELETE
                    // Note: Account may already be deleted, so this might return null
                    Map<String, Object> info = fetchAndBuildAccountInfo(accountId);
                    if (info == null) {
                        // If account not found (already deleted), create minimal payload with just ID
                        logger.warn("⚠️ [ACCOUNT-EVENT-WORKER] Account {} not found in DB for DELETE event, creating minimal payload", accountId);
                        info = new HashMap<>();
                        info.put("id", accountId);
                    }
                    yield new PayloadResult(info, null);
                }
                default -> {
                    logger.error("❌ [ACCOUNT-EVENT-WORKER] Unknown payload type: {}", payloadType);
                    yield new PayloadResult(null, null);
                }
            };
            
            accountInfo = result.accountInfo;
            changeSet = result.changeSet;
            
            if (accountInfo == null) {
                logger.error("❌ [ACCOUNT-EVENT-WORKER] Failed to build account info for account {}", accountIdStr);
                return;
            }
            
            logger.info("📤 [ACCOUNT-EVENT-WORKER] Publishing {} event for account {}", payloadType, accountIdStr);
            
            boolean success = messagingService.publishAccountEventWithRetry(
                payloadType, 
                accountIdStr, 
                accountInfo, 
                changeSet
            );
            
            if (success) {
                logger.info("✅ [ACCOUNT-EVENT-WORKER] Successfully published {} event for account {}", 
                           payloadType, accountIdStr);
            } else {
                logger.error("❌ [ACCOUNT-EVENT-WORKER] Failed to publish {} event for account {} after retries", 
                            payloadType, accountIdStr);
            }
        } catch (Exception e) {
            logger.error("❌ [ACCOUNT-EVENT-WORKER] Failed to process account event job {} for account {}: {}", 
                        jobId, accountId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Fetch account from DB and build account info map.
     * Uses findByIdWithRelations to load Organization and Product in a single query.
     */
    private Map<String, Object> fetchAndBuildAccountInfo(Long accountId) {
        try {
            Optional<Account> accountOpt = accountRepository.findByIdWithRelations(accountId);
            if (accountOpt.isEmpty()) {
                logger.warn("⚠️ [ACCOUNT-EVENT-WORKER] Account {} not found in database", accountId);
                return null;
            }
            
            Account account = accountOpt.get();
            return buildAccountInfo(account);
        } catch (Exception e) {
            logger.error("❌ [ACCOUNT-EVENT-WORKER] Failed to fetch account {} from DB: {}", 
                        accountId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Build account info map from Account entity.
     */
    private Map<String, Object> buildAccountInfo(Account account) {
        Map<String, Object> accountInfo = new HashMap<>();
        if (account == null) {
            return accountInfo;
        }
        
        accountInfo.put("id", account.getId());
        accountInfo.put("domain", account.getDomain());
        accountInfo.put("plan", account.getPlan() != null ? account.getPlan().name() : null);
        accountInfo.put("status", account.getStatus() != null ? account.getStatus().name() : null);
        accountInfo.put("industryType", account.getIndustryType());
        accountInfo.put("email", account.getEmail());
        accountInfo.put("companyName", account.getCompanyName());
        
        // Include updated_at timestamp with nanosecond precision (stored as microseconds in DB)
        if (account.getUpdatedAt() != null) {
            accountInfo.put("updatedAt", account.getUpdatedAt().toString());
        }
        
        if (account.getOrganization() != null) {
            accountInfo.put("organizationId", account.getOrganization().getId());
            accountInfo.put("organizationName", account.getOrganization().getName());
            accountInfo.put("organizationPrimaryDomain", account.getOrganization().getPrimaryDomain());
        }
        
        if (account.getProduct() != null) {
            accountInfo.put("productCode", account.getProduct().getCode());
            accountInfo.put("productName", account.getProduct().getName());
        }
        
        return accountInfo;
    }
    
    @Override
    public BatchJobType getJobType() {
        return BatchJobType.ACCOUNT_EVENT_PUBLISH;
    }
    
    /**
     * Helper class to hold payload result.
     */
    private static class PayloadResult {
        final Map<String, Object> accountInfo;
        final Map<String, Object> changeSet;
        
        PayloadResult(Map<String, Object> accountInfo, Map<String, Object> changeSet) {
            this.accountInfo = accountInfo;
            this.changeSet = changeSet;
        }
    }
}

