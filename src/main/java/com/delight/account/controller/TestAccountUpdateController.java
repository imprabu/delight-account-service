package com.delight.account.controller;

import com.delight.account.model.Account;
import com.delight.account.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Test controller to update an account and verify message posting.
 * This is for testing purposes only.
 */
@RestController
@RequestMapping("/test")
public class TestAccountUpdateController {

    @Autowired
    private AccountRepository accountRepository;

    @PostMapping("/accounts/{accountId}/update")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateAccount(@PathVariable Long accountId) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        
        if (accountOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Account not found");
            return ResponseEntity.notFound().build();
        }
        
        Account account = accountOpt.get();
        String oldName = account.getCompanyName();
        String newName = oldName + "_updated_" + System.currentTimeMillis();
        
        account.setCompanyName(newName);
        accountRepository.save(account);
        accountRepository.flush();
        
        response.put("success", true);
        response.put("accountId", accountId);
        response.put("oldCompanyName", oldName);
        response.put("newCompanyName", newName);
        response.put("message", "Account updated. Check logs for message posting.");
        
        return ResponseEntity.ok(response);
    }
}

