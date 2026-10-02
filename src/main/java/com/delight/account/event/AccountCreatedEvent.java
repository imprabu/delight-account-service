package com.delight.account.event;

import com.delight.account.model.Account;

/**
 * Spring application event published when an Account is created.
 * This event is published during the transaction, but listeners should use
 * @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) to handle it
 * only after the transaction commits successfully.
 */
public class AccountCreatedEvent {
    private final Account account;
    
    public AccountCreatedEvent(Account account) {
        this.account = account;
    }
    
    public Account getAccount() {
        return account;
    }
}






