package com.delight.account.service.impl;

import com.delight.account.dto.SigninRequest;
import com.delight.account.exception.ApiException;
import com.delight.account.model.Account;
import com.delight.account.model.User;
import com.delight.account.model.UserCredential;
import com.delight.account.repository.UserCredentialRepository;
import com.delight.account.repository.UserRepository;
import com.delight.account.service.SigninService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SigninServiceImpl implements SigninService {

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final String passwordSecret;
    private final String jwtSecret;
    private final long jwtExpiration;

    public SigninServiceImpl(UserRepository userRepository,
                             UserCredentialRepository credentialRepository,
                             @Value("${security.password.secret}") String passwordSecret,
                             @Value("${security.jwt.secret}") String jwtSecret,
                             @Value("${security.jwt.expiration}") long jwtExpiration) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.passwordSecret = passwordSecret;
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;
    }

    @Override
    public String signin(Long accountId, SigninRequest request) {
        User user = userRepository
            .findByAccountIdAndEmailAddress(accountId, request.getEmailAddress())
            .orElseThrow(() -> new ApiException("error.validation"));

        UserCredential credential = credentialRepository.findByUser(user)
            .orElseThrow(() -> new ApiException("error.validation"));

        String encrypted = encrypt(request.getPassword());
        if (!encrypted.equals(credential.getPasswordHash())) {
            throw new ApiException("error.validation");
        }

        return Jwts.builder()
            .setSubject(user.getEmailAddress())
            .claim("accountId", accountId)
            .claim("userId", user.getId())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), io.jsonwebtoken.SignatureAlgorithm.HS256)
            .compact();
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

