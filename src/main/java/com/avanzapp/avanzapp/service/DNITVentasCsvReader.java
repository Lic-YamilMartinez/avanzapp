package com.avanzapp.avanzapp.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
class DNITVentasCsvReader {

    List<String> readLines(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        String content = utf8.indexOf('\uFFFD') >= 0
                ? new String(bytes, Charset.forName("windows-1252"))
                : utf8;
        String[] split = content.replace("\r", "").split("\n");
        List<String> lines = new ArrayList<>(split.length);
        for (String line : split) {
            lines.add(line.replace("\uFEFF", ""));
        }
        return lines;
    }

    int findHeaderIndex(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String normalized = DNITVentasParsingUtils.normalizeText(lines.get(i));
            if (normalized.startsWith("DIA;NUMERO")) {
                return i;
            }
        }
        throw new IllegalStateException("No se encontro la fila de encabezados de ventas");
    }

    boolean shouldStop(String normalizedLine) {
        return normalizedLine.startsWith("RESUMEN") || normalizedLine.startsWith(";RESUMEN");
    }

    boolean shouldSkip(String trimmedLine) {
        return trimmedLine.startsWith(";") && !DNITVentasParsingUtils.startsWithDigit(trimmedLine);
    }
}
