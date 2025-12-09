package com.delight.account.event;

import com.delight.account.model.Account;
import java.util.Map;

/**
 * Spring application event published when an Account is updated.
 * This event is published during the transaction, but listeners should use
 * @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) to handle it
 * only after the transaction commits successfully.
 */
public class AccountUpdatedEvent {
    private final Account account;
    private final Map<String, Object> changeSet;
    
    public AccountUpdatedEvent(Account account, Map<String, Object> changeSet) {
        this.account = account;
        this.changeSet = changeSet;
    }
    
    public Account getAccount() {
        return account;
    }
    
    public Map<String, Object> getChangeSet() {
        return changeSet;
    }
}


