package dev.logicojp.reviewer.target;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/// Coordinates local file collection by orchestrating collector, reader, and formatter.
final class LocalFileCollectionCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileCollectionCoordinator.class);

    private final Path baseDirectory;
    private final LocalFileCandidateCollector candidateCollector;
    private final LocalFileReader fileReader;
    private final LocalFileContentFormatter contentFormatter;

    LocalFileCollectionCoordinator(Path baseDirectory,
                                   LocalFileCandidateCollector candidateCollector,
                                   LocalFileReader fileReader,
                                   LocalFileContentFormatter contentFormatter) {
        this.baseDirectory = baseDirectory;
        this.candidateCollector = candidateCollector;
        this.fileReader = fileReader;
        this.contentFormatter = contentFormatter;
    }

    List<LocalFileProvider.LocalFile> collectFiles() {
        if (isMissingBaseDirectory()) {
            logMissingBaseDirectory();
            return List.of();
        }
        List<LocalFileProvider.LocalFile> files = new ArrayList<>();
        try {
            List<LocalFileCandidateCollector.Candidate> candidates = candidateCollector.collectCandidates();
            LocalFileReader.ProcessingResult result = fileReader.processCandidates(candidates,
                (relativePath, content, size) -> files.add(new LocalFileProvider.LocalFile(relativePath, content, size)));
            logCollectedFiles(result.fileCount(), result.totalSize());
            return List.copyOf(files);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk directory: " + baseDirectory, e);
        }
    }

    LocalFileProvider.CollectionResult collectAndGenerate() {
        if (isMissingBaseDirectory()) {
            logMissingBaseDirectory();
            return new LocalFileProvider.CollectionResult(
                "(no source files found)", contentFormatter.noSourceFilesSummary(), 0, 0);
        }
        try {
            List<LocalFileCandidateCollector.Candidate> candidates = candidateCollector.collectCandidates();
            int reviewCapacity = contentFormatter.estimateReviewContentCapacity(candidates);
            var reviewContentBuilder = new StringBuilder(reviewCapacity);
            var fileListBuilder = new StringBuilder();
            LocalFileReader.ProcessingResult result = fileReader.processCandidates(candidates,
                (relativePath, content, size) -> {
                    contentFormatter.appendFileBlock(reviewContentBuilder, relativePath, content);
                    fileListBuilder.append("  - ").append(relativePath)
                        .append(" (").append(size).append(" bytes)\n");
                });
            int fileCount = result.fileCount();
            long totalSize = result.totalSize();
            logCollectedFiles(fileCount, totalSize);
            String reviewContent = fileCount == 0 ? "(no source files found)" : reviewContentBuilder.toString();
            String directorySummary = contentFormatter.generateDirectorySummary(fileCount, totalSize, fileListBuilder);
            return new LocalFileProvider.CollectionResult(reviewContent, directorySummary, fileCount, totalSize);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk directory: " + baseDirectory, e);
        }
    }

    private boolean isMissingBaseDirectory() {
        return !Files.isDirectory(baseDirectory);
    }

    private void logMissingBaseDirectory() {
        logger.warn("Base directory does not exist or is not a directory: {}", baseDirectory);
    }

    private void logCollectedFiles(int fileCount, long totalSize) {
        logger.info("Collected {} source files ({} bytes) from: {}", fileCount, totalSize, baseDirectory);
    }
}
