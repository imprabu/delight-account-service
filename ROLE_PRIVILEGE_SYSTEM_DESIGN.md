# Role and Privilege System Design

## Overview
This document describes the new role and privilege management system designed for Casbin integration with support for custom roles and privilege restrictions.

## Database Schema

### Tables Created

#### 1. `privileges` Table
Stores all available privileges in the system.

**Columns:**
- `id` (bigint, PK) - Primary key
- `code` (varchar(100), UNIQUE) - Unique privilege code (e.g., 'user.create', 'account.update')
- `name` (varchar(255)) - Human-readable privilege name
- `description` (text) - Description of what the privilege allows
- `category` (varchar(100)) - Category grouping (e.g., 'user_management', 'account_management')
- `is_system_privilege` (boolean) - If true, cannot be restricted from custom roles
- `created_at` (timestamp) - Creation timestamp
- `updated_at` (varchar(64)) - Last update timestamp

**Indexes:**
- `idx_privileges_code` on `code`

#### 2. `roles` Table
Stores both system/default roles and custom roles.

**Columns:**
- `id` (bigint, PK) - Primary key
- `account_id` (bigint, FK, NULLABLE) - NULL for system roles, account_id for custom roles
- `name` (varchar(255)) - Role name (e.g., 'Org Admin', 'Custom Manager Role')
- `code` (varchar(100)) - Unique role code (e.g., 'ORG_ADMIN', 'CUSTOM_MANAGER_001')
- `description` (text) - Role description
- `role_type` (varchar(50)) - 'SYSTEM' or 'CUSTOM'
- `base_role_id` (bigint, FK, NULLABLE) - For custom roles, references the system role it's based on
- `hierarchy_level` (int) - 1=Org Admin (highest), 2=Account Admin, 3=Agent (lowest)
- `is_active` (boolean) - Whether the role is active
- `created_at` (timestamp) - Creation timestamp
- `updated_at` (varchar(64)) - Last update timestamp

**Constraints:**
- Unique constraint on `(account_id, code)` - Allows same code for different accounts
- Foreign key to `accounts` table (on delete CASCADE)
- Foreign key to `roles` table (self-reference for base_role_id)

**Indexes:**
- `idx_roles_account_id` on `account_id`
- `idx_roles_code` on `code`

#### 3. `role_privileges` Table
Junction table for many-to-many relationship between roles and privileges.

**Columns:**
- `id` (bigint, PK) - Primary key
- `role_id` (bigint, FK) - Reference to roles table
- `privilege_id` (bigint, FK) - Reference to privileges table
- `created_at` (timestamp) - Creation timestamp

**Constraints:**
- Unique constraint on `(role_id, privilege_id)`
- Foreign keys to `roles` and `privileges` (on delete CASCADE)

**Indexes:**
- `idx_role_privileges_role_id` on `role_id`
- `idx_role_privileges_privilege_id` on `privilege_id`

#### 4. `restricted_privileges` Table
Stores privileges that cannot be added to custom roles.

**Columns:**
- `id` (bigint, PK) - Primary key
- `privilege_id` (bigint, FK) - Reference to privileges table
- `account_id` (bigint, FK, NULLABLE) - NULL for global restrictions, account_id for account-specific
- `reason` (text) - Reason why this privilege is restricted
- `created_at` (timestamp) - Creation timestamp

**Constraints:**
- Unique constraint on `(account_id, privilege_id)`
- Foreign keys to `privileges` and `accounts` (on delete CASCADE)

**Indexes:**
- `idx_restricted_privileges_privilege_id` on `privilege_id`
- `idx_restricted_privileges_account_id` on `account_id`

#### 5. `memberships` Table (Updated)
Updated to use `role_id` instead of `role` enum.

**Changes:**
- Added `role_id` (bigint, FK) - Reference to roles table
- Removed `role` (varchar) - Old enum column
- Foreign key to `roles` table (on delete RESTRICT)

## Default System Roles

### 1. Org Admin (ORG_ADMIN)
- **Hierarchy Level**: 1 (Highest)
- **Type**: SYSTEM
- **Privileges**: ALL privileges (super user)
- **Usage**: User who created the account is assigned this role by default

### 2. Account Admin (ACCOUNT_ADMIN)
- **Hierarchy Level**: 2 (Middle)
- **Type**: SYSTEM
- **Privileges**: 
  - User management (create, read, update, invite, activate, suspend)
  - Account management (read, update, billing, settings)
  - Role management (create, read, update, delete, assign)
  - Data access (read, write, delete, export)
  - **Excludes**: User delete, account delete, organization management, system privileges

### 3. Agent (AGENT)
- **Hierarchy Level**: 3 (Lowest)
- **Type**: SYSTEM
- **Privileges**:
  - User read
  - Account read
  - Role read
  - Data read and write
  - **Excludes**: Most management and administrative privileges

## Default Privileges

### User Management
- `user.create` - Create new users
- `user.read` - View users
- `user.update` - Update user information
- `user.delete` - Delete users
- `user.invite` - Invite users
- `user.activate` - Activate user accounts
- `user.suspend` - Suspend user accounts

