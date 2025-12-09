package com.delight.account.event;

import com.delight.account.model.Membership;

/**
 * Spring application event published when a Membership is removed.
 * This event is published during the transaction, but listeners should use
 * @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) to handle it
 * only after the transaction commits successfully.
 */
public class MembershipRemovedEvent {
    private final Membership membership;
    
    public MembershipRemovedEvent(Membership membership) {
        this.membership = membership;
    }
    
    public Membership getMembership() {
        return membership;
    }
}

