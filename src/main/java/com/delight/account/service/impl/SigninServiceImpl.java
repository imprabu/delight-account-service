package com.delight.account.service.impl;

import com.delight.account.dto.SigninRequest;
import com.delight.account.dto.SigninResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.model.User;
import com.delight.account.model.UserStatus;
import com.delight.account.model.UserCredential;
import com.delight.account.model.Membership;
import com.delight.account.model.MembershipStatus;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.model.OrganizationStatus;
import com.delight.account.model.Session;
import com.delight.account.model.Role;
import com.delight.account.repository.UserCredentialRepository;
import com.delight.account.repository.UserRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.AccountRepository;
import com.delight.account.service.SigninService;
import com.delight.account.service.SessionService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    public SigninServiceImpl(UserRepository userRepository,
                             UserCredentialRepository credentialRepository,
                             @Value("${security.password.secret}") String passwordSecret,
                             @Value("${security.jwt.secret}") String jwtSecret,
                             @Value("${security.jwt.expiration}") long jwtExpiration,
                             MembershipRepository membershipRepository,
                             AccountRepository accountRepository,
                             SessionService sessionService) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.passwordSecret = passwordSecret;
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;
        this.membershipRepository = membershipRepository;
        this.accountRepository = accountRepository;
        this.sessionService = sessionService;
    }

    @Override
    public String signin(Long accountId, String domain, SigninRequest request) {
        logger.info("Signin attempt domain={} accountId={} emailAddress={}", domain, accountId, request.getEmailAddress());

        User user = userRepository
            .findByAccountIdAndEmailAddress(accountId, request.getEmailAddress())
            .orElseThrow(() -> {
                logger.warn("User not found domain={} accountId={} emailAddress={}", domain, accountId, request.getEmailAddress());
                return new ApiException("error.user.notfound");
            });

        // Check if user is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            logger.warn("User is not active domain={} accountId={} emailAddress={} status={}", domain, accountId, request.getEmailAddress(), user.getStatus());
            throw new ApiException("error.user.inactive");
        }

        UserCredential credential = credentialRepository.findByUser(user)
            .orElseThrow(() -> {
                logger.warn("Credential missing for userId={} domain={}", user.getId(), domain);
                return new ApiException("error.signin.invalid");
            });

        String encrypted = encrypt(request.getPassword());
        if (!encrypted.equals(credential.getPasswordHash())) {
            logger.warn("Invalid password for userId={} domain={}", user.getId(), domain);
            throw new ApiException("error.signin.invalid");
        }

        // Resolve membership if present (optional for basic signin)
        Membership membership = membershipRepository
            .findByUserIdAndAccountId(user.getId(), accountId)
            .orElse(null);
        if (membership != null && membership.getStatus() != MembershipStatus.ACTIVE) {
            logger.warn("Inactive membership for userId={} accountId={} status={}", user.getId(), accountId, membership.getStatus());
            throw new ApiException("error.signin.invalid");
        }

        logger.info("Signin successful domain={} userId={} accountId={}", domain, user.getId(), accountId);

        // Resolve organization id and product code from account
        Long orgId = null;
        String productCode = null;
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account != null) {
            // Check if account is active
            if (account.getStatus() != AccountStatus.ACTIVE) {
                logger.warn("Account is not active domain={} accountId={} status={}", domain, accountId, account.getStatus());
                throw new ApiException("error.account.notfound");
            }
            
            if (account.getOrganization() != null) {
                // Check if organization is active
                if (account.getOrganization().getStatus() != OrganizationStatus.ACTIVE) {
                    logger.warn("Organization is not active domain={} accountId={} orgId={} status={}", domain, accountId, account.getOrganization().getId(), account.getOrganization().getStatus());
                    throw new ApiException("error.signin.invalid");
                }
                orgId = account.getOrganization().getId();
            }
            if (account.getProduct() != null) {
                productCode = account.getProduct().getCode();
            }
        } else {
            logger.warn("Account not found accountId={} domain={}", accountId, domain);
            throw new ApiException("error.signin.invalid");
        }

        return Jwts.builder()
            .subject(user.getEmailAddress())
            .claim("accountId", accountId)
            .claim("userId", user.getId())
            .claim("role", membership.getRole().name())
            .claim("orgId", orgId)
            .claim("productCode", productCode)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }

    @Override
    public SigninResponse signinWithSession(Long accountId, String domain, SigninRequest request, String userAgent, String ipAddress) {
        logger.info("Signin with session attempt domain={} accountId={} emailAddress={}", domain, accountId, request.getEmailAddress());

        User user = userRepository
            .findByAccountIdAndEmailAddress(accountId, request.getEmailAddress())
            .orElseThrow(() -> {
                logger.warn("User not found domain={} accountId={} emailAddress={}", domain, accountId, request.getEmailAddress());
                return new ApiException("error.user.notfound");
            });

        // Check if user is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            logger.warn("User is not active domain={} accountId={} emailAddress={} status={}", domain, accountId, request.getEmailAddress(), user.getStatus());
            throw new ApiException("error.user.inactive");
        }

        UserCredential credential = credentialRepository.findByUser(user)
            .orElseThrow(() -> {
                logger.warn("Credential missing for userId={} domain={}", user.getId(), domain);
                return new ApiException("error.signin.invalid");
            });

        String encrypted = encrypt(request.getPassword());
        if (!encrypted.equals(credential.getPasswordHash())) {
            logger.warn("Invalid password for userId={} domain={}", user.getId(), domain);
            throw new ApiException("error.signin.invalid");
        }

        // Check active membership in this account if present (optional)
        Membership membership = membershipRepository
            .findByUserIdAndAccountId(user.getId(), accountId)
            .orElse(null);
        if (membership != null && membership.getStatus() != MembershipStatus.ACTIVE) {
            logger.warn("Inactive membership for userId={} accountId={} status={}", user.getId(), accountId, membership.getStatus());
            throw new ApiException("error.signin.invalid");
        }

        logger.info("Signin successful domain={} userId={} accountId={}", domain, user.getId(), accountId);

        // Resolve organization id and product code from account
        Long orgId = null;
        String productCode = null;
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account != null) {
            // Check if account is active
            if (account.getStatus() != AccountStatus.ACTIVE) {
                logger.warn("Account is not active domain={} accountId={} status={}", domain, accountId, account.getStatus());
                throw new ApiException("error.signin.invalid");
            }
            
            if (account.getOrganization() != null) {
                // Check if organization is active
                if (account.getOrganization().getStatus() != OrganizationStatus.ACTIVE) {
                    logger.warn("Organization is not active domain={} accountId={} orgId={} status={}", domain, accountId, account.getOrganization().getId(), account.getOrganization().getStatus());
                    throw new ApiException("error.signin.invalid");
                }
                orgId = account.getOrganization().getId();
            }
            if (account.getProduct() != null) {
                productCode = account.getProduct().getCode();
            }
        } else {
            logger.warn("Account not found accountId={} domain={}", accountId, domain);
            throw new ApiException("error.signin.invalid");
        }

        String roleName = membership != null ? membership.getRole().name() : Role.MEMBER.name();
        String jwtToken = Jwts.builder()
            .subject(user.getEmailAddress())
            .claim("accountId", accountId)
            .claim("userId", user.getId())
            .claim("role", roleName)
            .claim("orgId", orgId)
            .claim("productCode", productCode)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact();

        // Create session with resolved role (default MEMBER if no membership)
        Role sessionRole = membership != null ? membership.getRole() : Role.MEMBER;
        Session session = sessionService.createSession(user, account, sessionRole, jwtToken, userAgent, ipAddress);
        
        return new SigninResponse(jwtToken, session.getSessionId());
    }

    private String encrypt(String value) {
        try {
            byte[] keyBytes = passwordSecret.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, 0, 16, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
}
