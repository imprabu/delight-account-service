# Delight Account Service - Application Flow Diagram

This document contains comprehensive flow diagrams for the Delight Account Service application.

## Entity Relationship Diagram

```mermaid
erDiagram
    Organization {
        Long id PK
        String name
        String primaryDomain
        OrganizationStatus status
    }
    
    Product {
        Long id PK
        String code UK
        String name
        boolean active
    }
    
    Account {
        Long id PK
        Long organization_id FK
        Long product_id FK
        String domain
        PlanType plan
        AccountStatus status
        String industryType
        String email
        String companyName
    }
    
    User {
        Long id PK
        Long account_id FK
        String userName
        String emailAddress
        String firstName
        String lastName
        String companyName
        String phoneNumber
        UserStatus status
    }
    
    Membership {
        Long id PK
        Long user_id FK
        Long account_id FK
        Role role
        MembershipStatus status
        OffsetDateTime createdAt
        Long invited_by FK
    }
    
    UserCredential {
        Long id PK
        Long user_profile_id FK
        Long account_id FK
        String passwordHash
        String apiKey
    }
    
    Session {
        Long id PK
        Long user_id FK
        Long account_id FK
        String sessionId
        String userAgent
        String ipAddress
        OffsetDateTime createdAt
        OffsetDateTime lastAccessedAt
        boolean active
    }

    Organization ||--o{ Account : "owns"
    Product ||--o{ Account : "provides"
    Account ||--o{ User : "contains"
    Account ||--o{ Membership : "has"
    User ||--o{ Membership : "participates"
    User ||--o{ UserCredential : "authenticates"
    User ||--o{ Session : "creates"
    Account ||--o{ Session : "tracks"
    User ||--o{ Membership : "invites (invited_by)"
```

## Application Flow Overview

```mermaid
flowchart TD
    A[Client Request] --> B{Authentication Required?}
    B -->|No| C[Public Endpoints]
    B -->|Yes| D[JWT Authentication Filter]
    
    C --> C1[/account/signup]
    C --> C2[/account/signin]
    C --> C3[/account/activate]
    
    D --> E[Domain Account Interceptor]
    E --> F{Account Found by Domain?}
    F -->|No| G[ApiException: Account Not Found]
    F -->|Yes| H[Set Account Context]
    
    H --> I[Protected Endpoints]
    I --> I1[/account/info]
    I --> I2[/account/members]
    I --> I3[/accounts/sessions]
    
    C1 --> J[SignupService]
    C2 --> K[SigninService]
    C3 --> L[ActivationService]
    
    I1 --> M[AccountService]
    I2 --> N[MembershipService]
    I3 --> O[SessionService]
    
    J --> P[(Database)]
    K --> P
    L --> P
    M --> P
    N --> P
    O --> P
```

## Signup Flow

```mermaid
sequenceDiagram
    participant Client
    participant SignupController
    participant SignupService
    participant DB as Database
    
    Client->>SignupController: POST /account/signup
    Note over Client,SignupController: SignupRequest: companyName, emailAddress, industryType, firstName, lastName
    
    SignupController->>SignupService: signup(request)
    
    SignupService->>DB: Find HRMS Product
    DB-->>SignupService: Product
    
    SignupService->>DB: Create Organization
    Note over SignupService,DB: name=companyName, status=ACTIVE
    DB-->>SignupService: Organization
    
    SignupService->>DB: Create Account
    Note over SignupService,DB: domain=generated, plan=TRIAL, status=ACTIVE
    DB-->>SignupService: Account
    
    SignupService->>DB: Create User
    Note over SignupService,DB: status=ACTIVE, account linked
    DB-->>SignupService: User
    
    SignupService->>DB: Create Membership
    Note over SignupService,DB: role=OWNER, status=ACTIVE
    DB-->>SignupService: Membership
    
    SignupService-->>SignupController: Success
    SignupController-->>Client: 200 "Signup successful"
```

## Signin Flow

```mermaid
sequenceDiagram
    participant Client
    participant SigninController
    participant SigninService
    participant DB as Database
    
    Client->>SigninController: POST /account/signin
    Note over Client,SigninController: SigninRequest: emailAddress, password, domain
    
    SigninController->>DB: Find Account by domain
    DB-->>SigninController: Account
    
    SigninController->>SigninService: signin(accountId, domain, request)
    
    SigninService->>DB: Find User by email + account
    DB-->>SigninService: User
    
    SigninService->>DB: Find UserCredential
    DB-->>SigninService: UserCredential
    
    SigninService->>SigninService: Verify password
    
    SigninService->>DB: Find Membership (optional)
    DB-->>SigninService: Membership
    
    SigninService->>SigninService: Validate Account/Organization status
    
    SigninService->>SigninService: Generate JWT Token
    Note over SigninService: Contains: userId, accountId, orgId, productCode, role
    
    SigninService-->>SigninController: JWT Token
    SigninController-->>Client: 200 JWT Token
```

## Authentication & Authorization Flow

```mermaid
flowchart TD
    A[Incoming Request] --> B[JwtAuthenticationFilter]
    B --> C{JWT Token Present?}
    C -->|No| D[Continue without auth]
    C -->|Yes| E[Validate JWT Token]
    
    E --> F{Token Valid?}
    F -->|No| G[401 Unauthorized]
    F -->|Yes| H[Extract Claims]
    
    H --> I[Set Request Attributes]
    I --> J[accountId]
    I --> K[userId]
    I --> L[productCode]
    I --> M[role]
    
    D --> N[DomainAccountInterceptor]
    I --> N
    
    N --> O{accountId Present?}
    O -->|Yes| P[Continue to Controller]
    O -->|No| Q[Find Account by Domain]
    
    Q --> R{Account Found?}
    R -->|No| S[ApiException: Account Not Found]
    R -->|Yes| T[Set accountId Attribute]
    T --> P
    
    P --> U[Controller Method]
```

