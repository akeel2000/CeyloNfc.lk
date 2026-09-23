package com.nfcplatform.media.service;

import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.config.AppProperties;
import com.nfcplatform.media.entity.MediaCategory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.UUID;

/**
 * S3-compatible alternative to LocalStorageService (app.storage.type=s3) - survives a redeploy
 * and replicates correctly across multiple backend instances, unlike local disk (see
 * docs/DEPLOYMENT.md "Go-live checklist"). Works against real AWS S3 or any S3-compatible
 * provider (MinIO, Cloudflare R2, DigitalOcean Spaces...) via app.storage.s3-endpoint.
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final AppProperties appProperties;
    private final S3Client s3Client;

    public S3StorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
        AppProperties.Storage storage = appProperties.getStorage();

        S3ClientBuilder builder = S3Client.builder().region(Region.of(storage.getS3Region()));

        if (hasText(storage.getS3AccessKey()) && hasText(storage.getS3SecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(storage.getS3AccessKey(), storage.getS3SecretKey())));
        }
        // else: leave unset so the SDK falls back to its default credential chain (IAM role,
        // environment variables, etc.) - the right behavior for real AWS in production, where
        // static keys checked into an env var are exactly what you don't want.

        if (hasText(storage.getS3Endpoint())) {
            builder.endpointOverride(URI.create(storage.getS3Endpoint()))
                    // Path-style (bucket in the URL path, not a subdomain) is what every
                    // non-AWS S3-compatible provider expects; virtual-hosted style only
                    // reliably resolves against real *.amazonaws.com DNS.
                    .forcePathStyle(true);
        }

        this.s3Client = builder.build();
    }

    @Override
    public String store(MultipartFile file, MediaCategory category) {
        FileValidator.ImageFormat format = FileValidator.validateImage(file);
        AppProperties.Storage storage = appProperties.getStorage();
        String key = category.folder() + "/" + UUID.randomUUID() + format.extension;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(storage.getS3Bucket())
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (Exception e) {
            throw new ValidationException("Failed to store the uploaded file");
        }

        return publicUrl(storage, key);
    }

    private String publicUrl(AppProperties.Storage storage, String key) {
        if (hasText(storage.getS3PublicUrl())) {
            return trimTrailingSlash(storage.getS3PublicUrl()) + "/" + key;
        }
        if (hasText(storage.getS3Endpoint())) {
            return trimTrailingSlash(storage.getS3Endpoint()) + "/" + storage.getS3Bucket() + "/" + key;
        }
        return "https://" + storage.getS3Bucket() + ".s3." + storage.getS3Region() + ".amazonaws.com/" + key;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
