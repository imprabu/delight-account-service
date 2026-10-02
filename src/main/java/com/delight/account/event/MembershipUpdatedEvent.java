package com.delight.account.event;

import com.delight.account.model.Membership;

import java.util.Map;

/**
 * Spring application event published when a Membership is updated.
 * This event is published during the transaction, but listeners should use
 * @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) to handle it
 * only after the transaction commits successfully.
 */
public class MembershipUpdatedEvent {
    private final Membership membership;
    private final Map<String, Object> changeSet;
    
    public MembershipUpdatedEvent(Membership membership, Map<String, Object> changeSet) {
        this.membership = membership;
        this.changeSet = changeSet;
    }
    
    public Membership getMembership() {
        return membership;
    }
    
    public Map<String, Object> getChangeSet() {
        return changeSet;
    }
}





