package com.nfcplatform.analytics.service;

/**
 * Minimal, dependency-free User-Agent parsing - device/browser/OS family only. Never
 * stores the raw header (privacy-friendly analytics, see docs/SECURITY.md).
 */
public final class UserAgentParser {

    private UserAgentParser() {
    }

    public record ParsedUserAgent(String deviceType, String browser, String os) {
    }

    public static ParsedUserAgent parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new ParsedUserAgent("unknown", "unknown", "unknown");
        }
        String ua = userAgent.toLowerCase();

        String deviceType = "desktop";
        if (ua.contains("tablet") || ua.contains("ipad")) {
            deviceType = "tablet";
        } else if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            deviceType = "mobile";
        }

        String browser = "other";
        if (ua.contains("edg/")) browser = "edge";
        else if (ua.contains("chrome/") && !ua.contains("chromium")) browser = "chrome";
        else if (ua.contains("firefox/")) browser = "firefox";
        else if (ua.contains("safari/") && !ua.contains("chrome")) browser = "safari";

        // iOS checked before macOS: every real iPhone/iPad Safari UA includes "like Mac OS X"
        // for site-compatibility reasons (e.g. "CPU iPhone OS 17_0 like Mac OS X"), which would
        // otherwise match the macOS check first and misclassify all iOS traffic as desktop macOS.
        String os = "other";
        if (ua.contains("windows")) os = "windows";
        else if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) os = "ios";
        else if (ua.contains("mac os") || ua.contains("macos")) os = "macos";
        else if (ua.contains("android")) os = "android";
        else if (ua.contains("linux")) os = "linux";

        return new ParsedUserAgent(deviceType, browser, os);
    }
}
