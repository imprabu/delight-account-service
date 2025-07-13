package com.delight.account.exception;

public class SignupException extends ApiException {
    public SignupException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
