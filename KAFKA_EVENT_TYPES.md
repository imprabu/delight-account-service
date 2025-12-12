# Kafka Event Types Reference

This document lists all possible event types published to each Kafka topic by the IAM service.

## Topic: `account_activities_v1`

**Purpose**: Account lifecycle events (changes to the `accounts` table)

### Event Types:

1. **`account.created`**
   - **Triggered when**: A new account is created
   - **Source**: `AccountEventJobEnqueueListener.handleAccountCreated()`
   - **Published by**: `AccountEventPublisherWorker`
   - **Payload**: Complete account information including domain, companyName, status, plan, organization, product, etc.

2. **`account.updated`**
   - **Triggered when**: Account information is updated
   - **Source**: `AccountEventJobEnqueueListener.handleAccountUpdated()`
   - **Published by**: `AccountEventPublisherWorker`
   - **Payload**: Complete account information with updated fields
   - **Note**: Also published by `BulkAccountSyncWorker` during bulk synchronization

3. **`account.deleted`**
   - **Triggered when**: An account is deleted
   - **Source**: `AccountEventJobEnqueueListener.handleAccountDeleted()`
   - **Published by**: `AccountEventPublisherWorker`
   - **Payload**: Minimal account information (may be null if account already deleted)

---

## Topic: `user_activities_v1`

**Purpose**: User table changes only (changes to the `users` table)

### Event Types:

1. **`user.created`**
   - **Triggered when**: A new user is created
   - **Source**: `UserEventJobEnqueueListener.handleUserCreated()`
   - **Published by**: `UserEventPublisherWorker`
   - **Payload**: Complete user information including userName, emailAddress, firstName, lastName, accountId, etc.

2. **`user.updated`**
   - **Triggered when**: User information is updated (name, email, status, etc.)
   - **Source**: `UserEventJobEnqueueListener.handleUserUpdated()`
   - **Published by**: `UserEventPublisherWorker`
   - **Payload**: Complete user information with updated fields
   - **Note**: This event is for user table changes only, not membership changes

3. **`user.deleted`**
   - **Triggered when**: A user is deleted
   - **Source**: `UserEventJobEnqueueListener.handleUserDeleted()`
   - **Published by**: `UserEventPublisherWorker`
   - **Payload**: Minimal user information (may be null if user already deleted)

---

## Topic: `user_role_activities_v1`

**Purpose**: Membership and role changes (changes to the `memberships` table and role assignments)

### Event Types:

1. **`user.membership.added`**
   - **Triggered when**: A user is added to an account (membership created)
   - **Source**: `MembershipEventJobEnqueueListener.handleMembershipAdded()`
   - **Published by**: `MembershipEventPublisherWorker`
   - **Payload**: Complete membership information including membership ID, userId, accountId, roleId, roleName, status, etc.
   - **Note**: `data.id` is the membership ID (primary key), not the user ID

2. **`user.membership.updated`**
   - **Triggered when**: Membership information is updated (status change, etc.)
   - **Source**: `MembershipEventJobEnqueueListener.handleMembershipUpdated()`
   - **Published by**: `MembershipEventPublisherWorker`
   - **Payload**: Complete membership information with updated fields
   - **Note**: This event is published even when role changes, but role changes also trigger a separate `user.role.changed` event

3. **`user.membership.removed`**
   - **Triggered when**: A user is removed from an account (membership deleted)
   - **Source**: `MembershipEventJobEnqueueListener.handleMembershipRemoved()`
   - **Published by**: `MembershipEventPublisherWorker`
   - **Payload**: Membership information (may be minimal if membership already deleted)
   - **Note**: `data.id` is the membership ID (primary key), not the user ID

4. **`user.role.changed`**
   - **Triggered when**: A user's role changes within an account (roleId changes in membership)
   - **Source**: `MembershipEventJobEnqueueListener.handleMembershipUpdated()` (when roleId changes)
   - **Published by**: `KafkaEventPublisher.publishUserRoleChangeEvent()` (directly, not via worker)
   - **Payload**: Role change information including:
     - `oldRoleId` - Previous role ID
     - `oldRoleName` - Previous role name
     - `newRoleId` - New role ID
     - `newRoleName` - New role name
     - `membershipId` - Membership ID
     - `roleId` - Current role ID (same as newRoleId)
     - `roleName` - Current role name (same as newRoleName)
     - `status` - Membership status
     - `updated_at` - Timestamp with nanosecond precision
   - **Note**: This event is published **in addition to** `user.membership.updated` when a role change is detected

---

## Event Type Summary Table

| Topic | Event Type | Description | Frequency |
|-------|-----------|-------------|-----------|
| `account_activities_v1` | `account.created` | New account created | On account creation |
| `account_activities_v1` | `account.updated` | Account information updated | On account update or bulk sync |
| `account_activities_v1` | `account.deleted` | Account deleted | On account deletion |
| `user_activities_v1` | `user.created` | New user created | On user creation |
| `user_activities_v1` | `user.updated` | User information updated | On user update |
| `user_activities_v1` | `user.deleted` | User deleted | On user deletion |
| `user_role_activities_v1` | `user.membership.added` | User added to account | On membership creation |
| `user_role_activities_v1` | `user.membership.updated` | Membership information updated | On membership update |
| `user_role_activities_v1` | `user.membership.removed` | User removed from account | On membership deletion |
| `user_role_activities_v1` | `user.role.changed` | User's role changed | On role change (in addition to membership.updated) |

---

## Important Notes

### ID Field Meanings

**Account Events**:
- `data.id` = Account ID (primary key)

**User Events**:
- `data.id` = User ID (primary key)
- `data.accountId` = Account ID (foreign key)

**Membership Events**:
- `data.id` = **Membership ID** (primary key of memberships table) ⚠️
- `data.userId` = User ID (foreign key)
- `data.accountId` = Account ID (foreign key)
- `data.roleId` = Role ID (foreign key)

### Event Ordering

- When a role changes, **both** `user.membership.updated` and `user.role.changed` events are published
- The `user.role.changed` event is published directly (not via worker) and contains additional role change details
- Events are published **after transaction commit** to ensure data consistency

### Versioning

- All event payloads include an `updated_at` field with **nanosecond precision**
- This field serves as the version identifier for the payload
- Use this field for conflict resolution and data synchronization

---

**Last Updated**: 2025-12-08
**Version**: 1.0





