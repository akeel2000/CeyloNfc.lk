package com.nfcplatform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String frontendUrl;
    private String backendUrl;

    private final Jwt jwt = new Jwt();
    private final Security security = new Security();
    private final Storage storage = new Storage();
    private final Mail mail = new Mail();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessTokenExpirationMs;
        private long refreshTokenExpirationMs;
        private String cookieDomain;
    }

    @Getter
    @Setter
    public static class Security {
        private String nfcTokenPepper;
        private int maxFailedLoginAttempts = 5;
        private int accountLockDurationMinutes = 15;
        private boolean cookieSecure = true;
    }

    @Getter
    @Setter
    public static class Storage {
        private String type;
        private String path;
        /** S3-compatible fields, only read when type=s3. s3Endpoint is optional - unset means
         *  real AWS S3; set it to point at any S3-compatible provider (MinIO, R2, Spaces...). */
        private String s3Bucket;
        private String s3Region;
        private String s3Endpoint;
        private String s3AccessKey;
        private String s3SecretKey;
        /** Optional override for the URL returned to callers, e.g. a CDN domain sitting in
         *  front of the bucket. Falls back to the endpoint/bucket's own URL when unset. */
        private String s3PublicUrl;
    }

    @Getter
    @Setter
    public static class Mail {
        private String from;
    }
}
