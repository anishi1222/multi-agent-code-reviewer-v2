package dev.logicojp.reviewer.report;

import dev.logicojp.reviewer.agent.AgentConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FindingsExtractor")
class FindingsExtractorTest {

    @Nested
    @DisplayName("extractFindings")
    class ExtractFindings {

        @Test
        @DisplayName("見出しとPriorityテーブルから指摘を抽出する")
        void extractsFromHeadingsAndTable() {
            String content = """
                ### 1. SQL Injection Risk
                | **Priority** | Critical |
                Some description.

                ### 2. Missing Input Validation
                | **Priority** | High |
                Details here.
                """;

            var findings = FindingsExtractor.extractFindings(content, "security");

            assertThat(findings).hasSize(2);
            assertThat(findings.get(0).title()).isEqualTo("SQL Injection Risk");
            assertThat(findings.get(0).priority()).isEqualTo("Critical");
            assertThat(findings.get(0).agent()).isEqualTo("security");
            assertThat(findings.get(1).priority()).isEqualTo("High");
        }

        @Test
        @DisplayName("指摘事項なしの場合は空リストを返す")
        void emptyForNoFindings() {
            String content = "指摘事項なし";
            var findings = FindingsExtractor.extractFindings(content, "agent");
            assertThat(findings).isEmpty();
        }

        @Test
        @DisplayName("見出しなしでPriorityのみの場合はデフォルトタイトルを使用する")
        void defaultTitlesWhenNoHeadings() {
            String content = """
                | **Priority** | Medium |
                Some finding.
                | **Priority** | Low |
                Another finding.
                """;

            var findings = FindingsExtractor.extractFindings(content, "agent");

            assertThat(findings).hasSize(2);
            assertThat(findings.get(0).title()).startsWith("Finding");
            assertThat(findings.get(0).priority()).isEqualTo("Medium");
        }

        @Test
        @DisplayName("Priorityなしで見出しのみの場合はUnknown優先度を使用する")
        void unknownPriorityWhenNoPriorityRow() {
            String content = """
                ### 1. Some Issue
                Description without priority table.
                """;

            var findings = FindingsExtractor.extractFindings(content, "agent");

            assertThat(findings).hasSize(1);
            assertThat(findings.getFirst().priority()).isEqualTo("Unknown");
        }
    }

    @Nested
    @DisplayName("buildFindingsSummary")
    class BuildFindingsSummary {

        @Test
        @DisplayName("優先度別にグループ化されたサマリーを生成する")
        void groupsByPriority() {
            var config = AgentConfig.builder().name("agent").displayName("Security Agent").build();
            String content = """
                ### 1. Critical Bug
                | **Priority** | Critical |

                ### 2. Minor Issue
                | **Priority** | Low |
                """;
            var result = ReviewResult.builder()
                .agentConfig(config)
                .content(content)
                .success(true)
                .build();

            String summary = FindingsExtractor.buildFindingsSummary(List.of(result));

            assertThat(summary).contains("#### Critical (1)");
            assertThat(summary).contains("#### Low (1)");
            assertThat(summary).contains("Critical Bug");
            assertThat(summary).contains("Minor Issue");
        }

        @Test
        @DisplayName("結果がnullの場合は空文字列を返す")
        void emptyForNull() {
            assertThat(FindingsExtractor.buildFindingsSummary(null)).isEmpty();
        }

        @Test
        @DisplayName("空の結果リストの場合は空文字列を返す")
        void emptyForEmptyList() {
            assertThat(FindingsExtractor.buildFindingsSummary(List.of())).isEmpty();
        }

        @Test
        @DisplayName("失敗結果は無視する")
        void ignoresFailedResults() {
            var result = ReviewResult.builder()
                .success(false)
                .errorMessage("timeout")
                .build();
            assertThat(FindingsExtractor.buildFindingsSummary(List.of(result))).isEmpty();
        }
    }
}
