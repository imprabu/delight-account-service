package com.delight.account.service.impl;

import com.delight.account.dto.SignupRequest;
import com.delight.account.exception.AccountCreationException;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.model.Membership;
import com.delight.account.model.Organization;
import com.delight.account.model.OrganizationStatus;
import com.delight.account.model.Product;
import com.delight.account.model.User;
import com.delight.account.model.AccountStatus;
import com.delight.account.model.MembershipStatus;
import com.delight.account.model.RoleEntity;
import com.delight.account.model.UserStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.OrganizationRepository;
import com.delight.account.repository.ProductRepository;
import com.delight.account.repository.RoleRepository;
import com.delight.account.repository.UserRepository;
import com.delight.account.service.AccountService;
import com.delight.account.service.CacheService;
import com.delight.account.service.FeatureConfigService;
import com.delight.account.service.SignupService;
import com.delight.account.service.SubscriptionService;
import com.delight.account.service.cache.IProductCacheService;
import com.delight.account.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SignupServiceImpl implements SignupService {

    private static final Logger logger = LoggerFactory.getLogger(SignupServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private AccountService accountService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private com.delight.account.service.RoleService roleService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private IProductCacheService productCacheService;

    @Autowired
    private com.delight.account.service.ActivationTokenService activationTokenService;

    @Autowired
    private com.delight.account.service.NotificationService notificationService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private FeatureConfigService featureConfigService;

    @Value("${signup.common.email.providers:gmail.com,yahoo.com,outlook.com,yandex.com,rediff.com,zoho.com}")
    private String commonEmailProvidersConfig;

    private final Random random = new Random();
    private Set<String> commonEmailProviders;

    /**
     * Initialize common email providers set from configuration
     */
    private Set<String> getCommonEmailProviders() {
        if (commonEmailProviders == null) {
            List<String> providers = Arrays.asList(commonEmailProvidersConfig.split(","));
            commonEmailProviders = providers.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
            logger.debug("Common email providers configured: {}", commonEmailProviders);
        }
        return commonEmailProviders;
    }

    /**
     * Extract domain from email address
     */
    private String extractEmailDomain(String emailAddress) {
        if (emailAddress == null || !emailAddress.contains("@")) {
            return null;
        }
        return emailAddress.substring(emailAddress.indexOf("@") + 1).toLowerCase().trim();
    }

    /**
     * Check if email domain is a common email provider
     */
    private boolean isCommonEmailProvider(String emailAddress) {
        String emailDomain = extractEmailDomain(emailAddress);
        if (emailDomain == null) {
            return false;
        }
        return getCommonEmailProviders().contains(emailDomain);
    }

    @Override
    @Transactional
    public String signup(SignupRequest request) {
        try {
            // Determine domain based on email provider
            String domain;
            String emailDomain = extractEmailDomain(request.getEmailAddress());
            boolean isCommonProvider = isCommonEmailProvider(request.getEmailAddress());
            
            if (isCommonProvider) {
                // For common email providers (gmail, yahoo, etc.), use company name
                domain = request.getCompanyName().trim().toLowerCase().replaceAll("\\s+", "");
                logger.debug("Using company name for domain (common email provider detected: {}): {}", 
                    emailDomain, domain);
            } else {
                // For custom email domains, use company name as well (existing behavior)
                // Could be changed to use email domain if needed: domain = emailDomain;
                domain = request.getCompanyName().trim().toLowerCase().replaceAll("\\s+", "");
                logger.debug("Using company name for domain (custom email domain: {}): {}", 
                    emailDomain, domain);
            }
            
            String base = domain;
            for (int i = 0; i < 5; i++) {
                Optional<Account> existingAccountOpt = accountRepository.findByDomain(domain);
                if (existingAccountOpt.isPresent() && accountService.isAccountActive(existingAccountOpt.get().getId())) {
                    domain = base + random.nextInt(10_000);
                } else {
                    break;
                }
            }
            Optional<Account> finalCheckOpt = accountRepository.findByDomain(domain);
            if (finalCheckOpt.isPresent() && accountService.isAccountActive(finalCheckOpt.get().getId())) {
                throw new ApiException("Unable to generate unique domain after 5 retries");
            }
            // Look up product from database using productCode from request
            Product product = productCacheService.findByCode(request.getProductCode())
                .orElseThrow(() -> {
                    logger.error("Product not found during signup: {}", request.getProductCode());
                    return new ApiException("error.product.notfound", request.getProductCode());
                });
            
            // Verify product is active (handle null as inactive)
            if (product.getActive() == null || !product.getActive()) {
                logger.error("Product is not active during signup: {}", request.getProductCode());
                throw new ApiException("error.product.inactive", request.getProductCode());
            }

            // Create organization for this signup
            Organization org = new Organization();
            org.setName(request.getCompanyName());
            org.setPrimaryDomain(domain);
            org.setStatus(OrganizationStatus.ACTIVE);
            organizationRepository.save(org);

            Account account = new Account();
            account.setOrganization(org);
            account.setProduct(product);
            account.setDomain(domain);
            // Account status will be set to ACTIVE when subscription is created (subscription status is TRIAL)
            account.setStatus(AccountStatus.ACTIVE);
            account.setIndustryType(request.getIndustryType());
            account.setCompanyName(request.getCompanyName());
            account.setRegion(request.getRegion());
            accountRepository.save(account);

            User user = new User();
            user.setAccount(account);
            user.setUserName(request.getFirstName() + request.getLastName());
            user.setEmailAddress(request.getEmailAddress());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setCompanyName(request.getCompanyName());
            // Initial status must be INSERTED; activation will set ACTIVE
            user.setStatus(UserStatus.INSERTED);
            userRepository.save(user);

            // Initialize default roles for this account (ORG_ADMIN, ACCOUNT_ADMIN, AGENT)
            // The account creator gets ORG_ADMIN role (which has all privileges including account admin privileges)
            logger.debug("🔄 [SIGNUP] Initializing default roles for account: {}", account.getId());
            RoleEntity orgAdminRole = roleService.initializeDefaultRolesForAccount(account.getId());
            logger.debug("✅ [SIGNUP] Default roles initialized. Org Admin role ID: {}", orgAdminRole.getId());

            // Get ACCOUNT_ADMIN role for this account to verify it exists
            RoleEntity accountAdminRole = roleRepository.findByAccountIdAndCode(account.getId(), "ACCOUNT_ADMIN")
                .orElseThrow(() -> {
                    logger.error("ACCOUNT_ADMIN role not found for account: {}", account.getId());
                    return new ApiException("error.role.notfound", "ACCOUNT_ADMIN");
                });
            logger.debug("✅ [SIGNUP] ACCOUNT_ADMIN role verified. Role ID: {}", accountAdminRole.getId());

            // Create membership for ORG_ADMIN role
            // Note: ORG_ADMIN has all privileges including ACCOUNT_ADMIN privileges (user.invite, etc.)
            // The user is both Org Admin and effectively Account Admin through the ORG_ADMIN role
            logger.debug("🔄 [SIGNUP] Creating ORG_ADMIN membership for user: {}, account: {}, role: {}", 
                user.getId(), account.getId(), orgAdminRole.getId());
            Membership membership = new Membership();
            membership.setAccount(account);
            membership.setUser(user);
            membership.setRole(orgAdminRole);
            membership.setStatus(MembershipStatus.ACTIVE);
            membershipRepository.save(membership);
            logger.debug("✅ [SIGNUP] ORG_ADMIN membership created. Membership ID: {} (user has all privileges including account admin)", membership.getId());
            
            // Create subscription with ENTERPRISE plan for the product
            try {
                String productCode = product.getCode();
                logger.debug("🔄 [SIGNUP] Creating ENTERPRISE subscription for account: {}, product: {}", 
                    account.getId(), productCode);
                
                // Verify ENTERPRISE plan exists for this product
                var planConfigOpt = featureConfigService.getPlanConfig(productCode, "ENTERPRISE");
                if (planConfigOpt.isEmpty()) {
                    logger.warn("⚠️ [SIGNUP] ENTERPRISE plan not found for product: {}. Skipping subscription creation.", 
                        productCode);
                } else {
                    com.delight.account.model.Subscription subscription = subscriptionService.createSubscription(
                        account.getId(), 
                        productCode, 
                        "ENTERPRISE"
                    );
                    logger.info("✅ [SIGNUP] ENTERPRISE subscription created. Subscription ID: {}, Account ID: {}, Product: {}", 
                        subscription.getId(), account.getId(), productCode);
                }
            } catch (Exception e) {
                // Log error but don't fail signup if subscription creation fails
                logger.error("⚠️ [SIGNUP] Failed to create ENTERPRISE subscription for account: {}, product: {}. Error: {}", 
                    account.getId(), product.getCode(), e.getMessage(), e);
                LoggingUtils.logBusinessOperation(logger, "CREATE_SUBSCRIPTION", "FAILED", 
                    "accountId", account.getId(), 
                    "productCode", product.getCode(),
                    "error", e.getMessage());
            }
            
            // Generate activation token and send welcome email with activation link
            try {
                LoggingUtils.logBusinessOperation(logger, "GENERATE_ACTIVATION_TOKEN", "STARTED", 
                    "emailAddress", request.getEmailAddress(), "accountId", account.getId());
                
                com.delight.account.model.ActivationToken activationToken = activationTokenService.generateToken(
                    request.getEmailAddress(), 
                    account.getId()
                );
                
                LoggingUtils.logBusinessOperation(logger, "GENERATE_ACTIVATION_TOKEN", "COMPLETED", 
                    "emailAddress", request.getEmailAddress(), "tokenId", activationToken.getId());
                
                // Send signup welcome email with activation link
                LoggingUtils.logBusinessOperation(logger, "SEND_SIGNUP_EMAIL", "STARTED", 
                    "emailAddress", request.getEmailAddress());
                
                notificationService.sendSignupEmail(
                    request.getEmailAddress(),
                    request.getFirstName(),
                    request.getLastName(),
                    activationToken.getToken(),
                    domain
                );
                
                LoggingUtils.logBusinessOperation(logger, "SEND_SIGNUP_EMAIL", "COMPLETED", 
                    "emailAddress", request.getEmailAddress());
            } catch (Exception e) {
                // Log error but don't fail signup if email/token generation fails
                LoggingUtils.logBusinessOperation(logger, "SEND_SIGNUP_EMAIL", "FAILED", 
                    "emailAddress", request.getEmailAddress(), 
                    "error", e.getMessage());
                logger.error("Failed to generate activation token or send signup email to: {}", 
                    request.getEmailAddress(), e);
            }
            
            return domain;
        } catch (DataAccessException ex) {
            logger.error("Data access error during signup: {}", ex.getMessage(), ex);
            throw new AccountCreationException();
        } catch (Exception ex) {
            // Catch any other exceptions (IllegalArgumentException, etc.) and wrap them
            logger.error("Unexpected error during signup: {}", ex.getMessage(), ex);
            throw new AccountCreationException();
        }
    }
}
