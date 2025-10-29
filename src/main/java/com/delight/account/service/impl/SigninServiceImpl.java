package com.delight.account.service.impl;

import com.delight.account.cache.CacheKeyManager;
import com.delight.account.dto.SigninRequest;
import com.delight.account.dto.SigninResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.model.ClientType;
import com.delight.account.model.Membership;
import com.delight.account.model.MembershipStatus;
import com.delight.account.model.Role;
import com.delight.account.model.Session;
import com.delight.account.model.User;
import com.delight.account.model.UserCredential;
import com.delight.account.model.UserStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.UserCredentialRepository;
import com.delight.account.repository.UserRepository;
import com.delight.account.service.CacheService;
import com.delight.account.service.SessionService;
import com.delight.account.service.SigninService;
import com.delight.account.util.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

@Service
public class SigninServiceImpl implements SigninService {

    private static final Logger logger = LoggerFactory.getLogger(SigninServiceImpl.class);

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final String passwordSecret;
    private final String jwtSecret;
    private final long jwtExpiration;
    private final MembershipRepository membershipRepository;
    private final AccountRepository accountRepository;
    private final SessionService sessionService;
    private final CacheService cacheService;
    private final CacheKeyManager cacheKeyManager;
    
    @Value("${cache.user.ttl:PT15M}")
    private Duration userCacheTtl;

