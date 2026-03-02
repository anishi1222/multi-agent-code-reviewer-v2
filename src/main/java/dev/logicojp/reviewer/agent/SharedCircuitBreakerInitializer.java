package dev.logicojp.reviewer.agent;

import dev.logicojp.reviewer.config.CircuitBreakerConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/// Initializes the global shared circuit breaker from Micronaut configuration.
@Singleton
final class SharedCircuitBreakerInitializer {

    @Inject
    SharedCircuitBreakerInitializer(CircuitBreakerConfig config) {
        SharedCircuitBreaker.reconfigure(config.failureThreshold(), config.resetTimeoutMs());
    }
}
