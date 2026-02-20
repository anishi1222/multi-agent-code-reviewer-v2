package dev.logicojp.reviewer.skill;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SkillMarkdownParser")
class SkillMarkdownParserTest {

    private final SkillMarkdownParser parser = new SkillMarkdownParser();

    @Nested
    @DisplayName("コンストラクタ")
    class Constructor {

        @Test
        @DisplayName("デフォルトファイル名はSKILL.md")
        void defaultFilename() {
            assertThat(parser.getSkillFilename()).isEqualTo("SKILL.md");
        }

        @Test
        @DisplayName("カスタムファイル名を指定できる")
        void customFilename() {
            var custom = new SkillMarkdownParser("CUSTOM.md");
            assertThat(custom.getSkillFilename()).isEqualTo("CUSTOM.md");
        }

        @Test
        @DisplayName("nullの場合はデフォルトにフォールバックする")
        void nullFallsBackToDefault() {
            var custom = new SkillMarkdownParser(null);
            assertThat(custom.getSkillFilename()).isEqualTo("SKILL.md");
        }
    }

    @Nested
    @DisplayName("parse")
    class Parse {

        @Test
        @DisplayName("有効なSKILL.mdをパースして返す")
        void parsesValidFile(@TempDir Path tempDir) throws IOException {
            Path skillDir = tempDir.resolve("my-skill");
            Files.createDirectories(skillDir);

            String content = """
                ---
                name: My Skill
                description: A test skill
                ---
                Execute this prompt.
                """;
            Files.writeString(skillDir.resolve("SKILL.md"), content, StandardCharsets.UTF_8);

            SkillDefinition skill = parser.parse(skillDir.resolve("SKILL.md"));

            assertThat(skill.id()).isEqualTo("my-skill");
            assertThat(skill.name()).isEqualTo("My Skill");
            assertThat(skill.description()).isEqualTo("A test skill");
            assertThat(skill.prompt()).contains("Execute this prompt.");
        }

        @Test
        @DisplayName("サポートされていないファイル名はIllegalArgumentExceptionをスローする")
        void rejectsUnsupportedFilename(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("README.md");
            Files.writeString(file, "content", StandardCharsets.UTF_8);

            assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported");
        }
    }

    @Nested
    @DisplayName("parseContent")
    class ParseContent {

        @Test
        @DisplayName("フロントマターとボディを解析する")
        void parsesWithFrontmatter() {
            String content = """
                ---
                name: Test Skill
                description: Skill description
                ---
                Prompt body here.
                """;

            SkillDefinition skill = parser.parseContent(content, "skill-id");

            assertThat(skill.id()).isEqualTo("skill-id");
            assertThat(skill.name()).isEqualTo("Test Skill");
            assertThat(skill.description()).isEqualTo("Skill description");
            assertThat(skill.prompt()).isEqualTo("Prompt body here.");
        }

        @Test
        @DisplayName("フロントマターなしの場合は全体をプロンプトとして使用する")
        void usesEntireContentWithoutFrontmatter() {
            String content = "Just a prompt without frontmatter.";
            SkillDefinition skill = parser.parseContent(content, "raw-skill");

            assertThat(skill.id()).isEqualTo("raw-skill");
            assertThat(skill.name()).isEqualTo("raw-skill");
            assertThat(skill.prompt()).isEqualTo(content);
        }

        @Test
        @DisplayName("フロントマターのみでボディが空の場合はIllegalArgumentExceptionをスローする")
        void throwsOnEmptyBody() {
            String content = """
                ---
                name: Empty Body
                ---
                """;

            assertThatThrownBy(() -> parser.parseContent(content, "empty"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no prompt content");
        }
    }

    @Nested
    @DisplayName("isSkillFile")
    class IsSkillFile {

        @Test
        @DisplayName("SKILL.mdの場合はtrueを返す")
        void trueForSkillMd(@TempDir Path tempDir) {
            assertThat(parser.isSkillFile(tempDir.resolve("SKILL.md"))).isTrue();
        }

        @Test
        @DisplayName("それ以外のファイル名ではfalseを返す")
        void falseForOtherFilename(@TempDir Path tempDir) {
            assertThat(parser.isSkillFile(tempDir.resolve("README.md"))).isFalse();
        }

        @Test
        @DisplayName("nullの場合はfalseを返す")
        void falseForNull() {
            assertThat(parser.isSkillFile(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("discoverSkills")
    class DiscoverSkills {

        @Test
        @DisplayName("サブディレクトリ内のSKILL.mdを発見する")
        void discoversSkillFiles(@TempDir Path tempDir) throws IOException {
            // Create skill directories
            Path skill1Dir = tempDir.resolve("skill-a");
            Path skill2Dir = tempDir.resolve("skill-b");
            Files.createDirectories(skill1Dir);
            Files.createDirectories(skill2Dir);
            Files.writeString(skill1Dir.resolve("SKILL.md"), "---\nname: A\n---\nPrompt A",
                StandardCharsets.UTF_8);
            Files.writeString(skill2Dir.resolve("SKILL.md"), "---\nname: B\n---\nPrompt B",
                StandardCharsets.UTF_8);

            // Non-skill directory (no SKILL.md)
            Path noSkillDir = tempDir.resolve("not-a-skill");
            Files.createDirectories(noSkillDir);
            Files.writeString(noSkillDir.resolve("README.md"), "Not a skill.", StandardCharsets.UTF_8);

            List<Path> skills = parser.discoverSkills(tempDir);

            assertThat(skills).hasSize(2);
            assertThat(skills).allSatisfy(p ->
                assertThat(p.getFileName().toString()).isEqualTo("SKILL.md"));
        }

        @Test
        @DisplayName("nullパスの場合は空リストを返す")
        void emptyForNull() {
            assertThat(parser.discoverSkills(null)).isEmpty();
        }

        @Test
        @DisplayName("存在しないディレクトリの場合は空リストを返す")
        void emptyForNonExistent(@TempDir Path tempDir) {
            assertThat(parser.discoverSkills(tempDir.resolve("nope"))).isEmpty();
        }
    }
}
