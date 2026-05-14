package com.avanzapp.avanzapp.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
class DNITComprasPeriodDetector {

    private static final Pattern MES_PATTERN = Pattern.compile("MES\\s*(DE|:)\\s*([A-ZÁÉÍÓÚÑ]+)");
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

    YearMonth detect(Sheet sheet, String filename) {
        Integer mes = null;
        Integer anio = null;

        int top = Math.min(sheet.getLastRowNum(), 80);
        for (int r = 0; r <= top; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                if (cell == null || cell.getCellType() != CellType.STRING) continue;
                String text = DNITComprasParsingUtils.normalizeText(cell.getStringCellValue());
                if (mes == null) mes = buscarMes(text);
                if (anio == null) anio = buscarAnio(text);
                if (mes != null && anio != null) {
                    return YearMonth.of(anio, mes);
                }
            }
        }

        // Si no encontró en el contenido, intentar con el nombre del archivo
        if (filename != null) {
            String normName = DNITComprasParsingUtils.normalizeText(filename);
            if (mes == null) mes = buscarMes(normName);
            if (anio == null) anio = buscarAnio(normName);
        }

        if (mes == null || anio == null) {
            throw new IllegalStateException("No se pudo determinar el periodo del archivo de compras");
        }

        return YearMonth.of(anio, mes);
    }

    private Integer buscarMes(String text) {
        Matcher matcher = MES_PATTERN.matcher(text);
        if (matcher.find()) {
            return MONTHS.get(matcher.group(2));
        }
        for (String token : text.split("[;\\s_]+")) {
            Integer m = MONTHS.get(token);
            if (m != null) return m;
        }
        return null;
    }

    private Integer buscarAnio(String text) {
        Matcher matcher = ANIO_PATTERN.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        for (String token : text.split("[;\\s_]+")) {
            if (token.matches("20\\d{2}")) {
                return Integer.parseInt(token);
            }
        }
        return null;
    }
}
