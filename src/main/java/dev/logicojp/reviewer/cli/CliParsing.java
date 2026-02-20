package dev.logicojp.reviewer.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/// Stateless CLI argument parsing utilities.
///
/// {@code hasHelpFlag} is {@code public} — the only public API.
/// All other parsing methods are package-private (used only within the {@code cli} package).
public final class CliParsing {
    private static final int MAX_STDIN_TOKEN_BYTES = 256;
    static final String STDIN_TOKEN_SENTINEL = "-";
    private static final TokenInput SYSTEM_TOKEN_INPUT = new SystemTokenInput();

    interface TokenInput {
        char[] readPassword();
        byte[] readStdin(int maxBytes) throws IOException;
    }

    private static final class SystemTokenInput implements TokenInput {
        @Override
        public char[] readPassword() {
            if (System.console() == null) return null;
            return System.console().readPassword("GitHub Token: ");
        }

        @Override
        public byte[] readStdin(int maxBytes) throws IOException {
            return System.in.readNBytes(maxBytes);
        }
    }

    record OptionValue(String value, int newIndex) {}

    record MultiValue(List<String> values, int newIndex) {}

    private CliParsing() {}

    public static boolean hasHelpFlag(String[] args) {
        if (args == null) return false;
        return Arrays.stream(args).anyMatch(CliParsing::isHelpOption);
    }

    static OptionValue readSingleValue(String arg, String[] args, int index, String optionName) {
        String inline = inlineValue(arg);
        if (inline != null) return new OptionValue(inline, index);
        if (isMissingOptionValue(args, index)) {
            throw new CliValidationException("Option requires a value: " + optionName, true);
        }
        return new OptionValue(args[index + 1], index + 1);
    }

    static MultiValue readMultiValues(String arg, String[] args, int index, String optionName) {
        List<String> values = new ArrayList<>();
        addInlineValue(arg, values);
        int newIndex = index;
        while (hasNextNonOptionArg(args, newIndex)) {
            values.add(args[++newIndex]);
        }
        if (values.isEmpty()) {
            throw new CliValidationException("Option requires at least one value: " + optionName, true);
        }
        return new MultiValue(values, newIndex);
    }

    static int readInto(String[] args, int i, String optName, Consumer<String> setter) {
        var value = readSingleValue(args[i], args, i, optName);
        setter.accept(value.value());
        return value.newIndex();
    }

    static int readMultiInto(String[] args, int i, String optName, Consumer<String> setter) {
        var values = readMultiValues(args[i], args, i, optName);
        for (String v : values.values()) setter.accept(v);
        return values.newIndex();
    }

    static int readTokenInto(String[] args, int i, String optName, Consumer<String> setter) {
        return readInto(args, i, optName, value -> setter.accept(readTokenWithWarning(value)));
    }

    static List<String> splitComma(String value) {
        if (value == null || value.isBlank()) return List.of();
        List<String> parts = new ArrayList<>();
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) parts.add(trimmed);
        }
        return List.copyOf(parts);
    }

    /// Rejects direct token values passed via command line to prevent
    /// exposure in process listings and shell history.
    static String readTokenWithWarning(String value) {
        if (!STDIN_TOKEN_SENTINEL.equals(value)) {
            throw new CliValidationException(
                "Direct token passing via command line is not supported for security reasons. "
                    + "Use '--token -' (stdin) or set GITHUB_TOKEN environment variable.", true);
        }
        return STDIN_TOKEN_SENTINEL;
    }

    /// Reads a token value, supporting stdin input via "-".
    static String readToken(String value) {
        return readToken(value, SYSTEM_TOKEN_INPUT);
    }

    static String readToken(String value, TokenInput tokenInput) {
        if (STDIN_TOKEN_SENTINEL.equals(value)) {
            try {
                char[] chars = tokenInput.readPassword();
                if (chars != null) {
                    String token = String.valueOf(chars).trim();
                    Arrays.fill(chars, '\0');
                    return token;
                }
                byte[] raw = tokenInput.readStdin(MAX_STDIN_TOKEN_BYTES);
                String token = new String(raw, StandardCharsets.UTF_8).trim();
                Arrays.fill(raw, (byte) 0);
                return token;
            } catch (IOException e) {
                throw new CliValidationException("Failed to read token from stdin: " + e.getMessage(), false);
            }
        }
        return value;
    }

    private static String inlineValue(String arg) {
        int idx = arg.indexOf('=');
        return (idx > 0 && idx < arg.length() - 1) ? arg.substring(idx + 1) : null;
    }

    private static boolean isHelpOption(String arg) {
        return "-h".equals(arg) || "--help".equals(arg);
    }

    private static void addInlineValue(String arg, List<String> values) {
        String inline = inlineValue(arg);
        if (inline != null) values.add(inline);
    }

    private static boolean hasNextNonOptionArg(String[] args, int index) {
        return index + 1 < args.length && !args[index + 1].startsWith("-");
    }

    private static boolean isMissingOptionValue(String[] args, int index) {
        return index + 1 >= args.length || args[index + 1].startsWith("-");
    }
}
