package com.nfcplatform.nfc.dto;

/**
 * rawToken and publicUrl are returned only once, at registration time, for writing to
 * the physical chip. They cannot be retrieved again afterwards (see SECURITY.md).
 */
public record NfcCardRegisterResponse(
        NfcCardResponse card,
        String rawToken,
        String publicUrl
) {
}
