package com.nfcplatform.qr.repository;

import com.nfcplatform.qr.entity.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QrCodeRepository extends JpaRepository<QrCode, Long> {

    Optional<QrCode> findByUuid(String uuid);

    /** Tenant-safe: only returns the QR code if it belongs to the given client. */
    Optional<QrCode> findByUuidAndClientId(String uuid, Long clientId);

    Optional<QrCode> findByTokenHash(String tokenHash);

    List<QrCode> findAllByClientIdOrderByCreatedAtDesc(Long clientId);
}
