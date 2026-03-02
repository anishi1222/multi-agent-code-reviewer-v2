package dev.logicojp.reviewer.agent;

import dev.logicojp.reviewer.config.CircuitBreakerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SharedCircuitBreakerInitializer")
class SharedCircuitBreakerInitializerTest {

    @AfterEach
    void restoreDefaults() {
        SharedCircuitBreaker.reconfigure(8, 30_000L);
        SharedCircuitBreaker.forReview().reset();
        SharedCircuitBreaker.forSkill().reset();
        SharedCircuitBreaker.forSummary().reset();
    }

    @Test
    @DisplayName("初期化時に全パスのサーキットブレーカー設定を反映する")
    void appliesConfiguredThresholdToAllPaths() {
        SharedCircuitBreaker.reconfigure(1, 100L);

        new SharedCircuitBreakerInitializer(new CircuitBreakerConfig(3, 5_000L));

        assertThresholdBehavior(SharedCircuitBreaker.forReview());
        assertThresholdBehavior(SharedCircuitBreaker.forSkill());
        assertThresholdBehavior(SharedCircuitBreaker.forSummary());
    }

    private void assertThresholdBehavior(SharedCircuitBreaker breaker) {
        assertThat(breaker.allowRequest()).isTrue();

        breaker.onFailure();
        breaker.onFailure();
        assertThat(breaker.allowRequest()).isTrue();

        breaker.onFailure();
        assertThat(breaker.allowRequest()).isFalse();
    }
}
