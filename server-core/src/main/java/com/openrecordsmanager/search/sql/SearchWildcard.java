package com.openrecordsmanager.search.sql;

/**
 * Translates user wildcards ({@code *}, {@code ?}) into SQL LIKE patterns and escapes
 * literal {@code %} / {@code _} in the input.
 */
public final class SearchWildcard {
    private SearchWildcard() {
    }

    /**
     * If the input contains {@code *} or {@code ?}, those become {@code %} / {@code _}.
     * Otherwise, wraps as a contains match ({@code %input%}).
     */
    public static String toLikePattern(String input) {
        boolean hasWildcard = input.indexOf('*') >= 0 || input.indexOf('?') >= 0;
        String escaped = escapeLikeLiterals(input);
        String translated = escaped.replace('*', '%').replace('?', '_');
        if (hasWildcard) {
            return translated;
        }
        return "%" + translated + "%";
    }

    private static String escapeLikeLiterals(String input) {
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '%' || c == '_' || c == '\\') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }
}
