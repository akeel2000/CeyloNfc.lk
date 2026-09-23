package com.nfcplatform.qr.dto;

public record QrCodeCreateResponse(
        QrCodeResponse qrCode,
        String publicUrl
) {
}
