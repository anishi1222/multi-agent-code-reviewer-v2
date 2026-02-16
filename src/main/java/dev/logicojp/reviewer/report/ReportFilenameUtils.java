package dev.logicojp.reviewer.report;

final class ReportFilenameUtils {

    private ReportFilenameUtils() {
    }

    static String sanitizeAgentName(String agentName) {
        return agentName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}