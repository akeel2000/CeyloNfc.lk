package com.nfcplatform.analytics.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAgentParserTest {

    @Test
    void nullOrBlankUserAgentParsesAsAllUnknown() {
        UserAgentParser.ParsedUserAgent parsed = UserAgentParser.parse(null);

        assertThat(parsed.deviceType()).isEqualTo("unknown");
        assertThat(parsed.browser()).isEqualTo("unknown");
        assertThat(parsed.os()).isEqualTo("unknown");
        assertThat(UserAgentParser.parse("   ").deviceType()).isEqualTo("unknown");
    }

    @Test
    void detectsAnIphoneAsMobileSafariOnIos() {
        UserAgentParser.ParsedUserAgent parsed = UserAgentParser.parse(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 "
                        + "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1");

        assertThat(parsed.deviceType()).isEqualTo("mobile");
        assertThat(parsed.browser()).isEqualTo("safari");
        assertThat(parsed.os()).isEqualTo("ios");
    }

    @Test
    void detectsAnIpadAsTabletRatherThanMobile() {
        UserAgentParser.ParsedUserAgent parsed = UserAgentParser.parse(
                "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Safari/604.1");

        assertThat(parsed.deviceType()).isEqualTo("tablet");
    }

    @Test
    void detectsChromeOnWindowsAsDesktop() {
        UserAgentParser.ParsedUserAgent parsed = UserAgentParser.parse(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/120.0.0.0 Safari/537.36");

        assertThat(parsed.deviceType()).isEqualTo("desktop");
        assertThat(parsed.browser()).isEqualTo("chrome");
        assertThat(parsed.os()).isEqualTo("windows");
    }

    @Test
    void distinguishesEdgeFromTheChromeItsBuiltOn() {
        UserAgentParser.ParsedUserAgent parsed = UserAgentParser.parse(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0");

        assertThat(parsed.browser()).isEqualTo("edge");
    }

    @Test
    void detectsAndroidMobileAndOs() {
        UserAgentParser.ParsedUserAgent parsed = UserAgentParser.parse(
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/120.0.0.0 Mobile Safari/537.36");

        assertThat(parsed.deviceType()).isEqualTo("mobile");
        assertThat(parsed.os()).isEqualTo("android");
    }

    @Test
    void detectsFirefoxOnLinuxAsDesktop() {
        UserAgentParser.ParsedUserAgent parsed = UserAgentParser.parse(
                "Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0");

        assertThat(parsed.deviceType()).isEqualTo("desktop");
        assertThat(parsed.browser()).isEqualTo("firefox");
        assertThat(parsed.os()).isEqualTo("linux");
    }

    @Test
    void detectsMacosAndSafariRatherThanFalsePositivingOnChrome() {
        UserAgentParser.ParsedUserAgent parsed = UserAgentParser.parse(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 "
                        + "(KHTML, like Gecko) Version/17.0 Safari/605.1.15");

        assertThat(parsed.browser()).isEqualTo("safari");
        assertThat(parsed.os()).isEqualTo("macos");
    }

    @Test
    void anUnrecognizableUserAgentFallsBackToOtherRatherThanThrowing() {
        UserAgentParser.ParsedUserAgent parsed = UserAgentParser.parse("SomeUnknownBot/1.0");

        assertThat(parsed.browser()).isEqualTo("other");
        assertThat(parsed.os()).isEqualTo("other");
        assertThat(parsed.deviceType()).isEqualTo("desktop");
    }
}
