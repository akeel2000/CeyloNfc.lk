package com.nfcplatform.support.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.notification.service.NotificationService;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.support.dto.MessageCreateRequest;
import com.nfcplatform.support.dto.SupportMessageDto;
import com.nfcplatform.support.dto.SupportTicketResponse;
import com.nfcplatform.support.dto.TicketCreateRequest;
import com.nfcplatform.support.entity.SupportMessage;
import com.nfcplatform.support.entity.SupportTicket;
import com.nfcplatform.support.entity.TicketPriority;
import com.nfcplatform.support.entity.TicketStatus;
import com.nfcplatform.support.repository.SupportMessageRepository;
import com.nfcplatform.support.repository.SupportTicketRepository;
import com.nfcplatform.user.entity.User;
import com.nfcplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupportService {

    private static final List<String> NOTIFY_ROLES = List.of("SUPER_ADMIN", "ADMIN");

    private final SupportTicketRepository ticketRepository;
    private final SupportMessageRepository messageRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public SupportTicketResponse createForOwnClient(TicketCreateRequest request, UserPrincipal actor) {
        Client client = ownClientOf(actor);

        SupportTicket ticket = new SupportTicket();
        ticket.setClientId(client.getId());
        ticket.setSubject(request.subject());
        ticket.setPriority(request.priority() == null ? TicketPriority.MEDIUM : parsePriority(request.priority()));
        ticket = ticketRepository.save(ticket);

        addMessage(ticket, actor.getId(), request.message(), request.attachmentUrl());

        notificationService.notifyUsers(userRepository.findAllByRoleCodesIn(NOTIFY_ROLES), "NEW_SUPPORT_TICKET",
                "New support ticket: " + ticket.getSubject(), client.getDisplayName(), "/admin/support");

        return toResponse(ticket, client);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> listForOwnClient(UserPrincipal actor) {
        Client client = ownClientOf(actor);
        // List rows don't render message bodies (see client-support-list-content.tsx) - skip
        // the per-ticket message/sender queries the detail view needs.
        return ticketRepository.findAllByClientIdOrderByCreatedAtDesc(client.getId()).stream()
                .map(ticket -> toSummaryResponse(ticket, client))
                .toList();
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse getForOwnClient(String uuid, UserPrincipal actor) {
        Client client = ownClientOf(actor);
        SupportTicket ticket = ticketRepository.findByUuidAndClientId(uuid, client.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket was not found"));
        return toResponse(ticket, client);
    }

    @Transactional
    public SupportTicketResponse addMessageOwnClient(String uuid, MessageCreateRequest request, UserPrincipal actor) {
        Client client = ownClientOf(actor);
        SupportTicket ticket = ticketRepository.findByUuidAndClientId(uuid, client.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket was not found"));

        addMessage(ticket, actor.getId(), request.body(), request.attachmentUrl());
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
            ticket.setStatus(TicketStatus.OPEN);
        }
        ticket = ticketRepository.save(ticket);

        notificationService.notifyUsers(userRepository.findAllByRoleCodesIn(NOTIFY_ROLES), "SUPPORT_REPLY",
                "New reply on: " + ticket.getSubject(), client.getDisplayName(), "/admin/support");

        return toResponse(ticket, client);
    }

    @Transactional(readOnly = true)
    public PageResponse<SupportTicketResponse> listForAdmin(String status, String search, Pageable pageable) {
        TicketStatus statusFilter = status == null || status.isBlank() ? null : parseStatus(status);
        String searchPattern = search == null || search.isBlank() ? null : "%" + search.toLowerCase() + "%";
        Page<SupportTicket> page = ticketRepository.search(statusFilter, searchPattern, pageable);

        List<Long> clientIds = page.getContent().stream().map(SupportTicket::getClientId).distinct().toList();
        Map<Long, Client> clientsById = new java.util.HashMap<>();
        if (!clientIds.isEmpty()) {
            clientRepository.findAllById(clientIds).forEach(c -> clientsById.put(c.getId(), c));
        }

        // List rows don't render message bodies (see admin-support-list-content.tsx) - skip
        // the per-ticket message/sender queries the detail view needs.
        return PageResponse.of(page, ticket -> toSummaryResponse(ticket, clientsById.get(ticket.getClientId())));
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse getForAdmin(String uuid) {
        SupportTicket ticket = ticketRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket was not found"));
        Client client = clientRepository.findById(ticket.getClientId()).orElse(null);
        return toResponse(ticket, client);
    }

    @Transactional
    public SupportTicketResponse addMessageAdmin(String uuid, MessageCreateRequest request, UserPrincipal actor) {
        SupportTicket ticket = ticketRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket was not found"));
        Client client = clientRepository.findById(ticket.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client was not found"));

        addMessage(ticket, actor.getId(), request.body(), request.attachmentUrl());
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket = ticketRepository.save(ticket);

        notificationService.notify(client.getOwnerUserId(), "SUPPORT_REPLY",
                "New reply on: " + ticket.getSubject(), "Support has replied to your ticket", "/client/support");

        return toResponse(ticket, client);
    }

    @Transactional
    public SupportTicketResponse updateStatus(String uuid, String status, UserPrincipal actor) {
        SupportTicket ticket = ticketRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket was not found"));
        ticket.setStatus(parseStatus(status));
        ticket = ticketRepository.save(ticket);

        auditService.record(actor.getId(), "SUPPORT_TICKET_STATUS_CHANGE", "SupportTicket", ticket.getUuid(), null,
                Map.of("status", ticket.getStatus().name()));

        Client client = clientRepository.findById(ticket.getClientId()).orElse(null);
        return toResponse(ticket, client);
    }

    private void addMessage(SupportTicket ticket, Long senderUserId, String body, String attachmentUrl) {
        SupportMessage message = new SupportMessage();
        message.setTicketId(ticket.getId());
        message.setSenderUserId(senderUserId);
        message.setBody(body);
        message.setAttachmentUrl(attachmentUrl);
        messageRepository.save(message);
    }

    private Client ownClientOf(UserPrincipal actor) {
        return clientRepository.findByOwnerUserIdAndDeletedAtIsNull(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No client account linked to this user"));
    }

    private SupportTicketResponse toSummaryResponse(SupportTicket ticket, Client client) {
        return SupportTicketResponse.from(ticket, client == null ? null : client.getUuid(),
                client == null ? null : client.getDisplayName(), List.of());
    }

    private SupportTicketResponse toResponse(SupportTicket ticket, Client client) {
        Long ownerUserId = client == null ? null : client.getOwnerUserId();
        List<SupportMessageDto> messages = messageRepository.findAllByTicketIdOrderByCreatedAtAsc(ticket.getId())
                .stream()
                .map(message -> {
                    User sender = userRepository.findById(message.getSenderUserId()).orElse(null);
                    boolean fromSupportStaff = ownerUserId == null || !ownerUserId.equals(message.getSenderUserId());
                    return new SupportMessageDto(message.getUuid(), sender == null ? null : sender.getEmail(),
                            fromSupportStaff, message.getBody(), message.getAttachmentUrl(), message.getCreatedAt());
                })
                .toList();
        return SupportTicketResponse.from(ticket, client == null ? null : client.getUuid(),
                client == null ? null : client.getDisplayName(), messages);
    }

    private TicketStatus parseStatus(String value) {
        try {
            return TicketStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid ticket status: " + value);
        }
    }

    private TicketPriority parsePriority(String value) {
        try {
            return TicketPriority.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid ticket priority: " + value);
        }
    }
}
