package com.nfcplatform.media.service;

import com.nfcplatform.common.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the magic-byte upload validator (docs/SECURITY.md "File uploads") - the
 * actual defense against a renamed malicious file being uploaded with a spoofed content-type,
 * so these deliberately construct real byte signatures rather than mocking anything away.
 */
class FileValidatorTest {

    private static final byte[] JPEG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final byte[] PNG_HEADER =
            {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x00};
    private static final byte[] GIF_HEADER = {'G', 'I', 'F', '8', '9', 'a', 0x00, 0x00};
    private static final byte[] WEBP_HEADER =
            {'R', 'I', 'F', 'F', 0x00, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P'};

    @Test
    void rejectsAnEmptyFile() {
        MultipartFile empty = new MockMultipartFile("file", "photo.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> FileValidator.validateImage(empty)).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsAFileOverTheFiveMegabyteLimit() {
        MultipartFile tooBig = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[6 * 1024 * 1024]);

        assertThatThrownBy(() -> FileValidator.validateImage(tooBig)).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsSvgByExtensionEvenWithAnImageContentType() {
        MultipartFile svg = new MockMultipartFile("file", "logo.svg", "image/svg+xml",
                "<svg><script>alert(1)</script></svg>".getBytes());

        assertThatThrownBy(() -> FileValidator.validateImage(svg)).isInstanceOf(ValidationException.class)
                .hasMessageContaining("SVG");
    }

    @Test
    void acceptsARealJpegAndReturnsTheDetectedFormat() {
        MultipartFile jpeg = new MockMultipartFile("file", "photo.jpg", "image/jpeg", JPEG_HEADER);

        assertThat(FileValidator.validateImage(jpeg)).isEqualTo(FileValidator.ImageFormat.JPEG);
    }

    @Test
    void acceptsARealPng() {
        MultipartFile png = new MockMultipartFile("file", "photo.png", "image/png", PNG_HEADER);

        assertThat(FileValidator.validateImage(png)).isEqualTo(FileValidator.ImageFormat.PNG);
    }

    @Test
    void acceptsARealGif() {
        MultipartFile gif = new MockMultipartFile("file", "photo.gif", "image/gif", GIF_HEADER);

        assertThat(FileValidator.validateImage(gif)).isEqualTo(FileValidator.ImageFormat.GIF);
    }

    @Test
    void acceptsARealWebp() {
        MultipartFile webp = new MockMultipartFile("file", "photo.webp", "image/webp", WEBP_HEADER);

        assertThat(FileValidator.validateImage(webp)).isEqualTo(FileValidator.ImageFormat.WEBP);
    }

    @Test
    void rejectsBytesThatDontMatchAnyKnownImageSignature() {
        MultipartFile fakeImage = new MockMultipartFile("file", "photo.png", "image/png",
                "not actually an image".getBytes());

        assertThatThrownBy(() -> FileValidator.validateImage(fakeImage)).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsARenamedExecutableWithASpoofedImageContentType() {
        // A real PNG signature but declared as a totally different content-type - the exact
        // "renamed malicious file" scenario this validator exists to catch, just inverted (the
        // bytes here are legitimate but the mismatch check itself is what's under test).
        MultipartFile mismatched = new MockMultipartFile("file", "photo.png", "application/octet-stream", PNG_HEADER);

        assertThatThrownBy(() -> FileValidator.validateImage(mismatched)).isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void rejectsAJpegDeclaredAsPng() {
        MultipartFile mismatched = new MockMultipartFile("file", "photo.png", "image/png", JPEG_HEADER);

        assertThatThrownBy(() -> FileValidator.validateImage(mismatched)).isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not match");
    }
}
