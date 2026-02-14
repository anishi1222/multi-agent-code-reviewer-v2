package dev.logicojp.reviewer.report;

import dev.logicojp.reviewer.service.TemplateService;
import com.github.copilot.sdk.CopilotClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.nio.file.Path;

/// Factory for creating {@link ReportGenerator} and {@link SummaryGenerator} instances.
///
/// Encapsulates the injection of shared services ({@link TemplateService})
/// so that generators can be created with per-invocation parameters
/// (output directory, model, etc.) while remaining testable via DI.
@Singleton
public class ReportGeneratorFactory {

    private final TemplateService templateService;

    @Inject
    public ReportGeneratorFactory(TemplateService templateService) {
        this.templateService = templateService;
    }

    /// Creates a new {@link ReportGenerator} for the given output directory.
    ///
    /// @param outputDirectory Directory to write reports to
    /// @return A new ReportGenerator instance
    public ReportGenerator createReportGenerator(Path outputDirectory) {
        return new ReportGenerator(outputDirectory, templateService);
    }

    /// Creates a new {@link SummaryGenerator} with the given configuration.
    ///
    /// @param outputDirectory      Directory to write summary to
    /// @param client               Copilot SDK client for AI summary generation
    /// @param summaryModel         LLM model to use for summary generation
    /// @param reasoningEffort      Reasoning effort level (nullable)
    /// @param timeoutMinutes       Timeout for summary generation
    /// @return A new SummaryGenerator instance
    public SummaryGenerator createSummaryGenerator(Path outputDirectory,
                                                    CopilotClient client,
                                                    String summaryModel,
                                                    String reasoningEffort,
                                                    long timeoutMinutes) {
        return new SummaryGenerator(outputDirectory, client, summaryModel,
            reasoningEffort, timeoutMinutes, templateService);
    }
}
