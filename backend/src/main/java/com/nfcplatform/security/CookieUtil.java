package com.nfcplatform.security;

import com.nfcplatform.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    public static final String CSRF_COOKIE = "csrf_token";

    private final AppProperties appProperties;

    public void addCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds, boolean httpOnly) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(appProperties.getSecurity().isCookieSecure())
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAgeSeconds);
        if (appProperties.getJwt().getCookieDomain() != null && !appProperties.getJwt().getCookieDomain().isBlank()) {
            builder.domain(appProperties.getJwt().getCookieDomain());
        }
        response.addHeader("Set-Cookie", builder.build().toString());
    }

    public void clearCookie(HttpServletResponse response, String name) {
        addCookie(response, name, "", 0, true);
    }

    public String readCookie(jakarta.servlet.http.HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
