package dev.logicojp.reviewer.orchestrator;

import dev.logicojp.reviewer.agent.AgentConfig;
import dev.logicojp.reviewer.report.ReviewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/// Merges multiple review results from the same agent (multi-pass reviews)
/// into a single consolidated {@link ReviewResult}.
///
/// When an agent performs multiple review passes, each pass may discover
/// overlapping findings. This merger consolidates identical findings and
/// emits a deduplicated per-agent report.
///
/// Integrates finding parsing, similarity checking (Dice coefficient ≥ 0.80),
/// aggregation, and merged content formatting — all previously split across
/// 5 v1 files (ReviewResultMerger, ReviewFindingParser, ReviewFindingSimilarity,
/// AggregatedFinding, ReviewMergedContentFormatter).
public final class ReviewResultMerger {

    private static final Logger logger = LoggerFactory.getLogger(ReviewResultMerger.class);

    private ReviewResultMerger() {
    }

    // ========================================================================
    // Inner records
    // ========================================================================

    /// A parsed finding block from review output.
    record FindingBlock(String title, String body) {}

    /// Aggregated finding with deduplication tracking across passes.
    record AggregatedFinding(
        String title,
        String body,
        Set<Integer> passNumbers,
        String normalizedTitle,
        String normalizedPriority,
        String normalizedSummary,
        String normalizedLocation,
        Set<Integer> titleBigrams,
        Set<Integer> summaryBigrams,
        Set<Integer> locationBigrams
    ) {
        AggregatedFinding {
            Objects.requireNonNull(title);
            Objects.requireNonNull(body);
            passNumbers = Collections.unmodifiableSet(new LinkedHashSet<>(passNumbers));
        }

        AggregatedFinding withPass(int passNumber) {
            var newPasses = new LinkedHashSet<>(passNumbers);
            newPasses.add(passNumber);
            return new AggregatedFinding(title, body, newPasses,
                normalizedTitle, normalizedPriority, normalizedSummary,
                normalizedLocation, titleBigrams, summaryBigrams, locationBigrams);
        }

        boolean isNearDuplicateOf(NormalizedFinding incoming) {
            if (hasPriorityMismatch(incoming.priority())) return false;
            if (hasLocationContext(incoming.location())) return matchByLocationAndContent(incoming);
            return matchBySummaryAndTitle(incoming);
        }

        private boolean hasPriorityMismatch(String incomingPriority) {
            return !normalizedPriority.isEmpty()
                && !incomingPriority.isEmpty()
                && !normalizedPriority.equals(incomingPriority);
        }

        private boolean hasLocationContext(String incomingLocation) {
            return !normalizedLocation.isEmpty() && !incomingLocation.isEmpty();
        }

        private boolean matchByLocationAndContent(NormalizedFinding incoming) {
            if (!ReviewFindingSimilarity.isSimilarText(normalizedLocation, incoming.location(),
                locationBigrams, incoming.locationBigrams())) return false;
            return ReviewFindingSimilarity.isSimilarText(normalizedSummary, incoming.summary(),
                summaryBigrams, incoming.summaryBigrams())
                || ReviewFindingSimilarity.isSimilarText(normalizedTitle, incoming.title(),
                titleBigrams, incoming.titleBigrams())
                || ReviewFindingSimilarity.hasCommonKeyword(normalizedTitle, incoming.title());
        }

        private boolean matchBySummaryAndTitle(NormalizedFinding incoming) {
            return ReviewFindingSimilarity.isSimilarText(normalizedSummary, incoming.summary(),
                summaryBigrams, incoming.summaryBigrams())
                && ReviewFindingSimilarity.isSimilarText(normalizedTitle, incoming.title(),
                titleBigrams, incoming.titleBigrams());
        }
    }

    /// Normalized representation of a finding for comparison.
    record NormalizedFinding(
        String title,
        String priority,
        String summary,
        String location,
        Set<Integer> titleBigrams,
        Set<Integer> summaryBigrams,
        Set<Integer> locationBigrams
    ) {}

    // ========================================================================
    // Public API
    // ========================================================================

    /// Merges a flat list of review results (potentially multiple per agent)
    /// into a list with exactly one result per agent.
    public static List<ReviewResult> mergeByAgent(List<ReviewResult> results) {
        if (results == null || results.isEmpty()) return List.of();

        Map<String, List<ReviewResult>> byAgent = new java.util.LinkedHashMap<>();
        for (ReviewResult result : results) {
            String agentName = result.agentConfig() != null
                ? result.agentConfig().name() : "__unknown__";
            byAgent.computeIfAbsent(agentName, _ -> new ArrayList<>()).add(result);
        }

        List<ReviewResult> merged = new ArrayList<>(byAgent.size());
        for (var entry : byAgent.entrySet()) {
            List<ReviewResult> agentResults = entry.getValue();
            if (agentResults.size() == 1) {
                merged.add(agentResults.getFirst());
            } else {
                merged.add(mergeAgentResults(agentResults));
            }
        }
        return merged;
    }

    // ========================================================================
    // Merge logic
    // ========================================================================

    private static ReviewResult mergeAgentResults(List<ReviewResult> agentResults) {
        AgentConfig config = agentResults.getFirst().agentConfig();
        String repository = agentResults.getFirst().repository();

        List<ReviewResult> successful = agentResults.stream()
            .filter(ReviewResult::success).toList();

        if (successful.isEmpty()) {
            logger.warn("Agent {}: all {} passes failed", config.name(), agentResults.size());
            return agentResults.getLast();
        }

        logger.info("Agent {}: merging {} successful pass(es) out of {} total",
            config.name(), successful.size(), agentResults.size());

        var deduplicator = new ReviewFindingDeduplicator();

        for (int i = 0; i < successful.size(); i++) {
            deduplicator.processPassResult(successful.get(i), i + 1);
        }

        int failedCount = agentResults.size() - successful.size();
        String content = formatMergedContent(deduplicator.aggregatedFindings(), agentResults.size(), failedCount);

        return ReviewResult.builder()
            .agentConfig(config)
            .repository(repository)
            .content(content)
            .success(true)
            .build();
    }

    static String normalizeText(String value) {
        return ReviewFindingDeduplicator.normalizeText(value);
    }

    // ========================================================================
    // Merged content formatting
    // ========================================================================

    private static String formatMergedContent(Map<String, AggregatedFinding> aggregatedFindings,
                                              int totalPasses, int failedPasses) {
        return ReviewMergedContentFormatter.formatMergedContent(aggregatedFindings, totalPasses, failedPasses);
    }
}
