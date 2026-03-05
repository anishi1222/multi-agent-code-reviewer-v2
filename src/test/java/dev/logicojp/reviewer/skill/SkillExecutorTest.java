package dev.logicojp.reviewer.skill;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SkillExecutor")
class SkillExecutorTest {

    @Test
    @DisplayName("必須パラメータ不足時は失敗結果を返す")
    void returnsFailureWhenRequiredParameterMissing() {
        SkillDefinition skill = new SkillDefinition(
            "s1",
            "skill",
            "desc",
            "hello ${name}",
            List.of(SkillParameter.required("name", "name")),
            Map.of()
        );

        SkillExecutor executor = new SkillExecutor(
            null,
            null,
            null,
            new SkillExecutor.SkillExecutorConfig("model", 1, 10_000, 30)
        );

        SkillResult result = executor.execute(skill, Map.of());

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Missing required parameter");
    }

    @Test
    @DisplayName("close は例外なく呼び出せる")
    void closeIsSafeNoOp() {
        SkillExecutor executor = new SkillExecutor(
            null,
            null,
            null,
            new SkillExecutor.SkillExecutorConfig("model", 1, 10_000, 30)
        );

        executor.close();
        assertThat(true).isTrue();
    }
}
