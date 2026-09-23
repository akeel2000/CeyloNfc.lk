package com.nfcplatform.client.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.auth.entity.RefreshToken;
import com.nfcplatform.auth.repository.RefreshTokenRepository;
import com.nfcplatform.client.dto.ClientCreateRequest;
import com.nfcplatform.client.dto.ClientCreateResponse;
import com.nfcplatform.client.dto.ClientStatusUpdateRequest;
import com.nfcplatform.client.dto.ClientUpdateRequest;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.entity.ClientStatus;
import com.nfcplatform.client.entity.ClientType;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.ConflictException;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.common.mail.EmailService;
import com.nfcplatform.config.AppProperties;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.role.repository.RoleRepository;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.user.entity.User;
import com.nfcplatform.user.entity.UserStatus;
import com.nfcplatform.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for client account creation, update-email-conflict checking, and the soft-delete
 * cascade (owner disabled + sessions revoked) - see docs/PROJECT_PROGRESS.md's
 * "RBAC admin UI, user management, client delete" entry for why the cascade exists. Pure
 * Mockito, no Spring context/DB.
 */
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;
    @Mock
    private EmailService emailService;
    @Mock
    private HttpServletRequest httpServletRequest;

    private ClientService clientService;

    private static final long CLIENT_ID = 5L;
    private static final long OWNER_USER_ID = 42L;
    private static final long ACTOR_USER_ID = 1L;
    private static final String CLIENT_UUID = "client-uuid";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AppProperties appProperties = new AppProperties();
        appProperties.setFrontendUrl("https://ceylonfc.com");
        clientService = new ClientService(clientRepository, userRepository, roleRepository, refreshTokenRepository,
                passwordEncoder, auditService, emailService, appProperties);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- createClient -----------------------------------------------------------------------

    @Test
    void createClientRejectsAnEmailAlreadyUsedByAnyUserAccount() {
        when(userRepository.existsByEmailIgnoreCase("taken@test.local")).thenReturn(true);

        assertThatThrownBy(() -> clientService.createClient(
                new ClientCreateRequest("INDIVIDUAL", "Jane Doe", "taken@test.local", null),
                principal(), httpServletRequest))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createClientRejectsAnEmailAlreadyUsedByAnActiveClient() {
        when(userRepository.existsByEmailIgnoreCase("taken@test.local")).thenReturn(false);
        when(clientRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("taken@test.local")).thenReturn(true);

        assertThatThrownBy(() -> clientService.createClient(
                new ClientCreateRequest("INDIVIDUAL", "Jane Doe", "taken@test.local", null),
                principal(), httpServletRequest))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createClientRejectsAnUnrecognizedType() {
        assertThatThrownBy(() -> clientService.createClient(
                new ClientCreateRequest("NOT_A_TYPE", "Jane Doe", "jane@test.local", null),
                principal(), httpServletRequest))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createClientLinksTheNewUserAsTheClientOwnerAndForcesAPasswordChange() {
        Role clientRole = new Role();
        clientRole.setCode(RoleCode.CLIENT);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(clientRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.CLIENT)).thenReturn(Optional.of(clientRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(OWNER_USER_ID);
            return user;
        });

        ClientCreateResponse response = clientService.createClient(
                new ClientCreateRequest("INDIVIDUAL", "Jane Doe", "jane@test.local", null),
                principal(), httpServletRequest);

        assertThat(response.temporaryPassword()).isNotBlank();
        verify(emailService).sendAccountCreatedEmail(eq("jane@test.local"), eq("https://ceylonfc.com/login"));

        ArgumentCaptor<Client> savedClient = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(savedClient.capture());
        assertThat(savedClient.getValue().getOwnerUserId()).isEqualTo(OWNER_USER_ID);
        assertThat(savedClient.getValue().getStatus()).isEqualTo(ClientStatus.ACTIVE);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().isMustChangePassword()).isTrue();
    }

    // --- updateClient -------------------------------------------------------------------------

    @Test
    void updateClientSkipsTheConflictCheckWhenTheEmailIsUnchanged() {
        Client client = client("same@test.local");
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client));

        clientService.updateClient(CLIENT_UUID,
                new ClientUpdateRequest("Jane Doe", "SAME@test.local", null), principal(), httpServletRequest);

        verify(clientRepository, never()).existsByEmailIgnoreCaseAndDeletedAtIsNull(any());
    }

    @Test
    void updateClientRejectsAnEmailAlreadyUsedByAnotherActiveClient() {
        Client client = client("old@test.local");
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(clientRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("new@test.local")).thenReturn(true);

        assertThatThrownBy(() -> clientService.updateClient(CLIENT_UUID,
                new ClientUpdateRequest("Jane Doe", "new@test.local", null), principal(), httpServletRequest))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateClientThrowsForAnUnknownOrDeletedClient() {
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.updateClient(CLIENT_UUID,
                new ClientUpdateRequest("Jane Doe", "jane@test.local", null), principal(), httpServletRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- updateStatus ---------------------------------------------------------------------------

    @Test
    void updateStatusRejectsAnUnrecognizedStatus() {
        Client client = client("jane@test.local");
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> clientService.updateStatus(CLIENT_UUID,
                new ClientStatusUpdateRequest("NOT_A_STATUS"), principal(), httpServletRequest))
                .isInstanceOf(ValidationException.class);
    }

    // --- deleteClient: soft-delete cascade -------------------------------------------------------

    @Test
    void deleteClientSoftDeletesRatherThanRemovingTheRow() {
        Client client = client("jane@test.local");
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.empty());

        clientService.deleteClient(CLIENT_UUID, principal(), httpServletRequest);

        assertThat(client.getDeletedAt()).isNotNull();
        assertThat(client.getStatus()).isEqualTo(ClientStatus.CLOSED);
    }

    @Test
    void deleteClientDisablesTheOwnerAccountAndRevokesTheirSessions() {
        Client client = client("jane@test.local");
        User owner = new User();
        owner.setId(OWNER_USER_ID);
        owner.setStatus(UserStatus.ACTIVE);
        RefreshToken activeSession = new RefreshToken();
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(userRepository.findById(OWNER_USER_ID)).thenReturn(Optional.of(owner));
        when(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(OWNER_USER_ID))
                .thenReturn(List.of(activeSession));

        clientService.deleteClient(CLIENT_UUID, principal(), httpServletRequest);

        assertThat(owner.getStatus()).isEqualTo(UserStatus.DISABLED);
        assertThat(activeSession.getRevokedAt()).isNotNull();
    }

    @Test
    void deleteClientThrowsForAnAlreadyDeletedClientRatherThanDeletingItAgain() {
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.deleteClient(CLIENT_UUID, principal(), httpServletRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Client client(String email) {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setUuid(CLIENT_UUID);
        client.setType(ClientType.INDIVIDUAL);
        client.setStatus(ClientStatus.ACTIVE);
        client.setDisplayName("Jane Doe");
        client.setEmail(email);
        client.setOwnerUserId(OWNER_USER_ID);
        return client;
    }

    private UserPrincipal principal() {
        User user = new User();
        user.setId(ACTOR_USER_ID);
        user.setEmail("actor@test.local");
        user.setPasswordHash("hash");
        Role role = new Role();
        role.setCode(RoleCode.SUPER_ADMIN);
        role.setName(RoleCode.SUPER_ADMIN.name());
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return new UserPrincipal(user);
    }
}
