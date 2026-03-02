package dev.logicojp.reviewer.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionEventException")
class SessionEventExceptionTest {

    @Test
    @DisplayName("メッセージを保持する")
    void keepsMessage() {
        SessionEventException exception = new SessionEventException("session error");

        assertThat(exception).hasMessage("session error");
    }
}
