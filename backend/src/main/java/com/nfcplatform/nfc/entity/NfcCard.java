package com.nfcplatform.nfc.entity;

import com.nfcplatform.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "nfc_cards")
public class NfcCard extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    /** SHA-256(rawToken + server pepper). The raw token itself is never persisted. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "destination_id")
    private Long destinationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NfcCardStatus status = NfcCardStatus.UNASSIGNED;

    private String notes;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "last_tapped_at")
    private Instant lastTappedAt;

    @Column(name = "total_taps", nullable = false)
    private long totalTaps = 0;

    public boolean isActive() {
        return status == NfcCardStatus.ACTIVE;
    }
}
