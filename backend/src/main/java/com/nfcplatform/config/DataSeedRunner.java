package com.nfcplatform.config;

import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.role.repository.RoleRepository;
import com.nfcplatform.user.entity.User;
import com.nfcplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Seeds the first Super Admin account from environment variables, if none exists yet.
 * No fixed/default production password is ever baked into code or migrations - if the
 * env vars are absent, seeding is skipped entirely and an operator must create the
 * first account manually (or set the env vars and restart).
 */
@Component
@RequiredArgsConstructor
public class DataSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeedRunner.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        String email = System.getenv("SUPER_ADMIN_EMAIL");
        String password = System.getenv("SUPER_ADMIN_PASSWORD");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.info("SUPER_ADMIN_EMAIL / SUPER_ADMIN_PASSWORD not set - skipping Super Admin seed");
            return;
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.info("Super Admin account already exists for {}", email);
            return;
        }

        Role superAdminRole = roleRepository.findByCode(RoleCode.SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role missing - check Flyway migrations"));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoles(Set.of(superAdminRole));
        userRepository.save(user);

        log.info("Seeded initial Super Admin account for {}", email);
    }
}
