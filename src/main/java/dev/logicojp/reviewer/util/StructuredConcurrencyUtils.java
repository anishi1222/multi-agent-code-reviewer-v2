package dev.logicojp.reviewer.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/// Utility for working with {@link StructuredTaskScope} in preview JDK releases.
///
/// Workaround: {@code StructuredTaskScope.join()} does not support timeout natively.
/// Wrapping in {@code CompletableFuture.runAsync()} to enforce a wall-clock deadline.
/// TODO: Replace with {@code scope.joinUntil(Instant)} when available in a future JDK release.
public final class StructuredConcurrencyUtils {

    private StructuredConcurrencyUtils() {
    }

    /// Joins the given scope with a wall-clock timeout.
    ///
    /// @param scope the structured task scope to join
    /// @param timeout the maximum time to wait
    /// @param unit the time unit for the timeout
    /// @throws InterruptedException if the current thread is interrupted
    /// @throws TimeoutException if the join does not complete within the timeout
    /// @throws ExecutionException if the join fails with an exception
    public static <T> void joinWithTimeout(StructuredTaskScope<T, ?> scope,
                                       long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException, ExecutionException {
        var joinFuture = CompletableFuture.runAsync(() -> {
            try {
                scope.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
        joinFuture.get(timeout, unit);
    }
}
