package com.nfcplatform.common.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends ApiException {
    public AccountLockedException(String message) {
        super(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED", message);
    }
}
