package com.nfcplatform.media.service;

import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.config.AppProperties;
import com.nfcplatform.media.entity.MediaCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Writes to the local filesystem under app.storage.path, served back out at /media/** (see
 * MediaResourceConfig). Filenames are always server-generated (a fresh UUID) - the original
 * filename is never trusted or reused, per docs/SECURITY.md. Default StorageService - active
 * whenever app.storage.type is unset or "local"; see S3StorageService for the alternative.
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    private final AppProperties appProperties;

    @Override
    public String store(MultipartFile file, MediaCategory category) {
        FileValidator.ImageFormat format = FileValidator.validateImage(file);
        String folder = category.folder();

        try {
            Path categoryDir = Path.of(appProperties.getStorage().getPath(), folder).normalize();
            Files.createDirectories(categoryDir);

            String filename = UUID.randomUUID() + format.extension;
            Path target = categoryDir.resolve(filename);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return appProperties.getBackendUrl() + "/media/" + folder + "/" + filename;
        } catch (IOException e) {
            throw new ValidationException("Failed to store the uploaded file");
        }
    }
}
