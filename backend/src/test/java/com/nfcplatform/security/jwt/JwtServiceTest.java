package com.nfcplatform.security.jwt;

import com.nfcplatform.config.AppProperties;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.user.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret("test-only-secret-test-only-secret-32bytes");
        props.getJwt().setAccessTokenExpirationMs(900_000);
        props.getJwt().setRefreshTokenExpirationMs(2_592_000_000L);
        jwtService = new JwtService(props);
    }

    private UserPrincipal principal(RoleCode roleCode) {
        Role role = new Role();
        role.setCode(roleCode);
        role.setName(roleCode.name());

        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("hash");
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return new UserPrincipal(user);
    }

    @Test
    void generatesTokenWithMinimalClaims() {
        UserPrincipal principal = principal(RoleCode.CLIENT);
        String token = jwtService.generateAccessToken(principal, "session-123");

        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo(principal.getUuid());
        assertThat(claims.get("role")).isEqualTo("CLIENT");
        assertThat(claims.get("sid")).isEqualTo("session-123");
        // No PII beyond the opaque user UUID should ever be embedded in the token.
        assertThat(claims.keySet()).containsExactlyInAnyOrder("sub", "role", "sid", "iat", "exp");
    }

    @Test
    void opaqueTokensAreHighEntropyAndUnique() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String token = jwtService.generateOpaqueToken();
            assertThat(token.length()).isGreaterThanOrEqualTo(48);
            assertThat(generated.add(token)).isTrue();
        }
    }
}
