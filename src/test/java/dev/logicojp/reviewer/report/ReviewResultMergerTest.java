package dev.logicojp.reviewer.report;

import dev.logicojp.reviewer.agent.AgentConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewResultMerger")
class ReviewResultMergerTest {

    private static AgentConfig createAgent(String name) {
        return new AgentConfig(name, name + " Review", "model",
            "system", "instruction", null,
            List.of("area"), List.of());
    }

    private static ReviewResult successResult(AgentConfig agent, String content) {
        return ReviewResult.builder()
            .agentConfig(agent)
            .repository("test/repo")
            .content(content)
            .success(true)
            .timestamp(LocalDateTime.now())
            .build();
    }

    private static ReviewResult failureResult(AgentConfig agent, String error) {
        return ReviewResult.builder()
            .agentConfig(agent)
            .repository("test/repo")
            .success(false)
            .errorMessage(error)
            .timestamp(LocalDateTime.now())
            .build();
    }

    @Nested
    @DisplayName("mergeByAgent - 空・null入力")
    class EmptyInput {

        @Test
        @DisplayName("nullの場合は空リストを返す")
        void nullReturnsEmptyList() {
            assertThat(ReviewResultMerger.mergeByAgent(null)).isEmpty();
        }

        @Test
        @DisplayName("空リストの場合は空リストを返す")
        void emptyListReturnsEmptyList() {
            assertThat(ReviewResultMerger.mergeByAgent(List.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("mergeByAgent - 単一パス")
    class SinglePass {

        @Test
        @DisplayName("エージェントごとに1つの結果の場合はそのまま返す")
        void singleResultPerAgentReturnsAsIs() {
            var agent1 = createAgent("security");
            var agent2 = createAgent("performance");
            var result1 = successResult(agent1, "Security findings");
            var result2 = successResult(agent2, "Performance findings");

            List<ReviewResult> merged = ReviewResultMerger.mergeByAgent(List.of(result1, result2));

            assertThat(merged).hasSize(2);
            assertThat(merged.get(0)).isSameAs(result1);
            assertThat(merged.get(1)).isSameAs(result2);
        }
    }

    @Nested
    @DisplayName("mergeByAgent - マルチパス")
    class MultiPass {

        @Test
        @DisplayName("同一エージェントの複数成功結果がマージされる")
        void multipleSuccessResultsAreMerged() {
            var agent = createAgent("security");
            var result1 = successResult(agent, "Finding A");
            var result2 = successResult(agent, "Finding B");

            List<ReviewResult> merged = ReviewResultMerger.mergeByAgent(List.of(result1, result2));

            assertThat(merged).hasSize(1);
            ReviewResult mergedResult = merged.getFirst();
            assertThat(mergedResult.isSuccess()).isTrue();
            assertThat(mergedResult.agentConfig().name()).isEqualTo("security");
            assertThat(mergedResult.content()).contains("レビューパス 1 / 2");
            assertThat(mergedResult.content()).contains("レビューパス 2 / 2");
            assertThat(mergedResult.content()).contains("Finding A");
            assertThat(mergedResult.content()).contains("Finding B");
        }

        @Test
        @DisplayName("3パスの結果が正しくマージされる")
        void threePassResultsAreMerged() {
            var agent = createAgent("quality");
            var result1 = successResult(agent, "Pass 1 content");
            var result2 = successResult(agent, "Pass 2 content");
            var result3 = successResult(agent, "Pass 3 content");

            List<ReviewResult> merged = ReviewResultMerger.mergeByAgent(
                List.of(result1, result2, result3));

            assertThat(merged).hasSize(1);
            ReviewResult mergedResult = merged.getFirst();
            assertThat(mergedResult.content()).contains("レビューパス 1 / 3");
            assertThat(mergedResult.content()).contains("レビューパス 2 / 3");
            assertThat(mergedResult.content()).contains("レビューパス 3 / 3");
        }

        @Test
        @DisplayName("成功と失敗が混在する場合は成功のみマージし注記を追加する")
        void mixedSuccessAndFailureResultsMergeSuccessfulOnly() {
            var agent = createAgent("security");
            var success1 = successResult(agent, "Finding A");
            var failure = failureResult(agent, "Timeout");
            var success2 = successResult(agent, "Finding B");

            List<ReviewResult> merged = ReviewResultMerger.mergeByAgent(
                List.of(success1, failure, success2));

            assertThat(merged).hasSize(1);
            ReviewResult mergedResult = merged.getFirst();
            assertThat(mergedResult.isSuccess()).isTrue();
            assertThat(mergedResult.content()).contains("Finding A");
            assertThat(mergedResult.content()).contains("Finding B");
            assertThat(mergedResult.content()).contains("1 パスが失敗しました");
        }

        @Test
        @DisplayName("全パス失敗の場合は最後の失敗結果を返す")
        void allFailuresReturnsLastFailure() {
            var agent = createAgent("security");
            var failure1 = failureResult(agent, "Error 1");
            var failure2 = failureResult(agent, "Error 2");

            List<ReviewResult> merged = ReviewResultMerger.mergeByAgent(
                List.of(failure1, failure2));

            assertThat(merged).hasSize(1);
            ReviewResult mergedResult = merged.getFirst();
            assertThat(mergedResult.isSuccess()).isFalse();
            assertThat(mergedResult.errorMessage()).isEqualTo("Error 2");
        }

        @Test
        @DisplayName("複数エージェントのマルチパスが正しくグループ化される")
        void multipleAgentsMultiPassGroupedCorrectly() {
            var agent1 = createAgent("security");
            var agent2 = createAgent("performance");
            var sec1 = successResult(agent1, "Sec finding 1");
            var perf1 = successResult(agent2, "Perf finding 1");
            var sec2 = successResult(agent1, "Sec finding 2");
            var perf2 = successResult(agent2, "Perf finding 2");

            // Results interleaved as they might arrive from parallel execution
            List<ReviewResult> merged = ReviewResultMerger.mergeByAgent(
                List.of(sec1, perf1, sec2, perf2));

            assertThat(merged).hasSize(2);
            assertThat(merged.get(0).agentConfig().name()).isEqualTo("security");
            assertThat(merged.get(0).content()).contains("Sec finding 1");
            assertThat(merged.get(0).content()).contains("Sec finding 2");
            assertThat(merged.get(1).agentConfig().name()).isEqualTo("performance");
            assertThat(merged.get(1).content()).contains("Perf finding 1");
            assertThat(merged.get(1).content()).contains("Perf finding 2");
        }

        @Test
        @DisplayName("パス間にセパレータ（---）が挿入される")
        void separatorInsertedBetweenPasses() {
            var agent = createAgent("security");
            var result1 = successResult(agent, "Finding A");
            var result2 = successResult(agent, "Finding B");

            List<ReviewResult> merged = ReviewResultMerger.mergeByAgent(List.of(result1, result2));

            String content = merged.getFirst().content();
            assertThat(content).contains("---");
        }
    }
}
