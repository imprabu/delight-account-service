package com.delight.account.service.impl;

import com.delight.account.dto.InviteMemberRequest;
import com.delight.account.dto.MemberResponse;
import com.delight.account.dto.UpdateMemberRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Membership;
import com.delight.account.model.MembershipStatus;
import com.delight.account.model.MembershipSite;
import com.delight.account.model.Site;
import com.delight.account.model.SiteScopeMode;
import com.delight.account.model.Role;
import com.delight.account.model.RoleEntity;
import com.delight.account.model.User;
import com.delight.account.model.UserStatus;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.RoleRepository;
import com.delight.account.repository.SiteRepository;
import com.delight.account.repository.MembershipSiteRepository;
import com.delight.account.repository.SessionRepository;
import com.delight.account.repository.UserRepository;
import com.delight.account.domain.BatchJobType;
import com.delight.account.model.ActivationToken;
import com.delight.account.service.ActivationTokenService;
import com.delight.account.service.AuthorizationService;
import com.delight.account.service.MembershipService;
import com.delight.account.service.job.JobQueueService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipServiceImpl implements MembershipService {

    private static final Logger logger = LoggerFactory.getLogger(MembershipServiceImpl.class);

    private final MembershipRepository membershipRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final SiteRepository siteRepository;
    private final MembershipSiteRepository membershipSiteRepository;
    private final AuthorizationService authorizationService;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ActivationTokenService activationTokenService;
    private final JobQueueService jobQueueService;
    private final com.delight.account.service.SessionService sessionService;
    private final String activationUrlBase;
    private final String appDomainName;
    private final String inviteTemplateId;

    public MembershipServiceImpl(MembershipRepository membershipRepository, 
                                 AccountRepository accountRepository, 
                                 RoleRepository roleRepository,
                                 SiteRepository siteRepository,
                                 MembershipSiteRepository membershipSiteRepository,
                                 AuthorizationService authorizationService,
                                 SessionRepository sessionRepository,
                                 UserRepository userRepository,
                                 ActivationTokenService activationTokenService,
                                 JobQueueService jobQueueService,
                                 com.delight.account.service.SessionService sessionService,
                                 @Value("${sendgrid.activation.url.base:http://localhost:8080}") String activationUrlBase,
                                 @Value("${app.domain.name:localhost}") String appDomainName,
                                 @Value("${sendgrid.template.invite:d-1a30d9f5fae34831a2b91698ad4eca2c}") String inviteTemplateId) {
        this.membershipRepository = membershipRepository;
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.siteRepository = siteRepository;
        this.membershipSiteRepository = membershipSiteRepository;
        this.authorizationService = authorizationService;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.activationTokenService = activationTokenService;
        this.jobQueueService = jobQueueService;
        this.sessionService = sessionService;
        this.activationUrlBase = activationUrlBase;
        this.appDomainName = appDomainName;
        this.inviteTemplateId = inviteTemplateId;
    }
    
    private Long getActorUserId() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attr.getRequest();
            Object userIdAttr = request.getAttribute("userId");
            if (userIdAttr instanceof Long) {
                return (Long) userIdAttr;
            } else if (userIdAttr != null) {
                try {
                    return Long.parseLong(userIdAttr.toString());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid user ID in request attributes: {}", userIdAttr);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not get user ID from request context: {}", e.getMessage());
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long accountId) {
        // For channel requests, skip user context and privilege checks
        // AuthorizationService will handle skipping privilege checks automatically
        if (!authorizationService.isChannelRequest()) {
            Long actorUserId = getActorUserId();
            if (actorUserId == null) {
                throw new ApiException("error.authorization", "User context not found");
            }
            
            // Check authorization - user needs user.read privilege to list members
            // For channel requests, this check is automatically skipped in AuthorizationService
            if (!authorizationService.hasPrivilege(actorUserId, accountId, "user.read")) {
                logger.warn("User {} does not have permission to list members in account {}", actorUserId, accountId);
                throw new ApiException("error.authorization", "Insufficient permissions to list members");
            }
        }
        
        ensureAccountProduct(accountId);
        
        // Get all memberships for the account
        List<Membership> memberships = membershipRepository.findByAccountId(accountId);
        
        // Get last active times for all users in this account
        List<Object[]> lastActiveTimes = sessionRepository.findLastActiveTimesByAccountId(accountId);
        Map<Long, OffsetDateTime> lastActiveMap = new HashMap<>();
        for (Object[] row : lastActiveTimes) {
            Long userId = (Long) row[0];
            OffsetDateTime lastActive = (OffsetDateTime) row[2];
            lastActiveMap.put(userId, lastActive);
        }
        
        return memberships.stream()
            .map(m -> {
                RoleEntity role = m.getRole();
                Long userId = m.getUser().getId();
                
                return MemberResponse.builder()
                    .userId(userId)
                    .emailAddress(m.getUser().getEmailAddress())
                    .firstName(m.getUser().getFirstName())
                    .lastName(m.getUser().getLastName())
                    .roleId(role != null ? role.getId() : null)
                    .roleCode(role != null ? role.getCode() : null)
                    .roleName(role != null ? role.getName() : null)
                    .status(m.getStatus())
                    .lastActiveAt(lastActiveMap.get(userId))
                    .twoFactorEnabled(false) // Placeholder - 2FA not implemented yet
                    .invitedAt(m.getStatus() == MembershipStatus.INVITED ? m.getCreatedAt() : null)
                    .invitedBy(m.getInvitedBy() != null ? m.getInvitedBy().getId() : null)
                    .build();
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MemberResponse updateMember(Long accountId, Long targetUserId, UpdateMemberRequest request, Role actorRole) {
        // Get actor user ID from context (set by JwtAuthenticationFilter)
        Long actorUserId = getActorUserId();
        if (actorUserId == null) {
            throw new ApiException("error.authorization", "User context not found");
        }
        
        // Use Casbin to check if user has permission to update members
        if (!authorizationService.hasPrivilege(actorUserId, accountId, "user.update")) {
            logger.warn("User {} does not have permission to update members in account {}", actorUserId, accountId);
            throw new ApiException("error.authorization", "Insufficient permissions to update member");
        }
        ensureAccountProduct(accountId);

        Membership membership = membershipRepository
            .findByUserIdAndAccountIdWithRole(targetUserId, accountId)
            .orElseThrow(() -> new ApiException("error.user.notfound"));

        if (request.getRoleId() != null) {
            RoleEntity role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ApiException("error.role.notfound"));
            membership.setRole(role);
        }
        if (request.getStatus() != null) {
            membership.setStatus(request.getStatus());
        }
        membershipRepository.save(membership);

        // Cache invalidation is now handled automatically by CacheInvalidationEntityListener
        
        // Invalidate all sessions for the user whose membership was changed
        // This ensures they re-authenticate with updated permissions
        try {
            sessionService.invalidateAllUserSessions(targetUserId);
            logger.info("Invalidated all sessions for user {} after membership update in account {}", targetUserId, accountId);
        } catch (Exception e) {
            logger.error("Failed to invalidate sessions for user {} after membership update: {}", targetUserId, e.getMessage(), e);
            // Don't fail the membership update if session invalidation fails
        }

        RoleEntity role = membership.getRole();
        return MemberResponse.builder()
            .userId(membership.getUser().getId())
            .emailAddress(membership.getUser().getEmailAddress())
            .firstName(membership.getUser().getFirstName())
            .lastName(membership.getUser().getLastName())
            .roleId(role != null ? role.getId() : null)
            .roleCode(role != null ? role.getCode() : null)
            .roleName(role != null ? role.getName() : null)
            .status(membership.getStatus())
            .build();
    }

    @Override
    @Transactional
    public MemberResponse inviteMember(Long accountId, InviteMemberRequest request) {
        Long actorUserId = getActorUserId();
        if (actorUserId == null) {
            throw new ApiException("error.authorization", "User context not found");
        }
        
        // Check authorization
        if (!authorizationService.hasPrivilege(actorUserId, accountId, "user.invite")) {
            logger.warn("User {} does not have permission to invite members in account {}", actorUserId, accountId);
            throw new ApiException("error.authorization", "Insufficient permissions to invite member");
        }
        
        ensureAccountProduct(accountId);
        
        // Validate role belongs to this account
        RoleEntity role = roleRepository.findById(request.getRoleId())
            .orElseThrow(() -> new ApiException("error.role.notfound"));
        if (role.getAccount() == null || !role.getAccount().getId().equals(accountId)) {
            throw new ApiException("error.role.notfound", "Role does not belong to this account");
        }
        
        // Check if user already exists in this account
        var account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ApiException("error.account.notfound"));
        
        Optional<User> existingUser = userRepository.findByAccountIdAndEmailAddress(accountId, request.getEmailAddress());
        if (existingUser.isPresent()) {
            // Check if membership already exists
            Optional<Membership> existingMembership = membershipRepository
                .findByUserIdAndAccountId(existingUser.get().getId(), accountId);
            if (existingMembership.isPresent()) {
                throw new ApiException("error.user.already.member", "User is already a member of this account");
            }
        }
        
        // Get or create user
        User user = existingUser.orElseGet(() -> {
            User newUser = new User();
            newUser.setAccount(account);
            newUser.setEmailAddress(request.getEmailAddress());
            newUser.setFirstName(request.getFirstName() != null ? request.getFirstName() : 
                request.getEmailAddress().substring(0, request.getEmailAddress().indexOf('@')));
            newUser.setLastName(request.getLastName() != null ? request.getLastName() : "");
            newUser.setUserName(newUser.getFirstName() + newUser.getLastName());
            newUser.setCompanyName(account.getCompanyName());
            newUser.setStatus(UserStatus.INSERTED); // Will be activated when they set password
            return userRepository.save(newUser);
        });
        
        // Get actor user for invitedBy
        User actorUser = userRepository.findById(actorUserId)
            .orElseThrow(() -> new ApiException("error.user.notfound", "Actor user not found"));
        
        // Create membership with INVITED status
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setAccount(account);
        membership.setRole(role);
        membership.setStatus(MembershipStatus.INVITED);
        membership.setInvitedBy(actorUser);

        String roleCode = role.getCode();
        if (Role.ACCOUNT_ADMIN.getCode().equals(roleCode)) {
            membership.setSiteScopeMode(SiteScopeMode.ALL_SITES);
        } else if (Role.ADMIN.getCode().equals(roleCode) || Role.USER.getCode().equals(roleCode)) {
            if (request.getSiteIds() == null || request.getSiteIds().isEmpty()) {
                throw new ApiException("error.site.required", "siteIds required for ADMIN and USER invites");
            }
            membership.setSiteScopeMode(SiteScopeMode.SELECTED);
        } else {
            membership.setSiteScopeMode(SiteScopeMode.SELECTED);
        }

        membership = membershipRepository.save(membership);

        if (membership.getSiteScopeMode() == SiteScopeMode.SELECTED && request.getSiteIds() != null) {
            for (String siteIdStr : request.getSiteIds()) {
                java.util.UUID siteUuid = java.util.UUID.fromString(siteIdStr);
                Site site = siteRepository.findByIdAndAccountId(siteUuid, accountId)
                    .orElseThrow(() -> new ApiException("error.site.notfound", siteIdStr));
                MembershipSite msLink = new MembershipSite();
                msLink.setMembership(membership);
                msLink.setSite(site);
                membershipSiteRepository.save(msLink);
            }
        }
        
        logger.info("User {} invited to account {} with role {}", request.getEmailAddress(), accountId, role.getCode());
        
        // Generate activation token and trigger notification job
        try {
            ActivationToken activationToken = activationTokenService.generateToken(user.getEmailAddress(), accountId);
            String domain = account.getDomain();
            
            // Build activation URL in subdomain.domain format
            boolean isHttps = activationUrlBase.startsWith("https://");
            String protocol = isHttps ? "https://" : "http://";
            
            // Extract port if present in base URL (for local development)
            String port = "";
            if (activationUrlBase.contains(":8080") || activationUrlBase.contains(":80") || activationUrlBase.contains(":443")) {
                String baseHost = activationUrlBase.replace("http://", "").replace("https://", "").split("/")[0];
                if (baseHost.contains(":")) {
                    port = ":" + baseHost.substring(baseHost.indexOf(":") + 1);
                }
            }
            
            // Construct URL as subdomain.domain format
            String activationUrl;
            if ("localhost".equals(appDomainName)) {
                // Local development: http://{subdomain}.localhost:8080
                activationUrl = String.format("%s%s.%s%s/activate?token=%s", 
                    protocol, domain, appDomainName, port.isEmpty() ? ":8080" : port, activationToken.getToken());
            } else {
                // Production: https://{subdomain}.{domain}
                activationUrl = String.format("%s%s.%s/activate?token=%s", 
                    protocol, domain, appDomainName, activationToken.getToken());
            }
            
            // Prepare template data for invite email with activation link
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
            templateData.put("lastName", user.getLastName() != null ? user.getLastName() : "");
            templateData.put("fullName", (user.getFirstName() != null ? user.getFirstName() : "") + " " + 
                (user.getLastName() != null ? user.getLastName() : "").trim());
            templateData.put("email", user.getEmailAddress());
            templateData.put("activationUrl", activationUrl);
            templateData.put("activationToken", activationToken.getToken());
            templateData.put("domain", domain);
            
            // Enqueue email notification job
            Map<String, Object> jobMetadata = new HashMap<>();
            jobMetadata.put("recipient", user.getEmailAddress());
            jobMetadata.put("type", "templated");
            jobMetadata.put("templateId", inviteTemplateId);
            jobMetadata.put("templateData", templateData);
            
            jobQueueService.enqueueJob(BatchJobType.EMAIL_NOTIFICATION, user.getId(), accountId, jobMetadata);
            logger.info("Enqueued invite email notification job for user {} in account {}", user.getEmailAddress(), accountId);
        } catch (Exception e) {
            logger.error("Failed to generate activation token or enqueue notification job for user {}: {}", 
                user.getEmailAddress(), e.getMessage(), e);
            // Don't fail the invite if notification fails - log and continue
        }
        
        // Build response
        return MemberResponse.builder()
            .userId(membership.getUser().getId())
            .emailAddress(membership.getUser().getEmailAddress())
            .firstName(membership.getUser().getFirstName())
            .lastName(membership.getUser().getLastName())
            .roleId(role.getId())
            .roleCode(role.getCode())
            .roleName(role.getName())
            .status(membership.getStatus())
            .lastActiveAt(null)
            .twoFactorEnabled(false)
            .invitedAt(membership.getCreatedAt())
            .invitedBy(actorUserId)
            .build();
    }
    
    @Override
    @Transactional
    public void removeMember(Long accountId, Long userId) {
        Long actorUserId = getActorUserId();
        if (actorUserId == null) {
            throw new ApiException("error.authorization", "User context not found");
        }
        
        // Check authorization
        if (!authorizationService.hasPrivilege(actorUserId, accountId, "user.update")) {
            logger.warn("User {} does not have permission to remove members in account {}", actorUserId, accountId);
            throw new ApiException("error.authorization", "Insufficient permissions to remove member");
        }
        
        ensureAccountProduct(accountId);
        
        Membership membership = membershipRepository
            .findByUserIdAndAccountIdWithRole(userId, accountId)
            .orElseThrow(() -> new ApiException("error.user.notfound", "Member not found"));
        
        // Prevent removing ORG_ADMIN members (or require special privilege)
        RoleEntity role = membership.getRole();
        if (role != null && "ORG_ADMIN".equals(role.getCode())) {
            // Check if this is the last ORG_ADMIN
            long orgAdminCount = membershipRepository.findByAccountId(accountId).stream()
                .filter(m -> m.getRole() != null && "ORG_ADMIN".equals(m.getRole().getCode()))
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .count();
            if (orgAdminCount <= 1) {
                throw new ApiException("error.member.cannot.remove.last.admin", "Cannot remove the last organization administrator");
            }
        }
        
        // Soft delete: set status to REMOVED
        membership.setStatus(MembershipStatus.REMOVED);
        membershipRepository.save(membership);
        
        // Invalidate all sessions for the user whose membership was removed
        // This ensures they are logged out immediately
        try {
            sessionService.invalidateAllUserSessions(userId);
            logger.info("Invalidated all sessions for user {} after membership removal from account {}", userId, accountId);
        } catch (Exception e) {
            logger.error("Failed to invalidate sessions for user {} after membership removal: {}", userId, e.getMessage(), e);
            // Don't fail the membership removal if session invalidation fails
        }
        
        logger.info("User {} removed from account {}", userId, accountId);
    }

    private void ensureAccountProduct(Long accountId) {
        // Use findByIdWithRelations to eagerly fetch Product to avoid lazy loading issues
        var account = accountRepository.findByIdWithRelations(accountId)
            .orElseThrow(() -> {
                logger.error("Account not found: {}", accountId);
                return new ApiException("error.account.notfound");
            });
        var acctProduct = account.getProduct();
        if (acctProduct == null) {
            logger.error("Account {} does not have a product assigned", accountId);
            throw new ApiException("error.account.product.missing", "Account does not have a product assigned");
        }
        if (acctProduct.getCode() == null || acctProduct.getCode().trim().isEmpty()) {
            logger.error("Account {} has a product (id={}) but product code is null or empty", 
                accountId, acctProduct.getId());
            throw new ApiException("error.account.product.invalid", "Account product code is invalid");
        }
        logger.debug("Account {} validated with product: {} ({})", accountId, acctProduct.getCode(), acctProduct.getId());
    }
}
