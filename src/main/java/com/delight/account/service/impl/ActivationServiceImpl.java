package com.delight.account.service.impl;

import com.delight.account.dto.ActivationRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.*;
import com.delight.account.repository.UserCredentialRepository;
import com.delight.account.repository.UserRepository;
import com.delight.account.service.ActivationService;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ActivationServiceImpl implements ActivationService {

    private static final Logger logger = LoggerFactory.getLogger(ActivationServiceImpl.class);

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final String secretKey;

    public ActivationServiceImpl(UserRepository userRepository,
                                 UserCredentialRepository credentialRepository,
                                 @Value("${security.password.secret}") String secretKey) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.secretKey = secretKey;
    }

    @Override
    @Transactional
    public void activateUser(Long accountId, ActivationRequest request, String country) {
        logger.info("Activation request accountId={} emailAddress={} firstName={} lastName={} phoneNumber={} country={}",
            accountId, request.getEmailAddress(), request.getFirstName(), request.getLastName(), request.getPhoneNumber(), country);
        if (!request.getPassword().equals(request.getRetypePassword())) {
            throw new ApiException("error.validation");
        }

        Account account = new Account();
        account.setId(accountId);

        User user = userRepository
            .findByAccountIdAndEmailAddress(accountId, request.getEmailAddress())
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setAccount(account);
                newUser.setEmailAddress(request.getEmailAddress());
                return newUser;
            });
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
    }

    private String encrypt(String value) {
        try {
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
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
