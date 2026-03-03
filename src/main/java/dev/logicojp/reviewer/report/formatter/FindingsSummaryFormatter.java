package dev.logicojp.reviewer.report.formatter;

import dev.logicojp.reviewer.report.finding.FindingsExtractor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

public final class FindingsSummaryFormatter {

    private FindingsSummaryFormatter() {
    }

    public static String formatSummary(List<FindingsExtractor.Finding> findings) {
        var sb = new StringBuilder();
        Map<String, List<MergedFinding>> grouped = groupByPriorityWithDedup(findings);

        for (String priority : List.of("Critical", "High", "Medium", "Low", "Unknown")) {
            List<MergedFinding> group = grouped.getOrDefault(priority, List.of());
            if (group.isEmpty()) {
                continue;
            }
            appendPriorityBlock(sb, priority, group);
        }

        return sb.toString().stripTrailing();
    }

    private static Map<String, List<MergedFinding>> groupByPriorityWithDedup(List<FindingsExtractor.Finding> findings) {
        Map<String, Map<String, MergedFinding>> groupedByPriority = new LinkedHashMap<>();
        for (FindingsExtractor.Finding finding : findings) {
            String priority = finding.priority();
            String dedupKey = normalizeDedupKey(finding.title());
            Map<String, MergedFinding> byKey = groupedByPriority.computeIfAbsent(priority, _ -> new LinkedHashMap<>());
            byKey.computeIfAbsent(dedupKey, _ -> new MergedFinding(finding.title()))
                .merge(finding);
        }

        Map<String, List<MergedFinding>> grouped = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, MergedFinding>> entry : groupedByPriority.entrySet()) {
            grouped.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
        }
        return grouped;
    }

    private static String normalizeDedupKey(String title) {
        if (title == null) {
            return "";
        }
        return title.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static void appendPriorityBlock(StringBuilder sb,
                                            String priority,
                                            List<MergedFinding> group) {
        sb.append("#### ").append(priority).append(" (").append(group.size()).append(")\n\n");
        for (MergedFinding finding : group) {
            sb.append("- **").append(finding.title).append("**")
                .append(" — カテゴリー: ").append(String.join(", ", finding.categories))
                .append(" / 指摘元: ").append(String.join(", ", finding.agents))
                .append("\n");
        }
        sb.append("\n");
    }

    private static final class MergedFinding {
        private final String title;
        private final Set<String> agents = new LinkedHashSet<>();
        private final Set<String> categories = new LinkedHashSet<>();

        private MergedFinding(String title) {
            this.title = title;
        }

        private MergedFinding merge(FindingsExtractor.Finding finding) {
            agents.add(finding.agent());
            categories.add(finding.category());
            return this;
        }
    }
}