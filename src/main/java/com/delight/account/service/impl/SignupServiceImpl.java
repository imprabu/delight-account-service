package com.delight.account.service.impl;

import com.delight.account.dto.SignupRequest;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.model.PlanType;
import com.delight.account.model.User;
import com.delight.account.model.UserStatus;
import com.delight.account.model.Membership;
import com.delight.account.model.MembershipStatus;
import com.delight.account.model.Role;
import com.delight.account.model.Organization;
import com.delight.account.model.OrganizationStatus;
import com.delight.account.model.Product;
import com.delight.account.exception.AccountCreationException;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.UserRepository;
import com.delight.account.repository.MembershipRepository;
import com.delight.account.repository.OrganizationRepository;
import com.delight.account.repository.ProductRepository;
import com.delight.account.service.SignupService;
import java.util.Random;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignupServiceImpl implements SignupService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();
    private final MembershipRepository membershipRepository;
    private final OrganizationRepository organizationRepository;
    private final ProductRepository productRepository;

    public SignupServiceImpl(AccountRepository accountRepository, UserRepository userRepository,
                             MembershipRepository membershipRepository,
                             OrganizationRepository organizationRepository,
                             ProductRepository productRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.organizationRepository = organizationRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void signup(SignupRequest request) {
        try {
            String domain = request.getCompanyName().toLowerCase().replaceAll("\\s+", "");

            if (accountRepository.findByDomainAndStatus(domain, AccountStatus.ACTIVE).isPresent()) {
                domain = domain + random.nextInt(90000) + 10000;
            }
            // Create or find product HRMS
            Product product = productRepository.findByCode("HRMS")
                .orElseGet(() -> {
                    Product p = new Product();
                    p.setCode("HRMS");
                    p.setName("HRMS");
                    return productRepository.save(p);
                });

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
            account.setPlan(PlanType.TRIAL);
            account.setStatus(AccountStatus.ACTIVE);
            account.setIndustryType(request.getIndustryType());
            account.setEmail(request.getEmailAddress());
            account.setCompanyName(request.getCompanyName());
            accountRepository.save(account);

            User user = new User();
            user.setAccount(account);
            user.setUserName(request.getFirstName() + request.getLastName());
            user.setEmailAddress(request.getEmailAddress());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setCompanyName(request.getCompanyName());
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);

            Membership membership = new Membership();
            membership.setAccount(account);
            membership.setUser(user);
            membership.setRole(Role.OWNER);
            membership.setStatus(MembershipStatus.ACTIVE);
            membershipRepository.save(membership);
        } catch (DataAccessException ex) {
            throw new AccountCreationException();
        }
    }
}
