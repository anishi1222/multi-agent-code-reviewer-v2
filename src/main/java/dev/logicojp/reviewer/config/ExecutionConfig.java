package dev.logicojp.reviewer.config;

import io.micronaut.context.annotation.ConfigurationProperties;

/// Configuration for execution settings (parallelism, timeouts).
@ConfigurationProperties("reviewer.execution")
public record ExecutionConfig(
    int parallelism,
    int reviewPasses,
    long orchestratorTimeoutMinutes,
    long agentTimeoutMinutes,
    long idleTimeoutMinutes,
    long skillTimeoutMinutes,
    long summaryTimeoutMinutes,
    long ghAuthTimeoutSeconds,
    int maxRetries
) {

    public static final int DEFAULT_MAX_RETRIES = 2;
    public static final long DEFAULT_IDLE_TIMEOUT_MINUTES = 5;
    public static final int DEFAULT_REVIEW_PASSES = 1;

    public ExecutionConfig {
        parallelism = (parallelism <= 0) ? 4 : parallelism;
        reviewPasses = (reviewPasses <= 0) ? DEFAULT_REVIEW_PASSES : reviewPasses;
        orchestratorTimeoutMinutes = (orchestratorTimeoutMinutes <= 0) ? 10 : orchestratorTimeoutMinutes;
        agentTimeoutMinutes = (agentTimeoutMinutes <= 0) ? 5 : agentTimeoutMinutes;
        idleTimeoutMinutes = (idleTimeoutMinutes <= 0) ? DEFAULT_IDLE_TIMEOUT_MINUTES : idleTimeoutMinutes;
        skillTimeoutMinutes = (skillTimeoutMinutes <= 0) ? 5 : skillTimeoutMinutes;
        summaryTimeoutMinutes = (summaryTimeoutMinutes <= 0) ? 5 : summaryTimeoutMinutes;
        ghAuthTimeoutSeconds = (ghAuthTimeoutSeconds <= 0) ? 10 : ghAuthTimeoutSeconds;
        maxRetries = (maxRetries < 0) ? DEFAULT_MAX_RETRIES : maxRetries;
    }

    /// Returns a copy of this config with the parallelism value replaced.
    /// @param newParallelism the new parallelism value
    /// @return a new ExecutionConfig with the updated parallelism
    public ExecutionConfig withParallelism(int newParallelism) {
        return new ExecutionConfig(newParallelism, reviewPasses, orchestratorTimeoutMinutes,
            agentTimeoutMinutes, idleTimeoutMinutes, skillTimeoutMinutes,
            summaryTimeoutMinutes, ghAuthTimeoutSeconds, maxRetries);
    }
}
