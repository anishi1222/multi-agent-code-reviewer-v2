package dev.logicojp.reviewer.config;

import io.micronaut.context.annotation.ConfigurationProperties;

/// Configuration for execution settings (parallelism, timeouts, retries, buffers)
/// and executive summary generation settings.
///
/// Summary settings are nested under `reviewer.execution.summary` in YAML.
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
    int maxRetries,
    int maxAccumulatedSize,
    int initialAccumulatedCapacity,
    int instructionBufferExtraCapacity,
    String checkpointDirectory,
    SummarySettings summary
) {

    public static final int DEFAULT_PARALLELISM = 4;
    public static final int DEFAULT_REVIEW_PASSES = 1;
    public static final long DEFAULT_ORCHESTRATOR_TIMEOUT_MINUTES = 10;
    public static final long DEFAULT_AGENT_TIMEOUT_MINUTES = 5;
    public static final long DEFAULT_IDLE_TIMEOUT_MINUTES = 5;
    public static final long DEFAULT_SKILL_TIMEOUT_MINUTES = 5;
    public static final long DEFAULT_SUMMARY_TIMEOUT_MINUTES = 5;
    public static final long DEFAULT_GH_AUTH_TIMEOUT_SECONDS = 10;
    public static final int DEFAULT_MAX_RETRIES = 2;
    public static final int DEFAULT_MAX_ACCUMULATED_SIZE = 4 * 1024 * 1024;
    public static final int DEFAULT_INITIAL_ACCUMULATED_CAPACITY = 4096;
    public static final int DEFAULT_INSTRUCTION_BUFFER_EXTRA_CAPACITY = 32;
    public static final String DEFAULT_CHECKPOINT_DIRECTORY = "reports/.checkpoints";

    public ExecutionConfig {
        parallelism = ConfigDefaults.defaultIfNonPositive(parallelism, DEFAULT_PARALLELISM);
        reviewPasses = ConfigDefaults.defaultIfNonPositive(reviewPasses, DEFAULT_REVIEW_PASSES);
        orchestratorTimeoutMinutes = ConfigDefaults.defaultIfNonPositive(orchestratorTimeoutMinutes, DEFAULT_ORCHESTRATOR_TIMEOUT_MINUTES);
        agentTimeoutMinutes = ConfigDefaults.defaultIfNonPositive(agentTimeoutMinutes, DEFAULT_AGENT_TIMEOUT_MINUTES);
        idleTimeoutMinutes = ConfigDefaults.defaultIfNonPositive(idleTimeoutMinutes, DEFAULT_IDLE_TIMEOUT_MINUTES);
        skillTimeoutMinutes = ConfigDefaults.defaultIfNonPositive(skillTimeoutMinutes, DEFAULT_SKILL_TIMEOUT_MINUTES);
        summaryTimeoutMinutes = ConfigDefaults.defaultIfNonPositive(summaryTimeoutMinutes, DEFAULT_SUMMARY_TIMEOUT_MINUTES);
        ghAuthTimeoutSeconds = ConfigDefaults.defaultIfNonPositive(ghAuthTimeoutSeconds, DEFAULT_GH_AUTH_TIMEOUT_SECONDS);
        maxRetries = ConfigDefaults.defaultIfNegative(maxRetries, DEFAULT_MAX_RETRIES);
        maxAccumulatedSize = ConfigDefaults.defaultIfNonPositive(maxAccumulatedSize, DEFAULT_MAX_ACCUMULATED_SIZE);
        initialAccumulatedCapacity = ConfigDefaults.defaultIfNonPositive(initialAccumulatedCapacity, DEFAULT_INITIAL_ACCUMULATED_CAPACITY);
        instructionBufferExtraCapacity = ConfigDefaults.defaultIfNonPositive(instructionBufferExtraCapacity, DEFAULT_INSTRUCTION_BUFFER_EXTRA_CAPACITY);
        checkpointDirectory = ConfigDefaults.defaultIfBlank(checkpointDirectory, DEFAULT_CHECKPOINT_DIRECTORY);
        summary = summary != null ? summary : new SummarySettings(0, 0, 0, 0, 0, 0);
    }

    /// Returns a copy with the parallelism value replaced.
    /// Uses named field access to prevent positional copy errors when fields are added.
    public ExecutionConfig withParallelism(int newParallelism) {
        return Builder.from(this)
            .parallelism(newParallelism)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int parallelism = DEFAULT_PARALLELISM;
        private int reviewPasses = DEFAULT_REVIEW_PASSES;
        private long orchestratorTimeoutMinutes = DEFAULT_ORCHESTRATOR_TIMEOUT_MINUTES;
        private long agentTimeoutMinutes = DEFAULT_AGENT_TIMEOUT_MINUTES;
        private long idleTimeoutMinutes = DEFAULT_IDLE_TIMEOUT_MINUTES;
        private long skillTimeoutMinutes = DEFAULT_SKILL_TIMEOUT_MINUTES;
        private long summaryTimeoutMinutes = DEFAULT_SUMMARY_TIMEOUT_MINUTES;
        private long ghAuthTimeoutSeconds = DEFAULT_GH_AUTH_TIMEOUT_SECONDS;
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private int maxAccumulatedSize = DEFAULT_MAX_ACCUMULATED_SIZE;
        private int initialAccumulatedCapacity = DEFAULT_INITIAL_ACCUMULATED_CAPACITY;
        private int instructionBufferExtraCapacity = DEFAULT_INSTRUCTION_BUFFER_EXTRA_CAPACITY;
        private String checkpointDirectory = DEFAULT_CHECKPOINT_DIRECTORY;
        private SummarySettings summary = new SummarySettings(0, 0, 0, 0, 0, 0);

        private Builder() {
        }

        public static Builder from(ExecutionConfig source) {
            return new Builder()
                .parallelism(source.parallelism)
                .reviewPasses(source.reviewPasses)
                .orchestratorTimeoutMinutes(source.orchestratorTimeoutMinutes)
                .agentTimeoutMinutes(source.agentTimeoutMinutes)
                .idleTimeoutMinutes(source.idleTimeoutMinutes)
                .skillTimeoutMinutes(source.skillTimeoutMinutes)
                .summaryTimeoutMinutes(source.summaryTimeoutMinutes)
                .ghAuthTimeoutSeconds(source.ghAuthTimeoutSeconds)
                .maxRetries(source.maxRetries)
                .maxAccumulatedSize(source.maxAccumulatedSize)
                .initialAccumulatedCapacity(source.initialAccumulatedCapacity)
                .instructionBufferExtraCapacity(source.instructionBufferExtraCapacity)
                .checkpointDirectory(source.checkpointDirectory)
                .summary(source.summary);
        }

        public Builder parallelism(int parallelism) { this.parallelism = parallelism; return this; }
        public Builder reviewPasses(int reviewPasses) { this.reviewPasses = reviewPasses; return this; }
        public Builder orchestratorTimeoutMinutes(long orchestratorTimeoutMinutes) {
            this.orchestratorTimeoutMinutes = orchestratorTimeoutMinutes;
            return this;
        }

        public Builder agentTimeoutMinutes(long agentTimeoutMinutes) {
            this.agentTimeoutMinutes = agentTimeoutMinutes;
            return this;
        }

        public Builder idleTimeoutMinutes(long idleTimeoutMinutes) {
            this.idleTimeoutMinutes = idleTimeoutMinutes;
            return this;
        }

        public Builder skillTimeoutMinutes(long skillTimeoutMinutes) {
            this.skillTimeoutMinutes = skillTimeoutMinutes;
            return this;
        }

        public Builder summaryTimeoutMinutes(long summaryTimeoutMinutes) {
            this.summaryTimeoutMinutes = summaryTimeoutMinutes;
            return this;
        }

        public Builder ghAuthTimeoutSeconds(long ghAuthTimeoutSeconds) {
            this.ghAuthTimeoutSeconds = ghAuthTimeoutSeconds;
            return this;
        }

        public Builder maxRetries(int maxRetries) { this.maxRetries = maxRetries; return this; }
        public Builder maxAccumulatedSize(int maxAccumulatedSize) {
            this.maxAccumulatedSize = maxAccumulatedSize;
            return this;
        }

        public Builder initialAccumulatedCapacity(int initialAccumulatedCapacity) {
            this.initialAccumulatedCapacity = initialAccumulatedCapacity;
            return this;
        }

        public Builder instructionBufferExtraCapacity(int instructionBufferExtraCapacity) {
            this.instructionBufferExtraCapacity = instructionBufferExtraCapacity;
            return this;
        }

        public Builder checkpointDirectory(String checkpointDirectory) {
            this.checkpointDirectory = checkpointDirectory;
            return this;
        }

        public Builder summary(SummarySettings summary) {
            this.summary = summary;
            return this;
        }

        public ExecutionConfig build() {
            return new ExecutionConfig(
                parallelism,
                reviewPasses,
                orchestratorTimeoutMinutes,
                agentTimeoutMinutes,
                idleTimeoutMinutes,
                skillTimeoutMinutes,
                summaryTimeoutMinutes,
                ghAuthTimeoutSeconds,
                maxRetries,
                maxAccumulatedSize,
                initialAccumulatedCapacity,
                instructionBufferExtraCapacity,
                checkpointDirectory,
                summary
            );
        }
    }

    /// Executive summary generation settings.
    /// Bound to `reviewer.execution.summary` in YAML.
    @ConfigurationProperties("summary")
    public record SummarySettings(
        int maxContentPerAgent,
        int maxTotalPromptContent,
        int fallbackExcerptLength,
        int averageResultContentEstimate,
        int initialBufferMargin,
        int excerptNormalizationMultiplier
    ) {

        public static final int DEFAULT_MAX_CONTENT_PER_AGENT = 50_000;
        public static final int DEFAULT_MAX_TOTAL_PROMPT_CONTENT = 200_000;
        public static final int DEFAULT_FALLBACK_EXCERPT_LENGTH = 180;
        public static final int DEFAULT_AVERAGE_RESULT_CONTENT_ESTIMATE = 8192;
        public static final int DEFAULT_INITIAL_BUFFER_MARGIN = 4096;
        public static final int DEFAULT_EXCERPT_NORMALIZATION_MULTIPLIER = 3;

        public SummarySettings {
            maxContentPerAgent = ConfigDefaults.defaultIfNonPositive(maxContentPerAgent, DEFAULT_MAX_CONTENT_PER_AGENT);
            maxTotalPromptContent = ConfigDefaults.defaultIfNonPositive(maxTotalPromptContent, DEFAULT_MAX_TOTAL_PROMPT_CONTENT);
            fallbackExcerptLength = ConfigDefaults.defaultIfNonPositive(fallbackExcerptLength, DEFAULT_FALLBACK_EXCERPT_LENGTH);
            averageResultContentEstimate = ConfigDefaults.defaultIfNonPositive(averageResultContentEstimate, DEFAULT_AVERAGE_RESULT_CONTENT_ESTIMATE);
            initialBufferMargin = ConfigDefaults.defaultIfNonPositive(initialBufferMargin, DEFAULT_INITIAL_BUFFER_MARGIN);
            excerptNormalizationMultiplier = ConfigDefaults.defaultIfNonPositive(excerptNormalizationMultiplier, DEFAULT_EXCERPT_NORMALIZATION_MULTIPLIER);
        }
    }
}
