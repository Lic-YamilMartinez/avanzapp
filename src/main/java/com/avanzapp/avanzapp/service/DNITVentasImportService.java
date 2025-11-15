package com.avanzapp.avanzapp.service;

import com.avanzapp.avanzapp.dto.ImportResultDTO;
import com.avanzapp.avanzapp.model.DNITVentasCabecera;
import com.avanzapp.avanzapp.model.DNITVentasDetalle;
import com.avanzapp.avanzapp.model.LineaTipo;
import com.avanzapp.avanzapp.model.Usuario;
import com.avanzapp.avanzapp.repository.DNITVentasCabeceraRepository;
import com.avanzapp.avanzapp.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DNITVentasImportService {

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

    private final DNITVentasCabeceraRepository cabeceraRepository;
    private final UsuarioRepository usuarioRepository;

    public DNITVentasImportService(DNITVentasCabeceraRepository cabeceraRepository,
                                   UsuarioRepository usuarioRepository) {
        this.cabeceraRepository = cabeceraRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ImportResultDTO importarVentas(MultipartFile file, Long usuarioId, boolean overwrite) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo de ventas esta vacio");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + usuarioId));

        List<String> lines = readLines(file);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("No se pudo leer informacion del archivo de ventas");
        }

        YearMonth periodo = detectarPeriodo(lines, file.getOriginalFilename());
        int periodoMes = periodo.getMonthValue();
        int periodoAnio = periodo.getYear();
        String periodoEmision = String.format("%02d/%d", periodoMes, periodoAnio);

        if (overwrite) {
            List<DNITVentasCabecera> existentes = cabeceraRepository
                    .findByUsuarioIdAndPeriodoMesAndPeriodoAnio(usuarioId, periodoMes, periodoAnio);
            if (!existentes.isEmpty()) {
                cabeceraRepository.deleteAll(existentes);
            }
        }

        int headerIndex = encontrarLineaHeader(lines);
        int insertadas = 0;
        int actualizadas = 0;
        int ignoradas = 0;
        int leidas = 0;
        YearMonth periodoRef = YearMonth.of(periodoAnio, periodoMes);

        for (int i = headerIndex + 1; i < lines.size(); i++) {
            String row = lines.get(i);
            if (row == null || row.isBlank()) {
                continue;
            }
            String trimmed = row.trim();
            String normalized = normalizeText(trimmed);
            if (normalized.startsWith("RESUMEN") || normalized.startsWith(";RESUMEN")) {
                break;
            }
            if (trimmed.startsWith(";") && !startsWithDigit(trimmed)) {
                continue;
            }

            String[] columns = row.split(";", -1);
            if (columns.length < 10) {
                continue;
            }

            Integer dia = parseInteger(columns[0]);
            String numero = normalizeNumero(getColumn(columns, 1));

            if (numero == null) {
                ignoradas++;
                continue;
            }

            leidas++;

            DNITVentasCabecera cabecera = cabeceraRepository
                    .findFirstByUsuarioIdAndPeriodoMesAndPeriodoAnioAndNumeroComprobante(usuarioId, periodoMes, periodoAnio, numero)
                    .orElse(null);

            boolean esNueva = cabecera == null;
            if (cabecera == null) {
                cabecera = new DNITVentasCabecera();
            }

                cabecera.setUsuario(usuario);
                cabecera.setDia(dia);
                cabecera.setNumeroComprobante(numero);
                cabecera.setClienteNombre(sanitizeText(getColumn(columns, 2)));
                cabecera.setClienteRuc(sanitizeText(getColumn(columns, 3)));

                BigDecimal grav10 = parseMoney(getColumn(columns, 4));
                BigDecimal grav5 = parseMoney(getColumn(columns, 5));
                BigDecimal iva10 = parseMoney(getColumn(columns, 6));
                BigDecimal iva5 = parseMoney(getColumn(columns, 7));
                BigDecimal exenta = parseMoney(getColumn(columns, 8));

                cabecera.setGravada10(grav10);
                cabecera.setGravada5(grav5);
                cabecera.setIva10(iva10);
                cabecera.setIva5(iva5);
                cabecera.setExenta(exenta);
                cabecera.setTotalComprobante(parseMoney(getColumn(columns, 9)));
                cabecera.setRetenciones(parseMoney(getColumn(columns, 10)));

                cabecera.setAfectacionExento(sanitizeText(getColumn(columns, 11)));
                cabecera.setAfectacionGrav10(sanitizeText(getColumn(columns, 12)));
                cabecera.setAfectacionGrav5(sanitizeText(getColumn(columns, 13)));

            cabecera.setPeriodoMes(periodoMes);
            cabecera.setPeriodoAnio(periodoAnio);
            cabecera.setPeriodoEmision(periodoEmision);
            cabecera.setFechaEmision(buildFecha(periodoRef, dia));

                List<DNITVentasDetalle> nuevos = new ArrayList<>();

                if (gtZero(grav10) || gtZero(iva10)) {
                    DNITVentasDetalle detalle10 = new DNITVentasDetalle();
                    detalle10.setTipoLinea(LineaTipo.IVA10);
                    detalle10.setTasa(10);
                    detalle10.setBase(safe(grav10));
                    detalle10.setIva(safe(iva10));
                    detalle10.setExento(BigDecimal.ZERO);
                    detalle10.setClasificacion(cabecera.getAfectacionGrav10());
                    nuevos.add(detalle10);
                }

                if (gtZero(grav5) || gtZero(iva5)) {
                    DNITVentasDetalle detalle5 = new DNITVentasDetalle();
                    detalle5.setTipoLinea(LineaTipo.IVA5);
                    detalle5.setTasa(5);
                    detalle5.setBase(safe(grav5));
                    detalle5.setIva(safe(iva5));
                    detalle5.setExento(BigDecimal.ZERO);
                    detalle5.setClasificacion(cabecera.getAfectacionGrav5());
                    nuevos.add(detalle5);
                }

                if (gtZero(exenta)) {
                    DNITVentasDetalle detalleExento = new DNITVentasDetalle();
                    detalleExento.setTipoLinea(LineaTipo.EXENTA);
                    detalleExento.setTasa(0);
                    detalleExento.setBase(BigDecimal.ZERO);
                    detalleExento.setIva(BigDecimal.ZERO);
                    detalleExento.setExento(safe(exenta));
                    detalleExento.setClasificacion(cabecera.getAfectacionExento());
                    nuevos.add(detalleExento);
                }

                cabecera.clearAndAddDetalles(nuevos);

                cabeceraRepository.save(cabecera);
            if (esNueva) {
                insertadas++;
            } else {
                actualizadas++;
            }
        }

        return new ImportResultDTO(leidas, insertadas, actualizadas, ignoradas, periodoMes, periodoAnio);
    }

    private static LocalDate buildFecha(YearMonth periodo, Integer dia) {
        int fallback = 1;
        int value = dia == null ? fallback : dia;
        if (value < 1) {
            value = fallback;
        }
        int max = periodo.lengthOfMonth();
        if (value > max) {
            value = max;
        }
        return periodo.atDay(value);
    }

    private static String getColumn(String[] columns, int index) {
        return index < columns.length ? columns[index] : "";
    }

    private static Integer parseInteger(String value) {
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

    private static String normalizeNumero(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BigDecimal parseMoney(String value) {
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

    private static String sanitizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.replace('\u00A0', ' ').trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean gtZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static List<String> readLines(MultipartFile file) throws IOException {
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

    private static YearMonth detectarPeriodo(List<String> lines, String fileName) {
        Integer mes = null;
        Integer anio = null;

        for (String line : lines) {
            String normalized = normalizeText(line);
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
            mes = buscarMes(normalizeText(fileName));
        }
        if (anio == null && fileName != null) {
            anio = buscarAnio(normalizeText(fileName));
        }

        if (mes == null || anio == null) {
            throw new IllegalStateException("No se pudo determinar el periodo del archivo de ventas");
        }

        return YearMonth.of(anio, mes);
    }

    private static Integer buscarMes(String text) {
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

    private static Integer buscarAnio(String text) {
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

    private static String normalizeText(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u00A0', ' ')
                .toUpperCase(Locale.ROOT);
        return normalized.trim();
    }

    private static boolean startsWithDigit(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        char first = text.charAt(0);
        return Character.isDigit(first);
    }

    private static int encontrarLineaHeader(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String normalized = normalizeText(lines.get(i));
            if (normalized.startsWith("DIA;NUMERO")) {
                return i;
            }
        }
        throw new IllegalStateException("No se encontro la fila de encabezados de ventas");
    }
}
