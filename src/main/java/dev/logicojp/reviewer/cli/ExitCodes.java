package dev.logicojp.reviewer.cli;

/// CLI exit codes using simplified values commonly used in CLI tools.
///
/// | Code | Meaning                       | sysexits.h reference |
/// |------|-------------------------------|----------------------|
/// | 0    | Successful execution          | EX_OK (0)            |
/// | 1    | Internal software error       | cf. EX_SOFTWARE (70) |
/// | 2    | Invalid command-line usage    | cf. EX_USAGE (64)    |
public final class ExitCodes {
    public static final int OK = 0;
    public static final int USAGE = 2;
    public static final int SOFTWARE = 1;

    private ExitCodes() {
    }
}
