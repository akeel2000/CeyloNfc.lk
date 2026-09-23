package com.nfcplatform.nfc.dto;

import jakarta.validation.constraints.NotBlank;

public record NfcCardRegisterRequest(
        @NotBlank String serialNumber,
        String notes
) {
}
