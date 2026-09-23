package com.nfcplatform.support.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.notification.service.NotificationService;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.support.entity.SupportTicket;
import com.nfcplatform.support.entity.TicketStatus;
import com.nfcplatform.support.repository.SupportMessageRepository;
import com.nfcplatform.support.repository.SupportTicketRepository;
import com.nfcplatform.support.dto.MessageCreateRequest;
import com.nfcplatform.user.entity.User;
import com.nfcplatform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the reply-driven status transitions described in
 * docs/TECHNICAL_DECISIONS.md#support-ticket-status-auto-transitions-on-reply. Pure Mockito,
 * no Spring context/DB.
 */
class SupportServiceTest {

    @Mock
    private SupportTicketRepository ticketRepository;
    @Mock
    private SupportMessageRepository messageRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditService auditService;

    private SupportService supportService;

    private static final long CLIENT_ID = 7L;
    private static final long OWNER_USER_ID = 100L;
    private static final String TICKET_UUID = "ticket-uuid";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        supportService = new SupportService(ticketRepository, messageRepository, clientRepository, userRepository,
                notificationService, auditService);

        when(messageRepository.findAllByTicketIdOrderByCreatedAtAsc(any())).thenReturn(Collections.emptyList());
        when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"RESOLVED", "CLOSED"})
    void clientReplyReopensAResolvedOrClosedTicket(TicketStatus startingStatus) {
        UserPrincipal clientOwner = principal(OWNER_USER_ID, RoleCode.CLIENT);
        Client client = client();
        SupportTicket ticket = ticket(startingStatus);

        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(ticketRepository.findByUuidAndClientId(TICKET_UUID, CLIENT_ID)).thenReturn(Optional.of(ticket));

        supportService.addMessageOwnClient(TICKET_UUID, new MessageCreateRequest("Any update?", null), clientOwner);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void clientReplyOnAnAlreadyOpenTicketLeavesStatusUnchanged() {
        UserPrincipal clientOwner = principal(OWNER_USER_ID, RoleCode.CLIENT);
        Client client = client();
        SupportTicket ticket = ticket(TicketStatus.IN_PROGRESS);

        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(ticketRepository.findByUuidAndClientId(TICKET_UUID, CLIENT_ID)).thenReturn(Optional.of(ticket));

        supportService.addMessageOwnClient(TICKET_UUID, new MessageCreateRequest("Following up", null), clientOwner);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void adminReplyOnAnOpenTicketMovesItToInProgress() {
        UserPrincipal admin = principal(999L, RoleCode.SUPER_ADMIN);
        Client client = client();
        SupportTicket ticket = ticket(TicketStatus.OPEN);

        when(ticketRepository.findByUuid(TICKET_UUID)).thenReturn(Optional.of(ticket));
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));

        supportService.addMessageAdmin(TICKET_UUID, new MessageCreateRequest("On it", null), admin);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(notificationService).notify(eq(OWNER_USER_ID), eq("SUPPORT_REPLY"), any(), any(), any());
    }

    @Test
    void adminReplyOnAnInProgressTicketLeavesStatusUnchanged() {
        UserPrincipal admin = principal(999L, RoleCode.SUPER_ADMIN);
        Client client = client();
        SupportTicket ticket = ticket(TicketStatus.IN_PROGRESS);

        when(ticketRepository.findByUuid(TICKET_UUID)).thenReturn(Optional.of(ticket));
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));

        supportService.addMessageAdmin(TICKET_UUID, new MessageCreateRequest("Still working on it", null), admin);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void fromSupportStaffIsDerivedFromSenderIdNotStored() {
        UserPrincipal clientOwner = principal(OWNER_USER_ID, RoleCode.CLIENT);
        Client client = client();
        SupportTicket ticket = ticket(TicketStatus.OPEN);

        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(ticketRepository.findByUuidAndClientId(TICKET_UUID, CLIENT_ID)).thenReturn(Optional.of(ticket));

        ArgumentCaptor<com.nfcplatform.support.entity.SupportMessage> messageCaptor =
                ArgumentCaptor.forClass(com.nfcplatform.support.entity.SupportMessage.class);

        supportService.addMessageOwnClient(TICKET_UUID, new MessageCreateRequest("hello", null), clientOwner);

        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getSenderUserId()).isEqualTo(OWNER_USER_ID);
    }

    private UserPrincipal principal(long userId, RoleCode roleCode) {
        Role role = new Role();
        role.setCode(roleCode);
        role.setName(roleCode.name());

        User user = new User();
        user.setId(userId);
        user.setEmail(roleCode.name().toLowerCase() + "@test.local");
        user.setPasswordHash("hash");
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return new UserPrincipal(user);
    }

    private Client client() {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setDisplayName("Test Client");
        client.setOwnerUserId(OWNER_USER_ID);
        return client;
    }

    private SupportTicket ticket(TicketStatus status) {
        SupportTicket ticket = new SupportTicket();
        ticket.setClientId(CLIENT_ID);
        ticket.setSubject("Test ticket");
        ticket.setStatus(status);
        return ticket;
    }
}
