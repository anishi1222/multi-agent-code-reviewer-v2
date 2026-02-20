package dev.logicojp.reviewer.target;

import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

public sealed interface ReviewTarget permits ReviewTarget.LocalTarget, ReviewTarget.GitHubTarget {

    Pattern REPOSITORY_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+/[a-zA-Z0-9._-]+$");

    record LocalTarget(Path directory) implements ReviewTarget {}
    record GitHubTarget(String repository) implements ReviewTarget {}

    static ReviewTarget gitHub(String repository) {
        if (repository == null || repository.isBlank()) {
            throw new IllegalArgumentException("Repository must not be null or blank");
        }
        if (!REPOSITORY_PATTERN.matcher(repository).matches()) {
            throw new IllegalArgumentException(
                "Invalid repository format: " + repository + ". Expected 'owner/repo' format.");
        }
        String[] segments = repository.split("/", 2);
        if (segments.length != 2 || isTraversalSegment(segments[0]) || isTraversalSegment(segments[1])) {
            throw new IllegalArgumentException("Repository name contains path traversal segment: " + repository);
        }
        return new GitHubTarget(repository);
    }

    static ReviewTarget local(Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("Directory must not be null");
        }
        return new LocalTarget(directory);
    }

    default String displayName() {
        return switch (this) {
            case GitHubTarget(String repository) -> repository;
            case LocalTarget(Path directory) -> directory.getFileName() != null
                ? directory.getFileName().toString()
                : directory.toString();
        };
    }

    default boolean isLocal() {
        return switch (this) {
            case LocalTarget _ -> true;
            case GitHubTarget _ -> false;
        };
    }

    default Optional<Path> localPath() {
        return switch (this) {
            case LocalTarget(Path directory) -> Optional.of(directory);
            case GitHubTarget(_) -> Optional.empty();
        };
    }

    default Path repositorySubPath() {
        return switch (this) {
            case GitHubTarget(String repository) -> {
                Path subPath = Path.of(repository).normalize();
                if (subPath.isAbsolute() || subPath.startsWith("..")) {
                    throw new IllegalArgumentException("Repository name contains path traversal: " + repository);
                }
                yield subPath;
            }
            case LocalTarget(Path directory) -> {
                Path fileName = directory.getFileName();
                yield fileName != null ? Path.of(fileName.toString()) : Path.of(directory.toString());
            }
        };
    }

    private static boolean isTraversalSegment(String segment) {
        return ".".equals(segment) || "..".equals(segment);
    }
}
