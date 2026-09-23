package com.nfcplatform.lead.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.client.dto.ClientCreateRequest;
import com.nfcplatform.client.dto.ClientCreateResponse;
import com.nfcplatform.client.service.ClientService;
import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.exception.ConflictException;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.lead.dto.LeadConvertRequest;
import com.nfcplatform.lead.dto.LeadConvertResponse;
import com.nfcplatform.lead.dto.LeadCreateRequest;
import com.nfcplatform.lead.dto.LeadResponse;
import com.nfcplatform.lead.dto.LeadUpdateRequest;
import com.nfcplatform.lead.entity.Lead;
import com.nfcplatform.lead.entity.LeadStatus;
import com.nfcplatform.lead.repository.LeadRepository;
import com.nfcplatform.notification.service.NotificationService;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LeadService {

    private static final List<String> NOTIFY_ROLES = List.of("SUPER_ADMIN", "ADMIN");

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ClientService clientService;

    @Transactional
    public LeadResponse createPublic(LeadCreateRequest request) {
        Lead lead = new Lead();
        lead.setName(request.name());
        lead.setEmail(request.email());
        lead.setPhone(request.phone());
        lead.setCompany(request.company());
        lead.setMessage(request.message());
        lead.setSource(request.source());
        lead = leadRepository.save(lead);

        notificationService.notifyUsers(userRepository.findAllByRoleCodesIn(NOTIFY_ROLES), "NEW_LEAD",
                "New lead: " + lead.getName(),
                lead.getCompany() != null ? lead.getCompany() : lead.getEmail(), "/admin/leads");

        return LeadResponse.from(lead);
    }

    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> list(String status, String search, Pageable pageable) {
        LeadStatus statusFilter = status == null || status.isBlank() ? null : parseStatus(status);
        String searchPattern = search == null || search.isBlank() ? null : "%" + search.toLowerCase() + "%";
        Page<Lead> page = leadRepository.search(statusFilter, searchPattern, pageable);
        return PageResponse.of(page, LeadResponse::from);
    }

    @Transactional
    public LeadResponse update(String uuid, LeadUpdateRequest request, UserPrincipal actor) {
        Lead lead = leadRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Lead was not found"));
        lead.setStatus(parseStatus(request.status()));
        if (request.notes() != null) {
            lead.setNotes(request.notes());
        }
        lead = leadRepository.save(lead);

        auditService.record(actor.getId(), "LEAD_STATUS_CHANGE", "Lead", lead.getUuid(), null,
                Map.of("status", lead.getStatus().name()));

        return LeadResponse.from(lead);
    }

    /**
     * Creates a real client account from a lead's captured details instead of leaving
     * "Convert to client" as a status label an admin sets after manually re-typing everything
     * into the Create Client dialog. Reuses ClientService.createClient as-is (same temp
     * password / welcome email / duplicate-email handling as a normal client creation) rather
     * than duplicating that logic - a lead is just an alternate source of the same inputs.
     */
    @Transactional
    public LeadConvertResponse convertToClient(String uuid, LeadConvertRequest request, UserPrincipal actor,
                                                HttpServletRequest httpRequest) {
        Lead lead = leadRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Lead was not found"));
        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new ConflictException("This lead has already been converted to a client");
        }

        String displayName = lead.getCompany() != null && !lead.getCompany().isBlank()
                ? lead.getCompany() : lead.getName();
        ClientCreateResponse client = clientService.createClient(
                new ClientCreateRequest(request.clientType(), displayName, lead.getEmail(), lead.getPhone()),
                actor, httpRequest);

        lead.setStatus(LeadStatus.CONVERTED);
        lead = leadRepository.save(lead);

        auditService.record(actor.getId(), "LEAD_CONVERTED", "Lead", lead.getUuid(), null,
                Map.of("clientUuid", client.client().uuid()));

        return new LeadConvertResponse(LeadResponse.from(lead), client);
    }

    private LeadStatus parseStatus(String value) {
        try {
            return LeadStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid lead status: " + value);
        }
    }
}