### Account Management
- `account.read` - View account information
- `account.update` - Update account settings
- `account.delete` - Delete the account (system privilege)
- `account.billing` - Manage billing and subscriptions
- `account.settings` - Manage account settings

### Role Management
- `role.create` - Create custom roles
- `role.read` - View roles and permissions
- `role.update` - Update role permissions
- `role.delete` - Delete custom roles
- `role.assign` - Assign roles to users

### Organization Management
- `org.read` - View organization information
- `org.update` - Update organization settings
- `org.delete` - Delete the organization (system privilege)
- `org.account.create` - Create new accounts
- `org.account.manage` - Manage all accounts

### Data Access
- `data.read` - Read data
- `data.write` - Create and update data
- `data.delete` - Delete data
- `data.export` - Export data

### System Privileges (Cannot be restricted)
- `system.admin` - Full system administration
- `system.audit` - View audit logs
- `system.config` - System configuration

## Custom Roles

### Features
1. **Based on System Roles**: Custom roles can be based on Org Admin, Account Admin, or Agent
2. **Selective Privileges**: Can choose specific privileges to include
3. **Restriction Enforcement**: Cannot include privileges marked as restricted
4. **Account-Scoped**: Custom roles are scoped to specific accounts

### Creation Process
1. Select base role (Org Admin, Account Admin, or Agent)
2. Choose privileges from available list
3. System validates that selected privileges are not restricted
4. Custom role is created with `role_type = 'CUSTOM'` and `base_role_id` set

## Privilege Restrictions

### Types
1. **Global Restrictions**: `account_id = NULL` - Applies to all accounts
2. **Account-Specific Restrictions**: `account_id = <id>` - Applies only to specific account

### Enforcement
- System privileges (`is_system_privilege = true`) cannot be restricted
- When creating/updating custom roles, system checks `restricted_privileges` table
- Restricted privileges are excluded from available privilege list for custom roles

## Migration Strategy

### Phase 1: Create New Tables
1. Create `privileges` table
2. Create `roles` table
3. Create `role_privileges` table
4. Create `restricted_privileges` table

### Phase 2: Insert Default Data
1. Insert default system roles (Org Admin, Account Admin, Agent)
2. Insert default privileges
3. Assign all privileges to Org Admin
4. Assign appropriate privileges to Account Admin
5. Assign basic privileges to Agent

### Phase 3: Migrate Memberships
1. Add `role_id` column to `memberships` table
2. Map existing role enum values to role_ids:
   - `OWNER` → `ORG_ADMIN`
   - `ADMIN` → `ACCOUNT_ADMIN`
   - `MEMBER` → `AGENT`
3. Make `role_id` required
4. Drop old `role` enum column

## Casbin Integration

### Policy Format
For Casbin, policies can be structured as:
```
p, user_id:account_id, resource, action
```

Where:
- `user_id:account_id` - Subject (from memberships)
- `resource` - Resource being accessed
- `action` - Action (from privileges)

### Role Hierarchy
Casbin can use role hierarchy:
```
g, user_id:account_id, role_id
g2, role_id, privilege_code
```

This allows:
- User → Role mapping (from memberships)
- Role → Privilege mapping (from role_privileges)

## Code Changes Required

### Entity Classes
1. Create `Privilege` entity
2. Create `Role` entity
3. Create `RolePrivilege` entity
4. Create `RestrictedPrivilege` entity
5. Update `Membership` entity to use `Role` instead of `Role` enum

### Repository Interfaces
1. `PrivilegeRepository`
2. `RoleRepository`
3. `RolePrivilegeRepository`
4. `RestrictedPrivilegeRepository`
5. Update `MembershipRepository` queries

### Service Classes
1. `RoleService` - Manage roles and privileges
2. `PrivilegeService` - Manage privileges
3. Update `MembershipService` - Use role_id
4. Update `SignupService` - Assign Org Admin role
5. Update authentication/authorization logic

### DTOs
1. `RoleResponse` - Role with privileges
2. `PrivilegeResponse` - Privilege information
3. `CreateRoleRequest` - Create custom role
4. `UpdateRoleRequest` - Update role privileges
5. Update `MemberResponse` - Include role information

## Business Rules

1. **Default Role Assignment**: User who creates account → Org Admin
2. **Role Hierarchy**: Org Admin > Account Admin > Agent
3. **Custom Role Base**: Must be based on one of the three system roles
4. **Privilege Restrictions**: Cannot add restricted privileges to custom roles
5. **System Privileges**: Cannot be restricted from custom roles
6. **Account Scoping**: Custom roles are account-specific
7. **Role Deletion**: Cannot delete system roles, can delete custom roles

## API Endpoints (To Be Created)

### Role Management
- `GET /api/roles` - List all roles (system + account custom)
- `GET /api/roles/{id}` - Get role details with privileges
- `POST /api/roles` - Create custom role
- `PUT /api/roles/{id}` - Update custom role
- `DELETE /api/roles/{id}` - Delete custom role

### Privilege Management
- `GET /api/privileges` - List all privileges
- `GET /api/privileges/available` - Get available privileges for custom role creation
- `GET /api/privileges/restricted` - Get restricted privileges

### Membership Updates
- `PUT /api/memberships/{id}` - Update membership (now uses role_id)







