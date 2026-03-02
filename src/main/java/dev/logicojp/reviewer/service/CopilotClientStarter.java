package dev.logicojp.reviewer.service;

import dev.logicojp.reviewer.util.RetryPolicyUtils;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Singleton
public class CopilotClientStarter {

    interface StartableClient {
        void start(long timeoutSeconds) throws ExecutionException, TimeoutException, InterruptedException;

        void close() throws Exception;
    }

    private static final Logger logger = LoggerFactory.getLogger(CopilotClientStarter.class);
    private static final int MAX_START_ATTEMPTS = 3;
    private static final long START_BACKOFF_BASE_MS = 2_000L;
    private static final long START_BACKOFF_MAX_MS = 15_000L;

    public void start(StartableClient client,
                      long timeoutSeconds,
                      CopilotStartupErrorFormatter startupErrorFormatter)
        throws InterruptedException {
        for (int attempt = 1; attempt <= MAX_START_ATTEMPTS; attempt++) {
            try {
                client.start(timeoutSeconds);
                return;
            } catch (ExecutionException e) {
                if (RetryPolicyUtils.shouldRetry(attempt, MAX_START_ATTEMPTS,
                        RetryPolicyUtils.isTransientException(e))) {
                    long backoff = RetryPolicyUtils.computeBackoffWithJitter(
                        START_BACKOFF_BASE_MS, START_BACKOFF_MAX_MS, attempt);
                    logger.warn("Copilot client start failed (attempt {}/{}), retrying in {}ms: {}",
                        attempt, MAX_START_ATTEMPTS, backoff, e.getMessage());
                    Thread.sleep(backoff);
                    continue;
                }
                closeQuietly(client);
                throw mapExecutionException(e, startupErrorFormatter);
            } catch (TimeoutException e) {
                if (RetryPolicyUtils.shouldRetry(attempt, MAX_START_ATTEMPTS, true)) {
                    long backoff = RetryPolicyUtils.computeBackoffWithJitter(
                        START_BACKOFF_BASE_MS, START_BACKOFF_MAX_MS, attempt);
                    logger.warn("Copilot client start timed out (attempt {}/{}), retrying in {}ms",
                        attempt, MAX_START_ATTEMPTS, backoff);
                    Thread.sleep(backoff);
                    continue;
                }
                closeQuietly(client);
                throw timeoutDuringStart(timeoutSeconds, startupErrorFormatter, e);
            }
        }
    }

    private CopilotCliException mapExecutionException(ExecutionException e,
                                                     CopilotStartupErrorFormatter startupErrorFormatter) {
        Throwable cause = e.getCause();
        if (cause instanceof TimeoutException) {
            return new CopilotCliException(startupErrorFormatter.buildProtocolTimeoutMessage(), cause);
        }
        if (cause != null) {
            return new CopilotCliException("Copilot client start failed: " + cause.getMessage(), cause);
        }
        return new CopilotCliException("Copilot client start failed", e);
    }

    private CopilotCliException timeoutDuringStart(long timeoutSeconds,
                                                  CopilotStartupErrorFormatter startupErrorFormatter,
                                                  TimeoutException e) {
        return new CopilotCliException(startupErrorFormatter.buildClientTimeoutMessage(timeoutSeconds), e);
    }

    private void closeQuietly(StartableClient client) {
        try {
            client.close();
        } catch (Exception e) {
            logger.debug("Failed to close Copilot client after startup failure: {}", e.getMessage(), e);
        }
    }
}
