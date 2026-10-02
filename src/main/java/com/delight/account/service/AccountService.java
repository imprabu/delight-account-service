package com.delight.account.service;

import com.delight.account.dto.AccountInfoResponse;
import java.util.List;

public interface AccountService {

    AccountInfoResponse getAccountInfo(Long accountId, Long userId);
    
    /**
     * Get account information for Channel context without user-specific data
     * @param accountId the account ID
     * @return account information without user-specific fields
     */
    AccountInfoResponse getAccountInfoForChannel(Long accountId);
    
    /**
     * Get all accounts information
     * @return list of all accounts information without user-specific data
     */
    List<AccountInfoResponse> getAllAccounts();
    
    /**
     * Check if an account is active.
     * An account is considered active if:
     * - Account.status is ACTIVE
     * - Subscription.status (for primary product) is TRIAL or ACTIVE
     * @param accountId the account ID
     * @return true if account is active, false otherwise
     */
    boolean isAccountActive(Long accountId);
}
