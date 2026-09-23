package com.nfcplatform.nfc.dto;

import jakarta.validation.constraints.NotBlank;

public record NfcCardAssignRequest(
        @NotBlank String clientUuid,
        @NotBlank String destinationUuid
) {
}
