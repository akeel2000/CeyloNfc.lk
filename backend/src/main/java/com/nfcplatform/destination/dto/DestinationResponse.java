package com.nfcplatform.destination.dto;

import com.nfcplatform.destination.entity.Destination;

public record DestinationResponse(
        String uuid,
        String name,
        String type,
        String externalUrl,
        boolean active
) {
    public static DestinationResponse from(Destination destination) {
        return new DestinationResponse(
                destination.getUuid(),
                destination.getName(),
                destination.getType().name(),
                destination.getExternalUrl(),
                destination.isActive()
        );
    }
}
