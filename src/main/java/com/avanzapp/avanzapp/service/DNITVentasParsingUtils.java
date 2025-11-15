package com.avanzapp.avanzapp.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

final class DNITVentasParsingUtils {

    private DNITVentasParsingUtils() {
    }

    static String normalizeText(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u00A0', ' ')
                .toUpperCase(Locale.ROOT);
        return normalized.trim();
    }

    static String sanitizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.replace('\u00A0', ' ').trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static Integer parseInteger(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static String normalizeNumero(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static BigDecimal parseMoney(String value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        String trimmed = value.replace("Gs", "")
                .replace("GS", "")
                .replace(".", "")
                .replace(" ", "")
                .replace(",", ".")
                .trim();
        if (trimmed.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(trimmed);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    static boolean startsWithDigit(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        char first = text.charAt(0);
        return Character.isDigit(first);
    }

    static boolean gtZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
