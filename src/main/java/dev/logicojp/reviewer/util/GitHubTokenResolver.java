package dev.logicojp.reviewer.util;

import dev.logicojp.reviewer.config.ExecutionConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/// Resolves a GitHub token from CLI options, environment, or gh auth.
@Singleton
public final class GitHubTokenResolver {

    private static final Logger logger = LoggerFactory.getLogger(GitHubTokenResolver.class);
    private static final String PLACEHOLDER = "${GITHUB_TOKEN}";
    private static final String PATH_ENV = "PATH";
    private static final String GH_CLI_PATH_ENV = "GH_CLI_PATH";
    private static final long DEFAULT_TIMEOUT_SECONDS = 10;

    private final long timeoutSeconds;

    public GitHubTokenResolver() {
        this(DEFAULT_TIMEOUT_SECONDS);
    }

    public GitHubTokenResolver(long timeoutSeconds) {
        this.timeoutSeconds = (timeoutSeconds <= 0) ? DEFAULT_TIMEOUT_SECONDS : timeoutSeconds;
    }

    @Inject
    public GitHubTokenResolver(ExecutionConfig executionConfig) {
        this(executionConfig.ghAuthTimeoutSeconds());
    }

    public Optional<String> resolve(String providedToken) {
        String normalized = normalizeToken(providedToken);
        if (normalized != null) {
            return Optional.of(normalized);
        }

        return resolveFromGhAuth();
    }

    private String normalizeToken(String token) {
        if (token == null) {
            return null;
        }
        String trimmed = token.trim();
        if (trimmed.isEmpty() || PLACEHOLDER.equals(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private Optional<String> resolveFromGhAuth() {
        String ghPath = resolveGhCliPath();
        if (ghPath == null) {
            logger.warn("gh CLI not found. Install GitHub CLI or set {}.", GH_CLI_PATH_ENV);
            return Optional.empty();
        }
        ProcessBuilder builder = new ProcessBuilder(ghPath, "auth", "token", "-h", "github.com");
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    logger.warn("gh auth token timed out after {} seconds", timeoutSeconds);
                    return Optional.empty();
                }
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    logger.warn("gh auth token failed with exit code {}", exitCode);
                    return Optional.empty();
                }
                if (line == null || line.isBlank()) {
                    return Optional.empty();
                }
                return Optional.of(line.trim());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while resolving token from gh auth", e);
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Failed to resolve token from gh auth", e);
            return Optional.empty();
        }
    }

    private String resolveGhCliPath() {
        String explicit = System.getenv(GH_CLI_PATH_ENV);
        if (explicit != null && !explicit.isBlank()) {
            Path explicitPath = Path.of(explicit.trim()).toAbsolutePath().normalize();
            if (Files.isExecutable(explicitPath)
                && "gh".equals(explicitPath.getFileName().toString())) {
                return explicitPath.toString();
            }
            logger.warn("Invalid {} value: {}", GH_CLI_PATH_ENV, explicitPath);
            return null;
        }

        String pathEnv = System.getenv(PATH_ENV);
        if (pathEnv == null || pathEnv.isBlank()) {
            return null;
        }

        List<Path> candidates = new ArrayList<>();
        for (String entry : pathEnv.split(File.pathSeparator)) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            candidates.add(Path.of(entry.trim()).resolve("gh"));
        }

        for (Path candidate : candidates) {
            if (Files.isExecutable(candidate)) {
                return candidate.toAbsolutePath().normalize().toString();
            }
        }

        return null;
    }
}