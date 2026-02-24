package dev.logicojp.reviewer.target;

import dev.logicojp.reviewer.config.ReviewerConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

/// Collects, filters, reads, and formats local source files for review.
/// Delegates directory walking, file reading, and markdown formatting to
/// dedicated collaborators for improved separation of concerns.
public class LocalFileProvider {

    // --- Public records ---

    public record LocalFile(String relativePath, String content, long sizeBytes) {}

    public record CollectionResult(String reviewContent,
                                   String directorySummary,
                                   int fileCount,
                                   long totalSizeBytes) {}

    // --- Fields ---

    private final Path baseDirectory;
    private final Path realBaseDirectory;
    private final LocalFileCandidateCollector candidateCollector;
    private final LocalFileReader localFileReader;
    private final LocalFileContentFormatter contentFormatter;
    private final LocalFileCollectionCoordinator collectionCoordinator;

    // --- Constructors ---

    public LocalFileProvider(Path baseDirectory) {
        this(baseDirectory, new ReviewerConfig.LocalFiles());
    }

    public LocalFileProvider(Path baseDirectory, ReviewerConfig.LocalFiles config) {
        if (baseDirectory == null) {
            throw new IllegalArgumentException("Base directory must not be null");
        }
        this.baseDirectory = baseDirectory.toAbsolutePath().normalize();
        try {
            this.realBaseDirectory = this.baseDirectory.toRealPath();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot resolve real path for base directory: " + baseDirectory, e);
        }
        LocalFileSelectionConfig selectionConfig = LocalFileSelectionConfig.from(config);
        this.candidateCollector = new LocalFileCandidateCollector(
            this.baseDirectory, this.realBaseDirectory, selectionConfig,
            org.slf4j.LoggerFactory.getLogger(LocalFileCandidateCollector.class));
        this.localFileReader = new LocalFileReader(
            this.baseDirectory, this.realBaseDirectory, selectionConfig,
            org.slf4j.LoggerFactory.getLogger(LocalFileReader.class));
        this.contentFormatter = new LocalFileContentFormatter(this.baseDirectory, selectionConfig);
        this.collectionCoordinator = new LocalFileCollectionCoordinator(
            this.baseDirectory, this.candidateCollector, this.localFileReader, this.contentFormatter);
    }

    // --- Public API ---

    List<LocalFile> collectFiles() {
        return collectionCoordinator.collectFiles();
    }

    public CollectionResult collectAndGenerate() {
        return collectionCoordinator.collectAndGenerate();
    }

    String generateReviewContent(List<LocalFile> files) {
        return contentFormatter.generateReviewContent(files);
    }

    String generateDirectorySummary(List<LocalFile> files) {
        return contentFormatter.generateDirectorySummary(files);
    }

}