## Membership Management Flow

```mermaid
sequenceDiagram
    participant Client
    participant MembershipController
    participant MembershipService
    participant DB as Database
    
    Note over Client,DB: List Members Flow
    Client->>MembershipController: GET /account/members
    MembershipController->>MembershipService: listMembers(accountId, productCode)
    MembershipService->>DB: Find Memberships by Account
    DB-->>MembershipService: List<Membership>
    MembershipService-->>MembershipController: List<MemberResponse>
    MembershipController-->>Client: 200 JSON Array
    
    Note over Client,DB: Update Member Flow
    Client->>MembershipController: PUT /account/members/{userId}
    Note over Client,MembershipController: UpdateMemberRequest: role, status
    MembershipController->>MembershipService: updateMember(accountId, userId, request, actorRole, productCode)
    MembershipService->>DB: Find Target Membership
    DB-->>MembershipService: Membership
    MembershipService->>MembershipService: Validate Permissions
    MembershipService->>DB: Update Membership
    DB-->>MembershipService: Updated Membership
    MembershipService-->>MembershipController: MemberResponse
    MembershipController-->>Client: 200 Updated Member
```

## Status and Role Transitions

```mermaid
stateDiagram-v2
    [*] --> UserCreated : Signup
    
    state "User Lifecycle" as UserLife {
        [*] --> ACTIVE : Account Creation
        ACTIVE --> DEACTIVATED : Admin Action
        ACTIVE --> BLOCKED : Security Issue
        DEACTIVATED --> ACTIVE : Reactivation
        BLOCKED --> ACTIVE : Unblock
        ACTIVE --> INSERTED : Data Migration
    }
    
    state "Membership Lifecycle" as MemberLife {
        [*] --> INVITED : Invitation Sent
        INVITED --> ACTIVE : Invitation Accepted
        ACTIVE --> SUSPENDED : Temporary Disable
        ACTIVE --> REMOVED : Permanent Removal
        SUSPENDED --> ACTIVE : Reactivation
    }
    
    state "Account Lifecycle" as AccountLife {
        [*] --> ACTIVE : Account Creation
        ACTIVE --> SUSPENDED : Payment Issue
        ACTIVE --> BLOCKED : Policy Violation
        ACTIVE --> TO_BE_DELETED : Deletion Request
        SUSPENDED --> ACTIVE : Payment Resolved
        BLOCKED --> ACTIVE : Issue Resolved
    }
    
    state "Role Hierarchy" as RoleHier {
        MEMBER --> ADMIN : Promotion
        ADMIN --> OWNER : Ownership Transfer
        OWNER --> ADMIN : Demotion
        ADMIN --> MEMBER : Demotion
    }
```

## Data Flow Architecture

```mermaid
flowchart LR
    subgraph "Presentation Layer"
        A[REST Controllers]
        A1[SignupController]
        A2[SigninController]
        A3[MembershipController]
        A4[AccountInfoController]
        A5[ActivationController]
        A6[SessionController]
    end
    
    subgraph "Security Layer"
        B[JwtAuthenticationFilter]
        C[DomainAccountInterceptor]
    end
    
    subgraph "Service Layer"
        D[SignupService]
        E[SigninService]
        F[MembershipService]
        G[AccountService]
        H[ActivationService]
        I[SessionService]
    end
    
    subgraph "Data Access Layer"
        J[Repositories]
        J1[AccountRepository]
        J2[UserRepository]
        J3[MembershipRepository]
        J4[OrganizationRepository]
        J5[ProductRepository]
        J6[UserCredentialRepository]
        J7[SessionRepository]
    end
    
    subgraph "Database"
        K[(MySQL Database)]
    end
    
    A --> B
    B --> C
    C --> A1
    C --> A2
    C --> A3
    C --> A4
    C --> A5
    C --> A6
    
    A1 --> D
    A2 --> E
    A3 --> F
    A4 --> G
    A5 --> H
    A6 --> I
    
    D --> J1
    D --> J2
    D --> J3
    D --> J4
    D --> J5
    
    E --> J1
    E --> J2
    E --> J3
    E --> J6
    
    F --> J1
    F --> J3
    
    G --> J1
    G --> J3
    
    H --> J1
    H --> J2
    H --> J3
    H --> J6
    
    I --> J7
    
    J --> K
```

## Enum Values Reference

### UserStatus
- `ACTIVE`: User is active and can sign in
- `DEACTIVATED`: User is temporarily deactivated
- `BLOCKED`: User is blocked due to security issues
- `INSERTED`: User record inserted (data migration status)

### MembershipStatus
- `INVITED`: User has been invited but not yet accepted
- `ACTIVE`: User is an active member of the account
- `SUSPENDED`: User membership is temporarily suspended
- `REMOVED`: User has been removed from the account

### AccountStatus
- `ACTIVE`: Account is active and operational
- `SUSPENDED`: Account is suspended (e.g., payment issues)
- `BLOCKED`: Account is blocked due to policy violations
- `TO_BE_DELETED`: Account is marked for deletion

### Role
- `OWNER`: Full administrative privileges
- `ADMIN`: Administrative privileges (limited)
- `MEMBER`: Basic user privileges (displayed as "USER" in API responses)

### PlanType
- `TRIAL`: Trial account
- `PAID`: Paid subscription account

### OrganizationStatus
- `ACTIVE`: Organization is active
- `SUSPENDED`: Organization is suspended
- `DELETED`: Organization is deleted