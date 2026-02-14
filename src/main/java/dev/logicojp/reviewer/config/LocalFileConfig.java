package dev.logicojp.reviewer.config;

import io.micronaut.context.annotation.ConfigurationProperties;

/// Configuration for local file collection limits.
@ConfigurationProperties("reviewer.local-files")
public record LocalFileConfig(
    long maxFileSize,
    long maxTotalSize
) {

    public static final long DEFAULT_MAX_FILE_SIZE = 256 * 1024;
    public static final long DEFAULT_MAX_TOTAL_SIZE = 2 * 1024 * 1024;

    public LocalFileConfig {
        maxFileSize = (maxFileSize <= 0) ? DEFAULT_MAX_FILE_SIZE : maxFileSize;
        maxTotalSize = (maxTotalSize <= 0) ? DEFAULT_MAX_TOTAL_SIZE : maxTotalSize;
    }
}
