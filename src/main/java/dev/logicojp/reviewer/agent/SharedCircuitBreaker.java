package dev.logicojp.reviewer.agent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

/// Simple circuit breaker that tracks consecutive failures and temporarily
/// blocks requests after a threshold is exceeded.
///
/// Thread-safe via atomic operations and volatile fields.
/// Shared across all Copilot call paths (review, skill, summary).
public final class SharedCircuitBreaker {

    private static final int DEFAULT_FAILURE_THRESHOLD = 8;
    private static final long DEFAULT_RESET_TIMEOUT_MS = 30_000L;

    private static final AtomicReference<SharedCircuitBreaker> GLOBAL =
        new AtomicReference<>(new SharedCircuitBreaker(DEFAULT_FAILURE_THRESHOLD, DEFAULT_RESET_TIMEOUT_MS));

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

    /// Returns the global circuit breaker shared across all Copilot call paths.
    public static SharedCircuitBreaker global() {
        return GLOBAL.get();
    }

    /// Reconfigures the global circuit breaker with new thresholds.
    public static void reconfigure(int failureThreshold, long resetTimeoutMs) {
        GLOBAL.set(new SharedCircuitBreaker(failureThreshold, resetTimeoutMs));
    }

    public boolean allowRequest() {
        int failures = consecutiveFailures.get();
        if (failures < failureThreshold) return true;
        long openedAt = openedAtMs;
        if (openedAt < 0) return true;
        long elapsedMs = clock.getAsLong() - openedAt;
        if (elapsedMs >= resetTimeoutMs) {
            // CAS ensures only one thread transitions to half-open
            if (consecutiveFailures.compareAndSet(failures, failureThreshold - 1)) {
                openedAtMs = -1L;
                return true;
            }
            return false;
        }
        return false;
    }

    public void onSuccess() {
        consecutiveFailures.set(0);
        openedAtMs = -1L;
    }

    public void onFailure() {
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
