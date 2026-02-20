package dev.logicojp.reviewer.cli;

import java.io.PrintStream;

/// Test helper to create CliOutput with custom streams.
/// Lives in the cli package to access the package-private constructor.
public final class TestCliOutput {
    private TestCliOutput() {}

    public static CliOutput create(PrintStream out, PrintStream err) {
        return new CliOutput(out, err);
    }
}
