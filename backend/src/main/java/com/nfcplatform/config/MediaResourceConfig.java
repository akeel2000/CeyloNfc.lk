package com.nfcplatform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serves locally-stored uploads (LocalStorageService) back out at /media/** - only relevant
 * for app.storage.type=local; S3StorageService serves directly from the bucket/CDN instead, so
 * this bean (and the local directory it eagerly creates) doesn't even activate under s3 mode.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class MediaResourceConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Path.of(appProperties.getStorage().getPath()).toAbsolutePath().normalize();
        try {
            // Path.toUri() only appends the trailing slash a resource-location URI needs when
            // the directory already exists on disk - on a fresh checkout this directory won't
            // exist until the first upload, silently breaking every /media/** request until
            // then. Create it eagerly here instead of trusting toUri()'s existence check.
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create media storage directory: " + root, e);
        }

        registry.addResourceHandler("/media/**").addResourceLocations(root.toUri().toString());
    }
}
