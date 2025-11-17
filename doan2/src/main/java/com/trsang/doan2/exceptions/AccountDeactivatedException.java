package com.trsang.doan2.exceptions;

import org.springframework.security.core.AuthenticationException;

public class AccountDeactivatedException extends AuthenticationException {
    private static final long serialVersionUID = 1L;

    public AccountDeactivatedException(String message) {
        super(message);
    }

    public AccountDeactivatedException(String message, Throwable cause) {
        super(message, cause);
    }

}
