package com.nfcplatform.common.exception;

import org.springframework.http.HttpStatus;

public class SubscriptionExpiredException extends ApiException {
    public SubscriptionExpiredException(String message) {
        super(HttpStatus.FORBIDDEN, "SUBSCRIPTION_EXPIRED", message);
    }
}
