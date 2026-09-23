package com.nfcplatform.nfc.dto;

import com.nfcplatform.nfc.entity.NfcCard;

import java.time.Instant;

public record NfcCardResponse(
        String uuid,
        String serialNumber,
        String status,
        String clientUuid,
        String clientDisplayName,
        String destinationUuid,
        String destinationName,
        String notes,
        Instant activatedAt,
        Instant lastTappedAt,
        long totalTaps,
        Instant createdAt
) {
    public static NfcCardResponse from(NfcCard card, String clientDisplayName, String clientUuid,
                                        String destinationName, String destinationUuid) {
        return new NfcCardResponse(
                card.getUuid(),
                card.getSerialNumber(),
                card.getStatus().name(),
                clientUuid,
                clientDisplayName,
                destinationUuid,
                destinationName,
                card.getNotes(),
                card.getActivatedAt(),
                card.getLastTappedAt(),
                card.getTotalTaps(),
                card.getCreatedAt()
        );
    }
}
