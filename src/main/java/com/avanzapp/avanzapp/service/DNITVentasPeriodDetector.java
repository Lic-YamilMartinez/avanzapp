package com.avanzapp.avanzapp.service;

import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
class DNITVentasPeriodDetector {

    private static final Pattern MES_PATTERN = Pattern.compile("MES\\s*(DE|:)\\s*([A-Z]+)");
    private static final Pattern ANIO_PATTERN = Pattern.compile("A(?:N|\\u00D1)O\\s*[:=]?\\s*(20\\d{2})");
    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("ENERO", 1), Map.entry("FEBRERO", 2), Map.entry("MARZO", 3), Map.entry("ABRIL", 4),
            Map.entry("MAYO", 5), Map.entry("JUNIO", 6), Map.entry("JULIO", 7), Map.entry("AGOSTO", 8),
            Map.entry("SEPTIEMBRE", 9), Map.entry("SETIEMBRE", 9), Map.entry("OCTUBRE", 10),
            Map.entry("NOVIEMBRE", 11), Map.entry("DICIEMBRE", 12),
            Map.entry("ENE", 1), Map.entry("FEB", 2), Map.entry("MAR", 3), Map.entry("ABR", 4),
            Map.entry("MAY", 5), Map.entry("JUN", 6), Map.entry("JUL", 7), Map.entry("AGO", 8),
            Map.entry("SEP", 9), Map.entry("SET", 9), Map.entry("OCT", 10), Map.entry("NOV", 11), Map.entry("DIC", 12)
    );

    YearMonth detect(List<String> lines, String fileName) {
        Integer mes = null;
        Integer anio = null;

        for (String line : lines) {
            String normalized = DNITVentasParsingUtils.normalizeText(line);
            if (mes == null) {
                mes = buscarMes(normalized);
            }
            if (anio == null) {
                anio = buscarAnio(normalized);
            }
            if (mes != null && anio != null) {
                break;
            }
        }

        if (mes == null && fileName != null) {
            mes = buscarMes(DNITVentasParsingUtils.normalizeText(fileName));
        }
        if (anio == null && fileName != null) {
            anio = buscarAnio(DNITVentasParsingUtils.normalizeText(fileName));
        }

        if (mes == null || anio == null) {
            throw new IllegalStateException("No se pudo determinar el periodo del archivo de ventas");
        }

        return YearMonth.of(anio, mes);
    }

    private Integer buscarMes(String text) {
        Matcher matcher = MES_PATTERN.matcher(text);
        if (matcher.find()) {
            return MONTHS.get(matcher.group(2));
        }
        for (String token : text.split("[;\\s]+")) {
            Integer month = MONTHS.get(token);
            if (month != null) {
                return month;
            }
        }
        return null;
    }

    private Integer buscarAnio(String text) {
        Matcher matcher = ANIO_PATTERN.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        for (String token : text.split("[;\\s]+")) {
            if (token.matches("20\\d{2}")) {
                return Integer.parseInt(token);
            }
        }
        return null;
    }
}
