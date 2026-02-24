package dev.logicojp.reviewer.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("BackoffUtils")
class BackoffUtilsTest {

    @Test
    @DisplayName("quiet版は割り込み時に例外を送出せず割り込みフラグを復元する")
    void quietlyRestoresInterruptedFlag() {
        Thread.currentThread().interrupt();
        try {
            assertThatCode(() -> BackoffUtils.sleepWithJitterQuietly(1, 1, 1))
                .doesNotThrowAnyException();
            assertThatCode(() -> {
                if (!Thread.currentThread().isInterrupted()) {
                    throw new IllegalStateException("interrupt flag should be restored");
                }
            }).doesNotThrowAnyException();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    @DisplayName("attemptが0以下でもsleepWithJitterは実行できる")
    void sleepWithJitterAcceptsNonPositiveAttempt() {
        assertThatCode(() -> BackoffUtils.sleepWithJitter(0, 1, 1))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Equal Jitterで最低待機時間を保証する")
    void sleepWithJitterHasMinimumDelay() throws InterruptedException {
        long start = System.nanoTime();
        BackoffUtils.sleepWithJitter(3, 8, 32);
        long elapsedNs = System.nanoTime() - start;

        assertThat(elapsedNs).isGreaterThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(14));
    }
}
