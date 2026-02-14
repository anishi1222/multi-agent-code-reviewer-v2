package dev.logicojp.reviewer.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/// Utility for working with {@link StructuredTaskScope} in preview JDK releases.
///
/// Workaround: {@code StructuredTaskScope.join()} does not support timeout natively.
/// Uses a virtual thread to call {@code scope.join()} and enforces a wall-clock deadline
/// via {@code CompletableFuture.get(timeout, unit)}.
/// TODO(JDK 25+): Replace with {@code scope.joinUntil(Instant)} when StructuredTaskScope
/// exits preview and supports deadline-based join.
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
        var future = new CompletableFuture<Void>();
        Thread.ofVirtual().name("scope-join").start(() -> {
            try {
                scope.join();
                future.complete(null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.completeExceptionally(e);
            }
        });
        future.get(timeout, unit);
    }
}
