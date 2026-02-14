package dev.logicojp.reviewer.instruction;

import java.util.List;
import java.util.Locale;

/// Validates custom instruction content for basic prompt-injection safeguards.
public final class CustomInstructionSafetyValidator {

    private static final int MAX_INSTRUCTION_SIZE = 64 * 1024;
    private static final List<String> SUSPICIOUS_PATTERNS = List.of(
        "ignore previous instructions",
        "ignore all previous",
        "disregard the above",
        "以下の指示を無視",
        "上記の指示を無視",
        "これまでの指示を無視"
    );

    public record ValidationResult(boolean safe, String reason) {}

    private CustomInstructionSafetyValidator() {
    }

    public static ValidationResult validate(CustomInstruction instruction) {
        if (instruction == null || instruction.isEmpty()) {
            return new ValidationResult(true, "empty");
        }

        String content = instruction.content();
        if (content.length() > MAX_INSTRUCTION_SIZE) {
            return new ValidationResult(false, "size limit exceeded");
        }

        String lower = content.toLowerCase(Locale.ROOT);
        for (String pattern : SUSPICIOUS_PATTERNS) {
            if (lower.contains(pattern.toLowerCase(Locale.ROOT))) {
                return new ValidationResult(false, "potential prompt-injection pattern");
            }
        }

        return new ValidationResult(true, "ok");
    }
}
