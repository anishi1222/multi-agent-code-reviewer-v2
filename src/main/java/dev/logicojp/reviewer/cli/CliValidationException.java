package dev.logicojp.reviewer.cli;

/// CLI input validation error. Used within the {@code cli} package
/// to signal invalid command-line arguments or options.
final class CliValidationException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final boolean showUsage;

    CliValidationException(String message, boolean showUsage) {
        super(message == null ? "" : message);
        this.showUsage = showUsage;
    }

    CliValidationException(String message, boolean showUsage, Throwable cause) {
        super(message == null ? "" : message, cause);
        this.showUsage = showUsage;
    }

    boolean showUsage() {
        return showUsage;
    }
}
