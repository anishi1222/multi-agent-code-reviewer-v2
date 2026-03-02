package dev.logicojp.reviewer.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SharedCircuitBreaker")
class SharedCircuitBreakerTest {

    @AfterEach
    void restoreDefaults() {
        SharedCircuitBreaker.reconfigure(8, 30_000L);
        SharedCircuitBreaker.forReview().reset();
        SharedCircuitBreaker.forSkill().reset();
        SharedCircuitBreaker.forSummary().reset();
    }

    @Test
    @DisplayName("パス別サーキットブレーカーは障害を分離する")
    void pathSpecificBreakersIsolateFailures() {
        SharedCircuitBreaker.reconfigure(2, 100L);
        SharedCircuitBreaker review = SharedCircuitBreaker.forReview();
        SharedCircuitBreaker skill = SharedCircuitBreaker.forSkill();

        review.onFailure();
        review.onFailure();

        assertThat(review.allowRequest()).isFalse();
        assertThat(skill.allowRequest()).isTrue();
    }

    @Test
    @DisplayName("失敗閾値に達するとリクエストを遮断する")
    void blocksAfterFailureThreshold() {
        AtomicLong clock = new AtomicLong(0L);
        SharedCircuitBreaker breaker = new SharedCircuitBreaker(3, 10_000L, clock::get);

        assertThat(breaker.allowRequest()).isTrue();
        breaker.onFailure();
        breaker.onFailure();
        breaker.onFailure();

        assertThat(breaker.allowRequest()).isFalse();
    }

    @Test
    @DisplayName("リセット時間経過後は再度リクエストを許可する")
    void allowsAfterResetTimeout() {
        AtomicLong clock = new AtomicLong(0L);
        SharedCircuitBreaker breaker = new SharedCircuitBreaker(2, 100L, clock::get);

        breaker.onFailure();
        breaker.onFailure();
        assertThat(breaker.allowRequest()).isFalse();

        clock.set(101L);
        assertThat(breaker.allowRequest()).isTrue();
    }

    @Test
    @DisplayName("成功時に状態をリセットする")
    void resetsOnSuccess() {
        AtomicLong clock = new AtomicLong(0L);
        SharedCircuitBreaker breaker = new SharedCircuitBreaker(2, 100L, clock::get);

        breaker.onFailure();
        breaker.onFailure();
        assertThat(breaker.allowRequest()).isFalse();

        breaker.onSuccess();

        assertThat(breaker.allowRequest()).isTrue();
    }
}
