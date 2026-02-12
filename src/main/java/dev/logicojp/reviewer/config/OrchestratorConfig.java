package dev.logicojp.reviewer.config;

import io.micronaut.context.annotation.ConfigurationProperties;

/// Configuration for the review orchestrator.
@ConfigurationProperties("reviewer.orchestrator")
public record OrchestratorConfig(
    int defaultParallelism,
    long timeoutMinutes
) {

    public OrchestratorConfig {
        defaultParallelism = (defaultParallelism <= 0) ? 4 : defaultParallelism;
        timeoutMinutes = (timeoutMinutes <= 0) ? 10 : timeoutMinutes;
    }
}
