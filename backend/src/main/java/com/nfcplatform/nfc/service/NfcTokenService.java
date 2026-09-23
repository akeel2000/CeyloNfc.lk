package com.nfcplatform.nfc.service;

import com.nfcplatform.common.util.HashUtil;
import com.nfcplatform.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Generates and hashes secure NFC/QR tokens. Raw tokens are never persisted - only
 * SHA-256(token + server pepper) is stored, so a database leak alone cannot yield
 * working redirect URLs (see docs/SECURITY.md "NFC / QR token security").
 */
@Service
@RequiredArgsConstructor
public class NfcTokenService {

    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final int TOKEN_LENGTH = 20;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppProperties appProperties;

    public String generateRawToken() {
        StringBuilder sb = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public String hash(String rawToken) {
        return HashUtil.sha256Hex(rawToken + appProperties.getSecurity().getNfcTokenPepper());
    }

    /** Cheap pre-check before hitting the database - rejects obviously-invalid input. */
    public boolean isValidFormat(String token) {
        if (token == null || token.length() != TOKEN_LENGTH) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            if (ALPHABET.indexOf(token.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }
}
