package dev.logicojp.reviewer.report;

import dev.logicojp.reviewer.agent.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Merges multiple review results from the same agent (multi-pass reviews)
/// into a single consolidated `ReviewResult`.
///
/// When an agent performs multiple review passes, each pass may discover
/// different findings. This merger concatenates the successful results
/// with pass markers so that the per-agent report contains all findings.
public final class ReviewResultMerger {

    private static final Logger logger = LoggerFactory.getLogger(ReviewResultMerger.class);

    private ReviewResultMerger() {
        // utility class
    }

    /// Merges a flat list of review results (potentially multiple per agent)
    /// into a list with exactly one result per agent.
    ///
    /// Results are grouped by agent name. If an agent has only one result,
    /// it is returned as-is. If an agent has multiple results, the successful
    /// ones are concatenated with pass markers.
    ///
    /// @param results flat list of all review results (may contain duplicates per agent)
    /// @return merged list with one result per agent (order preserved)
    public static List<ReviewResult> mergeByAgent(List<ReviewResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        // Group results by agent name, preserving insertion order
        Map<String, List<ReviewResult>> byAgent = new LinkedHashMap<>();
        for (ReviewResult result : results) {
            String agentName = result.agentConfig() != null
                ? result.agentConfig().name()
                : "__unknown__";
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

    /// Merges multiple results from the same agent into a single result.
    private static ReviewResult mergeAgentResults(List<ReviewResult> agentResults) {
        // Use the config from the first result
        AgentConfig config = agentResults.getFirst().agentConfig();
        String repository = agentResults.getFirst().repository();

        List<ReviewResult> successful = agentResults.stream()
            .filter(ReviewResult::isSuccess)
            .toList();

        if (successful.isEmpty()) {
            // All passes failed — return the last failure
            logger.warn("Agent {}: all {} passes failed", config.name(), agentResults.size());
            return agentResults.getLast();
        }

        logger.info("Agent {}: merging {} successful pass(es) out of {} total",
            config.name(), successful.size(), agentResults.size());

        // Concatenate content with pass markers
        var contentBuilder = new StringBuilder();
        int passNumber = 1;
        for (ReviewResult result : successful) {
            if (passNumber > 1) {
                contentBuilder.append("\n\n---\n\n");
            }
            contentBuilder.append("## レビューパス %d / %d\n\n".formatted(passNumber, successful.size()));
            contentBuilder.append(result.content() != null ? result.content() : "");
            passNumber++;
        }

        // If some passes failed, append a note
        int failedCount = agentResults.size() - successful.size();
        if (failedCount > 0) {
            contentBuilder.append("\n\n---\n\n> **注記**: %d パス中 %d パスが失敗しました。上記は成功したパスの結果のみです。\n"
                .formatted(agentResults.size(), failedCount));
        }

        return ReviewResult.builder()
            .agentConfig(config)
            .repository(repository)
            .content(contentBuilder.toString())
            .success(true)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
