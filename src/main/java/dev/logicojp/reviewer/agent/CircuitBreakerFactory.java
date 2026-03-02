package dev.logicojp.reviewer.agent;

import dev.logicojp.reviewer.config.CircuitBreakerConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/// Provides path-specific circuit breaker instances managed by Micronaut DI.
@Singleton
public final class CircuitBreakerFactory {

    private final SharedCircuitBreaker review;
    private final SharedCircuitBreaker skill;
    private final SharedCircuitBreaker summary;

    @Inject
    public CircuitBreakerFactory(CircuitBreakerConfig config) {
        this.review = new SharedCircuitBreaker(config.failureThreshold(), config.resetTimeoutMs());
        this.skill = new SharedCircuitBreaker(config.failureThreshold(), config.resetTimeoutMs());
        this.summary = new SharedCircuitBreaker(config.failureThreshold(), config.resetTimeoutMs());
    }

    public SharedCircuitBreaker forReview() {
        return review;
    }

    public SharedCircuitBreaker forSkill() {
        return skill;
    }

    public SharedCircuitBreaker forSummary() {
        return summary;
    }
}
