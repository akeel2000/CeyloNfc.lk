package com.nfcplatform.lead.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.client.dto.ClientCreateRequest;
import com.nfcplatform.client.dto.ClientCreateResponse;
import com.nfcplatform.client.dto.ClientResponse;
import com.nfcplatform.client.service.ClientService;
import com.nfcplatform.common.exception.ConflictException;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.lead.dto.LeadConvertRequest;
import com.nfcplatform.lead.dto.LeadConvertResponse;
import com.nfcplatform.lead.dto.LeadResponse;
import com.nfcplatform.lead.dto.LeadUpdateRequest;
import com.nfcplatform.lead.entity.Lead;
import com.nfcplatform.lead.entity.LeadStatus;
import com.nfcplatform.lead.repository.LeadRepository;
import com.nfcplatform.notification.service.NotificationService;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.user.entity.User;
import com.nfcplatform.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for lead status transitions and lead-to-client conversion - see
 * docs/PROJECT_PROGRESS.md's "Post-roadmap: Convert to client" entry for the frontend bug
 * (ConvertLeadDialog hiding its own result screen) this backend guard exists alongside. Pure
 * Mockito, no Spring context/DB.
 */
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditService auditService;
    @Mock
    private ClientService clientService;
    @Mock
    private HttpServletRequest httpServletRequest;

    private LeadService leadService;

    private static final String LEAD_UUID = "lead-uuid";
    private static final long ACTOR_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        leadService = new LeadService(leadRepository, userRepository, notificationService, auditService, clientService);
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findAllByRoleCodesIn(anyList())).thenReturn(List.of());
    }

    // --- update -----------------------------------------------------------------------------

    @Test
    void updateThrowsForAnUnknownLeadUuid() {
        when(leadRepository.findByUuid(LEAD_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.update(LEAD_UUID, new LeadUpdateRequest("CONTACTED", null), principal()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRejectsAnUnrecognizedStatus() {
        Lead lead = lead();
        when(leadRepository.findByUuid(LEAD_UUID)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> leadService.update(LEAD_UUID, new LeadUpdateRequest("NOT_A_STATUS", null), principal()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updatePreservesExistingNotesWhenNoneAreSupplied() {
        Lead lead = lead();
        lead.setNotes("Called them Tuesday");
        when(leadRepository.findByUuid(LEAD_UUID)).thenReturn(Optional.of(lead));

        leadService.update(LEAD_UUID, new LeadUpdateRequest("CONTACTED", null), principal());

        assertThat(lead.getNotes()).isEqualTo("Called them Tuesday");
    }

    @Test
    void updateOverwritesNotesWhenSupplied() {
        Lead lead = lead();
        lead.setNotes("Old note");
        when(leadRepository.findByUuid(LEAD_UUID)).thenReturn(Optional.of(lead));

        leadService.update(LEAD_UUID, new LeadUpdateRequest("CONTACTED", "New note"), principal());

        assertThat(lead.getNotes()).isEqualTo("New note");
    }

    // --- convertToClient ----------------------------------------------------------------------

    @Test
    void convertToClientThrowsForAnUnknownLeadUuid() {
        when(leadRepository.findByUuid(LEAD_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.convertToClient(LEAD_UUID, new LeadConvertRequest("BUSINESS"),
                principal(), httpServletRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void convertToClientRejectsALeadThatWasAlreadyConverted() {
        Lead lead = lead();
        lead.setStatus(LeadStatus.CONVERTED);
        when(leadRepository.findByUuid(LEAD_UUID)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> leadService.convertToClient(LEAD_UUID, new LeadConvertRequest("BUSINESS"),
                principal(), httpServletRequest))
                .isInstanceOf(ConflictException.class);

        verify(clientService, never()).createClient(any(), any(), any());
    }

    @Test
    void convertToClientUsesTheCompanyNameAsDisplayNameWhenPresent() {
        Lead lead = lead();
        lead.setCompany("Acme Traders");
        when(leadRepository.findByUuid(LEAD_UUID)).thenReturn(Optional.of(lead));
        when(clientService.createClient(any(), any(), any())).thenReturn(clientCreateResponse());

        leadService.convertToClient(LEAD_UUID, new LeadConvertRequest("BUSINESS"), principal(), httpServletRequest);

        org.mockito.ArgumentCaptor<ClientCreateRequest> captor =
                org.mockito.ArgumentCaptor.forClass(ClientCreateRequest.class);
        verify(clientService).createClient(captor.capture(), any(), any());
        assertThat(captor.getValue().displayName()).isEqualTo("Acme Traders");
    }

    @Test
    void convertToClientFallsBackToTheLeadNameWhenNoCompanyIsSet() {
        Lead lead = lead();
        lead.setCompany(null);
        when(leadRepository.findByUuid(LEAD_UUID)).thenReturn(Optional.of(lead));
        when(clientService.createClient(any(), any(), any())).thenReturn(clientCreateResponse());

        leadService.convertToClient(LEAD_UUID, new LeadConvertRequest("INDIVIDUAL"), principal(), httpServletRequest);

        org.mockito.ArgumentCaptor<ClientCreateRequest> captor =
                org.mockito.ArgumentCaptor.forClass(ClientCreateRequest.class);
        verify(clientService).createClient(captor.capture(), any(), any());
        assertThat(captor.getValue().displayName()).isEqualTo("Kasun Silva");
    }

    @Test
    void convertToClientMarksTheLeadConvertedOnlyAfterClientCreationSucceeds() {
        Lead lead = lead();
        when(leadRepository.findByUuid(LEAD_UUID)).thenReturn(Optional.of(lead));
        when(clientService.createClient(any(), any(), any())).thenReturn(clientCreateResponse());

        LeadConvertResponse response = leadService.convertToClient(LEAD_UUID, new LeadConvertRequest("BUSINESS"),
                principal(), httpServletRequest);

        assertThat(lead.getStatus()).isEqualTo(LeadStatus.CONVERTED);
        assertThat(response.lead().status()).isEqualTo("CONVERTED");
    }

    @Test
    void convertToClientLeavesTheLeadUnconvertedWhenClientCreationFails() {
        Lead lead = lead();
        when(leadRepository.findByUuid(LEAD_UUID)).thenReturn(Optional.of(lead));
        when(clientService.createClient(any(), any(), any()))
                .thenThrow(new ConflictException("A client already exists with this email"));

        assertThatThrownBy(() -> leadService.convertToClient(LEAD_UUID, new LeadConvertRequest("BUSINESS"),
                principal(), httpServletRequest))
                .isInstanceOf(ConflictException.class);

        assertThat(lead.getStatus()).isEqualTo(LeadStatus.NEW);
        verify(leadRepository, never()).save(any());
    }

    private Lead lead() {
        Lead lead = new Lead();
        lead.setUuid(LEAD_UUID);
        lead.setName("Kasun Silva");
        lead.setEmail("kasun@example.com");
        lead.setStatus(LeadStatus.NEW);
        return lead;
    }

    private ClientCreateResponse clientCreateResponse() {
        ClientResponse client = new ClientResponse("new-client-uuid", "BUSINESS", "ACTIVE", "Acme Traders",
                "kasun@example.com", null, Instant.now(), Instant.now());
        return new ClientCreateResponse(client, "TempPass123!");
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
