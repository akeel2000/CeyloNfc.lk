package com.nfcplatform.security.jwt;

import com.nfcplatform.security.CookieUtil;
import com.nfcplatform.security.PrincipalFactory;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final CookieUtil cookieUtil;
    private final UserRepository userRepository;
    private final PrincipalFactory principalFactory;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = cookieUtil.readCookie(request, CookieUtil.ACCESS_TOKEN_COOKIE);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parseClaims(token);
                String userUuid = claims.getSubject();
                userRepository.findByUuidWithRolesAndPermissions(userUuid).ifPresent(user -> {
                    UserPrincipal principal = principalFactory.build(user);
                    if (principal.isEnabled() && principal.isAccountNonLocked()) {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                });
            } catch (JwtException e) {
                log.debug("Rejected invalid access token: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
