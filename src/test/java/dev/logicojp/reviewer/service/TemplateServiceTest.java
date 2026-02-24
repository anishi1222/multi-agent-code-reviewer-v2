package dev.logicojp.reviewer.service;

import dev.logicojp.reviewer.config.TemplateConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TemplateService")
class TemplateServiceTest {

    @Test
    @DisplayName("ファイルシステム上のテンプレートを読み込める")
    void loadsTemplateFromFile(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("report.md"), "Hello {{name}}");
        var service = newService(tempDir);

        String content = service.loadTemplateContent("report.md");

        assertThat(content).isEqualTo("Hello {{name}}");
    }

    @Test
    @DisplayName("一度読み込んだテンプレートはキャッシュされる")
    void usesCacheAfterFirstLoad(@TempDir Path tempDir) throws IOException {
        Path template = tempDir.resolve("cached.md");
        Files.writeString(template, "v1");
        var service = newService(tempDir);

        String first = service.loadTemplateContent("cached.md");
        Files.writeString(template, "v2");
        String second = service.loadTemplateContent("cached.md");

        assertThat(first).isEqualTo("v1");
        assertThat(second).isEqualTo("v1");
    }

    @Test
    @DisplayName("不正なテンプレート名は拒否する")
    void rejectsInvalidTemplateName(@TempDir Path tempDir) {
        var service = newService(tempDir);

        assertThatThrownBy(() -> service.loadTemplateContent("../secret.md"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid template name");
    }

    @Test
    @DisplayName("存在しないテンプレートは例外になる")
    void throwsWhenTemplateNotFound(@TempDir Path tempDir) {
        var service = newService(tempDir);

        assertThatThrownBy(() -> service.loadTemplateContent("missing.md"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Template not found");
    }

    @Test
    @DisplayName("プレースホルダは既知キーのみ置換され未知キーは維持される")
    void applyPlaceholdersReplacesKnownKeysOnly(@TempDir Path tempDir) {
        var service = newService(tempDir);

        String rendered = service.applyPlaceholders(
            "Hello {{name}} {{unknown}}",
            Map.of("name", "Copilot")
        );

        assertThat(rendered).isEqualTo("Hello Copilot {{unknown}}");
    }

    private static TemplateService newService(Path templateDir) {
        var config = new TemplateConfig(templateDir.toString(), null, null, null, null, null, null, null);
        return new TemplateService(config);
    }
}
