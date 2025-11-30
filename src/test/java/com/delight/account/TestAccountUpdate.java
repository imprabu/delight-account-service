package com.delight.account;

import com.delight.account.model.Account;
import com.delight.account.model.AccountStatus;
import com.delight.account.model.PlanType;
import com.delight.account.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Simple test class to update an account and trigger the account update event.
 * Run this with: ./gradlew bootRun --args='--spring.main.web-application-type=none --spring.profiles.active=test'
 * Or run as a test: ./gradlew test --tests TestAccountUpdate
 */
@SpringBootApplication
public class TestAccountUpdate implements CommandLineRunner {

    @Autowired
    private AccountRepository accountRepository;

    public static void main(String[] args) {
        SpringApplication.run(TestAccountUpdate.class, args);
    }

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println("=== Testing Account Update Message Posting ===");
        
        // Find an existing account
        Optional<Account> accountOpt = accountRepository.findById(7L);
        
        if (accountOpt.isEmpty()) {
            System.out.println("❌ Account with ID 7 not found. Creating a test account...");
            // Create a test account
            Account account = new Account();
            account.setDomain("testdomain" + System.currentTimeMillis());
            account.setPlan(PlanType.TRIAL);
            account.setStatus(AccountStatus.ACTIVE);
            account.setIndustryType("Technology");
            account.setEmail("test@example.com");
            account.setCompanyName("Test Company");
            account = accountRepository.save(account);
            accountRepository.flush();
            System.out.println("✅ Created test account with ID: " + account.getId());
            
            // Now update it
            account.setCompanyName("Updated Test Company " + System.currentTimeMillis());
            accountRepository.save(account);
            accountRepository.flush();
            System.out.println("✅ Updated account ID: " + account.getId());
        } else {
            Account account = accountOpt.get();
            System.out.println("✅ Found account ID: " + account.getId());
            System.out.println("   Current company name: " + account.getCompanyName());
            
            // Update the account
            String newName = account.getCompanyName() + "_updated_" + System.currentTimeMillis();
            account.setCompanyName(newName);
            accountRepository.save(account);
            accountRepository.flush();
            System.out.println("✅ Updated account company name to: " + newName);
        }
        
        System.out.println("✅ Account update completed. Check logs for message posting.");
        System.out.println("   Look for logs with: ACCOUNT-EVENT, MESSAGING, or ACCOUNT-EVENT-WORKER");
    }
}

