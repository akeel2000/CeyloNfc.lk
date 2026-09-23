package com.nfcplatform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.entity.ClientStatus;
import com.nfcplatform.client.entity.ClientType;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.destination.entity.Destination;
import com.nfcplatform.destination.entity.DestinationType;
import com.nfcplatform.destination.repository.DestinationRepository;
import com.nfcplatform.menu.entity.Menu;
import com.nfcplatform.menu.entity.MenuCategory;
import com.nfcplatform.menu.repository.MenuCategoryRepository;
import com.nfcplatform.menu.repository.MenuRepository;
import com.nfcplatform.nfc.entity.NfcCard;
import com.nfcplatform.nfc.entity.NfcCardStatus;
import com.nfcplatform.nfc.repository.NfcCardRepository;
import com.nfcplatform.nfc.service.NfcTokenService;
import com.nfcplatform.qr.entity.QrCode;
import com.nfcplatform.qr.repository.QrCodeRepository;
import com.nfcplatform.review.entity.GoogleReviewLocation;
import com.nfcplatform.review.repository.GoogleReviewLocationRepository;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.role.repository.RoleRepository;
import com.nfcplatform.support.entity.SupportMessage;
import com.nfcplatform.support.entity.SupportTicket;
import com.nfcplatform.support.repository.SupportMessageRepository;
import com.nfcplatform.support.repository.SupportTicketRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The critical security test described in docs/SECURITY.md#critical-security-test: Client A,
 * authenticated, must never be able to read or write Client B's resources by guessing/reusing
 * a UUID. Every client-scoped repository lookup in the codebase is written as
 * findByUuidAndClientId(...) rather than findByUuid(...) followed by an after-the-fact check -
 * this test is the regression guard for that invariant.
 * <p>
 * NOTE: requires a real Docker daemon reachable via Testcontainers. In this project's dev
 * environment (Windows + Docker Desktop) Testcontainers has a documented npipe protocol
 * mismatch (see docs/PROJECT_PROGRESS.md "Known Issues") that prevents this test from
 * executing locally even though `docker` CLI commands work fine - it is written to run in CI
 * (Linux runner, standard Docker socket). The same invariant was verified empirically against
 * the live dev stack via curl for this exact scenario (two real client accounts, five resource
 * types, cross-tenant GET/PATCH/PUT all returned 404) before this test was written.
 * <p>
 * Profile and Analytics are intentionally not covered here: their client-facing endpoints
 * (GET/PUT /client/profile, GET /client/analytics/summary) take no resource id at all - they
 * are always resolved from the authenticated principal's own client, so cross-tenant access
 * is structurally impossible rather than something to test for. Destination has no
 * client-facing endpoint yet (admin-only today), so it is not applicable either.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TenantIsolationTest {

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
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private DestinationRepository destinationRepository;
    @Autowired
    private NfcCardRepository nfcCardRepository;
    @Autowired
    private QrCodeRepository qrCodeRepository;
    @Autowired
    private NfcTokenService nfcTokenService;
    @Autowired
    private GoogleReviewLocationRepository reviewLocationRepository;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private MenuCategoryRepository menuCategoryRepository;
    @Autowired
    private SupportTicketRepository supportTicketRepository;
    @Autowired
    private SupportMessageRepository supportMessageRepository;

    private static final String RAW_PASSWORD = "S3cure!Passw0rd";

    private String clientAEmail;
    private Cookie clientAAccessToken;

    private String nfcCardBUuid;
    private String qrCodeBUuid;
    private String reviewLocationBUuid;
    private String menuCategoryBUuid;
    private String supportTicketBUuid;

    @BeforeEach
    void seedTwoTenantsWithOwnedResources() throws Exception {
        Role clientRole = roleRepository.findByCode(RoleCode.CLIENT).orElseThrow();

        clientAEmail = "tenant-a-" + System.nanoTime() + "@test.local";
        User ownerA = persistUser(clientAEmail, clientRole);
        Client clientA = persistClient("Tenant A Co", ownerA.getId());

        String clientBEmail = "tenant-b-" + System.nanoTime() + "@test.local";
        User ownerB = persistUser(clientBEmail, clientRole);
        Client clientB = persistClient("Tenant B Co", ownerB.getId());

        // Client B's resources - exactly what Client A will attempt to read/modify.
        Destination destinationB = new Destination();
        destinationB.setClientId(clientB.getId());
        destinationB.setName("Tenant B Destination");
        destinationB.setType(DestinationType.WEBSITE);
        destinationB.setExternalUrl("https://tenant-b.example.com");
        destinationB = destinationRepository.save(destinationB);

        NfcCard cardB = new NfcCard();
        cardB.setSerialNumber("TENANT-B-" + System.nanoTime());
        cardB.setTokenHash(nfcTokenService.hash(nfcTokenService.generateRawToken()));
        cardB.setClientId(clientB.getId());
        cardB.setDestinationId(destinationB.getId());
        cardB.setStatus(NfcCardStatus.ACTIVE);
        nfcCardBUuid = nfcCardRepository.save(cardB).getUuid();

        QrCode qrB = new QrCode();
        qrB.setClientId(clientB.getId());
        qrB.setDestinationId(destinationB.getId());
        qrB.setTokenHash(nfcTokenService.hash(nfcTokenService.generateRawToken()));
        qrB.setName("Tenant B QR");
        qrCodeBUuid = qrCodeRepository.save(qrB).getUuid();

        GoogleReviewLocation locationB = new GoogleReviewLocation();
        locationB.setClientId(clientB.getId());
        locationB.setBusinessName("Tenant B Shop");
        locationB.setGoogleReviewUrl("https://g.page/r/tenant-b/review");
        reviewLocationBUuid = reviewLocationRepository.save(locationB).getUuid();

        Menu menuB = new Menu();
        menuB.setClientId(clientB.getId());
        menuB.setSlug("tenant-b-" + System.nanoTime());
        menuB.setName("Tenant B Menu");
        menuB = menuRepository.save(menuB);

        MenuCategory categoryB = new MenuCategory();
        categoryB.setMenuId(menuB.getId());
        categoryB.setName("Tenant B Category");
        menuCategoryBUuid = menuCategoryRepository.save(categoryB).getUuid();

        SupportTicket ticketB = new SupportTicket();
        ticketB.setClientId(clientB.getId());
        ticketB.setSubject("Tenant B confidential ticket");
        ticketB = supportTicketRepository.save(ticketB);
        supportTicketBUuid = ticketB.getUuid();

        SupportMessage messageB = new SupportMessage();
        messageB.setTicketId(ticketB.getId());
        messageB.setSenderUserId(ownerB.getId());
        messageB.setBody("This message must never be visible to Tenant A");
        supportMessageRepository.save(messageB);

        clientAAccessToken = login(clientAEmail);
    }

    private User persistUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
        user.setRoles(Set.of(role));
        return userRepository.save(user);
    }

    private Client persistClient(String displayName, Long ownerUserId) {
        Client client = new Client();
        client.setType(ClientType.BUSINESS);
        client.setStatus(ClientStatus.ACTIVE);
        client.setDisplayName(displayName);
        client.setEmail(displayName.toLowerCase().replace(" ", "-") + "@test.local");
        client.setOwnerUserId(ownerUserId);
        return clientRepository.save(client);
    }

    private Cookie login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", RAW_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie accessToken = result.getResponse().getCookie("access_token");
        if (accessToken == null) {
            throw new IllegalStateException("Login did not return an access_token cookie for " + email);
        }
        return accessToken;
    }

    @Test
    void clientCannotReadAnotherClientsNfcCard() throws Exception {
        mockMvc.perform(get("/api/v1/client/nfc-cards/" + nfcCardBUuid).cookie(clientAAccessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void clientCannotModifyAnotherClientsQrCode() throws Exception {
        mockMvc.perform(patch("/api/v1/client/qr-codes/" + qrCodeBUuid + "/status")
                        .cookie(clientAAccessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void clientCannotModifyAnotherClientsReviewLocation() throws Exception {
        mockMvc.perform(put("/api/v1/client/google-reviews/" + reviewLocationBUuid)
                        .cookie(clientAAccessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "businessName", "Hijacked", "googleReviewUrl", "https://evil.example.com"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void clientCannotModifyAnotherClientsMenuCategory() throws Exception {
        mockMvc.perform(put("/api/v1/client/menu/categories/" + menuCategoryBUuid)
                        .cookie(clientAAccessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "Hijacked", "sortOrder", 0))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void clientCannotReadAnotherClientsSupportTicket() throws Exception {
        mockMvc.perform(get("/api/v1/client/support/" + supportTicketBUuid).cookie(clientAAccessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("This message must never be visible to Tenant A"))));
    }
}
