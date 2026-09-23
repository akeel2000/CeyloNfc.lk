package com.nfcplatform.qr.entity;

import com.nfcplatform.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "qr_codes")
public class QrCode extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "destination_id", nullable = false)
    private Long destinationId;

    /** SHA-256(rawToken + server pepper) - same scheme as nfc_cards.token_hash. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QrCodeStatus status = QrCodeStatus.ACTIVE;

    @Column(name = "total_scans", nullable = false)
    private long totalScans = 0;

    @Column(name = "last_scanned_at")
    private Instant lastScannedAt;
}
