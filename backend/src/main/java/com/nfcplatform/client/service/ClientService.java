package com.nfcplatform.client.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.auth.repository.RefreshTokenRepository;
import com.nfcplatform.client.dto.*;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.entity.ClientStatus;
import com.nfcplatform.client.entity.ClientType;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.exception.ConflictException;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.common.mail.EmailService;
import com.nfcplatform.common.util.PasswordGenerator;
import com.nfcplatform.config.AppProperties;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.role.repository.RoleRepository;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.user.entity.User;
import com.nfcplatform.user.entity.UserStatus;
import com.nfcplatform.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final EmailService emailService;
    private final AppProperties appProperties;

    @Transactional
    public ClientCreateResponse createClient(ClientCreateRequest request, UserPrincipal actor, HttpServletRequest httpRequest) {
        ClientType type = parseType(request.type());

        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("A user account already exists with this email");
        }
        if (clientRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(request.email())) {
            throw new ConflictException("A client already exists with this email");
        }

        Role clientRole = roleRepository.findByCode(RoleCode.CLIENT)
                .orElseThrow(() -> new IllegalStateException("CLIENT role missing - check Flyway seed migration"));

        String temporaryPassword = PasswordGenerator.generate();

        User user = new User();
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setRoles(Set.of(clientRole));
        user = userRepository.save(user);

        Client client = new Client();
        client.setType(type);
        client.setStatus(ClientStatus.ACTIVE);
        client.setDisplayName(request.displayName());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setOwnerUserId(user.getId());
        client = clientRepository.save(client);

        emailService.sendAccountCreatedEmail(user.getEmail(), appProperties.getFrontendUrl() + "/login");

        auditService.record(actor.getId(), "CLIENT_CREATE", "Client", client.getUuid(),
                clientIp(httpRequest), java.util.Map.of("email", client.getEmail()));

        return new ClientCreateResponse(ClientResponse.from(client), temporaryPassword);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> listClients(String status, String type, String search, Pageable pageable) {
        ClientStatus statusFilter = status == null || status.isBlank() ? null : parseStatus(status);
        ClientType typeFilter = type == null || type.isBlank() ? null : parseType(type);
        String searchPattern = search == null || search.isBlank() ? null : "%" + search.toLowerCase() + "%";

        Page<Client> page = clientRepository.search(statusFilter, typeFilter, searchPattern, pageable);
        return PageResponse.of(page, ClientResponse::from);
    }

    @Transactional(readOnly = true)
    public ClientResponse getClient(String uuid) {
        return ClientResponse.from(findActiveOrThrow(uuid));
    }

    @Transactional
    public ClientResponse updateClient(String uuid, ClientUpdateRequest request, UserPrincipal actor, HttpServletRequest httpRequest) {
        Client client = findActiveOrThrow(uuid);

        if (!client.getEmail().equalsIgnoreCase(request.email())
                && clientRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(request.email())) {
            throw new ConflictException("Another client already uses this email");
        }

        client.setDisplayName(request.displayName());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client = clientRepository.save(client);

        auditService.record(actor.getId(), "CLIENT_UPDATE", "Client", client.getUuid(), clientIp(httpRequest), null);

        return ClientResponse.from(client);
    }

    @Transactional
    public ClientResponse updateStatus(String uuid, ClientStatusUpdateRequest request, UserPrincipal actor, HttpServletRequest httpRequest) {
        Client client = findActiveOrThrow(uuid);
        ClientStatus newStatus = parseStatus(request.status());

        client.setStatus(newStatus);
        client = clientRepository.save(client);

        auditService.record(actor.getId(), "CLIENT_STATUS_CHANGE", "Client", client.getUuid(),
                clientIp(httpRequest), java.util.Map.of("status", newStatus.name()));

        return ClientResponse.from(client);
    }

    @Transactional
    public void deleteClient(String uuid, UserPrincipal actor, HttpServletRequest httpRequest) {
        Client client = findActiveOrThrow(uuid);

        client.setDeletedAt(Instant.now());
        client.setStatus(ClientStatus.CLOSED);
        clientRepository.save(client);

        userRepository.findById(client.getOwnerUserId()).ifPresent(owner -> {
            owner.setStatus(UserStatus.DISABLED);
            userRepository.save(owner);
            refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(owner.getId())
                    .forEach(rt -> rt.setRevokedAt(Instant.now()));
        });

        auditService.record(actor.getId(), "CLIENT_DELETE", "Client", client.getUuid(), clientIp(httpRequest), null);
    }

    private Client findActiveOrThrow(String uuid) {
        return clientRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Client was not found"));
    }

    private ClientType parseType(String value) {
        try {
            return ClientType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid client type: " + value);
        }
    }

    private ClientStatus parseStatus(String value) {
        try {
            return ClientStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid client status: " + value);
        }
    }

    private String clientIp(HttpServletRequest request) {
        return com.nfcplatform.common.web.ClientIpResolver.resolve(request);
    }
}
