package dev.logicojp.reviewer.cli;

public final class CliValidationException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final boolean showUsage;

    public CliValidationException(String message, boolean showUsage) {
        super(normalizeMessage(message));
        this.showUsage = showUsage;
    }

    private static String normalizeMessage(String message) {
        return message == null ? "" : message;
    }

    public boolean showUsage() {
        return showUsage;
    }
}

