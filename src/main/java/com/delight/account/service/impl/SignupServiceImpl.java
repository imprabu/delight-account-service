package com.delight.account.service.impl;

import com.delight.account.dto.SignupRequest;
import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.model.PlanType;
import com.delight.account.model.User;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.UserRepository;
import com.delight.account.service.SignupService;
import java.util.Random;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignupServiceImpl implements SignupService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    public SignupServiceImpl(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void signup(SignupRequest request) {
        String domain = request.getCompanyName();
        if (accountRepository.findByDomain(domain).isPresent()) {
            domain = domain + random.nextInt(90000) + 10000;
        }
        Account account = new Account();
        account.setDomain(domain);
        account.setPlanId(PlanType.TRIAL);
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
        userRepository.save(user);
    }
}
