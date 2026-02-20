package dev.logicojp.reviewer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ModelConfig")
class ModelConfigTest {

    @Nested
    @DisplayName("コンストラクタ")
    class Constructor {

        @Test
        @DisplayName("引数なしコンストラクタはデフォルトモデルを設定する")
        void noArgConstructorSetsDefaults() {
            var config = new ModelConfig();

            assertThat(config.reviewModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
            assertThat(config.reportModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
            assertThat(config.summaryModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
            assertThat(config.defaultModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
            assertThat(config.reasoningEffort()).isEqualTo(ModelConfig.DEFAULT_REASONING_EFFORT);
        }

        @Test
        @DisplayName("nullのフィールドはデフォルト値に変換される")
        void nullFieldsAreConvertedToDefaults() {
            var config = new ModelConfig(null, null, null, null, null);

            assertThat(config.reviewModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
            assertThat(config.reportModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
            assertThat(config.summaryModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
            assertThat(config.reasoningEffort()).isEqualTo(ModelConfig.DEFAULT_REASONING_EFFORT);
        }

        @Test
        @DisplayName("空白のみのフィールドはデフォルト値に変換される")
        void blankFieldsAreConvertedToDefaults() {
            var config = new ModelConfig("  ", "\t", "\n", "  ", null);

            assertThat(config.reviewModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
            assertThat(config.reportModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
            assertThat(config.summaryModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
            assertThat(config.reasoningEffort()).isEqualTo(ModelConfig.DEFAULT_REASONING_EFFORT);
        }

        @Test
        @DisplayName("空文字列のフィールドはデフォルト値に変換される")
        void emptyFieldsAreConvertedToDefaults() {
            var config = new ModelConfig("", "", "", "", null);

            assertThat(config.reviewModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
            assertThat(config.reportModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
            assertThat(config.summaryModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
            assertThat(config.reasoningEffort()).isEqualTo(ModelConfig.DEFAULT_REASONING_EFFORT);
        }

        @Test
        @DisplayName("各フィールドを個別に設定する")
        void canonicalConstructorSetsIndividualFields() {
            var config = new ModelConfig("model-a", "model-b", "model-c", null, null);

            assertThat(config.reviewModel()).isEqualTo("model-a");
            assertThat(config.reportModel()).isEqualTo("model-b");
            assertThat(config.summaryModel()).isEqualTo("model-c");
            assertThat(config.reasoningEffort()).isEqualTo(ModelConfig.DEFAULT_REASONING_EFFORT);
        }

        @Test
        @DisplayName("reasoningEffortを個別に設定する")
        void canonicalConstructorSetsReasoningEffort() {
            var config = new ModelConfig("model-a", "model-b", "model-c", "low", null);

            assertThat(config.reasoningEffort()).isEqualTo("low");
        }
    }

    @Nested
    @DisplayName("defaultModel")
    class DefaultModelTests {

        @Test
        @DisplayName("defaultModelがYAMLで設定された場合、未指定のモデルはdefaultModelにフォールバックする")
        void defaultModelFallsBackForUnsetModels() {
            var config = new ModelConfig(null, null, null, null, "custom-fallback");

            assertThat(config.defaultModel()).isEqualTo("custom-fallback");
            assertThat(config.reviewModel()).isEqualTo("custom-fallback");
            assertThat(config.reportModel()).isEqualTo("custom-fallback");
            assertThat(config.summaryModel()).isEqualTo("custom-fallback");
        }

        @Test
        @DisplayName("defaultModelが未設定の場合はDEFAULT_MODELにフォールバックする")
        void nullDefaultModelFallsBackToConstant() {
            var config = new ModelConfig(null, null, null, null, null);

            assertThat(config.defaultModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
        }

        @Test
        @DisplayName("個別モデルが設定されている場合はdefaultModelより優先される")
        void individualModelsOverrideDefaultModel() {
            var config = new ModelConfig("review-x", null, null, null, "custom-fallback");

            assertThat(config.reviewModel()).isEqualTo("review-x");
            assertThat(config.reportModel()).isEqualTo("custom-fallback");
            assertThat(config.summaryModel()).isEqualTo("custom-fallback");
        }
    }

    @Nested
    @DisplayName("isReasoningModel")
    class IsReasoningModel {

        @Test
        @DisplayName("Opusモデルはreasoningモデルと判定される")
        void opusModelIsReasoning() {
            assertThat(ModelConfig.isReasoningModel("claude-opus-4.6")).isTrue();
            assertThat(ModelConfig.isReasoningModel("claude-opus-4.6-fast")).isTrue();
            assertThat(ModelConfig.isReasoningModel("Claude-Opus-4.5")).isTrue();
        }

        @Test
        @DisplayName("o3モデルはreasoningモデルと判定される")
        void o3ModelIsReasoning() {
            assertThat(ModelConfig.isReasoningModel("o3")).isTrue();
            assertThat(ModelConfig.isReasoningModel("o3-mini")).isTrue();
        }

        @Test
        @DisplayName("o4-miniモデルはreasoningモデルと判定される")
        void o4MiniModelIsReasoning() {
            assertThat(ModelConfig.isReasoningModel("o4-mini")).isTrue();
        }

        @Test
        @DisplayName("非reasoningモデルはfalseを返す")
        void nonReasoningModelReturnsFalse() {
            assertThat(ModelConfig.isReasoningModel("claude-sonnet-4")).isFalse();
            assertThat(ModelConfig.isReasoningModel("gpt-4o")).isFalse();
        }

        @Test
        @DisplayName("nullの場合はfalseを返す")
        void nullReturnsFalse() {
            assertThat(ModelConfig.isReasoningModel(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("resolveReasoningEffort")
    class ResolveReasoningEffort {

        @Test
        @DisplayName("推論モデルの場合は設定されたeffortを返す")
        void reasoningModelReturnsConfiguredEffort() {
            assertThat(ModelConfig.resolveReasoningEffort("claude-opus-4.6", "medium"))
                .isEqualTo("medium");
        }

        @Test
        @DisplayName("推論モデルでeffortがnullの場合はデフォルトeffortを返す")
        void reasoningModelWithNullEffortReturnsDefault() {
            assertThat(ModelConfig.resolveReasoningEffort("o3", null))
                .isEqualTo(ModelConfig.DEFAULT_REASONING_EFFORT);
        }

        @Test
        @DisplayName("非推論モデルの場合はnullを返す")
        void nonReasoningModelReturnsNull() {
            assertThat(ModelConfig.resolveReasoningEffort("claude-sonnet-4", "high")).isNull();
        }

        @Test
        @DisplayName("nullモデルの場合はnullを返す")
        void nullModelReturnsNull() {
            assertThat(ModelConfig.resolveReasoningEffort(null, "high")).isNull();
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("デフォルトのビルダーはデフォルトモデルで構築する")
        void defaultBuilderBuildsWithDefaults() {
            var config = ModelConfig.builder().build();

            assertThat(config.reviewModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
            assertThat(config.reportModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
            assertThat(config.summaryModel()).isEqualTo(ModelConfig.DEFAULT_MODEL);
            assertThat(config.reasoningEffort()).isEqualTo(ModelConfig.DEFAULT_REASONING_EFFORT);
        }

        @Test
        @DisplayName("allModelsで全フィールドを一括設定できる")
        void allModelsSetsAllFields() {
            var config = ModelConfig.builder().allModels("unified-model").build();

            assertThat(config.reviewModel()).isEqualTo("unified-model");
            assertThat(config.reportModel()).isEqualTo("unified-model");
            assertThat(config.summaryModel()).isEqualTo("unified-model");
            assertThat(config.defaultModel()).isEqualTo("unified-model");
        }

        @Test
        @DisplayName("メソッドチェーンで複数フィールドを設定できる")
        void canChainMultipleMethods() {
            var config = ModelConfig.builder()
                .reviewModel("review")
                .reportModel("report")
                .summaryModel("summary")
                .reasoningEffort("low")
                .build();

            assertThat(config.reviewModel()).isEqualTo("review");
            assertThat(config.reportModel()).isEqualTo("report");
            assertThat(config.summaryModel()).isEqualTo("summary");
            assertThat(config.reasoningEffort()).isEqualTo("low");
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toStringはモデル名とreasoningEffortを含む")
        void toStringContainsModelNames() {
            var config = new ModelConfig("r-model", "p-model", "s-model", "medium", null);
            var result = config.toString();

            assertThat(result).contains("r-model", "p-model", "s-model", "medium");
        }
    }
}
