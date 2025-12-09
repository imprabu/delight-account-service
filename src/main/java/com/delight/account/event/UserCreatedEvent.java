package com.delight.account.event;

import com.delight.account.model.User;

/**
 * Spring application event published when a User is created.
 * This event is published during the transaction, but listeners should use
 * @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) to handle it
 * only after the transaction commits successfully.
 */
public class UserCreatedEvent {
    private final User user;
    
    public UserCreatedEvent(User user) {
        this.user = user;
    }
    
    public User getUser() {
        return user;
    }
}

