package dev.logicojp.reviewer.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StructuredConcurrencyUtils")
class StructuredConcurrencyUtilsTest {

    @Nested
    @DisplayName("joinWithTimeout")
    class JoinWithTimeout {

        @Test
        @DisplayName("タイムアウト期間内に完了しない場合はTimeoutExceptionをスローする")
        void throwsTimeoutException() throws Exception {
            try (var scope = StructuredTaskScope.<String>open()) {
                scope.fork(() -> {
                    Thread.sleep(60_000);
                    return "late";
                });
                assertThatThrownBy(() ->
                    StructuredConcurrencyUtils.joinWithTimeout(scope, 200, TimeUnit.MILLISECONDS)
                ).isInstanceOf(TimeoutException.class);
                // Must join before scope.close() to satisfy StructuredTaskScope contract
                try { scope.join(); } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
