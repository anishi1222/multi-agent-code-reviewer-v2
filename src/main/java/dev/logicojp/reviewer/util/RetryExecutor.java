package dev.logicojp.reviewer.util;

import org.slf4j.Logger;

import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Predicate;

/// Generic retry executor with circuit-breaker integration and jittered backoff.
public final class RetryExecutor {

    @FunctionalInterface
    public interface Attempt<T> {
        T execute() throws Exception;
    }

    private RetryExecutor() {
    }

    public static <T> T execute(
        Attempt<T> attempt,
        Function<Exception, T> exceptionMapper,
        Predicate<T> isSuccess,
        Function<T, String> errorMessageExtractor,
        int maxAttempts,
        long backoffBaseMs,
        long backoffMaxMs,
        ApiCircuitBreaker circuitBreaker,
        Logger logger,
        String operationName
    ) {
        return executeWithGate(
            attempt,
            exceptionMapper,
            isSuccess,
            errorMessageExtractor,
            result -> BackoffUtils.isRetryableMessage(errorMessageExtractor.apply(result)),
            exception -> BackoffUtils.isRetryableMessage(exception.getMessage()),
            _ -> true,
            _ -> true,
            maxAttempts,
            backoffBaseMs,
            backoffMaxMs,
            circuitBreaker,
            logger,
            operationName,
            null
        );
    }

    public static <T> T executeWithGate(
        Attempt<T> attempt,
        Function<Exception, T> exceptionMapper,
        Predicate<T> isSuccess,
        Function<T, String> errorMessageExtractor,
        Predicate<T> isRetryableResult,
        Predicate<Exception> isRetryableException,
        Predicate<T> shouldRecordFailureResult,
        Predicate<Exception> shouldRecordFailureException,
        int maxAttempts,
        long backoffBaseMs,
        long backoffMaxMs,
        ApiCircuitBreaker circuitBreaker,
        Logger logger,
        String operationName,
        LongFunction<T> onCircuitOpen
    ) {
        int totalAttempts = Math.max(1, maxAttempts);
        T lastResult = null;

        for (int attemptNumber = 1; attemptNumber <= totalAttempts; attemptNumber++) {
            if (onCircuitOpen != null && !circuitBreaker.isRequestAllowed()) {
                return onCircuitOpen.apply(circuitBreaker.remainingOpenMs());
            }

            try {
                lastResult = attempt.execute();
                if (isSuccess.test(lastResult)) {
                    circuitBreaker.recordSuccess();
                    if (attemptNumber > 1) {
                        logger.info("{} succeeded on attempt {}/{}", operationName, attemptNumber, totalAttempts);
                    }
                    return lastResult;
                }

                if (shouldRecordFailureResult.test(lastResult)) {
                    circuitBreaker.recordFailure();
                }
                String errorMessage = errorMessageExtractor.apply(lastResult);
                if (attemptNumber < totalAttempts && isRetryableResult.test(lastResult)) {
                    BackoffUtils.sleepWithJitterQuietly(attemptNumber, backoffBaseMs, backoffMaxMs);
                    logger.warn("{} failed on attempt {}/{}: {}. Retrying...",
                        operationName, attemptNumber, totalAttempts, errorMessage);
                    continue;
                }
                logger.error("{} failed with non-retryable result on attempt {}/{}: {}",
                    operationName, attemptNumber, totalAttempts, errorMessage);
                return lastResult;
            } catch (Exception e) {
                if (shouldRecordFailureException.test(e)) {
                    circuitBreaker.recordFailure();
                }
                lastResult = exceptionMapper.apply(e);
                if (attemptNumber < totalAttempts && isRetryableException.test(e)) {
                    BackoffUtils.sleepWithJitterQuietly(attemptNumber, backoffBaseMs, backoffMaxMs);
                    logger.warn("{} threw exception on attempt {}/{}: {}. Retrying...",
                        operationName, attemptNumber, totalAttempts, e.getMessage(), e);
                    continue;
                }
                logger.error("{} threw non-retryable exception on attempt {}/{}: {}",
                    operationName, attemptNumber, totalAttempts, e.getMessage(), e);
                return lastResult;
            }
        }

        return lastResult;
    }
}
