package dev.logicojp.reviewer.agent;

import dev.logicojp.reviewer.config.CircuitBreakerConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/// Initializes path-specific circuit breakers from Micronaut configuration.
@Singleton
final class SharedCircuitBreakerInitializer {

    @Inject
    SharedCircuitBreakerInitializer(CircuitBreakerConfig config) {
        SharedCircuitBreaker.reconfigure(config.failureThreshold(), config.resetTimeoutMs());
    }
}
