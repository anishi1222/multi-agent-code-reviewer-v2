package dev.logicojp.reviewer.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CopilotCliHealthChecker")
class CopilotCliHealthCheckerTest {

    private final CopilotCliHealthChecker checker = new CopilotCliHealthChecker();

    @Test
    @DisplayName("cliPath が空の場合は何もしない")
    void doesNothingWhenCliPathIsBlank() {
        assertThatCode(() -> checker.verifyCliHealthy("", false)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("正常なCLIパスではヘルスチェックが成功する")
    void succeedsWithValidCliPath() {
        assertThatCode(() -> checker.verifyCliHealthy("/bin/true", true)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("不正なCLIパスではCopilotCliExceptionを送出する")
    void throwsWhenCliPathIsInvalid() {
        assertThatThrownBy(() -> checker.verifyCliHealthy("/path/does/not/exist", true))
            .isInstanceOf(CopilotCliException.class)
            .hasMessageContaining("Failed to execute Copilot CLI");
    }
}
