package dev.logicojp.reviewer.instruction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScopedInstructionLoader")
class ScopedInstructionLoaderTest {

    private static final String INSTRUCTIONS_DIR = ".github/instructions";
    private static final String EXTENSION = ".instructions.md";

    @Nested
    @DisplayName("loadFromInstructionsDirectory")
    class LoadFromInstructionsDirectory {

        @Test
        @DisplayName("指定ディレクトリからインストラクションファイルを読み込む")
        void loadsInstructionFiles(@TempDir Path tempDir) throws IOException {
            Path instDir = tempDir.resolve(INSTRUCTIONS_DIR);
            Files.createDirectories(instDir);
            Files.writeString(instDir.resolve("java.instructions.md"),
                "Follow Java best practices.", StandardCharsets.UTF_8);

            var loader = new ScopedInstructionLoader(INSTRUCTIONS_DIR, EXTENSION);
            List<CustomInstruction> result = loader.loadFromInstructionsDirectory(tempDir);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().content()).isEqualTo("Follow Java best practices.");
            assertThat(result.getFirst().source()).isEqualTo(InstructionSource.LOCAL_FILE);
        }

        @Test
        @DisplayName("ディレクトリが存在しない場合は空リストを返す")
        void returnsEmptyForMissingDirectory(@TempDir Path tempDir) {
            var loader = new ScopedInstructionLoader(INSTRUCTIONS_DIR, EXTENSION);
            List<CustomInstruction> result = loader.loadFromInstructionsDirectory(tempDir);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("拡張子が一致しないファイルはスキップされる")
        void skipsFilesWithWrongExtension(@TempDir Path tempDir) throws IOException {
            Path instDir = tempDir.resolve(INSTRUCTIONS_DIR);
            Files.createDirectories(instDir);
            Files.writeString(instDir.resolve("readme.md"), "Not an instruction file.", StandardCharsets.UTF_8);

            var loader = new ScopedInstructionLoader(INSTRUCTIONS_DIR, EXTENSION);
            List<CustomInstruction> result = loader.loadFromInstructionsDirectory(tempDir);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("空のファイルはスキップされる")
        void skipsEmptyFiles(@TempDir Path tempDir) throws IOException {
            Path instDir = tempDir.resolve(INSTRUCTIONS_DIR);
            Files.createDirectories(instDir);
            Files.writeString(instDir.resolve("empty.instructions.md"), "", StandardCharsets.UTF_8);

            var loader = new ScopedInstructionLoader(INSTRUCTIONS_DIR, EXTENSION);
            List<CustomInstruction> result = loader.loadFromInstructionsDirectory(tempDir);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("空白のみのファイルはスキップされる")
        void skipsBlankFiles(@TempDir Path tempDir) throws IOException {
            Path instDir = tempDir.resolve(INSTRUCTIONS_DIR);
            Files.createDirectories(instDir);
            Files.writeString(instDir.resolve("blank.instructions.md"), "   \n  \n  ", StandardCharsets.UTF_8);

            var loader = new ScopedInstructionLoader(INSTRUCTIONS_DIR, EXTENSION);
            List<CustomInstruction> result = loader.loadFromInstructionsDirectory(tempDir);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("複数のファイルをソート順で読み込む")
        void loadsMultipleFilesSorted(@TempDir Path tempDir) throws IOException {
            Path instDir = tempDir.resolve(INSTRUCTIONS_DIR);
            Files.createDirectories(instDir);
            Files.writeString(instDir.resolve("b.instructions.md"), "B content", StandardCharsets.UTF_8);
            Files.writeString(instDir.resolve("a.instructions.md"), "A content", StandardCharsets.UTF_8);

            var loader = new ScopedInstructionLoader(INSTRUCTIONS_DIR, EXTENSION);
            List<CustomInstruction> result = loader.loadFromInstructionsDirectory(tempDir);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).content()).isEqualTo("A content");
            assertThat(result.get(1).content()).isEqualTo("B content");
        }

        @Test
        @DisplayName("フロントマター付きファイルからメタデータを抽出する")
        void extractsFrontmatterMetadata(@TempDir Path tempDir) throws IOException {
            Path instDir = tempDir.resolve(INSTRUCTIONS_DIR);
            Files.createDirectories(instDir);
            String content = """
                ---
                applyTo: '**/*.java'
                description: Java conventions
                ---
                Follow Java conventions.
                """;
            Files.writeString(instDir.resolve("java.instructions.md"), content, StandardCharsets.UTF_8);

            var loader = new ScopedInstructionLoader(INSTRUCTIONS_DIR, EXTENSION);
            List<CustomInstruction> result = loader.loadFromInstructionsDirectory(tempDir);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().applyTo()).isEqualTo("**/*.java");
            assertThat(result.getFirst().description()).isEqualTo("Java conventions");
            assertThat(result.getFirst().content()).isEqualTo("Follow Java conventions.");
        }
    }
}
