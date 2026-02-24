package dev.logicojp.reviewer.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Nullable;

/// Resilience settings for circuit breakers and retries.
/// Bound to `reviewer.resilience` in YAML.
@ConfigurationProperties("reviewer.resilience")
public record ResilienceConfig(
    @Nullable
    OperationSettings review,
    @Nullable
    OperationSettings summary,
    @Nullable
    OperationSettings skill
) {

    public ResilienceConfig {
        review = OperationSettings.mergeWithDefaults(review, OperationSettings.reviewDefaults());
        summary = OperationSettings.mergeWithDefaults(summary, OperationSettings.summaryDefaults());
        skill = OperationSettings.mergeWithDefaults(skill, OperationSettings.skillDefaults());
    }

    public record OperationSettings(
        int failureThreshold,
        long openDurationSeconds,
        int maxAttempts,
        long backoffBaseMs,
        long backoffMaxMs
    ) {
        public static final int DEFAULT_REVIEW_FAILURE_THRESHOLD = 5;
        public static final long DEFAULT_REVIEW_OPEN_DURATION_SECONDS = 30;
        public static final int DEFAULT_REVIEW_MAX_ATTEMPTS = 3;
        public static final long DEFAULT_REVIEW_BACKOFF_BASE_MS = 1000;
        public static final long DEFAULT_REVIEW_BACKOFF_MAX_MS = 8000;

        public static final int DEFAULT_SUMMARY_FAILURE_THRESHOLD = 3;
        public static final long DEFAULT_SUMMARY_OPEN_DURATION_SECONDS = 20;
        public static final int DEFAULT_SUMMARY_MAX_ATTEMPTS = 3;
        public static final long DEFAULT_SUMMARY_BACKOFF_BASE_MS = 500;
        public static final long DEFAULT_SUMMARY_BACKOFF_MAX_MS = 4000;

        public static final int DEFAULT_SKILL_FAILURE_THRESHOLD = 3;
        public static final long DEFAULT_SKILL_OPEN_DURATION_SECONDS = 20;
        public static final int DEFAULT_SKILL_MAX_ATTEMPTS = 3;
        public static final long DEFAULT_SKILL_BACKOFF_BASE_MS = 500;
        public static final long DEFAULT_SKILL_BACKOFF_MAX_MS = 4000;

        public OperationSettings {
            failureThreshold = Math.max(0, failureThreshold);
            openDurationSeconds = Math.max(0, openDurationSeconds);
            maxAttempts = Math.max(0, maxAttempts);
            backoffBaseMs = Math.max(0, backoffBaseMs);
            backoffMaxMs = Math.max(0, backoffMaxMs);
            if (backoffBaseMs > 0 && backoffMaxMs > 0 && backoffMaxMs < backoffBaseMs) {
                backoffMaxMs = backoffBaseMs;
            }
        }

        public static OperationSettings mergeWithDefaults(OperationSettings partial,
                                                          OperationSettings defaults) {
            if (partial == null) {
                return defaults;
            }
            int failureThreshold = partial.failureThreshold() > 0
                ? partial.failureThreshold()
                : defaults.failureThreshold();
            long openDurationSeconds = partial.openDurationSeconds() > 0
                ? partial.openDurationSeconds()
                : defaults.openDurationSeconds();
            int maxAttempts = partial.maxAttempts() > 0
                ? partial.maxAttempts()
                : defaults.maxAttempts();
            long backoffBaseMs = partial.backoffBaseMs() > 0
                ? partial.backoffBaseMs()
                : defaults.backoffBaseMs();
            long backoffMaxMs = partial.backoffMaxMs() > 0
                ? partial.backoffMaxMs()
                : defaults.backoffMaxMs();
            if (backoffMaxMs < backoffBaseMs) {
                backoffMaxMs = backoffBaseMs;
            }

            return new OperationSettings(
                failureThreshold,
                openDurationSeconds,
                maxAttempts,
                backoffBaseMs,
                backoffMaxMs
            );
        }

        public static OperationSettings reviewDefaults() {
            return new OperationSettings(
                DEFAULT_REVIEW_FAILURE_THRESHOLD,
                DEFAULT_REVIEW_OPEN_DURATION_SECONDS,
                DEFAULT_REVIEW_MAX_ATTEMPTS,
                DEFAULT_REVIEW_BACKOFF_BASE_MS,
                DEFAULT_REVIEW_BACKOFF_MAX_MS);
        }

        public static OperationSettings summaryDefaults() {
            return new OperationSettings(
                DEFAULT_SUMMARY_FAILURE_THRESHOLD,
                DEFAULT_SUMMARY_OPEN_DURATION_SECONDS,
                DEFAULT_SUMMARY_MAX_ATTEMPTS,
                DEFAULT_SUMMARY_BACKOFF_BASE_MS,
                DEFAULT_SUMMARY_BACKOFF_MAX_MS);
        }

        public static OperationSettings skillDefaults() {
            return new OperationSettings(
                DEFAULT_SKILL_FAILURE_THRESHOLD,
                DEFAULT_SKILL_OPEN_DURATION_SECONDS,
                DEFAULT_SKILL_MAX_ATTEMPTS,
                DEFAULT_SKILL_BACKOFF_BASE_MS,
                DEFAULT_SKILL_BACKOFF_MAX_MS);
        }
    }
}