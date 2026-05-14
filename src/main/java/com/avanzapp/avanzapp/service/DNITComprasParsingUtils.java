package com.avanzapp.avanzapp.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

final class DNITComprasParsingUtils {

    private DNITComprasParsingUtils() {
    }

    // ==================== Texto ====================

    static String normalizeText(String s) {
        if (s == null) return "";
        String noNbsp = s.replace('\u00A0', ' ');
        String normalized = Normalizer.normalize(noNbsp, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    static String normalizeHeader(String h) {
        String t = normalizeText(h)
                .replace("º", "")
                .replace("°", "")
                .replace(".", " ")
                .replace("-", " ")
                .replace("/", " ")
                .replace("%", "")
                .trim();
        return t.replaceAll("\\s+", "_");
    }

    static String sanitizeText(String s) {
        if (s == null) return null;
        String t = s.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        return t.isEmpty() ? null : t;
    }

    static String normalizarFormaPago(String fp) {
        if (fp == null) return null;
        fp = fp.trim().toUpperCase();
        return switch (fp) {
            case "CO", "CONTADO" -> "CONTADO";
            case "CR", "CREDITO", "CRÉDITO" -> "CREDITO";
            default -> fp;
        };
    }

    static String normalizarNro(String nro) {
        if (nro == null) return null;
        return nro.trim().replaceAll("\\s+", " ");
    }

    // ==================== Números y Money ====================

    static boolean gtZero(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }

    static boolean isZeroOrNull(BigDecimal v) {
        return v == null || v.compareTo(BigDecimal.ZERO) == 0;
    }

    static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    static BigDecimal parseMoney(String s) {
        if (s == null) return BigDecimal.ZERO;
        String v = s.replace("Gs", "")
                .replace("GS", "")
                .replace("₲", "")
                .replace(".", "")
                .replace(",", ".")
                .trim();
        if (v.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(v);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // ==================== Lectura desde Excel ====================

    static Integer readInteger(Row row, int idx) {
        if (idx < 0 || row == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try { yield Integer.parseInt(cell.getStringCellValue().trim()); }
                catch (Exception e) { yield null; }
            }
            default -> null;
        };
    }

    static String readString(Row row, int idx) {
        if (idx < 0 || row == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDate d = cell.getLocalDateTimeCellValue().toLocalDate();
                    yield d.toString();
                }
                DecimalFormat df = new DecimalFormat("#.###############");
                yield df.format(cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    static BigDecimal readMoney(Row row, int idx) {
        if (idx < 0 || row == null) return BigDecimal.ZERO;
        Cell cell = row.getCell(idx);
        if (cell == null) return BigDecimal.ZERO;
        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> parseMoney(cell.getStringCellValue());
            default -> BigDecimal.ZERO;
        };
    }
}
