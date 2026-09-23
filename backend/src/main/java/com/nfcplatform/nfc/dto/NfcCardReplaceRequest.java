package com.nfcplatform.nfc.dto;

import jakarta.validation.constraints.NotBlank;

public record NfcCardReplaceRequest(
        @NotBlank String newSerialNumber,
        String notes
) {
}
