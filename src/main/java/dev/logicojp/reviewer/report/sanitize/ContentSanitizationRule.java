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
        StringBuilder sb = null;
        while (matcher.find()) {
            if (sb == null) {
                sb = new StringBuilder(input.length());
            }
            matcher.appendReplacement(sb, replacement);
        }
        if (sb == null) {
            return input;
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private boolean containsAnyMarker(String input) {
        for (String marker : fastCheckMarkers) {
            if (containsIgnoreCase(input, marker)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        int limit = haystack.length() - needle.length();
        for (int i = 0; i <= limit; i++) {
            if (haystack.regionMatches(true, i, needle, 0, needle.length())) {
                return true;
            }
        }
        return false;
    }
}