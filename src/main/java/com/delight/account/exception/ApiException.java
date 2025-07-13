package com.delight.account.exception;

public class ApiException extends RuntimeException {
    private final String messageKey;
    private final Object[] args;

    public ApiException(String messageKey, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
