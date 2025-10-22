package com.delight.account.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    
    // Password must contain at least one uppercase, one lowercase, one digit, and one special character
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{" + MIN_LENGTH + "," + MAX_LENGTH + "}$"
    );
    
    // Common weak passwords to reject
    private static final String[] WEAK_PASSWORDS = {
        "password", "123456", "password123", "admin", "qwerty", "letmein",
        "welcome", "monkey", "1234567890", "password1", "123456789"
    };

    /**
     * Validates password strength according to security requirements
     * @param password the password to validate
     * @return true if password meets strength requirements
     */
    public boolean isValid(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        
        // Check length
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return false;
        }
        
        // Check pattern (complexity requirements)
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return false;
        }
        
        // Check against common weak passwords
        String lowerPassword = password.toLowerCase();
        for (String weakPassword : WEAK_PASSWORDS) {
            if (lowerPassword.contains(weakPassword)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Gets a user-friendly error message for password validation failures
     * @param password the password that failed validation
     * @return error message describing what's wrong with the password
     */
    public String getValidationError(String password) {
        if (password == null || password.trim().isEmpty()) {
            return "Password cannot be empty";
        }
        
        if (password.length() < MIN_LENGTH) {
            return "Password must be at least " + MIN_LENGTH + " characters long";
        }
        
        if (password.length() > MAX_LENGTH) {
            return "Password must not exceed " + MAX_LENGTH + " characters";
        }
        
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter";
        }
        
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter";
        }
        
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one digit";
        }
        
        if (!password.matches(".*[@$!%*?&].*")) {
            return "Password must contain at least one special character (@$!%*?&)";
        }
        
        // Check against common weak passwords
        String lowerPassword = password.toLowerCase();
        for (String weakPassword : WEAK_PASSWORDS) {
            if (lowerPassword.contains(weakPassword)) {
                return "Password contains common weak patterns and is not allowed";
            }
        }
        
        return "Password does not meet security requirements";
    }
}