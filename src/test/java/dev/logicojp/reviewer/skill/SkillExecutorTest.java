package dev.logicojp.reviewer.skill;

import com.github.copilot.sdk.CopilotClient;
import dev.logicojp.reviewer.util.ApiCircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SkillExecutor")
class SkillExecutorTest {

    @Nested
    @DisplayName("Result")
    class ResultFactory {

        @Test
        @DisplayName("success factory は成功結果を生成する")
        void successFactoryCreatesSuccessResult() {
            var result = SkillExecutor.Result.success("skill-a", "ok-content");

            assertThat(result.skillId()).isEqualTo("skill-a");
            assertThat(result.success()).isTrue();
            assertThat(result.content()).isEqualTo("ok-content");
            assertThat(result.errorMessage()).isNull();
            assertThat(result.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("failure factory は失敗結果を生成する")
        void failureFactoryCreatesFailureResult() {
            var result = SkillExecutor.Result.failure("skill-a", "boom");

            assertThat(result.skillId()).isEqualTo("skill-a");
            assertThat(result.success()).isFalse();
            assertThat(result.content()).isNull();
            assertThat(result.errorMessage()).isEqualTo("boom");
            assertThat(result.timestamp()).isBeforeOrEqualTo(Instant.now());
        }
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("サーキットブレーカーopen時は即時失敗を返す")
        void returnsFailureImmediatelyWhenCircuitBreakerOpen() throws Exception {
            try (var fixture = newFixture()) {
                forceCircuitOpen(fixture.executor);
                var skill = SkillDefinition.of("skill-a", "Skill A", "desc", "prompt");

                var result = fixture.executor.execute(skill, Map.of());

                assertThat(result.success()).isFalse();
                assertThat(result.errorMessage()).contains("circuit breaker is open");
            }
        }

        @Test
        @DisplayName("必須パラメータ不足は失敗結果を返す")
        void returnsFailureWhenRequiredParameterMissing() {
            try (var fixture = newFixture()) {
                var skill = new SkillDefinition(
                    "skill-required",
                    "Required Skill",
                    "desc",
                    "hello ${name}",
                    java.util.List.of(SkillDefinition.Parameter.required("name", "user name")),
                    Map.of()
                );

                var result = fixture.executor.execute(skill, Map.of());

                assertThat(result.success()).isFalse();
                assertThat(result.errorMessage()).contains("Missing required parameter: name");
            }
        }

        @Test
        @DisplayName("パラメータ値長さ超過は失敗結果を返す")
        void returnsFailureWhenParameterValueTooLong() {
            try (var fixture = newFixtureWithLimit(5)) {
                var skill = new SkillDefinition(
                    "skill-long",
                    "Long Skill",
                    "desc",
                    "hello ${name}",
                    java.util.List.of(SkillDefinition.Parameter.required("name", "user name")),
                    Map.of()
                );

                var result = fixture.executor.execute(skill, Map.of("name", "too-long-value"));

                assertThat(result.success()).isFalse();
                assertThat(result.errorMessage()).contains("Parameter value too long for: name");
            }
        }
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("defaultModelがnullなら例外")
        void throwsWhenDefaultModelNull() {
            assertThatThrownBy(() -> new SkillExecutor.Config(
                null, 1, 100, 10, 1, 10, 2, 1, 2
            )).isInstanceOf(NullPointerException.class)
                .hasMessage("defaultModel must not be null");
        }

        @Test
        @DisplayName("mcpServersは防御的コピーされる")
        void copiesMcpServersDefensively() throws Exception {
            try (var fixture = newFixture()) {
                Map<String, Object> mcpServers = new HashMap<>();
                mcpServers.put("github", Map.of("url", "https://example"));

                var executor = new SkillExecutor(
                    fixture.client,
                    mcpServers,
                    new SkillExecutor.Config("gpt-5", 1, 100, 10, 1, 10, 2, 1, 2)
                );

                mcpServers.clear();

                Field field = SkillExecutor.class.getDeclaredField("cachedMcpServers");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Object> cached = (Map<String, Object>) field.get(executor);

                assertThat(cached).containsKey("github");
                assertThatThrownBy(() -> cached.put("x", "y"))
                    .isInstanceOf(UnsupportedOperationException.class);
            }
        }
    }

    private static void forceCircuitOpen(SkillExecutor executor) throws Exception {
        Field field = SkillExecutor.class.getDeclaredField("apiCircuitBreaker");
        field.setAccessible(true);
        ApiCircuitBreaker breaker = (ApiCircuitBreaker) field.get(executor);
        breaker.recordFailure();
    }

    private static SkillExecutorFixture newFixture() {
        return newFixtureWithLimit(100);
    }

    private static SkillExecutorFixture newFixtureWithLimit(int maxParameterValueLength) {
        var client = new CopilotClient();
        var config = new SkillExecutor.Config(
            "gpt-5",
            1,
            maxParameterValueLength,
            10,
            1,
            30,
            2,
            1,
            2
        );
        return new SkillExecutorFixture(new SkillExecutor(client, Map.of(), config), client);
    }

    private record SkillExecutorFixture(SkillExecutor executor, CopilotClient client) implements AutoCloseable {
        @Override
        public void close() {
            client.close();
        }
    }
}
