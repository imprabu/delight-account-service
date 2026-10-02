# Role and Privilege System Migration Summary

## ✅ Completed

### Database Schema Created

1. **`privileges` table** - Stores all available privileges
2. **`roles` table** - Stores system and custom roles
3. **`role_privileges` table** - Junction table for role-privilege mapping
4. **`restricted_privileges` table** - Stores privileges restricted from custom roles
5. **`memberships` table updated** - Changed from `role` enum to `role_id` foreign key

### Default Data Inserted

1. **System Roles**:
   - Org Admin (ORG_ADMIN) - Hierarchy level 1, all privileges
   - Account Admin (ACCOUNT_ADMIN) - Hierarchy level 2, management privileges
   - Agent (AGENT) - Hierarchy level 3, basic privileges

2. **Default Privileges** (25+ privileges across categories):
   - User Management (7 privileges)
   - Account Management (5 privileges)
   - Role Management (5 privileges)
   - Organization Management (5 privileges)
   - Data Access (4 privileges)
   - System Privileges (3 privileges)

3. **Privilege Assignments**:
   - Org Admin: ALL privileges
   - Account Admin: Management and data access privileges
   - Agent: Read and basic write privileges

### Migration Files Created

1. `20251201-create-roles-privileges-system.yaml` - Creates all new tables
2. `20251201-insert-default-roles-privileges.yaml` - Inserts default data
3. `20251201-migrate-memberships-to-role-id.yaml` - Migrates memberships table

## 🔄 Next Steps (Java Code Updates)

### 1. Create Entity Classes

**Privilege.java**
```java
@Entity
@Table(name = "privileges")
public class Privilege {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String code;
    
    private String name;
    private String description;
    private String category;
    
    @Column(name = "is_system_privilege")
    private Boolean isSystemPrivilege;
    
    // Getters, setters, etc.
}
```

**Role.java** (Replace enum)
```java
@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
    
    private String name;
    
    @Column(nullable = false)
    private String code;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false)
    private RoleType roleType; // SYSTEM or CUSTOM
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_role_id")
    private Role baseRole;
    
    @Column(name = "hierarchy_level", nullable = false)
    private Integer hierarchyLevel;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL)
    private Set<RolePrivilege> rolePrivileges = new HashSet<>();
    
    // Getters, setters, etc.
}
```

**RolePrivilege.java**
```java
@Entity
@Table(name = "role_privileges")
public class RolePrivilege {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "privilege_id", nullable = false)
    private Privilege privilege;
    
    // Getters, setters, etc.
}
```

**RestrictedPrivilege.java**
```java
@Entity
@Table(name = "restricted_privileges")
public class RestrictedPrivilege {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "privilege_id", nullable = false)
    private Privilege privilege;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account; // NULL for global restrictions
    
    private String reason;
    
    // Getters, setters, etc.
}
```

**Update Membership.java**
```java
// Replace:
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private Role role;

// With:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "role_id", nullable = false)
private Role role;
```

### 2. Create Repository Interfaces

- `PrivilegeRepository`
- `RoleRepository`
- `RolePrivilegeRepository`
- `RestrictedPrivilegeRepository`
- Update `MembershipRepository` queries

### 3. Update Service Classes

**SignupService** - Assign Org Admin role:
```java
// Find Org Admin role
Role orgAdminRole = roleRepository.findByCodeAndRoleType("ORG_ADMIN", RoleType.SYSTEM)
    .orElseThrow(() -> new AccountCreationException());

membership.setRole(orgAdminRole);
```

**MembershipService** - Update to use role_id:
```java
// Update member role
if (request.getRoleId() != null) {
    Role role = roleRepository.findById(request.getRoleId())
        .orElseThrow(() -> new ApiException("error.role.notfound"));
    membership.setRole(role);
}
```

**JwtAuthenticationFilter** - Update role extraction:
```java
// Get role from membership
Role role = membership.getRole();
String roleCode = role != null ? role.getCode() : "AGENT";
```

### 4. Create New Services

**RoleService** - Manage roles and privileges
**PrivilegeService** - Manage privileges and restrictions

### 5. Update DTOs

- `MemberResponse` - Include role information
- `CreateRoleRequest` - For creating custom roles
- `UpdateRoleRequest` - For updating roles
- `RoleResponse` - Role with privileges
- `PrivilegeResponse` - Privilege information

### 6. Create Controllers

- `RoleController` - CRUD operations for roles
- `PrivilegeController` - List privileges and restrictions

## 📋 Migration Execution Order

The migrations will run in this order (as defined in `db.changelog-master.yaml`):

1. ✅ Drop unused role tables
2. ✅ Create roles and privileges tables
3. ✅ Insert default roles and privileges
4. ✅ Migrate memberships to use role_id

## ⚠️ Important Notes

1. **Default Role Assignment**: User who creates account gets Org Admin role automatically
2. **Migration Safety**: Existing memberships are mapped:
   - OWNER → ORG_ADMIN
   - ADMIN → ACCOUNT_ADMIN
   - MEMBER → AGENT
3. **Backward Compatibility**: Old `Role` enum needs to be replaced with `Role` entity
4. **Casbin Integration**: The schema supports Casbin policy format:
   - User → Role mapping (memberships)
   - Role → Privilege mapping (role_privileges)

## 🚀 Testing Checklist

- [ ] Run migrations successfully
- [ ] Verify default roles are created
- [ ] Verify default privileges are created
- [ ] Verify existing memberships are migrated correctly
- [ ] Test account creation assigns Org Admin
- [ ] Test custom role creation
- [ ] Test privilege restrictions
- [ ] Test role hierarchy enforcement
- [ ] Test Casbin integration







