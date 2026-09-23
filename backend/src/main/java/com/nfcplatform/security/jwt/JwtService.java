package com.nfcplatform.security.jwt;

import com.nfcplatform.config.AppProperties;
import com.nfcplatform.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates short-lived access JWTs. Claims are intentionally minimal
 * (subject, role, session id) - no PII is ever placed in the token payload.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    private SecretKey signingKey() {
        byte[] keyBytes = appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes.length >= 32 ? keyBytes : pad(keyBytes));
    }

    private byte[] pad(byte[] input) {
        byte[] padded = new byte[32];
        System.arraycopy(input, 0, padded, 0, Math.min(input.length, 32));
        return padded;
    }

    public String generateAccessToken(UserPrincipal principal, String sessionId) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(appProperties.getJwt().getAccessTokenExpirationMs());
        return Jwts.builder()
                .subject(principal.getUuid())
                .claim("role", principal.getRoleCodes().stream().findFirst().orElse("CLIENT"))
                .claim("sid", sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();
    }

    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpirationSeconds() {
        return appProperties.getJwt().getAccessTokenExpirationMs() / 1000;
    }

    public long getRefreshTokenExpirationSeconds() {
        return appProperties.getJwt().getRefreshTokenExpirationMs() / 1000;
    }

    /** Opaque, high-entropy refresh token value (not a JWT) - stored server-side only as a hash. */
    public String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String newSessionId() {
        return UUID.randomUUID().toString();
    }
}
