package dev.logicojp.reviewer.target;

import dev.logicojp.reviewer.config.ReviewerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/// Collects, filters, reads, and formats local source files for review.
/// Merges the former LocalFileCandidateCollector, LocalFileCandidateProcessor,
/// LocalFileContentFormatter, LocalFileCandidate, and LocalFileSelectionConfig.
public class LocalFileProvider {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileProvider.class);

    private static final Map<String, String> LANGUAGE_MAP = Map.ofEntries(
        Map.entry("js", "javascript"), Map.entry("mjs", "javascript"), Map.entry("cjs", "javascript"),
        Map.entry("ts", "typescript"), Map.entry("jsx", "jsx"), Map.entry("tsx", "tsx"),
        Map.entry("py", "python"), Map.entry("rb", "ruby"), Map.entry("rs", "rust"),
        Map.entry("kt", "kotlin"), Map.entry("kts", "kotlin"),
        Map.entry("cs", "csharp"), Map.entry("fs", "fsharp"),
        Map.entry("sh", "bash"), Map.entry("bash", "bash"), Map.entry("zsh", "bash"),
        Map.entry("ps1", "powershell"), Map.entry("psm1", "powershell"),
        Map.entry("yml", "yaml"), Map.entry("md", "markdown")
    );

    // --- Public records ---

    public record LocalFile(String relativePath, String content, long sizeBytes) {}

    public record CollectionResult(String reviewContent,
                                   String directorySummary,
                                   int fileCount,
                                   long totalSizeBytes) {}

    // --- Private inner records ---

    private record Candidate(Path path, long size) {}

    private record SelectionConfig(
        long maxFileSize,
        long maxTotalSize,
        Set<String> ignoredDirectories,
        Set<String> sourceExtensions,
        Set<String> sensitiveFilePatterns,
        Set<String> sensitiveExtensions
    ) {
        static SelectionConfig from(ReviewerConfig.LocalFiles config) {
            return new SelectionConfig(
                config.maxFileSize(),
                config.maxTotalSize(),
                normalizeSet(config.ignoredDirectories()),
                normalizeSet(config.sourceExtensions()),
                normalizeSet(config.sensitiveFilePatterns()),
                normalizeSet(config.sensitiveExtensions())
            );
        }

        private static Set<String> normalizeSet(List<String> values) {
            if (values == null || values.isEmpty()) return Set.of();
            return values.stream()
                .map(SelectionConfig::normalizeValue)
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableSet());
        }

        private static Optional<String> normalizeValue(String value) {
            if (value == null || value.isBlank()) return Optional.empty();
            return Optional.of(value.toLowerCase(Locale.ROOT));
        }
    }

    private record ReadResult(boolean exceededLimit, String content, long sizeBytes) {
        static ReadResult included(String content, long sizeBytes) { return new ReadResult(false, content, sizeBytes); }
        static ReadResult exceeded() { return new ReadResult(true, null, 0); }
    }

    private record ProcessedCandidate(boolean included, boolean stopProcessing,
                                      String relativePath, String content, long size) {
        static ProcessedCandidate included(String relativePath, String content, long size) {
            return new ProcessedCandidate(true, false, relativePath, content, size);
        }
        static ProcessedCandidate skip() { return new ProcessedCandidate(false, false, null, null, 0); }
        static ProcessedCandidate stop() { return new ProcessedCandidate(false, true, null, null, 0); }
    }

    @FunctionalInterface
    private interface FileConsumer {
        void accept(String relativePath, String content, long sizeBytes);
    }

    private record ProcessingResult(long totalSize, int fileCount) {}

    // --- Fields ---

    private final Path baseDirectory;
    private final Path realBaseDirectory;
    private final SelectionConfig selectionConfig;

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
        this.selectionConfig = SelectionConfig.from(config);
    }

    // --- Public API ---

    List<LocalFile> collectFiles() {
        if (isMissingBaseDirectory()) { logMissingBaseDirectory(); return List.of(); }
        List<LocalFile> files = new ArrayList<>();
        try {
            List<Candidate> candidates = collectCandidates();
            ProcessingResult result = processCandidates(candidates,
                (relativePath, content, size) -> files.add(new LocalFile(relativePath, content, size)));
            logCollectedFiles(result.fileCount(), result.totalSize());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk directory: " + baseDirectory, e);
        }
        return List.copyOf(files);
    }

    public CollectionResult collectAndGenerate() {
        if (isMissingBaseDirectory()) {
            logMissingBaseDirectory();
            return new CollectionResult("(no source files found)", noSourceFilesSummary(), 0, 0);
        }
        try {
            List<Candidate> candidates = collectCandidates();
            int reviewCapacity = estimateReviewContentCapacity(candidates);
            var reviewContentBuilder = new StringBuilder(reviewCapacity);
            var fileListBuilder = new StringBuilder();
            ProcessingResult result = processCandidates(candidates,
                (relativePath, content, size) -> {
                    appendFileBlock(reviewContentBuilder, relativePath, content);
                    fileListBuilder.append("  - ").append(relativePath)
                        .append(" (").append(size).append(" bytes)\n");
                });
            long totalSize = result.totalSize();
            int fileCount = result.fileCount();
            logCollectedFiles(fileCount, totalSize);
            String reviewContent = fileCount == 0 ? "(no source files found)" : reviewContentBuilder.toString();
            String directorySummary = generateDirectorySummary(fileCount, totalSize, fileListBuilder);
            return new CollectionResult(reviewContent, directorySummary, fileCount, totalSize);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk directory: " + baseDirectory, e);
        }
    }

    String generateReviewContent(List<LocalFile> files) {
        if (files == null || files.isEmpty()) return "(no source files found)";
        long estimatedSize = files.stream().mapToLong(LocalFile::sizeBytes).sum();
        var sb = new StringBuilder((int) Math.min(
            estimatedSize + files.size() * 30L, selectionConfig.maxTotalSize() + 4096));
        for (LocalFile file : files) {
            appendFileBlock(sb, file.relativePath(), file.content());
        }
        return sb.toString();
    }

    String generateDirectorySummary(List<LocalFile> files) {
        if (files == null || files.isEmpty()) return noSourceFilesSummary();
        long totalSize = files.stream().mapToLong(LocalFile::sizeBytes).sum();
        var fileListBuilder = new StringBuilder();
        for (LocalFile file : files) {
            fileListBuilder.append("  - ").append(file.relativePath())
                .append(" (").append(file.sizeBytes()).append(" bytes)\n");
        }
        return generateDirectorySummary(files.size(), totalSize, fileListBuilder);
    }

    // --- Candidate collection (directory walk) ---

    private List<Candidate> collectCandidates() throws IOException {
        List<Candidate> candidates = new ArrayList<>();
        Files.walkFileTree(baseDirectory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!dir.equals(baseDirectory)
                    && selectionConfig.ignoredDirectories()
                        .contains(dir.getFileName().toString().toLowerCase(Locale.ROOT))) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (isCollectableCandidate(file, attrs)) {
                    candidates.add(new Candidate(file, attrs.size()));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        candidates.sort(Comparator.comparing(Candidate::path));
        return candidates;
    }

    private boolean isCollectableCandidate(Path file, BasicFileAttributes attrs) {
        if (!attrs.isRegularFile() || attrs.isSymbolicLink()) return false;
        if (!isWithinBaseDirectory(file, attrs)) return false;
        String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return isSourceFile(fileName) && isNotSensitiveFile(fileName);
    }

    private boolean isSourceFile(String fileName) {
        if (isSpecialSourceFilename(fileName)) return true;
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) return false;
        String ext = fileName.substring(dotIndex + 1);
        return selectionConfig.sourceExtensions().contains(ext);
    }

    private boolean isSpecialSourceFilename(String fileName) {
        return fileName.equals("makefile") || fileName.equals("dockerfile")
            || fileName.equals("rakefile") || fileName.equals("gemfile");
    }

    private boolean isWithinBaseDirectory(Path path, BasicFileAttributes attrs) {
        if (!attrs.isSymbolicLink()) return true;
        try {
            Path realPath = path.toRealPath();
            return realPath.startsWith(realBaseDirectory);
        } catch (IOException e) {
            logger.debug("Cannot resolve real path for {}: {}", path, e.getMessage());
            return false;
        }
    }

    private boolean isNotSensitiveFile(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            String ext = fileName.substring(dotIndex + 1);
            if (selectionConfig.sensitiveExtensions().contains(ext)) return false;
        }
        for (String pattern : selectionConfig.sensitiveFilePatterns()) {
            if (fileName.contains(pattern)) return false;
        }
        return true;
    }

    // --- Candidate processing (read with limits) ---

    private ProcessingResult processCandidates(List<Candidate> candidates, FileConsumer consumer) {
        long totalSize = 0;
        int fileCount = 0;
        for (Candidate candidate : candidates) {
            ProcessedCandidate processed = processCandidate(candidate, totalSize);
            if (processed.stopProcessing()) break;
            if (!processed.included()) continue;
            consumer.accept(processed.relativePath(), processed.content(), processed.size());
            totalSize += processed.size();
            fileCount++;
        }
        return new ProcessingResult(totalSize, fileCount);
    }

    private ProcessedCandidate processCandidate(Candidate candidate, long totalSize) {
        Path path = candidate.path();
        long size = candidate.size();
        long maxFileSize = selectionConfig.maxFileSize();
        long maxTotalSize = selectionConfig.maxTotalSize();

        if (size > maxFileSize) {
            logger.debug("Skipping large file ({} bytes): {}", size, path);
            return ProcessedCandidate.skip();
        }
        if (totalSize + size > maxTotalSize) {
            logger.warn("Total content size limit reached ({} bytes). Stopping collection.", totalSize);
            return ProcessedCandidate.stop();
        }
        try {
            if (Files.isSymbolicLink(path)) {
                logger.warn("File became symbolic link after collection, skipping: {}", path);
                return ProcessedCandidate.skip();
            }
            Path realPath = path.toRealPath();
            if (!realPath.startsWith(realBaseDirectory)) {
                logger.warn("File escaped base directory (possible race), skipping: {}", path);
                return ProcessedCandidate.skip();
            }
            long remainingBudget = maxTotalSize - totalSize;
            long readLimit = Math.min(maxFileSize, remainingBudget);
            if (readLimit <= 0) {
                logger.warn("Total content size limit reached ({} bytes). Stopping collection.", totalSize);
                return ProcessedCandidate.stop();
            }
            ReadResult readResult = readUtf8WithLimit(realPath, readLimit, size);
            if (readResult.exceededLimit()) {
                if (maxFileSize <= remainingBudget) {
                    logger.warn("File size exceeded limit during read (possible race), skipping: {}", path);
                    return ProcessedCandidate.skip();
                }
                logger.warn("Total content size limit reached ({} bytes). Stopping collection.", totalSize);
                return ProcessedCandidate.stop();
            }
            String relativePath = baseDirectory.relativize(path).toString().replace('\\', '/');
            return ProcessedCandidate.included(relativePath, readResult.content(), readResult.sizeBytes());
        } catch (IOException e) {
            logger.warn("Failed to read file {}: {}", candidate.path(), e.getMessage(), e);
            return ProcessedCandidate.skip();
        }
    }

    private ReadResult readUtf8WithLimit(Path path, long maxBytes, long expectedSize) throws IOException {
        int initialCapacity = (int) Math.min(expectedSize, maxBytes);
        try (InputStream inputStream = Files.newInputStream(path);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream(Math.max(initialCapacity, 32))) {
            byte[] buffer = new byte[8192];
            long totalRead = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                totalRead += read;
                if (totalRead > maxBytes) return ReadResult.exceeded();
                outputStream.write(buffer, 0, read);
            }
            return ReadResult.included(outputStream.toString(StandardCharsets.UTF_8), totalRead);
        }
    }

    // --- Content formatting ---

    private int estimateReviewContentCapacity(List<Candidate> candidates) {
        long estimatedSize = 0;
        for (Candidate candidate : candidates) {
            estimatedSize += candidate.size() + 64L;
            if (estimatedSize >= selectionConfig.maxTotalSize()) break;
        }
        return (int) Math.min(estimatedSize + 1024L, selectionConfig.maxTotalSize() + 4096);
    }

    private void appendFileBlock(StringBuilder sb, String relativePath, String content) {
        String lang = detectLanguage(relativePath);
        sb.append("### ").append(relativePath).append("\n\n");
        sb.append("```").append(lang).append("\n");
        sb.append(content);
        if (!content.endsWith("\n")) sb.append("\n");
        sb.append("```\n\n");
    }

    private String generateDirectorySummary(int fileCount, long totalSize, StringBuilder fileListBuilder) {
        if (fileCount == 0) return noSourceFilesSummary();
        return new StringBuilder()
            .append("Directory: ").append(baseDirectory).append("\n")
            .append("Files: ").append(fileCount).append("\n")
            .append("Total size: ").append(totalSize).append(" bytes\n\n")
            .append("File list:\n").append(fileListBuilder)
            .toString();
    }

    private String noSourceFilesSummary() { return "No source files found in: " + baseDirectory; }

    private String detectLanguage(String relativePath) {
        int dotIndex = relativePath.lastIndexOf('.');
        if (dotIndex < 0) return "";
        String ext = relativePath.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return LANGUAGE_MAP.getOrDefault(ext, ext);
    }

    // --- Utility ---

    private boolean isMissingBaseDirectory() { return !Files.isDirectory(baseDirectory); }
    private void logMissingBaseDirectory() { logger.warn("Base directory does not exist or is not a directory: {}", baseDirectory); }
    private void logCollectedFiles(int fileCount, long totalSize) { logger.info("Collected {} source files ({} bytes) from: {}", fileCount, totalSize, baseDirectory); }
}
