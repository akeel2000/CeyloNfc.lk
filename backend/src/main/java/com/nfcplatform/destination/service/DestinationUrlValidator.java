package com.nfcplatform.destination.service;

import com.nfcplatform.common.exception.ValidationException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Open-redirect protection (docs/SECURITY.md): only http/https with a non-empty host are
 * ever accepted for a destination's external_url. Validated at write time so bad data can
 * never reach the public redirect hot path.
 */
public final class DestinationUrlValidator {

    private DestinationUrlValidator() {
    }

    public static void validate(String url) {
        if (url == null || url.isBlank()) {
            throw new ValidationException("Destination URL is required");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new ValidationException("Destination URL is malformed");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new ValidationException("Destination URL must use http or https");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new ValidationException("Destination URL must include a valid host");
        }
    }
}
