package dev.logicojp.reviewer.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/// Utility methods for graceful {@link ExecutorService} shutdown.
public final class ExecutorUtils {

    private ExecutorUtils() {
    }

    /// Shuts down the given {@link ExecutorService} gracefully.
    /// Waits up to {@code timeoutSeconds} for tasks to finish,
    /// then forcibly shuts down if they have not completed.
    ///
    /// @param executor the executor service to shut down (may be {@code null})
    /// @param timeoutSeconds maximum seconds to wait for orderly termination
    public static void shutdownGracefully(ExecutorService executor, long timeoutSeconds) {
        if (executor == null) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException _) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
