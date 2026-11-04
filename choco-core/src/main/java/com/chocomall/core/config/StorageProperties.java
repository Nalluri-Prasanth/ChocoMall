package com.chocomall.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * File storage for product images and proofs.
 *
 * @param type          {@code local} (default) or {@code minio}
 * @param localDir      directory for local storage
 * @param publicBaseUrl prefix for URLs returned to clients when using local storage (served under /files)
 * @param minio         MinIO / S3-compatible settings
 */
@ConfigurationProperties(prefix = "chocomall.storage")
public record StorageProperties(String type, String localDir, String publicBaseUrl, Minio minio) {

    public StorageProperties {
        type = type == null || type.isBlank() ? "local" : type;
        localDir = localDir == null || localDir.isBlank() ? System.getProperty("user.home") + "/.chocomall/files" : localDir;
        publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl;
    }

    public record Minio(String endpoint, String accessKey, String secretKey, String bucket) {
    }
}
