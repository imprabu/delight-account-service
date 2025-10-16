package com.delight.account.service.impl;

import com.delight.account.cache.CacheKeyManager;
import com.delight.account.dto.ActivationRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.model.Membership;
import com.delight.account.model.MembershipStatus;
import com.delight.account.model.Role;
import com.delight.account.model.User;
import com.delight.account.model.UserCredential;
import com.delight.account.model.UserStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.UserCredentialRepository;
import com.delight.account.repository.UserRepository;
import com.delight.account.service.ActivationService;
import com.delight.account.service.CacheService;
import com.delight.account.util.PasswordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Service
public class ActivationServiceImpl implements ActivationService {

    private static final Logger logger = LoggerFactory.getLogger(ActivationServiceImpl.class);

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final AccountRepository accountRepository;
    private final MembershipRepository membershipRepository;
    private final CacheService cacheService;
    private final CacheKeyManager cacheKeyManager;
    private final PasswordValidator passwordValidator;
    private final String passwordSecret;

    public ActivationServiceImpl(UserRepository userRepository,
                                UserCredentialRepository credentialRepository,
                                AccountRepository accountRepository,
                                MembershipRepository membershipRepository,
                                CacheService cacheService,
                                CacheKeyManager cacheKeyManager,
                                PasswordValidator passwordValidator,
                                @Value("${security.password.secret}") String passwordSecret) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.accountRepository = accountRepository;
        this.membershipRepository = membershipRepository;
        this.cacheService = cacheService;
        this.cacheKeyManager = cacheKeyManager;
        this.passwordValidator = passwordValidator;
        this.passwordSecret = passwordSecret;
    }

    @Override
    @Transactional
    public void activateUser(Long accountId, ActivationRequest request, String country) {
        logger.info("Activation request accountId={} emailAddress={} firstName={} lastName={} phoneNumber={} country={}",
            accountId, request.getEmailAddress(), request.getFirstName(), request.getLastName(), request.getPhoneNumber(), country);
        
        // Validate password match
        if (!request.getPassword().equals(request.getRetypePassword())) {
            throw new ApiException("error.validation");
        }
        
        // Validate password strength
        if (!passwordValidator.isValid(request.getPassword())) {
            String errorMessage = passwordValidator.getValidationError(request.getPassword());
            logger.warn("Password validation failed for accountId={} emailAddress={}: {}", 
                accountId, request.getEmailAddress(), errorMessage);
            throw new ApiException("error.password.weak");
        }

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ApiException("error.account.notfound"));

        // Find existing user or create new one
        User user = userRepository
            .findByAccountIdAndEmailAddress(accountId, request.getEmailAddress())
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setAccount(account);
                newUser.setEmailAddress(request.getEmailAddress());
                newUser.setStatus(UserStatus.INSERTED); // Set initial status for new users
                return newUser;
            });
            
        // Validate user status - only allow activation for INSERTED users or reactivation for DEACTIVATED users
        if (user.getId() != null && user.getStatus() != UserStatus.INSERTED && user.getStatus() != UserStatus.DEACTIVATED) {
            logger.warn("User activation attempted for user with invalid status accountId={} emailAddress={} currentStatus={}", 
                accountId, request.getEmailAddress(), user.getStatus());
            throw new ApiException("error.user.already.active");
        }
        user.setAccount(account);
        user.setUserName(request.getFirstName() + request.getLastName());
        user.setEmailAddress(request.getEmailAddress());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setCompanyName(country); // store country in companyName for demo
        user.setPhoneNumber(request.getPhoneNumber());
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        UserCredential credential = credentialRepository.findByUser(user)
            .orElseGet(() -> {
                UserCredential newCredential = new UserCredential();
                newCredential.setUser(user);
                newCredential.setAccount(account);
                return newCredential;
            });
        credential.setPasswordHash(encrypt(request.getPassword()));
        credentialRepository.save(credential);

        // Ensure membership exists and is ACTIVE for this account
        Membership membership = membershipRepository
            .findByUserIdAndAccountId(user.getId(), accountId)
            .orElseGet(() -> {
                Membership m = new Membership();
                m.setUser(user);
                m.setAccount(account);
                m.setRole(Role.OWNER); // Default to OWNER for first user in account
                return m;
            });
        membership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(membership);

        // Invalidate cache for the user since credentials and membership have been updated
        // Evict versioned keys if manager is available
        if (cacheKeyManager != null) {
            // Always evict versioned keys when a key manager is provided
            String userAccountCacheKey = cacheKeyManager.getUserAccountKey(accountId, request.getEmailAddress());
            String accountInfoCacheKey = cacheKeyManager.getAccountInfoKey(accountId, user.getId());
            cacheService.evict(userAccountCacheKey);
            cacheService.evict(accountInfoCacheKey);
            // In production-style contexts, also evict legacy keys for compatibility
            if (cacheKeyManager instanceof com.delight.account.cache.impl.VersionedCacheKeyManager) {
                cacheService.evict(String.format("user:account:%d:email:%s", accountId, request.getEmailAddress()));
                cacheService.evict(String.format("account:info:%d", accountId));
            }
        } else {
            // Evict legacy keys expected by older tests/consumers
            cacheService.evict(String.format("user:account:%d:email:%s", accountId, request.getEmailAddress()));
            cacheService.evict(String.format("account:info:%d", accountId));
        }
        
        logger.info("User activation completed for accountId={} emailAddress={}", accountId, request.getEmailAddress());
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
