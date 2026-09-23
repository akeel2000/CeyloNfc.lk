package com.nfcplatform.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.role.repository.RoleRepository;
import com.nfcplatform.user.entity.User;
import com.nfcplatform.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end auth flow against a real MySQL instance (Testcontainers), verifying the
 * login -> me -> logout cycle and cookie-based session handling described in SECURITY.md.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthFlowIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("nfc_platform_test")
            .withUsername("nfc_app")
            .withPassword("nfc_app_password");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String RAW_PASSWORD = "S3cure!Passw0rd";

    @BeforeEach
    void seedUser() {
        if (userRepository.existsByEmailIgnoreCase("admin@test.local")) {
            return;
        }
        Role superAdmin = roleRepository.findByCode(RoleCode.SUPER_ADMIN).orElseThrow();
        User user = new User();
        user.setEmail("admin@test.local");
        user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
        user.setRoles(Set.of(superAdmin));
        userRepository.save(user);
    }

    @Test
    void loginMeLogoutFlowWorks() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "admin@test.local",
                                "password", RAW_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("admin@test.local"))
                .andExpect(jsonPath("$.data.roles[0]").value("SUPER_ADMIN"))
                .andReturn();

        Cookie accessTokenCookie = loginResult.getResponse().getCookie("access_token");
        assertThat(accessTokenCookie).isNotNull();
        assertThat(accessTokenCookie.isHttpOnly()).isTrue();

        Cookie refreshTokenCookie = loginResult.getResponse().getCookie("refresh_token");
        assertThat(refreshTokenCookie).isNotNull();
        assertThat(refreshTokenCookie.isHttpOnly()).isTrue();

        mockMvc.perform(get("/api/v1/auth/me").cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin@test.local"));

        mockMvc.perform(post("/api/v1/auth/logout").cookie(accessTokenCookie, refreshTokenCookie))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "admin@test.local",
                                "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
