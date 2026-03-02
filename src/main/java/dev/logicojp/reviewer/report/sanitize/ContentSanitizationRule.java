package dev.logicojp.reviewer.report.sanitize;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record ContentSanitizationRule(Pattern pattern, String replacement, List<String> fastCheckMarkers) {

    ContentSanitizationRule(Pattern pattern, String replacement) {
        this(pattern, replacement, List.of());
    }

    String apply(String input) {
        if (!fastCheckMarkers.isEmpty() && !containsAnyMarker(input)) {
            return input;
        }
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) {
            return input;
        }
        matcher.reset();
        return matcher.replaceAll(replacement);
    }

    private boolean containsAnyMarker(String input) {
        String lower = input.toLowerCase(java.util.Locale.ROOT);
        for (String marker : fastCheckMarkers) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }
}