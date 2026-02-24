package dev.logicojp.reviewer.util;

import java.util.concurrent.ThreadLocalRandom;

/// Utility methods for retry backoff with jitter.
public final class BackoffUtils {

    private BackoffUtils() {
    }

    public static void sleepWithJitter(int attempt, long baseMs, long maxMs) throws InterruptedException {
        long exponentialMs = Math.min(baseMs << Math.max(0, attempt - 1), maxMs);
        long halfMs = Math.max(1L, exponentialMs / 2L);
        long jitteredMs = halfMs + ThreadLocalRandom.current().nextLong(halfMs + 1L);
        Thread.sleep(jitteredMs);
    }

    public static void sleepWithJitterQuietly(int attempt, long baseMs, long maxMs) {
        try {
            sleepWithJitter(attempt, baseMs, maxMs);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}