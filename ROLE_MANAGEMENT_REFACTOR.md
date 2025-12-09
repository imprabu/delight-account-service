# Role Management Refactoring - Drop Unused Tables

## Overview
This refactoring removes unused role management tables in preparation for Casbin integration. The current system uses a simple role-based access control (RBAC) model with the `memberships` table, which is sufficient for Casbin integration.

## Tables Analysis

### Tables to Keep ✅
- **`users`** - Core user information (required)
- **`memberships`** - User-account-role relationships (required for Casbin)

### Tables to Drop ❌
- **`user_roles`** - Unused legacy table
- **`role_permissions`** - Unused legacy table  
- **`role_definitions`** - Unused legacy table
- **`role_custom_permissions`** - Unused legacy table

## Current Role Management

### Role Enum
The system uses a simple `Role` enum with three roles:
- `OWNER` - Full administrative privileges
- `ADMIN` - Administrative privileges (limited)
- `MEMBER` - Basic user privileges (displayed as "USER" in API responses)

### Memberships Table
The `memberships` table stores:
- `user_id` - Reference to user
- `account_id` - Reference to account
- `role` - Role enum value (OWNER, ADMIN, MEMBER)
- `status` - Membership status (ACTIVE, SUSPENDED, REMOVED, INVITED)
- `created_at` - Creation timestamp
- `invited_by` - User who invited this membership
- `updated_at` - Last update timestamp

**Unique Constraint**: `(user_id, account_id)` - One membership per user per account

## Migration Details

### Migration File
- **File**: `src/main/resources/db/changelog/20251201-drop-unused-role-tables.yaml`
- **ChangeSet ID**: `20251201-drop-unused-role-tables`
- **Author**: codex

### What It Does
1. Drops `user_roles` table (if exists)
2. Drops `role_permissions` table (if exists)
3. Drops `role_definitions` table (if exists)
4. Drops `role_custom_permissions` table (if exists)

### Safety Features
- Uses `preConditions` with `onFail: MARK_RAN` to handle cases where tables don't exist
- Each drop operation checks if table exists before attempting to drop
- Migration is idempotent - safe to run multiple times

## Code Verification

### No Code References Found ✅
Verified that no Java code references these tables:
- No `@Table` annotations referencing these tables
- No SQL queries using these tables
- No repository interfaces for these tables
- No entity classes for these tables

## Casbin Integration Readiness

### Current State
The `memberships` table is well-suited for Casbin integration:

1. **User-Role Mapping**: `memberships` table provides user-account-role relationships
   - Format: `(user_id, account_id) -> role`
   - Can be mapped to Casbin policies: `p, user_id:account_id, role`

2. **Account Context**: Each membership is scoped to an account
   - Enables multi-tenant role management
   - Supports account-specific permissions

3. **Status Management**: Membership status allows for:
   - Active memberships (ACTIVE)
   - Suspended access (SUSPENDED)
   - Removed memberships (REMOVED)
   - Pending invitations (INVITED)

### Recommended Casbin Model
For Casbin integration, consider using:

```
[request_definition]
r = sub, obj, act

[policy_definition]
p = sub, obj, act

[role_definition]
g = _, _

[policy_effect]
e = some(where (p.eft == allow))

[matchers]
m = g(r.sub, p.sub) && r.obj == p.obj && r.act == p.act
```

Where:
- `sub` = user_id:account_id (from memberships)
- `obj` = resource/resource_id
- `act` = action (read, write, delete, etc.)
- `g` = role hierarchy (OWNER > ADMIN > MEMBER)

## Migration Execution

### To Apply Migration
```bash
# The migration will run automatically on next application startup
./gradlew bootRun

# Or manually with Liquibase
./gradlew liquibaseUpdate
```

### To Verify
```sql
-- Check that tables are dropped
SHOW TABLES LIKE '%role%';

-- Should only show memberships table (if any)
-- Verify memberships table still exists
SHOW TABLES LIKE 'memberships';
DESCRIBE memberships;
```

## Rollback (if needed)

If you need to rollback, you would need to:
1. Recreate the tables (if you have their original schema)
2. Restore any data (if you have backups)
3. Mark the changeset as executed: `liquibase rollbackCount 1`

**Note**: This migration is designed to be one-way. Ensure you have backups before running if you need to preserve data from these tables.

## Next Steps for Casbin Integration

1. **Add Casbin Dependency**
   ```gradle
   implementation 'org.casbin:casbin-spring-boot-starter:1.x.x'
   ```

2. **Create Casbin Configuration**
   - Configure Casbin enforcer
   - Set up adapter to read from `memberships` table
   - Define policy model

3. **Update Security Configuration**
   - Replace or enhance Spring Security with Casbin
   - Integrate Casbin enforcer in filters/interceptors

4. **Create Casbin Adapter**
   - Read policies from `memberships` table
   - Map roles to permissions
   - Handle account-scoped permissions

## Files Modified

1. **Created**: `src/main/resources/db/changelog/20251201-drop-unused-role-tables.yaml`
2. **Modified**: `src/main/resources/db/changelog/db.changelog-master.yaml`

## Testing

After migration:
1. Verify application starts successfully
2. Verify user authentication still works
3. Verify role-based access control still functions
4. Verify memberships can be created/updated/deleted
5. Check application logs for any errors



