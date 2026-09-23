package com.nfcplatform.destination.service;

import com.nfcplatform.common.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the open-redirect protection at destination write time (docs/SECURITY.md).
 * Every case here is something a malicious client could submit as a destination's
 * external_url, so each is deliberately exercised against the real validator rather than
 * inferred from reading the code.
 */
class DestinationUrlValidatorTest {

    @Test
    void rejectsANullUrl() {
        assertThatThrownBy(() -> DestinationUrlValidator.validate(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsABlankUrl() {
        assertThatThrownBy(() -> DestinationUrlValidator.validate("   ")).isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "ftp://example.com/file"})
    void rejectsNonHttpSchemes(String url) {
        assertThatThrownBy(() -> DestinationUrlValidator.validate(url)).isInstanceOf(ValidationException.class)
                .hasMessageContaining("http or https");
    }

    @Test
    void rejectsADataUrlAsMalformedRatherThanAcceptingIt() {
        // Contains characters java.net.URI itself rejects as illegal (<, >), so this is caught
        // by the URISyntaxException branch rather than the scheme check - still correctly
        // rejected either way, just via a different message.
        assertThatThrownBy(() -> DestinationUrlValidator.validate("data:text/html,<script>alert(1)</script>"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsAUrlWithNoHost() {
        assertThatThrownBy(() -> DestinationUrlValidator.validate("https:///path-only"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsAMalformedUri() {
        assertThatThrownBy(() -> DestinationUrlValidator.validate("http://[not-valid"))
                .isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com", "http://example.com/page?query=1", "https://sub.example.com:8080/path"})
    void acceptsValidHttpAndHttpsUrls(String url) {
        assertThatCode(() -> DestinationUrlValidator.validate(url)).doesNotThrowAnyException();
    }
}
