package com.nfcplatform.qr.dto;

import com.nfcplatform.qr.entity.QrCode;

import java.time.Instant;

public record QrCodeResponse(
        String uuid,
        String name,
        String status,
        String destinationUuid,
        String destinationName,
        long totalScans,
        Instant lastScannedAt,
        Instant createdAt
) {
    public static QrCodeResponse from(QrCode qr, String destinationName, String destinationUuid) {
        return new QrCodeResponse(qr.getUuid(), qr.getName(), qr.getStatus().name(), destinationUuid,
                destinationName, qr.getTotalScans(), qr.getLastScannedAt(), qr.getCreatedAt());
    }
}
