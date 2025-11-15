package com.avanzapp.avanzapp.service;

import com.avanzapp.avanzapp.dto.ImportResultDTO;
import com.avanzapp.avanzapp.model.DNITVentasCabecera;
import com.avanzapp.avanzapp.model.DNITVentasDetalle;
import com.avanzapp.avanzapp.model.Usuario;
import com.avanzapp.avanzapp.repository.DNITVentasCabeceraRepository;
import com.avanzapp.avanzapp.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class DNITVentasImportService {

    private static final int MIN_COLUMNS = 10;

    private final DNITVentasCabeceraRepository cabeceraRepository;
    private final UsuarioRepository usuarioRepository;
    private final DNITVentasCsvReader csvReader;
    private final DNITVentasPeriodDetector periodDetector;
    private final DNITVentasDetalleFactory detalleFactory;

    public DNITVentasImportService(DNITVentasCabeceraRepository cabeceraRepository,
                                   UsuarioRepository usuarioRepository,
                                   DNITVentasCsvReader csvReader,
                                   DNITVentasPeriodDetector periodDetector,
                                   DNITVentasDetalleFactory detalleFactory) {
        this.cabeceraRepository = cabeceraRepository;
        this.usuarioRepository = usuarioRepository;
        this.csvReader = csvReader;
        this.periodDetector = periodDetector;
        this.detalleFactory = detalleFactory;
    }

    @Transactional
    public ImportResultDTO importarVentas(MultipartFile file, Long usuarioId, boolean overwrite) throws IOException {
        validateFile(file);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + usuarioId));

        List<String> lines = csvReader.readLines(file);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("No se pudo leer informacion del archivo de ventas");
        }

        YearMonth periodo = periodDetector.detect(lines, file.getOriginalFilename());
        int periodoMes = periodo.getMonthValue();
        int periodoAnio = periodo.getYear();
        String periodoEmision = String.format("%02d/%d", periodoMes, periodoAnio);

        borrarRegistrosPreviosIfNeeded(overwrite, usuarioId, periodoMes, periodoAnio);

        int headerIndex = csvReader.findHeaderIndex(lines);
        ImportCounters counters = new ImportCounters();
        YearMonth periodoReferencia = YearMonth.of(periodoAnio, periodoMes);

        for (int i = headerIndex + 1; i < lines.size(); i++) {
            String row = lines.get(i);
            if (!isProcessableRow(row, csvReader)) {
                if (shouldStopProcessing(row, csvReader)) {
                    break;
                }
                continue;
            }

            String[] columns = row.split(";", -1);
            if (columns.length < MIN_COLUMNS) {
                continue;
            }

            Optional<VentaRow> maybeRow = parseVentaRow(columns);
            if (maybeRow.isEmpty()) {
                counters.ignorar();
                continue;
            }

            counters.leer();

            VentaRow data = maybeRow.get();
            DNITVentasCabecera cabecera = cabeceraRepository
                    .findFirstByUsuarioIdAndPeriodoMesAndPeriodoAnioAndNumeroComprobante(usuarioId, periodoMes, periodoAnio, data.numero)
                    .orElseGet(DNITVentasCabecera::new);

            boolean esNueva = cabecera.getId() == null;
            actualizarCabecera(cabecera, usuario, periodoEmision, periodoReferencia, data, periodoMes, periodoAnio);

            List<DNITVentasDetalle> detalles = detalleFactory.build(
                    data.gravada10,
                    data.gravada5,
                    data.iva10,
                    data.iva5,
                    data.exenta,
                    data.afectacionExento,
                    data.afectacionGrav10,
                    data.afectacionGrav5
            );
            cabecera.clearAndAddDetalles(detalles);

            cabeceraRepository.save(cabecera);
            counters.registrarResultado(esNueva);
        }

        return counters.toResult(periodoMes, periodoAnio);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo de ventas esta vacio");
        }
    }

    private void borrarRegistrosPreviosIfNeeded(boolean overwrite, Long usuarioId, int periodoMes, int periodoAnio) {
        if (!overwrite) {
            return;
        }
        List<DNITVentasCabecera> existentes = cabeceraRepository
                .findByUsuarioIdAndPeriodoMesAndPeriodoAnio(usuarioId, periodoMes, periodoAnio);
        if (!existentes.isEmpty()) {
            cabeceraRepository.deleteAll(existentes);
        }
    }

    private boolean isProcessableRow(String row, DNITVentasCsvReader reader) {
        if (row == null || row.isBlank()) {
            return false;
        }
        String trimmed = row.trim();
        return !reader.shouldSkip(trimmed);
    }

    private boolean shouldStopProcessing(String row, DNITVentasCsvReader reader) {
        if (row == null || row.isBlank()) {
            return false;
        }
        String normalized = DNITVentasParsingUtils.normalizeText(row.trim());
        return reader.shouldStop(normalized);
    }

    private Optional<VentaRow> parseVentaRow(String[] columns) {
        Integer dia = DNITVentasParsingUtils.parseInteger(columns[0]);
        String numero = DNITVentasParsingUtils.normalizeNumero(getColumn(columns, 1));
        if (numero == null) {
            return Optional.empty();
        }
        VentaRow row = new VentaRow(
                dia,
                numero,
                DNITVentasParsingUtils.sanitizeText(getColumn(columns, 2)),
                DNITVentasParsingUtils.sanitizeText(getColumn(columns, 3)),
                DNITVentasParsingUtils.parseMoney(getColumn(columns, 4)),
                DNITVentasParsingUtils.parseMoney(getColumn(columns, 5)),
                DNITVentasParsingUtils.parseMoney(getColumn(columns, 6)),
                DNITVentasParsingUtils.parseMoney(getColumn(columns, 7)),
                DNITVentasParsingUtils.parseMoney(getColumn(columns, 8)),
                DNITVentasParsingUtils.parseMoney(getColumn(columns, 9)),
                DNITVentasParsingUtils.parseMoney(getColumn(columns, 10)),
                DNITVentasParsingUtils.sanitizeText(getColumn(columns, 11)),
                DNITVentasParsingUtils.sanitizeText(getColumn(columns, 12)),
                DNITVentasParsingUtils.sanitizeText(getColumn(columns, 13))
        );
        return Optional.of(row);
    }

    private void actualizarCabecera(DNITVentasCabecera cabecera,
                                    Usuario usuario,
                                    String periodoEmision,
                                    YearMonth periodoRef,
                                    VentaRow data,
                                    int periodoMes,
                                    int periodoAnio) {
        cabecera.setUsuario(usuario);
        cabecera.setDia(data.dia);
        cabecera.setNumeroComprobante(data.numero);
        cabecera.setClienteNombre(data.clienteNombre);
        cabecera.setClienteRuc(data.clienteRuc);

        cabecera.setGravada10(data.gravada10);
        cabecera.setGravada5(data.gravada5);
        cabecera.setIva10(data.iva10);
        cabecera.setIva5(data.iva5);
        cabecera.setExenta(data.exenta);
        cabecera.setTotalComprobante(data.totalComprobante);
        cabecera.setRetenciones(data.retenciones);

        cabecera.setAfectacionExento(data.afectacionExento);
        cabecera.setAfectacionGrav10(data.afectacionGrav10);
        cabecera.setAfectacionGrav5(data.afectacionGrav5);

        cabecera.setPeriodoMes(periodoMes);
        cabecera.setPeriodoAnio(periodoAnio);
        cabecera.setPeriodoEmision(periodoEmision);
        cabecera.setFechaEmision(buildFecha(periodoRef, data.dia));
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

    private record VentaRow(
            Integer dia,
            String numero,
            String clienteNombre,
            String clienteRuc,
            BigDecimal gravada10,
            BigDecimal gravada5,
            BigDecimal iva10,
            BigDecimal iva5,
            BigDecimal exenta,
            BigDecimal totalComprobante,
            BigDecimal retenciones,
            String afectacionExento,
            String afectacionGrav10,
            String afectacionGrav5
    ) {
    }

    private static class ImportCounters {
        private int insertadas;
        private int actualizadas;
        private int ignoradas;
        private int leidas;

        void leer() {
            leidas++;
        }

        void ignorar() {
            ignoradas++;
        }

        void registrarResultado(boolean esNueva) {
            if (esNueva) {
                insertadas++;
            } else {
                actualizadas++;
            }
        }

        ImportResultDTO toResult(int periodoMes, int periodoAnio) {
            return new ImportResultDTO(leidas, insertadas, actualizadas, ignoradas, periodoMes, periodoAnio);
        }
    }
}
