package dev.logicojp.reviewer.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/// Shared utility for securely reading tokens from console/password input or stdin.
public final class TokenReadUtils {

    @FunctionalInterface
    public interface PasswordReader {
        char[] readPassword();
    }

    @FunctionalInterface
    public interface StdinReader {
        byte[] readStdin(int maxBytes) throws IOException;
    }

    private TokenReadUtils() {
        // Utility class
    }

    /// Reads a token and clears temporary char[]/byte[] buffers after conversion.
    public static String readTrimmedToken(PasswordReader passwordReader,
                                          StdinReader stdinReader,
                                          int maxBytes) throws IOException {
        char[] chars = passwordReader.readPassword();
        if (chars != null) {
            try {
                return String.valueOf(chars).trim();
            } finally {
                Arrays.fill(chars, '\0');
            }
        }

        byte[] raw = stdinReader.readStdin(maxBytes);
        try {
            return new String(raw, StandardCharsets.UTF_8).trim();
        } finally {
            Arrays.fill(raw, (byte) 0);
        }
    }
}
