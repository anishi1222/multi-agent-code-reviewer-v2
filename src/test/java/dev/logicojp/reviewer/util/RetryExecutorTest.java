package dev.logicojp.reviewer.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RetryExecutor")
class RetryExecutorTest {

    private record AttemptResult(boolean success, String errorMessage) {
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("リトライ可能な例外は再試行後に成功できる")
        void retriesOnRetryableException() {
            var attempts = new AtomicInteger();
            var circuitBreaker = new ApiCircuitBreaker(3, TimeUnit.SECONDS.toMillis(10), Clock.systemUTC());

            AttemptResult result = RetryExecutor.execute(
                () -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw new RuntimeException("network timeout");
                    }
                    return new AttemptResult(true, null);
                },
                e -> new AttemptResult(false, e.getMessage()),
                AttemptResult::success,
                AttemptResult::errorMessage,
                2,
                1,
                1,
                circuitBreaker,
                LoggerFactory.getLogger(RetryExecutorTest.class),
                "review-op"
            );

            assertThat(attempts.get()).isEqualTo(2);
            assertThat(result.success()).isTrue();
            assertThat(circuitBreaker.isRequestAllowed()).isTrue();
        }

        @Test
        @DisplayName("リトライ不可の例外は再試行せず終了する")
        void doesNotRetryOnNonRetryableException() {
            var attempts = new AtomicInteger();
            var circuitBreaker = new ApiCircuitBreaker(3, TimeUnit.SECONDS.toMillis(10), Clock.systemUTC());

            AttemptResult result = RetryExecutor.execute(
                () -> {
                    attempts.incrementAndGet();
                    throw new IllegalStateException("invalid input");
                },
                e -> new AttemptResult(false, e.getMessage()),
                AttemptResult::success,
                AttemptResult::errorMessage,
                3,
                1,
                1,
                circuitBreaker,
                LoggerFactory.getLogger(RetryExecutorTest.class),
                "review-op"
            );

            assertThat(attempts.get()).isEqualTo(1);
            assertThat(result.success()).isFalse();
            assertThat(result.errorMessage()).contains("invalid input");
        }

        @Test
        @DisplayName("リトライ可能な失敗結果は再試行後に成功できる")
        void retriesOnRetryableFailureResult() {
            var attempts = new AtomicInteger();
            var circuitBreaker = new ApiCircuitBreaker(3, TimeUnit.SECONDS.toMillis(10), Clock.systemUTC());

            AttemptResult result = RetryExecutor.execute(
                () -> {
                    if (attempts.incrementAndGet() == 1) {
                        return new AttemptResult(false, "429 rate limit");
                    }
                    return new AttemptResult(true, null);
                },
                e -> new AttemptResult(false, e.getMessage()),
                AttemptResult::success,
                AttemptResult::errorMessage,
                2,
                1,
                1,
                circuitBreaker,
                LoggerFactory.getLogger(RetryExecutorTest.class),
                "review-op"
            );

            assertThat(attempts.get()).isEqualTo(2);
            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("リトライ不可の失敗結果は再試行せず終了する")
        void doesNotRetryOnNonRetryableFailureResult() {
            var attempts = new AtomicInteger();
            var circuitBreaker = new ApiCircuitBreaker(3, TimeUnit.SECONDS.toMillis(10), Clock.systemUTC());

            AttemptResult result = RetryExecutor.execute(
                () -> {
                    attempts.incrementAndGet();
                    return new AttemptResult(false, "validation error");
                },
                e -> new AttemptResult(false, e.getMessage()),
                AttemptResult::success,
                AttemptResult::errorMessage,
                3,
                1,
                1,
                circuitBreaker,
                LoggerFactory.getLogger(RetryExecutorTest.class),
                "review-op"
            );

            assertThat(attempts.get()).isEqualTo(1);
            assertThat(result.success()).isFalse();
            assertThat(result.errorMessage()).contains("validation error");
        }
    }
}
