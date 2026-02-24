package dev.logicojp.reviewer.orchestrator;

import dev.logicojp.reviewer.agent.ReviewAgent;
import dev.logicojp.reviewer.config.ExecutionConfig;
import dev.logicojp.reviewer.config.ResilienceConfig;
import dev.logicojp.reviewer.config.ReviewerConfig;
import dev.logicojp.reviewer.instruction.CustomInstruction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ReviewOrchestrator.Config")
class ReviewOrchestratorConfigTest {

    @Nested
    @DisplayName("コンストラクタ")
    class Constructor {

        @Test
        @DisplayName("executionConfigがnullの場合は例外")
        void throwsWhenExecutionConfigIsNull() {
            assertThatThrownBy(() -> new ReviewOrchestrator.Config(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            )).isInstanceOf(NullPointerException.class)
                .hasMessage("executionConfig must not be null");
        }

        @Test
        @DisplayName("null項目は安全なデフォルトに正規化される")
        void normalizesNullValuesToDefaults() {
            var config = new ReviewOrchestrator.Config(
                null,
                null,
                null,
                defaultExecutionConfig(),
                null,
                null,
                null,
                null,
                null
            );

            assertThat(config.localFileConfig()).isNotNull();
            assertThat(config.customInstructions()).isEmpty();
            assertThat(config.promptTemplates()).isNull();
            assertThat(config.resilienceConfig()).isNotNull();
            assertThat(config.resilienceConfig().review()).isNotNull();
        }

        @Test
        @DisplayName("customInstructionsは防御的コピーされる")
        void copiesCustomInstructionsDefensively() {
            var source = new ArrayList<>(List.of(
                new CustomInstruction("/tmp/a.instructions.md", "rule", CustomInstruction.Source.LOCAL_FILE, "**/*.java", "desc")
            ));

            var config = new ReviewOrchestrator.Config(
                null,
                null,
                new ReviewerConfig.LocalFiles(),
                defaultExecutionConfig(),
                source,
                "medium",
                "constraints",
                new ReviewAgent.PromptTemplates("focus", "header", "result"),
                new ResilienceConfig(null, null, null)
            );

            source.clear();

            assertThat(config.customInstructions()).hasSize(1);
            assertThatThrownBy(() -> config.customInstructions().add(
                new CustomInstruction("x", "y", CustomInstruction.Source.LOCAL_FILE, null, null)
            )).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    private static ExecutionConfig defaultExecutionConfig() {
        return new ExecutionConfig(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, null);
    }
}
