# IAM Service Integration Guide for Microservices

## Overview
This guide provides step-by-step instructions for integrating new microservices with the Delight Account Service (IAM service) for authentication, authorization, and user context management.

## Important Architectural Principle

**All server-to-server calls (microservice to IAM service) MUST use the `/channel` context.**

- **Microservices** → **IAM Service**: Always use `/channel/*` endpoints with Channel JWT authentication
- **Client Applications** → **IAM Service**: Can use direct endpoints (`/accounts/*`) with user JWT authentication
- **Client Applications** → **Your Microservice**: Use user JWT tokens (Bearer or cookie)
- **Your Microservice** → **IAM Service**: Use Channel JWT tokens with `/channel/*` endpoints

This ensures proper authentication and authorization separation between client-facing and service-to-service communication.

## Table of Contents
1. [Authentication Flow](#1-authentication-flow)
2. [Session Validation and User Context](#2-session-validation-and-user-context)
3. [Authorization via IAM Service](#3-authorization-via-iam-service)
4. [Channel Request Handling](#4-channel-request-handling)
5. [Account and User Synchronization](#5-account-and-user-synchronization)
6. [API Endpoints Reference](#6-api-endpoints-reference)
7. [Best Practices](#7-best-practices)

---

## 1. Authentication Flow

### 1.1 Request Authentication
Every incoming request to your microservice must be authenticated using one of the following methods:

#### Option A: Bearer Token (JWT) in Authorization Header
```
Authorization: Bearer <jwt_token>
```

#### Option B: Session Cookie (hrsession)
```
Cookie: hrsession=<jwt_token>
```

#### Option C: POST Request Body (for specific endpoints)
```json
{
  "jwtToken": "<jwt_token>"
}
```

### 1.2 Session Validation Flow
For each request, your microservice should:

1. **Extract the user's JWT token** from the incoming request:
   - `Authorization` header (Bearer token)
   - `hrsession` cookie
   - Request body (if POST)

2. **Call IAM Service using `/channel` context** to validate the token and get session information:
   ```
   POST http://iam-service:8080/channel/accounts/session
   Headers:
     Authorization: Bearer <channel_jwt_token>
     Content-Type: application/json
   Body:
     {
       "jwtToken": "<user_jwt_token>"
     }
   ```
   
   **Important**: All server-to-server calls (microservice → IAM service) MUST use the `/channel` context with Channel JWT authentication. The Channel JWT token goes in the Authorization header, and the user's JWT token (if validating a user session) goes in the request body.

3. **Parse the SessionResponse** to extract:
   - `userId` - Current user ID
   - `accountId` - Current account/tenant ID
   - `roleId`, `roleCode`, `roleName` - User's role information
   - `privileges` - List of privilege codes (e.g., ["user.create", "user.read"])
   - `accountDomain` - Account domain/subdomain
   - `isActive` - Session validity status

4. **Set User Context** in your application:
   - Store `userId` and `accountId` in request attributes or thread-local context
   - Use this context for all subsequent operations

### 1.3 Error Handling
- **401 Unauthorized**: Token is invalid, expired, or missing
- **403 Forbidden**: Session is inactive or user lacks required permissions
- **404 Not Found**: Session not found

**Action**: Return 401/403 to client and log the security event.

---

## 2. Session Validation and User Context

### 2.1 Session Validation Endpoint

**Endpoint**: `GET/POST /channel/accounts/session`

**Note**: Microservices should use the channel endpoint (`/channel/accounts/session`) when calling IAM service. This requires a Channel JWT token in the Authorization header. The user's JWT token can be passed in the request body for validation.

**Request Examples**:

```bash
# Using Channel JWT in header and user JWT in body (POST)
curl -X POST http://iam-service:8080/channel/accounts/session \
  -H "Authorization: Bearer <channel_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"jwtToken": "<user_jwt_token>"}'

# Using Channel JWT only (GET) - validates channel session
curl -X GET http://iam-service:8080/channel/accounts/session \
  -H "Authorization: Bearer <channel_jwt_token>"
```

**Note**: The `/accounts/session` endpoint is for **client applications** calling IAM service directly. **Microservices should NEVER use this endpoint** - always use `/channel/accounts/session` for server-to-server calls.

**Response**:
```json
{
  "sessionId": "uuid",
  "userId": 123,
  "accountId": 456,
  "roleId": 789,
  "roleCode": "ORG_ADMIN",
  "roleName": "Organization Admin",
  "emailAddress": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "accountDomain": "acmecorp",
  "companyName": "Acme Corp",
  "expiresAt": "2025-12-09T10:00:00Z",
  "lastAccessedAt": "2025-12-08T19:00:00Z",
  "isActive": true,
  "privileges": ["user.create", "user.read", "user.update", "user.delete"]
}
```

### 2.2 Account Information Endpoint

**Endpoint**: `GET /accounts/info` or `GET /channel/accounts/info`

Use this endpoint to get comprehensive account and organization details.

**Response**:
```json
{
  "id": 456,
  "domain": "acmecorp",
  "plan": "TRIAL",
  "status": "ACTIVE",
  "industryType": "Software",
  "email": "owner@acme.com",
  "companyName": "Acme Corp",
  "currentUserId": 123,
  "currentUserRole": "ORG_ADMIN",
  "organizationId": 789,
  "organizationName": "Acme Corp",
  "organizationPrimaryDomain": "acmecorp",
  "productCode": "CHAT",
  "productName": "CHAT"
}
```

### 2.3 Implementation Example (Spring Boot Filter)

```java
@Component
public class IamAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${iam.service.url:http://iam-service:8080}")
    private String iamServiceUrl;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        // Extract JWT token
        String jwtToken = extractJwtToken(request);
        if (jwtToken == null) {
            sendUnauthorized(response, "Missing authentication token");
            return;
        }
        
        // Validate session with IAM service
        try {
            SessionResponse session = validateSession(jwtToken);
            
            // Set user context
            request.setAttribute("userId", session.getUserId());
            request.setAttribute("accountId", session.getAccountId());
            request.setAttribute("roleCode", session.getRoleCode());
            request.setAttribute("privileges", session.getPrivileges());
            
            // Continue filter chain
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            sendUnauthorized(response, "Invalid or expired session");
        }
    }
    
    private SessionResponse validateSession(String jwtToken) {
        // For microservices, use channel authentication
        // Channel JWT should be configured separately
        String channelJwt = getChannelJwtToken(); // Get from configuration or generate
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + channelJwt);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Pass user JWT in request body
        SessionValidationRequest requestBody = new SessionValidationRequest();
        requestBody.setJwtToken(jwtToken);
        
        HttpEntity<SessionValidationRequest> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<SessionResponse> response = restTemplate.exchange(
            iamServiceUrl + "/channel/accounts/session",
            HttpMethod.POST,
            entity,
            SessionResponse.class
        );
        
        return response.getBody();
    }
    
    private String getChannelJwtToken() {
        // Get channel JWT token from configuration or generate it
        // This should be a valid channel JWT token with appropriate account_id/user_id
        // See Section 4.3 for how to generate channel JWT tokens
        return channelJwtTokenProvider.getToken();
    }
    
    private String extractJwtToken(HttpServletRequest request) {
        // Try Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Try cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("hrsession".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
}
```

---

## 3. Authorization via IAM Service

### 3.1 Overview
Instead of maintaining local Casbin instances in each microservice, all privilege checks should be delegated to the IAM service. This ensures:
- **Centralized authorization logic** - Single source of truth
- **Consistent policy enforcement** - All services use the same rules
- **Simplified microservice architecture** - No need to manage Casbin locally
- **Real-time policy updates** - Changes in IAM service are immediately effective

**Why Authorization Checks Are Needed**:
- **Business Operations**: Check if users have privileges to perform business operations in your microservice (e.g., `conversation.create`, `report.view`, `data.export`)
- **Resource Access Control**: Control access to resources based on user roles and privileges
- **Multi-tenant Isolation**: Ensure users can only access resources within their account/tenant

**What Authorization is NOT For**:
- **User/Account Management**: Microservices should NOT expose APIs to create/update/delete users or accounts
- **User/Account CRUD Operations**: All user/account management is handled by IAM service only
- **Reading User Data**: Microservices can read user/account data from their local DB (synced via Kafka) for business logic, but should NOT expose user management endpoints

### 3.2 Privilege Check Endpoint

**Note**: The IAM service needs to expose an endpoint for privilege checking. Since this is a server-to-server call, it MUST use the `/channel` context.

**Endpoint**: `POST /channel/accounts/authorize/check` or `GET /channel/accounts/authorize/check`

**Authentication**: Channel JWT token in Authorization header (required for all `/channel/*` endpoints)

**Request**:
```json
{
  "userId": 123,
  "accountId": 456,
  "privilegeCode": "user.read"
}
```

**OR** using query parameters (for GET):
```
GET /accounts/authorize/check?userId=123&accountId=456&privilegeCode=user.read
```

**Response**:
```json
{
  "authorized": true,
  "userId": 123,
  "accountId": 456,
  "privilegeCode": "user.read",
  "message": "User has required privilege"
}
```

**Error Response (403 Forbidden)**:
```json
{
  "authorized": false,
  "userId": 123,
  "accountId": 456,
  "privilegeCode": "user.read",
  "message": "User does not have required privilege"
}
```

### 3.3 Implementation in Your Microservice

#### Step 1: Create Authorization Service

```java
@Service
public class AuthorizationService {
    
    private final RestTemplate restTemplate;
    private final String iamServiceUrl;
    private final Cache<String, Boolean> privilegeCache;
    
    @Autowired
    public AuthorizationService(RestTemplate restTemplate,
                               @Value("${iam.service.url}") String iamServiceUrl,
                               CacheManager cacheManager) {
        this.restTemplate = restTemplate;
        this.iamServiceUrl = iamServiceUrl;
        this.privilegeCache = cacheManager.getCache("privilegeCache");
    }
    
    /**
     * Check if user has a specific privilege by calling IAM service.
     * Results are cached for a short duration to reduce IAM service load.
     * 
     * @param userId The user ID
     * @param accountId The account ID
     * @param privilegeCode The privilege code (e.g., "user.create", "user.read")
     * @return true if authorized, false otherwise
     */
    public boolean hasPrivilege(Long userId, Long accountId, String privilegeCode) {
        // Check cache first
        String cacheKey = userId + ":" + accountId + ":" + privilegeCode;
        Boolean cached = privilegeCache != null ? privilegeCache.get(cacheKey, Boolean.class) : null;
        if (cached != null) {
            return cached;
        }
        
        // Call IAM service
        try {
            boolean authorized = checkPrivilegeWithIam(userId, accountId, privilegeCode);
            
            // Cache the result (TTL: 30 seconds)
            if (privilegeCache != null) {
                privilegeCache.put(cacheKey, authorized);
            }
            
            return authorized;
        } catch (Exception e) {
            logger.error("Error checking privilege with IAM service: {}", e.getMessage(), e);
            // Fail closed - deny access on error
            return false;
        }
    }
    
    /**
     * Check permission by calling IAM service.
     * 
     * @param userId The user ID
     * @param accountId The account ID
     * @param resource The resource (e.g., "user", "account")
     * @param action The action (e.g., "create", "read", "update", "delete")
     * @return true if authorized, false otherwise
     */
    public boolean hasPermission(Long userId, Long accountId, String resource, String action) {
        String privilegeCode = resource + "." + action;
        return hasPrivilege(userId, accountId, privilegeCode);
    }
    
    private boolean checkPrivilegeWithIam(Long userId, Long accountId, String privilegeCode) {
        // Build request
        PrivilegeCheckRequest request = new PrivilegeCheckRequest();
        request.setUserId(userId);
        request.setAccountId(accountId);
        request.setPrivilegeCode(privilegeCode);
        
        // Get Channel JWT token for server-to-server authentication
        String channelJwt = getChannelJwtToken();
        
        // Call IAM service using /channel context (required for server-to-server calls)
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + channelJwt);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PrivilegeCheckRequest> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<PrivilegeCheckResponse> response = restTemplate.exchange(
                iamServiceUrl + "/channel/accounts/authorize/check",
                HttpMethod.POST,
                entity,
                PrivilegeCheckResponse.class
            );
            
            return response.getBody() != null && response.getBody().isAuthorized();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                return false;
            }
            throw e;
        }
    }
    
    private String getChannelJwtToken() {
        // Get channel JWT token from configuration or generate it
        // This should be a valid channel JWT token with appropriate account_id/user_id
        // See Section 4.3 for how to generate channel JWT tokens
        return channelJwtTokenProvider.getToken();
    }
    
    /**
     * Invalidate privilege cache for a user/account.
     * Call this when you know permissions have changed.
     */
    public void invalidateCache(Long userId, Long accountId) {
        if (privilegeCache != null) {
            // Remove all entries for this user/account
            // Implementation depends on cache library
        }
    }
}
```

#### Step 2: Request/Response DTOs

```java
@Data
public class PrivilegeCheckRequest {
    private Long userId;
    private Long accountId;
    private String privilegeCode;
}

@Data
public class PrivilegeCheckResponse {
    private boolean authorized;
    private Long userId;
    private Long accountId;
    private String privilegeCode;
    private String message;
}
```

#### Step 3: Use Authorization in Controllers

**Why Authorization Checks Are Needed**:
Authorization checks ensure that users can only perform operations they have privileges for. This is essential for:
- **Security**: Prevent unauthorized access to resources
- **Multi-tenant Isolation**: Ensure users can only access data within their account
- **Role-Based Access Control**: Enforce business rules based on user roles (e.g., only managers can view reports)

**Important**: Microservices should NOT expose APIs to create/update/delete users or accounts. All user/account management is handled by the IAM service. However, microservices DO need to:
- **Read** user/account data from their local database for business logic
- **Check privileges** before performing business operations on behalf of users

**Example: Reading User Data for Business Logic**

```java
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    
    @Autowired
    private AuthorizationService authorizationService;
    
    @Autowired
    private UserRepository userRepository; // Local user data (synced from IAM via Kafka)
    
    /**
     * Get conversations for a user.
     * This is a business operation that requires user data and privilege check.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Conversation>> getUserConversations(
            @PathVariable Long userId,
            HttpServletRequest request) {
        
        Long actorUserId = (Long) request.getAttribute("userId");
        Long accountId = (Long) request.getAttribute("accountId");
        
        // Check privilege: user needs to be able to read conversations
        // OR if viewing own conversations, allow it
        if (!userId.equals(actorUserId) && 
            !authorizationService.hasPrivilege(actorUserId, accountId, "conversation.read")) {
            throw new ForbiddenException("Insufficient permissions to view conversations");
        }
        
        // Read user from local database (synced from IAM)
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
        
        // Perform business logic
        List<Conversation> conversations = conversationService.getConversationsForUser(userId);
        return ResponseEntity.ok(conversations);
    }
    
    /**
     * Create a conversation (business operation, not user management).
     */
    @PostMapping
    public ResponseEntity<Conversation> createConversation(
            @RequestBody CreateConversationRequest request,
            HttpServletRequest httpRequest) {
        
        Long actorUserId = (Long) httpRequest.getAttribute("userId");
        Long accountId = (Long) httpRequest.getAttribute("accountId");
        
        // Check privilege: user needs to be able to create conversations
        if (!authorizationService.hasPrivilege(actorUserId, accountId, "conversation.create")) {
            throw new ForbiddenException("Insufficient permissions to create conversations");
        }
        
        // Read user from local database
        User user = userRepository.findById(actorUserId)
            .orElseThrow(() -> new NotFoundException("User not found"));
        
        // Perform business logic (create conversation)
        Conversation conversation = conversationService.createConversation(request, user);
        return ResponseEntity.ok(conversation);
    }
}
```

**Key Points**:
- **Authorization checks** are needed to ensure users have the right privileges for business operations
- **Reading user/account data** from local DB is allowed (for business logic)
- **Creating/updating/deleting users/accounts** is NOT allowed - use IAM service for that
- **Privilege codes** should match your business domain (e.g., `conversation.read`, `conversation.create`, not `user.create`)

### 3.4 Caching Strategy

**Recommendation**: Cache privilege check results for 30-60 seconds to:
- Reduce load on IAM service
- Improve response times
- Handle IAM service unavailability gracefully

**Cache Key Format**: `{userId}:{accountId}:{privilegeCode}`

**Cache Invalidation**: 
- Invalidate cache when membership/role changes are detected
- Consider subscribing to IAM service events for real-time invalidation
- Or implement a cache TTL (time-to-live) of 30-60 seconds

### 3.5 Error Handling

- **IAM Service Unavailable**: 
  - Option 1: Fail closed (deny access) - Most secure
  - Option 2: Use cached results if available
  - Option 3: Implement circuit breaker pattern
  
- **Invalid Response**: Log error and fail closed (deny access)

- **Timeout**: Set reasonable timeout (e.g., 2 seconds) and fail closed on timeout

---

## 4. Channel Request Handling

### 4.1 Channel Authentication Overview
Channel requests are authenticated using Channel JWT tokens (different from user JWT tokens). These requests bypass privilege checks and only require JWT validation. Channel authentication is used for service-to-service communication.

### 4.2 Channel JWT Token Structure

Channel JWT tokens are built with the following claims:

**Required Claims**:
- `key_version` (string, required) - Key version identifier (e.g., "v1", "v2")
- `iat` (number, required) - Issued at timestamp (Unix epoch)
- `exp` (number, required) - Expiration timestamp (Unix epoch)

**Optional Claims**:
- `account_id` (string, optional) - Account ID. If not provided, account will be resolved from domain/subdomain
- `user_id` (string, optional) - User ID. If not provided, user will be resolved from request body JWT if available

**Token Format Example**:
```json
{
  "key_version": "v1",
  "account_id": "12345",
  "user_id": "67890",
  "iat": 1702051200,
  "exp": 1702054800
}
```

### 4.3 Building Channel JWT Tokens

Channel JWT tokens are signed using **HMAC-SHA256** with versioned shared secret keys. The secret keys are plain strings (not base64-encoded).

#### Python Example (for generating tokens):

```python
import jwt
from datetime import datetime, timedelta

def create_channel_jwt(account_id=None, user_id=None, key_version="v1", secret_key=None):
    """
    Create a channel JWT token.
    
    Args:
        account_id (str, optional): Account ID
        user_id (str, optional): User ID
        key_version (str): Key version (v1, v2, etc.)
        secret_key (str): Plain secret key (not base64-encoded)
    
    Returns:
        str: JWT token
    """
    # Default keys - these should match IAM service configuration
    # Note: Keys are plain strings, not base64-encoded
    default_keys = {
        "v1": "your-secret-key-v1",
        "v2": "your-secret-key-v2"
    }
    
    if secret_key is None:
        secret_key = default_keys.get(key_version)
        if secret_key is None:
            raise ValueError(f"Unknown key version: {key_version}")
    
    # Use secret key directly (no base64 decoding needed)
    # The secret key is a plain string - use it directly with JWT library
    
    # Create payload
    now = datetime.utcnow()
    payload = {
        "key_version": key_version,
        "iat": int(now.timestamp()),
        "exp": int((now + timedelta(hours=1)).timestamp())  # 1 hour expiry
    }
    
    # Add optional claims
    if account_id is not None:
        payload["account_id"] = str(account_id)
    
    if user_id is not None:
        payload["user_id"] = str(user_id)
    
    # Sign token - use secret key directly
    token = jwt.encode(payload, secret_key, algorithm="HS256")
    return token
```

#### Java Example:

```java
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;

public class ChannelJwtBuilder {
    
    public String createChannelJwt(String accountId, String userId, 
                                   String keyVersion, String secretKey) {
        // Use secret key directly (no base64 decoding needed)
        // The secret key is a plain string - convert to bytes for HMAC-SHA256
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        
        // Build claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("key_version", keyVersion);
        if (accountId != null) {
            claims.put("account_id", accountId);
        }
        if (userId != null) {
            claims.put("user_id", userId);
        }
        
        // Build token
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 3600000); // 1 hour
        
        return Jwts.builder()
            .claims(claims)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact();
    }
}
```

### 4.4 Channel Authentication in IAM Service

The IAM service validates channel JWT tokens as follows:

1. **Token Extraction**: Extracts token from `Authorization: Bearer <token>` header
2. **Key Version Detection**: Reads `key_version` claim from token
3. **Key Lookup**: Looks up the corresponding secret key for the version
4. **Token Validation**: 
   - Verifies signature using the secret key
   - Checks expiration (`exp` claim)
   - Validates `key_version` matches
5. **Account Resolution**:
   - If `account_id` is in token, use it
   - If not, extract domain from request and look up account
   - If still not found, return 401 Unauthorized
6. **User Resolution**:
   - If `user_id` is in token, use it
   - If not, try to extract from request body JWT (for nested requests)
   - If not found, `userId` remains null (channel requests don't always need user context)

**Important for Microservices**: When your microservice receives account_id or user_id (from channel JWT or session validation), you should check if they exist in your local database. If not found, call IAM service endpoints to fetch and upsert:
- **For Account**: Call `/channel/accounts/info` with accountId to get account details and upsert in your DB
- **For User**: Call `/channel/accounts/members` to get user details and upsert in your DB

See Section 5.5 for detailed implementation of on-demand synchronization.
7. **Context Setting**: Sets request attributes:
   - `accountId` - Resolved account ID
   - `userId` - Resolved user ID (if available)
   - `channelAuth` - Set to `true`
   - `keyVersion` - Key version used

### 4.5 Channel Request Flow in Your Microservice

1. **Detect Channel Request**: Check if request URI starts with `/channel/`

2. **Extract Channel JWT Token**:
   ```
   Authorization: Bearer <channel_jwt_token>
   ```

3. **Validate Token with IAM Service**:
   ```
   POST /channel/validateSession
   Headers:
     Authorization: Bearer <channel_jwt_token>
   ```

4. **IAM Service Response**:
   ```json
   {
     "accountId": 12345,
     "userId": 67890,
     "isActive": true,
     "sessionId": "channel-12345-1702051200"
   }
   ```

5. **Set Request Context**:
   - `accountId` - From IAM response
   - `userId` - From IAM response (may be null)
   - `channelAuth` - Set to `true`

6. **Skip Privilege Checks**: For channel requests, authorization service should return `true` without calling IAM service

### 4.6 Implementation Example

```java
@Component
public class ChannelAuthFilter extends OncePerRequestFilter {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${iam.service.url}")
    private String iamServiceUrl;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        
        // Check if this is a channel request
        if (requestURI.startsWith("/channel/")) {
            // Extract Channel JWT token
            String channelToken = extractBearerToken(request);
            if (channelToken == null) {
                sendUnauthorized(response, "Missing channel token");
                return;
            }
            
            // Validate channel token with IAM service
            try {
                ChannelSessionResponse sessionResponse = validateChannelToken(channelToken);
                
                // Set channel-specific attributes
                request.setAttribute("accountId", sessionResponse.getAccountId());
                if (sessionResponse.getUserId() != null) {
                    request.setAttribute("userId", sessionResponse.getUserId());
                }
                request.setAttribute("channelAuth", true);
                
                filterChain.doFilter(request, response);
            } catch (Exception e) {
                logger.error("Channel token validation failed: {}", e.getMessage());
                sendUnauthorized(response, "Invalid channel token");
            }
        } else {
            // Regular user authentication
            filterChain.doFilter(request, response);
        }
    }
    
    private ChannelSessionResponse validateChannelToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<ChannelSessionResponse> response = restTemplate.exchange(
            iamServiceUrl + "/channel/validateSession",
            HttpMethod.POST,
            entity,
            ChannelSessionResponse.class
        );
        
        return response.getBody();
    }
    
    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
```

### 4.7 Authorization Service - Skip Checks for Channel Requests

```java
@Service
public class AuthorizationService {
    
    public boolean hasPrivilege(Long userId, Long accountId, String privilegeCode) {
        // Check if this is a channel request
        if (isChannelRequest()) {
            // Skip privilege checks for channel requests
            logger.debug("Skipping privilege check for channel request: privilege={}", privilegeCode);
            return true;
        }
        
        // Normal privilege check via IAM service
        return checkPrivilegeWithIam(userId, accountId, privilegeCode);
    }
    
    private boolean isChannelRequest() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) 
                RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attr.getRequest();
            
            // Check URI
            if (request.getRequestURI().startsWith("/channel/")) {
                return true;
            }
            
            // Check attribute (set by ChannelAuthFilter)
            Object channelAuth = request.getAttribute("channelAuth");
            return Boolean.TRUE.equals(channelAuth);
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 4.8 Channel JWT Configuration

**IAM Service Configuration** (`application.properties`):
```properties
# Channel Authentication Keys (plain strings, not base64-encoded)
channel.auth.keys.v1=your-secret-key-v1
channel.auth.keys.v2=your-secret-key-v2
```

**Your Microservice Configuration**:
```properties
# Channel JWT Secret Keys (must match IAM service)
# Note: Keys are plain strings, not base64-encoded - use them directly
channel.jwt.secret.v1=your-secret-key-v1
channel.jwt.secret.v2=your-secret-key-v2
```

**Security Note**: 
- Keep secret keys secure and never commit them to version control
- Use environment variables or secure key management services
- Rotate keys periodically by introducing new versions

---

## 5. Account and User Synchronization

### 5.1 Overview

Account and user data synchronization between IAM service and microservices happens through **asynchronous Kafka messaging**. Microservices should **NOT** implement management APIs for accounts or users - all account/user management is handled by the IAM service.

**Key Principles**:
- **IAM Service** publishes account/user events to Kafka topics
- **Microservices** subscribe to Kafka topics to receive account/user updates
- **Microservices** maintain local copies of account/user data in their databases
- **On-demand sync**: If a microservice can't find account/user in their DB, they can call IAM service to fetch and upsert the data

### 5.2 Kafka Event Topics

The IAM service publishes events to the following Kafka topics:

#### Account Events
- **Topic**: `account_activities_v1`
- **Event Types**:
  - `account.created` - New account created
  - `account.updated` - Account information updated
  - `account.deleted` - Account deleted

#### User Events (User Table Changes Only)
- **Topic**: `user_activities_v1`
- **Event Types**:
  - `user.created` - New user created
  - `user.updated` - User information updated (name, email, status, etc.)
  - `user.deleted` - User deleted

**Important**: This topic contains **only** changes to the `users` table. Membership-related events are published to a separate topic.

#### Membership Events (Membership and Role Changes)
- **Topic**: `user_role_activities_v1`
- **Event Types**:
  - `user.membership.added` - User added to an account (membership created)
  - `user.membership.updated` - User membership updated (status change, etc.)
  - `user.membership.removed` - User removed from an account (membership deleted)
  - `user.role.changed` - User's role changed within an account

**Important**: This topic contains all membership-related events, including role changes. All membership events go to this topic, not to `user_activities_v1`.

#### Topic Summary

| Topic | Purpose | Event Types |
|-------|---------|-------------|
| `account_activities_v1` | Account lifecycle events | `account.created`, `account.updated`, `account.deleted` |
| `user_activities_v1` | User table changes only | `user.created`, `user.updated`, `user.deleted` |
| `user_role_activities_v1` | Membership and role changes | `user.membership.added`, `user.membership.updated`, `user.membership.removed`, `user.role.changed` |

### 5.3 Event Message Format

All event messages follow a consistent structure with the following fields:
- `eventType` - The type of event (e.g., "account.created", "user.updated")
- `eventId` - Unique identifier for this event (UUID)
- `timestamp` - Event timestamp (ISO 8601 format with nanosecond precision)
- `accountId` - Account ID (present in all events)
- `userId` - User ID (present in user and membership events)
- `data` - Event payload containing the entity data

**Important**: All payloads include an `updated_at` field with nanosecond precision, which serves as the version identifier for the payload.

#### Account Event Example
```json
{
  "eventType": "account.updated",
  "eventId": "a1aef04d-7a54-44af-ad07-0731e1679843",
  "timestamp": "2025-12-08T20:00:00.123456789Z",
  "accountId": 5764607523034234882,
  "data": {
    "id": 5764607523034234882,
    "domain": "acmecorp",
    "companyName": "Acme Corp",
    "status": "ACTIVE",
    "plan": "TRIAL",
    "industryType": "Software",
    "region": "us-east-1",
    "organizationId": 123,
    "organizationName": "Acme Organization",
    "organizationPrimaryDomain": "acme.com",
    "productCode": "PRODUCT_CODE",
    "productName": "Product Name",
    "updated_at": "2025-12-08T20:00:00.123456789+05:30"
  }
}
```

#### User Event Example (from user_activities_v1 topic)
```json
{
  "eventType": "user.updated",
  "eventId": "b2bff15e-8b65-55bg-be18-1842f2780954",
  "timestamp": "2025-12-08T20:00:00.123456789Z",
  "userId": 6789012345678901234,
  "accountId": 5764607523034234882,
  "data": {
    "id": 6789012345678901234,
    "userName": "jdoe",
    "emailAddress": "user@acme.com",
    "firstName": "John",
    "lastName": "Doe",
    "companyName": "Acme Corp",
    "phoneNumber": "+1234567890",
    "status": "ACTIVE",
    "accountId": 5764607523034234882,
    "updated_at": "2025-12-08T20:00:00.123456789+05:30"
  }
}
```

#### Membership Event Example (from user_role_activities_v1 topic)
```json
{
  "eventType": "user.membership.updated",
  "eventId": "c3cgg26f-9c76-66ch-cf29-2953g3891065",
  "timestamp": "2025-12-08T20:00:00.123456789Z",
  "userId": 6789012345678901234,
  "accountId": 5764607523034234882,
  "data": {
    "id": 3456789012345678901,
    "userId": 6789012345678901234,
    "accountId": 5764607523034234882,
    "status": "ACTIVE",
    "roleId": 1,
    "roleName": "Org Admin",
    "invitedBy": 9876543210987654321,
    "created_at": "2025-12-08T09:12:57Z",
    "updated_at": "2025-12-08T22:42:42.393719+05:30"
  }
}
```

#### Role Change Event Example (from user_role_activities_v1 topic)
```json
{
  "eventType": "user.role.changed",
  "eventId": "d4dhh37g-0d87-77di-dg3a-3a64h4902176",
  "timestamp": "2025-12-08T20:00:00.123456789Z",
  "userId": 6789012345678901234,
  "accountId": 5764607523034234882,
  "data": {
    "oldRoleId": 2,
    "oldRoleName": "Member",
    "newRoleId": 1,
    "newRoleName": "Org Admin",
    "roleId": 1,
    "roleName": "Org Admin",
    "membershipId": 3456789012345678901,
    "status": "ACTIVE",
    "updated_at": "2025-12-08T22:42:42.393719+05:30"
  }
}
```

#### Key Field Notes

**Account Events (`account_activities_v1`)**:
- `data.id` = Account ID (primary key)
- `data.organizationId` = Organization ID (foreign key, nullable)
- `data.productCode` = Product code (from product relationship, nullable)
- `data.updated_at` = Version identifier (nanosecond precision)

**User Events (`user_activities_v1`)**:
- `data.id` = User ID (primary key)
- `data.accountId` = Account ID (foreign key, from user.account relationship)
- `data.updated_at` = Version identifier (nanosecond precision)

**Membership Events (`user_role_activities_v1`)**:
- `data.id` = Membership ID (primary key of memberships table)
- `data.userId` = User ID (foreign key, from membership.user relationship)
- `data.accountId` = Account ID (foreign key, from membership.account relationship)
- `data.roleId` = Role ID (foreign key, from membership.role relationship)
- `data.roleName` = Role name (from membership.role relationship)
- `data.updated_at` = Version identifier (nanosecond precision)

**Important**: 
- The `id` field in membership events is the **membership ID**, not the user ID
- The `userId` and `accountId` in membership events are foreign keys, not primary keys
- All timestamps use nanosecond precision for versioning

### 5.4 Implementing Kafka Consumer in Your Microservice

#### Step 1: Add Dependencies

```gradle
dependencies {
    implementation 'org.springframework.kafka:spring-kafka'
    // Or use your preferred Kafka client library
}
```

#### Step 2: Configure Kafka Consumer

```properties
# application.properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=your-microservice-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer

# Topic names (configure based on IAM service)
kafka.topics.account-events=account_activities_v1
kafka.topics.user-events=user_activities_v1
kafka.topics.user-role-events=user_role_activities_v1
```

#### Step 3: Implement Event Consumers

```java
@Component
@Slf4j
public class AccountEventConsumer {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @KafkaListener(topics = "${kafka.topics.account-events}")
    public void handleAccountEvent(AccountEventMessage event) {
        log.info("Received account event: type={}, accountId={}", 
                event.getEventType(), event.getAccountId());
        
        try {
            switch (event.getEventType()) {
                case "account.created":
                case "account.updated":
                    upsertAccount(event.getData());
                    break;
                case "account.deleted":
                    deleteAccount(event.getAccountId());
                    break;
                default:
                    log.warn("Unknown account event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing account event: {}", e.getMessage(), e);
            // Implement retry logic or dead letter queue handling
            throw e;
        }
    }
    
    private void upsertAccount(AccountData accountData) {
        Optional<Account> existing = accountRepository.findById(accountData.getId());
        
        if (existing.isPresent()) {
            Account account = existing.get();
            account.setDomain(accountData.getDomain());
            account.setCompanyName(accountData.getCompanyName());
            account.setStatus(accountData.getStatus());
            account.setPlan(accountData.getPlan());
            account.setIndustryType(accountData.getIndustryType());
            account.setEmail(accountData.getEmail());
            account.setUpdatedAt(OffsetDateTime.now());
            accountRepository.save(account);
            log.info("Updated account: {}", accountData.getId());
        } else {
            Account account = new Account();
            account.setId(accountData.getId());
            account.setDomain(accountData.getDomain());
            account.setCompanyName(accountData.getCompanyName());
            account.setStatus(accountData.getStatus());
            account.setPlan(accountData.getPlan());
            account.setIndustryType(accountData.getIndustryType());
            account.setEmail(accountData.getEmail());
            account.setCreatedAt(OffsetDateTime.now());
            account.setUpdatedAt(OffsetDateTime.now());
            accountRepository.save(account);
            log.info("Created account: {}", accountData.getId());
        }
    }
    
    private void deleteAccount(Long accountId) {
        accountRepository.deleteById(accountId);
        log.info("Deleted account: {}", accountId);
    }
    
    private void updateAccountStatus(Long accountId, String status) {
        accountRepository.findById(accountId).ifPresent(account -> {
            account.setStatus(status);
            account.setUpdatedAt(OffsetDateTime.now());
            accountRepository.save(account);
            log.info("Updated account status: accountId={}, status={}", accountId, status);
        });
    }
}
```

```java
@Component
@Slf4j
public class UserEventConsumer {
    
    @Autowired
    private UserRepository userRepository;
    
    @KafkaListener(topics = "${kafka.topics.user-events}")
    public void handleUserEvent(UserEventMessage event) {
        log.info("Received user event: type={}, userId={}, accountId={}", 
                event.getEventType(), event.getUserId(), event.getAccountId());
        
        try {
            switch (event.getEventType()) {
                case "user.created":
                case "user.updated":
                    upsertUser(event.getData());
                    break;
                case "user.deleted":
                    deleteUser(event.getUserId());
                    break;
                default:
                    log.warn("Unknown user event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing user event: {}", e.getMessage(), e);
            // Implement retry logic or dead letter queue handling
            throw e;
        }
    }
    
    private void upsertUser(Map<String, Object> userData) {
        // Extract user fields from userData and upsert in local database
        // Note: userData.id is the user ID, userData.accountId is the account ID
    }
    
    private void deleteUser(Long userId) {
        // Delete user from local database
    }
}

@Component
@Slf4j
public class MembershipEventConsumer {
    
    @Autowired
    private MembershipRepository membershipRepository;
    
    @KafkaListener(topics = "${kafka.topics.user-role-events}")
    public void handleMembershipEvent(MembershipEventMessage event) {
        log.info("Received membership event: type={}, userId={}, accountId={}", 
                event.getEventType(), event.getUserId(), event.getAccountId());
        
        try {
            switch (event.getEventType()) {
                case "user.membership.added":
                case "user.membership.updated":
                    upsertMembership(event.getData());
                    break;
                case "user.membership.removed":
                    // data.id is the membership ID, not user ID
                    Long membershipId = Long.valueOf(event.getData().get("id").toString());
                    removeMembership(membershipId, event.getUserId(), event.getAccountId());
                    break;
                case "user.role.changed":
                    updateUserRole(event.getUserId(), event.getAccountId(), event.getData());
                    break;
                default:
                    log.warn("Unknown membership event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing membership event: {}", e.getMessage(), e);
            // Implement retry logic or dead letter queue handling
            throw e;
        }
    }
    
    private void upsertMembership(Map<String, Object> membershipData) {
        // Extract membership fields from membershipData and upsert in local database
        // Note: membershipData.id is the membership ID (primary key)
        //       membershipData.userId is the user ID (foreign key)
        //       membershipData.accountId is the account ID (foreign key)
    }
    
    private void removeMembership(Long membershipId, Long userId, Long accountId) {
        // Remove membership record from local database
    }
    
    private void updateUserRole(Long userId, Long accountId, Map<String, Object> roleChangeData) {
        // Update user's role in local database
        // roleChangeData contains: oldRoleId, newRoleId, oldRoleName, newRoleName
    }
    
    private void upsertUser(UserData userData) {
        Optional<User> existing = userRepository.findById(userData.getId());
        
        if (existing.isPresent()) {
            User user = existing.get();
            user.setEmailAddress(userData.getEmailAddress());
            user.setFirstName(userData.getFirstName());
            user.setLastName(userData.getLastName());
            user.setStatus(userData.getStatus());
            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);
            log.info("Updated user: {}", userData.getId());
        } else {
            User user = new User();
            user.setId(userData.getId());
            user.setEmailAddress(userData.getEmailAddress());
            user.setFirstName(userData.getFirstName());
            user.setLastName(userData.getLastName());
            user.setStatus(userData.getStatus());
            user.setAccountId(userData.getAccountId());
            user.setCreatedAt(OffsetDateTime.now());
            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);
            log.info("Created user: {}", userData.getId());
        }
    }
    
    // Similar methods for delete, status update, membership operations...
}
```

### 5.5 On-Demand Synchronization (Fallback)

**Critical Pattern**: Whenever your microservice receives `account_id` or `user_id` (from channel JWT tokens, session validation, or any IAM service response), you should:

1. **Check local database** first
2. **If not found**, call IAM service to fetch the data
3. **Upsert** (insert if new, update if exists) in your local database

This ensures your microservice always has the latest account and user data, even if:
- Kafka events were missed
- Service was restarted
- New accounts/users were created before your service subscribed to events
- Database was restored from backup

**When to trigger on-demand sync**:
- After session validation (when you get accountId/userId from session response)
- After channel authentication (when you get accountId/userId from channel JWT)
- Before processing business logic that requires account/user data
- When account/user is not found in local database during any operation

#### Fetching Account Information

```java
@Service
public class AccountSyncService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Value("${iam.service.url}")
    private String iamServiceUrl;
    
    /**
     * Fetch account from IAM service and upsert in local database.
     * Use this when account is not found locally.
     * 
     * @param accountId The account ID to fetch
     * @return The account entity (from local DB after upsert)
     */
    public Account fetchAndUpsertAccount(Long accountId) {
        log.info("Fetching account {} from IAM service", accountId);
        
        // Get Channel JWT token (required for /channel/* endpoints)
        String channelJwt = getChannelJwtToken();
        
        // Call IAM service using /channel context
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + channelJwt);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        // Note: You may need to pass accountId as query parameter or in Channel JWT
        // Adjust the endpoint based on your IAM service implementation
        ResponseEntity<AccountInfoResponse> response = restTemplate.exchange(
            iamServiceUrl + "/channel/accounts/info?accountId=" + accountId,
            HttpMethod.GET,
            entity,
            AccountInfoResponse.class
        );
        
        AccountInfoResponse accountInfo = response.getBody();
        if (accountInfo == null) {
            throw new RuntimeException("Account not found in IAM service: " + accountId);
        }
        
        // Upsert in local database
        Account account = upsertAccount(accountInfo);
        log.info("Successfully synced account {} to local database", accountId);
        return account;
    }
    
    /**
     * Upsert account in local database.
     * Inserts if new, updates if exists.
     */
    private Account upsertAccount(AccountInfoResponse accountInfo) {
        Optional<Account> existing = accountRepository.findById(accountInfo.getId());
        
        Account account = existing.orElse(new Account());
        account.setId(accountInfo.getId());
        account.setDomain(accountInfo.getDomain());
        account.setCompanyName(accountInfo.getCompanyName());
        account.setStatus(accountInfo.getStatus());
        account.setPlan(accountInfo.getPlan());
        account.setIndustryType(accountInfo.getIndustryType());
        account.setEmail(accountInfo.getEmail());
        
        if (existing.isEmpty()) {
            account.setCreatedAt(OffsetDateTime.now());
            log.debug("Inserting new account: {}", accountInfo.getId());
        } else {
            log.debug("Updating existing account: {}", accountInfo.getId());
        }
        account.setUpdatedAt(OffsetDateTime.now());
        
        return accountRepository.save(account);
    }
    
    /**
     * Get or fetch account - checks local DB first, fetches from IAM if not found.
     * This is the recommended pattern to use in your business logic.
     */
    public Account getOrFetchAccount(Long accountId) {
        return accountRepository.findById(accountId)
            .orElseGet(() -> fetchAndUpsertAccount(accountId));
    }
}
```

#### Fetching User Information

```java
@Service
public class UserSyncService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Value("${iam.service.url}")
    private String iamServiceUrl;
    
    /**
     * Fetch user from IAM service and upsert in local database.
     * Use this when user is not found locally.
     * 
     * @param userId The user ID to fetch
     * @param accountId The account ID (required to call /channel/accounts/members)
     * @return The user entity (from local DB after upsert)
     */
    public User fetchAndUpsertUser(Long userId, Long accountId) {
        log.info("Fetching user {} from IAM service for account {}", userId, accountId);
        
        // Get Channel JWT token (required for /channel/* endpoints)
        String channelJwt = getChannelJwtToken();
        
        // Call IAM service using /channel context
        // Get all members for the account and find the specific user
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + channelJwt);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        // Call /channel/accounts/members to get all members
        ResponseEntity<List<MemberResponse>> response = restTemplate.exchange(
            iamServiceUrl + "/channel/accounts/members?accountId=" + accountId,
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<List<MemberResponse>>() {}
        );
        
        // Find the specific user in the members list
        MemberResponse member = response.getBody().stream()
            .filter(m -> m.getUserId().equals(userId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                "User " + userId + " not found in account " + accountId));
        
        // Upsert in local database
        User user = upsertUser(member, accountId);
        log.info("Successfully synced user {} to local database", userId);
        return user;
    }
    
    /**
     * Upsert user in local database.
     * Inserts if new, updates if exists.
     */
    private User upsertUser(MemberResponse member, Long accountId) {
        Optional<User> existing = userRepository.findById(member.getUserId());
        
        User user = existing.orElse(new User());
        user.setId(member.getUserId());
        user.setEmailAddress(member.getEmailAddress());
        user.setFirstName(member.getFirstName());
        user.setLastName(member.getLastName());
        user.setAccountId(accountId);
        // Add any other fields from MemberResponse that you need
        
        if (existing.isEmpty()) {
            user.setCreatedAt(OffsetDateTime.now());
            log.debug("Inserting new user: {}", member.getUserId());
        } else {
            log.debug("Updating existing user: {}", member.getUserId());
        }
        user.setUpdatedAt(OffsetDateTime.now());
        
        return userRepository.save(user);
    }
    
    /**
     * Get or fetch user - checks local DB first, fetches from IAM if not found.
     * This is the recommended pattern to use in your business logic.
     */
    public User getOrFetchUser(Long userId, Long accountId) {
        return userRepository.findById(userId)
            .orElseGet(() -> fetchAndUpsertUser(userId, accountId));
    }
}
```

### 5.6 Usage Pattern in Your Microservice

**Recommended Pattern**: Always use the `getOrFetch` methods which automatically handle the check-and-fetch logic.

```java
@Service
public class YourBusinessService {
    
    @Autowired
    private AccountSyncService accountSyncService;
    
    @Autowired
    private UserSyncService userSyncService;
    
    /**
     * Example: After session validation, ensure account and user exist locally.
     */
    public void processBusinessLogic(Long accountId, Long userId) {
        // Get or fetch account - automatically fetches from IAM if not in local DB
        Account account = accountSyncService.getOrFetchAccount(accountId);
        
        // Get or fetch user - automatically fetches from IAM if not in local DB
        User user = userSyncService.getOrFetchUser(userId, accountId);
        
        // Proceed with business logic using account and user
        // ...
    }
    
    /**
     * Example: In authentication filter after session validation.
     */
    public void afterSessionValidation(Long accountId, Long userId) {
        // Always ensure account and user exist in local DB
        // This handles cases where Kafka events were missed or service was restarted
        accountSyncService.getOrFetchAccount(accountId);
        if (userId != null) {
            userSyncService.getOrFetchUser(userId, accountId);
        }
    }
    
    /**
     * Example: After channel authentication when you get accountId from Channel JWT.
     */
    public void afterChannelAuth(Long accountId, Long userId) {
        // Sync account and user to local DB
        accountSyncService.getOrFetchAccount(accountId);
        if (userId != null) {
            userSyncService.getOrFetchUser(userId, accountId);
        }
    }
}
```

**Key Points**:
- Always call `getOrFetchAccount()` and `getOrFetchUser()` after receiving accountId/userId from IAM service
- This ensures your local database is always up-to-date
- The methods are idempotent - safe to call multiple times
- They only fetch from IAM service if data is missing locally

### 5.7 Best Practices

1. **Primary Sync Method**: Always rely on Kafka events for synchronization
2. **On-Demand Sync Pattern**: 
   - **Always** call `getOrFetchAccount()` and `getOrFetchUser()` after receiving accountId/userId from IAM service
   - This includes: after session validation, after channel authentication, before business logic
   - Use on-demand sync as a safety net, not as primary sync method
3. **Idempotency**: Ensure your upsert operations are idempotent (handle duplicate events gracefully)
4. **Error Handling**: 
   - Implement retry logic and dead letter queues for failed event processing
   - Handle IAM service unavailability gracefully (log and continue if possible)
5. **Event Ordering**: Consider event ordering if your business logic depends on it
6. **Performance**: 
   - Cache frequently accessed account/user data to reduce database queries
   - Use `getOrFetch` methods which check local DB first before calling IAM service
7. **Monitoring**: 
   - Monitor Kafka consumer lag and event processing times
   - Track on-demand sync calls (should be minimal if Kafka is working properly)
   - Alert if on-demand sync calls spike (indicates Kafka issues)
8. **Data Consistency**: Periodically validate local data against IAM service (optional health check)
9. **Always Use `/channel` Context**: All IAM service calls for fetching account/user data must use `/channel/*` endpoints with Channel JWT

### 5.8 Important Notes

- **No Management APIs**: Microservices should NOT expose APIs to create/update/delete accounts or users
- **Read-Only Local Copies**: Local account/user data is read-only from the microservice's perspective
- **IAM Service is Source of Truth**: IAM service is the authoritative source for all account/user data
- **Eventual Consistency**: Local data may be slightly out of sync due to async processing - design your system to handle this
- **On-Demand Sync is Mandatory**: Whenever you receive accountId or userId (from session, channel JWT, or any IAM response), you MUST check local DB and fetch from IAM if missing
- **Use `/channel` Endpoints**: All on-demand sync calls must use `/channel/accounts/info` and `/channel/accounts/members` with Channel JWT authentication
- **Upsert Pattern**: Always use upsert (insert if new, update if exists) - never just insert, as data might exist from Kafka events

---

## 6. API Endpoints Reference

### 5.1 Session Management

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/channel/accounts/session` | GET/POST | Validate session and get user context (for microservices) | Channel JWT + User JWT (in body for POST) |
| `/accounts/session` | GET/POST | Validate session (direct user auth) | Bearer/Cookie |
| `/channel/accounts/session/validate` | POST | Validate user session (channel auth) | Channel JWT |
| `/channel/validateSession` | GET/POST | Validate channel session | Channel JWT |

### 5.2 Account Information

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/accounts/info` | GET | Get account and user info | Bearer/Cookie |
| `/channel/accounts/info` | GET | Get account info (channel) | Channel JWT |
| `/channel/allAccounts` | GET | Get all accounts (channel) | Channel JWT |

### 5.3 User Management

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/accounts/members` | GET | List account members | Bearer/Cookie |
| `/channel/accounts/members` | GET | List account members (channel) | Channel JWT |

### 5.4 Authorization (To Be Implemented)

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/channel/accounts/authorize/check` | POST | Check if user has privilege (server-to-server) | Channel JWT |

---

## 7. Best Practices

### 6.1 Caching
- **Cache session validation results** for a short duration (e.g., 30 seconds) to reduce IAM service load
- **Cache privilege check results** for 30-60 seconds
- **Invalidate cache** when membership or role changes are detected

### 6.2 Error Handling
- **Always validate sessions** before processing requests
- **Log security events** for authentication/authorization failures
- **Return appropriate HTTP status codes**: 401 (Unauthorized), 403 (Forbidden)

### 6.3 Performance
- **Use connection pooling** for HTTP clients calling IAM service
- **Implement circuit breakers** to handle IAM service unavailability
- **Consider async validation** for non-critical paths
- **Set reasonable timeouts** (e.g., 2-5 seconds) for IAM service calls

### 6.4 Security
- **Never trust client-provided user/account IDs** - always validate via IAM service
- **Validate JWT signatures** - IAM service handles this, but verify responses
- **Use HTTPS** for all communication with IAM service
- **Implement rate limiting** on authentication endpoints
- **Keep channel JWT secret keys secure** - use environment variables or key management services

### 6.5 Monitoring
- **Log all authentication attempts** (success and failure)
- **Monitor IAM service response times**
- **Alert on authentication failure spikes**
- **Track privilege check performance**
- **Monitor cache hit rates**

### 6.6 Testing
- **Mock IAM service** in unit tests
- **Use test tokens** for integration tests
- **Test both user and channel authentication flows**
- **Verify privilege checks work correctly**
- **Test error scenarios** (IAM service down, invalid tokens, etc.)

---

## Appendix: Configuration Example

### application.properties

```properties
# IAM Service Configuration
iam.service.url=http://iam-service:8080
iam.service.timeout=5000
iam.service.retry.max-attempts=3

# Session Validation
session.validation.cache.enabled=true
session.validation.cache.ttl=30

# Privilege Check Caching
privilege.cache.enabled=true
privilege.cache.ttl=60

# Channel Authentication (REQUIRED for all server-to-server calls)
channel.auth.enabled=true
channel.jwt.secret.v1=zcPXq33YkT6zxK950bMR+I5bmEEJ/YZ/IFeR/ogeVVM=
channel.jwt.secret.v2=sQ35GIHp/YqoIxNtuutTHzWN0xgWLkAWZG0z2UArMe0=

# IAM Service Endpoints (all use /channel context for server-to-server)
iam.service.session.endpoint=/channel/accounts/session
iam.service.authorize.endpoint=/channel/accounts/authorize/check
iam.service.account.info.endpoint=/channel/accounts/info
```

---

## Summary Checklist

When integrating a new microservice with IAM:

- [ ] **Understand the `/channel` context rule**: All server-to-server calls to IAM service must use `/channel/*` endpoints
- [ ] Configure Channel JWT token generation/retrieval for your microservice
- [ ] Implement authentication filter to validate user JWT tokens from incoming requests
- [ ] Implement session validation that calls `/channel/accounts/session` with Channel JWT
- [ ] Extract and set user/account context from session response
- [ ] Implement authorization service that calls `/channel/accounts/authorize/check` for privilege checks
- [ ] Implement channel request handling for incoming `/channel/*` requests
- [ ] **Set up Kafka consumers** for account and user events
- [ ] **Implement local account/user database tables** and repositories
- [ ] **Implement on-demand sync services** for fetching missing account/user data from IAM
- [ ] Add privilege checks to protected endpoints
- [ ] Configure caching for session and privilege checks
- [ ] Set up error handling and logging
- [ ] Add monitoring and alerting (including Kafka consumer lag)
- [ ] Write integration tests
- [ ] Document channel JWT token generation process

---

**Last Updated**: 2025-12-08
**Version**: 2.1

