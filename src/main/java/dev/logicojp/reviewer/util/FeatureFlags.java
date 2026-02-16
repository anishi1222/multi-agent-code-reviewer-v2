package dev.logicojp.reviewer.util;

import io.micronaut.context.annotation.ConfigurationProperties;

/// Centralized feature flags backed by Micronaut configuration binding.
@ConfigurationProperties("reviewer.feature-flags")
public record FeatureFlags(
    boolean structuredConcurrency,
    boolean structuredConcurrencySkills
) {
}
