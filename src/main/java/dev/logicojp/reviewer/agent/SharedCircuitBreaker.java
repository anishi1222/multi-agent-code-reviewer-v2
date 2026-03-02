package dev.logicojp.reviewer.agent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/// Simple circuit breaker that tracks consecutive failures and temporarily
/// blocks requests after a threshold is exceeded.
///
/// Thread-safe via atomic operations and volatile fields.
final class SharedCircuitBreaker {
    private final int failureThreshold;
    private final long resetTimeoutMs;
    private final LongSupplier clock;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private volatile long openedAtMs = -1L;

    SharedCircuitBreaker(int failureThreshold, long resetTimeoutMs) {
        this(failureThreshold, resetTimeoutMs, System::currentTimeMillis);
    }

    SharedCircuitBreaker(int failureThreshold, long resetTimeoutMs, LongSupplier clock) {
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMs = resetTimeoutMs;
        this.clock = clock;
    }

    boolean allowRequest() {
        int failures = consecutiveFailures.get();
        if (failures < failureThreshold) return true;
        long openedAt = openedAtMs;
        if (openedAt < 0) return true;
        long elapsedMs = clock.getAsLong() - openedAt;
        if (elapsedMs >= resetTimeoutMs) {
            consecutiveFailures.set(Math.max(0, failureThreshold - 1));
            openedAtMs = -1L;
            return true;
        }
        return false;
    }

    void onSuccess() {
        consecutiveFailures.set(0);
        openedAtMs = -1L;
    }

    void onFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= failureThreshold && openedAtMs < 0) {
            openedAtMs = clock.getAsLong();
        }
    }

    /// Resets the circuit breaker to its initial state.
    void reset() {
        consecutiveFailures.set(0);
        openedAtMs = -1L;
    }
}