    @org.springframework.beans.factory.annotation.Autowired
    public SigninServiceImpl(UserRepository userRepository,
                             UserCredentialRepository credentialRepository,
                             @Value("${security.password.secret}") String passwordSecret,
                             @Value("${security.jwt.secret}") String jwtSecret,
                             @Value("${security.jwt.expiration}") long jwtExpiration,
                             MembershipRepository membershipRepository,
                             AccountRepository accountRepository,
                             SessionService sessionService,
                             CacheService cacheService,
                             CacheKeyManager cacheKeyManager) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.passwordSecret = passwordSecret;
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;
        this.membershipRepository = membershipRepository;
        this.accountRepository = accountRepository;
        this.sessionService = sessionService;
        this.cacheService = cacheService;
        this.cacheKeyManager = cacheKeyManager;
    }

    // Backward-compatible constructor for tests without CacheKeyManager
    public SigninServiceImpl(UserRepository userRepository,
                             UserCredentialRepository credentialRepository,
                             String passwordSecret,
                             String jwtSecret,
                             long jwtExpiration,
                             MembershipRepository membershipRepository,
                             AccountRepository accountRepository,
                             SessionService sessionService,
                             CacheService cacheService) {
        this(userRepository, credentialRepository, passwordSecret, jwtSecret, jwtExpiration,
             membershipRepository, accountRepository, sessionService, cacheService, null);
    }

    @Override
    public String signin(Long accountId, String domain, SigninRequest request) {
        LoggingUtils.info(logger, "Signin attempt emailAddress={}", request.getEmailAddress());

        // Cache user lookup by account and email
        String userCacheKey = cacheKeyManager.getUserAccountKey(accountId, request.getEmailAddress());
        Optional<User> userOpt = cacheService.get(userCacheKey, () -> {
            return userRepository.findByAccountIdAndEmailAddress(accountId, request.getEmailAddress());
        }, userCacheTtl, User.class);
        
        User user = userOpt.orElseThrow(() -> {
            LoggingUtils.warn(logger, "User not found emailAddress={}", request.getEmailAddress());
            return new ApiException("error.user.notfound");
        });

        // Check if user is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            LoggingUtils.warn(logger, "User is not active emailAddress={} status={}", request.getEmailAddress(), user.getStatus());
            throw new ApiException("error.user.inactive");
        }

        // Cache user credential lookup
        String credentialCacheKey = cacheKeyManager != null
            ? cacheKeyManager.getUserCredentialKey(user.getId())
            : String.format("user:credential:%d", user.getId());
        Optional<UserCredential> credentialOpt = cacheService.get(credentialCacheKey, () -> {
            return credentialRepository.findByUser(user);
        }, userCacheTtl, UserCredential.class);
        
        UserCredential credential = credentialOpt.orElseThrow(() -> {
            LoggingUtils.warn(logger, "Credential missing for userId={}", user.getId());
            return new ApiException("error.signin.invalid");
        });

        String encrypted = encrypt(request.getPassword());
        if (!encrypted.equals(credential.getPasswordHash())) {
            LoggingUtils.warn(logger, "Invalid password for userId={}", user.getId());
            throw new ApiException("error.signin.invalid");
        }

        // Require active membership in this account
        Membership membership = membershipRepository
            .findByUserIdAndAccountId(user.getId(), accountId)
            .orElseThrow(() -> new ApiException("error.signin.invalid"));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            LoggingUtils.warn(logger, "Inactive membership for userId={} accountId={} status={}", user.getId(), accountId, membership.getStatus());
            throw new ApiException("error.signin.invalid");
        }

        LoggingUtils.info(logger, "Signin successful userId={} accountId={}", user.getId(), accountId);

        // Resolve organization id and product code from account
        Long orgId = null;
        String productCode = null;
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account != null) {
            if (account.getOrganization() != null) {
                orgId = account.getOrganization().getId();
            }
            if (account.getProduct() != null) {
                productCode = account.getProduct().getCode();
            }
        } else {
            throw new ApiException("error.signin.invalid");
        }

        String roleName = membership.getRole() != null ? membership.getRole().name() : Role.MEMBER.name();
        // Generate JWT token with legacy claims expected by tests
        return io.jsonwebtoken.Jwts.builder()
            .subject(user.getEmailAddress())
            .claim("accountId", accountId)
            .claim("userId", user.getId())
            .claim("role", roleName)
            .claim("orgId", orgId)
            .claim("productCode", productCode)
            .issuedAt(new java.util.Date())
            .expiration(new java.util.Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .compact();
    }

    @Override
    public SigninResponse signinWithSession(Long accountId, String domain, SigninRequest request, String userAgent, String ipAddress, ClientType clientType) {
        LoggingUtils.info(logger, "Signin with session attempt emailAddress={} clientType={}", request.getEmailAddress(), clientType);

        // Cache user lookup by account and email (reuse same caching logic)
        String userCacheKey = cacheKeyManager.getUserAccountKey(accountId, request.getEmailAddress());
        Optional<User> userOpt = cacheService.get(userCacheKey, () -> {
            return userRepository.findByAccountIdAndEmailAddress(accountId, request.getEmailAddress());
        }, userCacheTtl, User.class);
        
        User user = userOpt.orElseThrow(() -> {
            LoggingUtils.warn(logger, "User not found emailAddress={}", request.getEmailAddress());
            return new ApiException("error.user.notfound");
        });

        // Check if user is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            LoggingUtils.warn(logger, "User is not active emailAddress={} status={}", request.getEmailAddress(), user.getStatus());
            throw new ApiException("error.user.inactive");
        }

        // Cache user credential lookup (reuse same caching logic)
        String credentialCacheKey = cacheKeyManager != null
            ? cacheKeyManager.getUserCredentialKey(user.getId())
            : String.format("user:credential:%d", user.getId());
        Optional<UserCredential> credentialOpt = cacheService.get(credentialCacheKey, () -> {
            return credentialRepository.findByUser(user);
        }, userCacheTtl, UserCredential.class);
        
        UserCredential credential = credentialOpt.orElseThrow(() -> {
            LoggingUtils.warn(logger, "Credential missing for userId={}", user.getId());
            return new ApiException("error.signin.invalid");
        });

        String encrypted = encrypt(request.getPassword());
        if (!encrypted.equals(credential.getPasswordHash())) {
            LoggingUtils.warn(logger, "Invalid password for userId={}", user.getId());
            throw new ApiException("error.signin.invalid");
        }

        // Allow signin even if membership not found; only reject if present and inactive
        Optional<Membership> membershipOpt = membershipRepository
            .findByUserIdAndAccountId(user.getId(), accountId);
        if (membershipOpt.isPresent() && membershipOpt.get().getStatus() != MembershipStatus.ACTIVE) {
            LoggingUtils.warn(logger, "Inactive membership for userId={} accountId={} status={}", user.getId(), accountId, membershipOpt.get().getStatus());
            throw new ApiException("error.signin.invalid");
        }

        LoggingUtils.info(logger, "Signin successful userId={} accountId={}", user.getId(), accountId);

        // Get account and membership for session creation
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ApiException("error.account.notfound"));
        
        Membership membership = membershipOpt.orElse(null);

        // Create session with client type for proper TTL calculation
        Session session = sessionService.createSession(user, account, membership != null && membership.getRole() != null ? membership.getRole() : Role.MEMBER, "", userAgent, ipAddress, clientType);

        // Resolve organization and product for JWT, include sessionId claim
        Long orgId = account.getOrganization() != null ? account.getOrganization().getId() : null;
        String productCode = account.getProduct() != null ? account.getProduct().getCode() : null;
        String roleName = membership != null && membership.getRole() != null ? membership.getRole().name() : Role.MEMBER.name();
        String jwtToken = io.jsonwebtoken.Jwts.builder()
            .subject(user.getEmailAddress())
            .claim("accountId", accountId)
            .claim("userId", user.getId())
            .claim("role", roleName)
            .claim("orgId", orgId)
            .claim("productCode", productCode)
            .claim("sessionId", session.getSessionId())
            .issuedAt(new java.util.Date())
            .expiration(new java.util.Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .compact();

        // Update session with final token
        sessionService.updateSessionToken(session.getSessionId(), jwtToken);

        return new SigninResponse(jwtToken, session.getSessionId());
    }

    private String encrypt(String plainText) {
        try {
            // Align with ActivationServiceImpl: use first 16 bytes for AES key
            byte[] keyBytes = passwordSecret.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, 0, 16, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting password", e);
        }
    }
}
