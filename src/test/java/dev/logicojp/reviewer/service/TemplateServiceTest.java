package dev.logicojp.reviewer.service;

import dev.logicojp.reviewer.config.TemplateConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TemplateService")
class TemplateServiceTest {

    @TempDir
    Path tempDir;

    private TemplateConfig createConfig() {
        return new TemplateConfig(
            tempDir.toString(),
            "summary-system.md",
            "summary-prompt.md",
            "default-output-format.md",
            "report.md",
            "executive-summary.md",
            "fallback-summary.md",
            "local-review-content.md",
            "summary-result-entry.md",
            "summary-result-error-entry.md",
            "fallback-agent-row.md",
            "fallback-agent-success.md",
            "fallback-agent-failure.md",
            "report-link-entry.md",
            "output-constraints.md"
        );
    }

    @Nested
    @DisplayName("loadTemplateContent")
    class LoadTemplateContent {

        @Test
        @DisplayName("存在するテンプレートファイルを読み込める")
        void loadsExistingTemplate() throws IOException {
            Files.writeString(tempDir.resolve("test.md"), "Hello {{name}}");
            TemplateService service = new TemplateService(createConfig());
            String content = service.loadTemplateContent("test.md");
            assertThat(content).isEqualTo("Hello {{name}}");
        }

        @Test
        @DisplayName("パストラバーサルを含むテンプレート名は拒否される")
        void rejectsPathTraversal() {
            TemplateService service = new TemplateService(createConfig());
            String content = service.loadTemplateContent("../../../etc/passwd");
            assertThat(content).isEmpty();
        }

        @Test
        @DisplayName("スラッシュを含むテンプレート名は拒否される")
        void rejectsSlashInName() {
            TemplateService service = new TemplateService(createConfig());
            String content = service.loadTemplateContent("sub/template.md");
            assertThat(content).isEmpty();
        }

        @Test
        @DisplayName("バックスラッシュを含むテンプレート名は拒否される")
        void rejectsBackslashInName() {
            TemplateService service = new TemplateService(createConfig());
            String content = service.loadTemplateContent("sub\\template.md");
            assertThat(content).isEmpty();
        }

        @Test
        @DisplayName("存在しないテンプレートは空文字列を返す")
        void nonExistentReturnsEmpty() {
            TemplateService service = new TemplateService(createConfig());
            String content = service.loadTemplateContent("nonexistent.md");
            assertThat(content).isEmpty();
        }
    }

    @Nested
    @DisplayName("applyPlaceholders")
    class ApplyPlaceholders {

        @Test
        @DisplayName("プレースホルダーを置換する")
        void replacesPlaceholders() {
            TemplateService service = new TemplateService(createConfig());
            String result = service.applyPlaceholders(
                "Hello {{name}}, welcome to {{place}}!",
                Map.of("name", "Alice", "place", "Wonderland"));
            assertThat(result).isEqualTo("Hello Alice, welcome to Wonderland!");
        }

        @Test
        @DisplayName("未知のプレースホルダーはそのまま残る")
        void unknownPlaceholdersRemain() {
            TemplateService service = new TemplateService(createConfig());
            String result = service.applyPlaceholders(
                "Hello {{name}}!",
                Map.of());
            assertThat(result).isEqualTo("Hello {{name}}!");
        }

        @Test
        @DisplayName("nullテンプレートは空文字列を返す")
        void nullTemplateReturnsEmpty() {
            TemplateService service = new TemplateService(createConfig());
            String result = service.applyPlaceholders(null, Map.of("k", "v"));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("空のプレースホルダーMapではテンプレートをそのまま返す")
        void emptyPlaceholdersReturnTemplate() {
            TemplateService service = new TemplateService(createConfig());
            String result = service.applyPlaceholders("unchanged", Map.of());
            assertThat(result).isEqualTo("unchanged");
        }

        @Test
        @DisplayName("nullのプレースホルダーMapではテンプレートをそのまま返す")
        void nullPlaceholdersReturnTemplate() {
            TemplateService service = new TemplateService(createConfig());
            String result = service.applyPlaceholders("unchanged", null);
            assertThat(result).isEqualTo("unchanged");
        }
    }
}
