package dev.logicojp.reviewer.orchestrator;

import dev.logicojp.reviewer.report.ReviewResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/// Aggregates finding blocks across review passes and deduplicates near-identical findings.
final class ReviewFindingDeduplicator {

    private static final int FALLBACK_SIMILARITY_PREFIX_LENGTH = 500;
    private static final int FALLBACK_KEY_HASH_LENGTH_BYTES = 12;

    private final Map<String, ReviewResultMerger.AggregatedFinding> aggregatedFindings = new LinkedHashMap<>();
    private final Map<String, Set<String>> findingKeysByPriority = new LinkedHashMap<>();
    private final Map<String, Set<String>> findingKeysByPriorityAndPrefix = new LinkedHashMap<>();
    private final Set<String> fallbackPassContents = new LinkedHashSet<>();

    void processPassResult(ReviewResult result, int passNumber) {
        String content = result.content();
        if (content == null || content.isBlank()) {
            return;
        }

        var blocks = ReviewFindingBlockParser.extractFindingBlocks(content);
        if (blocks.isEmpty()) {
            addFallbackIfAbsent(content, passNumber);
            return;
        }

        for (var block : blocks) {
            mergeFindingBlock(block, passNumber);
        }
    }

    Map<String, ReviewResultMerger.AggregatedFinding> aggregatedFindings() {
        return aggregatedFindings;
    }

    private void addFallbackIfAbsent(String content, int passNumber) {
        String normalizedContent = normalizeText(content);
        if (!normalizedContent.isEmpty() && fallbackPassContents.add(normalizedContent)) {
            String contentHash = shortSha256(normalizedContent);
            aggregatedFindings.putIfAbsent(
                "fallback|" + contentHash,
                createFallback(content, passNumber));
        }
    }

    private static String shortSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, FALLBACK_KEY_HASH_LENGTH_BYTES);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private void mergeFindingBlock(ReviewResultMerger.FindingBlock block, int passNumber) {
        var normalized = normalizeFinding(block);
        String key = findingKeyFromNormalized(normalized, block.body());

        if (appendPassToExactMatch(key, passNumber)) {
            return;
        }

        String nearDuplicateKey = findNearDuplicateKey(normalized);
        if (nearDuplicateKey != null) {
            appendPassToNearDuplicate(nearDuplicateKey, passNumber);
            return;
        }

        addNewFinding(block, normalized, key, passNumber);
    }

    private boolean appendPassToExactMatch(String key, int passNumber) {
        var existingExact = aggregatedFindings.get(key);
        if (existingExact == null) {
            return false;
        }
        aggregatedFindings.put(key, existingExact.withPass(passNumber));
        return true;
    }

    private void appendPassToNearDuplicate(String nearDuplicateKey, int passNumber) {
        var nearExisting = aggregatedFindings.get(nearDuplicateKey);
        aggregatedFindings.put(nearDuplicateKey, nearExisting.withPass(passNumber));
    }

    private void addNewFinding(ReviewResultMerger.FindingBlock block,
                               ReviewResultMerger.NormalizedFinding normalized,
                               String key,
                               int passNumber) {
        aggregatedFindings.put(key, fromNormalized(block, normalized, passNumber));
        indexByPriority(normalized.priority(), key);
        indexByPriorityAndPrefix(
            normalized.priority(),
            ReviewFindingSimilarity.buildPrefixKey(normalized.title()),
            key);
    }

    private static ReviewResultMerger.NormalizedFinding normalizeFinding(ReviewResultMerger.FindingBlock block) {
        String normalizedTitle = normalizeText(block.title());
        String normalizedPriority = normalizeText(ReviewFindingBlockParser.extractTableValue(block.body(), "Priority"));
        String normalizedSummary = normalizeText(ReviewFindingBlockParser.extractTableValue(block.body(), "指摘の概要"));
        String normalizedLocation = normalizeText(ReviewFindingBlockParser.extractTableValue(block.body(), "該当箇所"));
        return new ReviewResultMerger.NormalizedFinding(
            normalizedTitle, normalizedPriority, normalizedSummary, normalizedLocation,
            ReviewFindingSimilarity.bigrams(normalizedTitle),
            ReviewFindingSimilarity.bigrams(normalizedSummary),
            ReviewFindingSimilarity.bigrams(normalizedLocation));
    }

    private static String findingKeyFromNormalized(ReviewResultMerger.NormalizedFinding normalized, String rawBody) {
        if (!normalized.title().isEmpty()
            && (!normalized.summary().isEmpty() || !normalized.location().isEmpty()
                || !normalized.priority().isEmpty())) {
            return String.join("|", normalized.title(), normalized.priority(),
                normalized.location(), normalized.summary());
        }
        return "raw|" + normalizeText(rawBody);
    }

    private static ReviewResultMerger.AggregatedFinding fromNormalized(ReviewResultMerger.FindingBlock block,
                                                                       ReviewResultMerger.NormalizedFinding normalized,
                                                                       int passNumber) {
        return new ReviewResultMerger.AggregatedFinding(
            block.title(), block.body(), new LinkedHashSet<>(Set.of(passNumber)),
            normalized.title(), normalized.priority(), normalized.summary(), normalized.location(),
            normalized.titleBigrams(), normalized.summaryBigrams(), normalized.locationBigrams());
    }

    private static ReviewResultMerger.AggregatedFinding createFallback(String rawContent, int passNumber) {
        String normalizedRaw = normalizeText(rawContent);
        String similarityTarget = normalizedRaw.length() > FALLBACK_SIMILARITY_PREFIX_LENGTH
            ? normalizedRaw.substring(0, FALLBACK_SIMILARITY_PREFIX_LENGTH)
            : normalizedRaw;
        return new ReviewResultMerger.AggregatedFinding(
            "レビュー結果", rawContent, new LinkedHashSet<>(Set.of(passNumber)),
            normalizeText("レビュー結果"), "", normalizedRaw, "",
            ReviewFindingSimilarity.bigrams(normalizeText("レビュー結果")),
            ReviewFindingSimilarity.bigrams(similarityTarget),
            Set.of());
    }

    private String findNearDuplicateKey(ReviewResultMerger.NormalizedFinding incoming) {
        String priorityKey = incoming.priority().isBlank() ? "" : incoming.priority();
        String titlePrefix = ReviewFindingSimilarity.buildPrefixKey(incoming.title());
        Set<String> keys = findingKeysByPriorityAndPrefix.get(priorityKey + "|" + titlePrefix);
        if (keys == null || keys.isEmpty()) {
            keys = findingKeysByPriority.getOrDefault(priorityKey, Set.of());
        }
        for (String key : keys) {
            var candidate = aggregatedFindings.get(key);
            if (candidate != null && candidate.isNearDuplicateOf(incoming)) {
                return key;
            }
        }
        return null;
    }

    private void indexByPriority(String priority, String key) {
        String indexKey = (priority == null || priority.isBlank()) ? "" : priority;
        findingKeysByPriority.computeIfAbsent(indexKey, _ -> new LinkedHashSet<>()).add(key);
    }

    private void indexByPriorityAndPrefix(String priority, String prefix, String key) {
        String priorityKey = (priority == null || priority.isBlank()) ? "" : priority;
        findingKeysByPriorityAndPrefix.computeIfAbsent(priorityKey + "|" + prefix, _ -> new LinkedHashSet<>()).add(key);
    }

    static String normalizeText(String value) {
        return ReviewFindingSimilarity.normalizeText(value);
    }
}