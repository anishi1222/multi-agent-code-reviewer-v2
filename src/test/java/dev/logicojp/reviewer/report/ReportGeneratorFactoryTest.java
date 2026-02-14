package dev.logicojp.reviewer.report;

import dev.logicojp.reviewer.config.TemplateConfig;
import dev.logicojp.reviewer.service.TemplateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReportGeneratorFactory")
class ReportGeneratorFactoryTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("createReportGeneratorはReportGeneratorインスタンスを返す")
    void createsReportGenerator() {
        var config = new TemplateConfig(tempDir.toString(),
            null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        var templateService = new TemplateService(config);
        var factory = new ReportGeneratorFactory(templateService);
        ReportGenerator generator = factory.createReportGenerator(Path.of("/tmp/reports"));
        assertThat(generator).isNotNull();
    }
}
