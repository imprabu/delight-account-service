package com.delight.account.event;

import com.delight.account.model.User;

import java.util.Map;

/**
 * Spring application event published when a User is updated.
 * This event is published during the transaction, but listeners should use
 * @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) to handle it
 * only after the transaction commits successfully.
 */
public class UserUpdatedEvent {
    private final User user;
    private final Map<String, Object> changeSet;
    
    public UserUpdatedEvent(User user, Map<String, Object> changeSet) {
        this.user = user;
        this.changeSet = changeSet;
    }
    
    public User getUser() {
        return user;
    }
    
    public Map<String, Object> getChangeSet() {
        return changeSet;
    }
}





