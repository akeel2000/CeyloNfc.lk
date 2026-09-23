package com.nfcplatform.common.exception;

import org.springframework.http.HttpStatus;

public class NfcCardInactiveException extends ApiException {
    public NfcCardInactiveException(String message) {
        super(HttpStatus.GONE, "NFC_CARD_INACTIVE", message);
    }
}
