package com.nfcplatform.media.service;

import com.nfcplatform.common.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Image upload validation per docs/SECURITY.md "File uploads": MIME + extension + magic-byte
 * signature check (never trust the client-supplied content-type or filename alone), size cap,
 * SVGs rejected outright since this project has no SVG sanitizer.
 */
public final class FileValidator {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private FileValidator() {
    }

    public enum ImageFormat {
        JPEG(".jpg"), PNG(".png"), WEBP(".webp"), GIF(".gif");

        final String extension;

        ImageFormat(String extension) {
            this.extension = extension;
        }
    }

    public static ImageFormat validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("A file is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ValidationException("File exceeds the 5MB size limit");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (originalFilename.endsWith(".svg") || "image/svg+xml".equals(contentType)) {
            throw new ValidationException("SVG uploads are not supported");
        }

        byte[] header;
        try (InputStream in = file.getInputStream()) {
            header = in.readNBytes(12);
        } catch (IOException e) {
            throw new ValidationException("Could not read the uploaded file");
        }

        ImageFormat detected = detectFromMagicBytes(header);
        if (detected == null) {
            throw new ValidationException("Unsupported or unrecognized image format - only JPEG, PNG, WEBP and GIF are accepted");
        }

        // Cross-check the declared content-type against what the bytes actually are - a
        // mismatch (e.g. a renamed .exe with a fake image content-type) is rejected outright
        // rather than trusted.
        if (contentType != null && !matchesDeclaredType(detected, contentType)) {
            throw new ValidationException("The file's content does not match its declared type");
        }

        return detected;
    }

    private static ImageFormat detectFromMagicBytes(byte[] header) {
        if (header.length >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return ImageFormat.JPEG;
        }
        if (header.length >= 8 && (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) {
            return ImageFormat.PNG;
        }
        if (header.length >= 6 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F'
                && header[3] == '8' && (header[4] == '7' || header[4] == '9') && header[5] == 'a') {
            return ImageFormat.GIF;
        }
        if (header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return ImageFormat.WEBP;
        }
        return null;
    }

    private static boolean matchesDeclaredType(ImageFormat detected, String contentType) {
        return switch (detected) {
            case JPEG -> contentType.equals("image/jpeg") || contentType.equals("image/jpg");
            case PNG -> contentType.equals("image/png");
            case GIF -> contentType.equals("image/gif");
            case WEBP -> contentType.equals("image/webp");
        };
    }
}
