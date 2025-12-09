package com.delight.account.service.impl;

import com.delight.account.dto.ActivationRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.model.Membership;
import com.delight.account.model.MembershipStatus;
import com.delight.account.model.RoleEntity;
import com.delight.account.model.User;
import com.delight.account.model.UserCredential;
import com.delight.account.model.UserStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.RoleRepository;
import com.delight.account.repository.UserCredentialRepository;
import com.delight.account.repository.UserRepository;
import com.delight.account.service.ActivationService;
import com.delight.account.service.NotificationService;
import com.delight.account.service.RoleService;
import com.delight.account.util.LoggingUtils;
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

@Service
public class ActivationServiceImpl implements ActivationService {

    private static final Logger logger = LoggerFactory.getLogger(ActivationServiceImpl.class);

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final AccountRepository accountRepository;
    private final MembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final RoleService roleService;
    private final PasswordValidator passwordValidator;
    private final String passwordSecret;
    private final NotificationService notificationService;

    public ActivationServiceImpl(UserRepository userRepository,
                                UserCredentialRepository credentialRepository,
                                AccountRepository accountRepository,
                                MembershipRepository membershipRepository,
                                RoleRepository roleRepository,
                                RoleService roleService,
                                PasswordValidator passwordValidator,
                                NotificationService notificationService,
                                @Value("${security.password.secret}") String passwordSecret) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.accountRepository = accountRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.roleService = roleService;
        this.passwordValidator = passwordValidator;
        this.notificationService = notificationService;
        this.passwordSecret = passwordSecret;
    }

    @Override
    @Transactional
    public void activateUser(Long accountId, ActivationRequest request, String country) {
        LoggingUtils.logMethodEntry(logger, "activateUser", 
            "emailAddress", request.getEmailAddress(), 
            "country", country);
        
        // Note: accountId and domain are now automatically captured from MDC by LoggingUtils
        LoggingUtils.addBusinessContext("USER_ACTIVATION", "User");
        
        try {
            LoggingUtils.logBusinessOperation(logger, "USER_ACTIVATION", "STARTED", 
                "emailAddress", request.getEmailAddress());
            
            // Validate password match
            if (!request.getPassword().equals(request.getRetypePassword())) {
                LoggingUtils.logSecurityEvent(logger, "PASSWORD_MISMATCH", "FAILED", 
                    "emailAddress", request.getEmailAddress());
                throw new ApiException("error.validation");
            }
            
            // Validate password strength
            if (!passwordValidator.isValid(request.getPassword())) {
                String errorMessage = passwordValidator.getValidationError(request.getPassword());
                LoggingUtils.logSecurityEvent(logger, "PASSWORD_VALIDATION_FAILED", "FAILED", 
                    "emailAddress", request.getEmailAddress(), 
                    "reason", errorMessage);
                throw new ApiException("error.password.weak");
            }

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ApiException("error.account.notfound"));

        LoggingUtils.logDatabaseOperation(logger, "FIND_ACCOUNT", "Account", "SUCCESS");

        // Find existing user or create new one
        User user = userRepository
            .findByAccountIdAndEmailAddress(accountId, request.getEmailAddress())
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setAccount(account);
                newUser.setEmailAddress(request.getEmailAddress());
                newUser.setStatus(UserStatus.INSERTED); // Set initial status for new users
                LoggingUtils.logDatabaseOperation(logger, "CREATE_USER", "User", "NEW", 
                    "emailAddress", request.getEmailAddress());
                return newUser;
            });
            
        // Validate user status - only allow activation for INSERTED users or reactivation for DEACTIVATED users
        if (user.getId() != null && user.getStatus() != UserStatus.INSERTED && user.getStatus() != UserStatus.DEACTIVATED) {
            LoggingUtils.logSecurityEvent(logger, "USER_ACTIVATION_INVALID_STATUS", "FAILED", 
                "emailAddress", request.getEmailAddress(), 
                "currentStatus", user.getStatus());
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
        
        LoggingUtils.logDatabaseOperation(logger, "SAVE_USER", "User", "SUCCESS", 
            "userId", user.getId(), 
            "emailAddress", request.getEmailAddress());

        UserCredential credential = credentialRepository.findByUser(user)
            .orElseGet(() -> {
                UserCredential newCredential = new UserCredential();
                newCredential.setUser(user);
                newCredential.setAccount(account);
                return newCredential;
            });
        credential.setPasswordHash(encrypt(request.getPassword()));
        credentialRepository.save(credential);
        
        LoggingUtils.logDatabaseOperation(logger, "SAVE_CREDENTIAL", "UserCredential", "SUCCESS", 
            "userId", user.getId());

            // Ensure membership exists and is ACTIVE for this account
            Membership membership = membershipRepository
                .findByUserIdAndAccountIdWithRole(user.getId(), accountId)
                .orElseGet(() -> {
                    // Find OWNER role for this account - user who created account is by default OWNER
                    RoleEntity ownerRole = roleRepository.findByAccountIdAndCode(accountId, "OWNER")
                        .orElseGet(() -> {
                            // If roles not initialized, initialize them now
                            return roleService.initializeDefaultRolesForAccount(accountId);
                        });
                    Membership m = new Membership();
                    m.setUser(user);
                    m.setAccount(account);
                    m.setRole(ownerRole);
                    return m;
                });
        membership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(membership);
        
        LoggingUtils.logDatabaseOperation(logger, "SAVE_MEMBERSHIP", "Membership", "SUCCESS", 
            "userId", user.getId(), "role", membership.getRole() != null ? membership.getRole().getCode() : "null");

        // Cache invalidation is now handled automatically by CacheInvalidationEntityListener
        // for User, UserCredential, and Membership entities
        
        // Send activation email notification
        try {
            LoggingUtils.logBusinessOperation(logger, "SEND_ACTIVATION_EMAIL", "STARTED", 
                "emailAddress", request.getEmailAddress());
            notificationService.sendActivationEmail(
                request.getEmailAddress(), 
                request.getFirstName(), 
                request.getLastName()
            );
            LoggingUtils.logBusinessOperation(logger, "SEND_ACTIVATION_EMAIL", "COMPLETED", 
                "emailAddress", request.getEmailAddress());
        } catch (Exception e) {
            // Log error but don't fail activation if email fails
            LoggingUtils.logBusinessOperation(logger, "SEND_ACTIVATION_EMAIL", "FAILED", 
                "emailAddress", request.getEmailAddress(), 
                "error", e.getMessage());
            logger.error("Failed to send activation email to: {}", request.getEmailAddress(), e);
        }
        
        LoggingUtils.logBusinessOperation(logger, "USER_ACTIVATION", "COMPLETED", 
            "emailAddress", request.getEmailAddress(), 
            "userId", user.getId());
        
        LoggingUtils.logMethodExit(logger, "activateUser");
        
        } finally {
            LoggingUtils.clearBusinessContext();
        }
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
