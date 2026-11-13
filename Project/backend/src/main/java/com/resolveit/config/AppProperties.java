package com.resolveit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        String frontendOrigin,
        FileStorage fileStorage,
        Sla sla,
        Mail mail,
        Security security
) {
    public record Jwt(String secret, long accessTtlSeconds, long refreshTtlSeconds, String issuer) {}
    public record FileStorage(String path, long maxFileSizeBytes, int maxFilesPerComplaint, List<String> allowedExtensions) {}
    public record Sla(long scanDelayMs, int batchSize) {}
    public record Mail(boolean enabled, String from) {}
    public record Security(int rateLimitPerMinute) {}
}

