package com.delight.account.exception;

public class AccountCreationException extends SignupException {
    public AccountCreationException() {
        super("error.signup.failed");
    }
}
