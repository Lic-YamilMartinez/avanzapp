package com.avanzapp.avanzapp.service;

import com.avanzapp.avanzapp.dto.ImportResultDTO;
import com.avanzapp.avanzapp.model.DNITCompraCabecera;
import com.avanzapp.avanzapp.model.DNITCompraDetalle;
import com.avanzapp.avanzapp.model.FuenteCompra;
import com.avanzapp.avanzapp.model.LineaTipo;
import com.avanzapp.avanzapp.model.Usuario;
import com.avanzapp.avanzapp.repository.DNITCompraCabeceraRepository;
import com.avanzapp.avanzapp.repository.UsuarioRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DNITComprasImportService {

    private final DNITCompraCabeceraRepository cabeceraRepo;
    private final UsuarioRepository usuarioRepository;

    public DNITComprasImportService(DNITCompraCabeceraRepository cabeceraRepo,
                                    UsuarioRepository usuarioRepository) {
        this.cabeceraRepo = cabeceraRepo;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ImportResultDTO importarCompras(MultipartFile file, Long usuarioId, boolean overwrite) throws Exception {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + usuarioId));

        final String filename = file.getOriginalFilename();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheet("Hoja1");
            if (sheet == null) sheet = workbook.getSheetAt(0);

            int headerRowIdx = encontrarFilaEncabezado(sheet);
            if (headerRowIdx < 0) {
                throw new RuntimeException(
                        "No se reconoció el formato del archivo. Se esperaba un reporte de Compras IVA o Libro de Egresos IRP."
                );
            }

            Row header = sheet.getRow(headerRowIdx);
            Map<String, Integer> col = mapearColumnasNormalizado(header);
            FuenteCompra fuente = detectarFuente(col);

            if (fuente == FuenteCompra.IRP) {
                return importarIrp(sheet, headerRowIdx, col, usuario, overwrite);
            }

            return importarIva(sheet, headerRowIdx, col, usuario, filename, overwrite);
        }
    }

    private ImportResultDTO importarIva(Sheet sheet,
                                        int headerRowIdx,
                                        Map<String, Integer> col,
                                        Usuario usuario,
                                        String filename,
                                        boolean overwrite) {

        Long usuarioId = usuario.getId();
        YearMonth periodo = detectarPeriodoObligatorio(sheet, filename);
        int periodoMes = periodo.getMonthValue();
        int periodoAnio = periodo.getYear();
        String periodoEmision = String.format("%02d/%d", periodoMes, periodoAnio);

        if (overwrite) {
            var existentes = cabeceraRepo.findByUsuarioIdAndPeriodoMesAndPeriodoAnioAndFuente(
                    usuarioId, periodoMes, periodoAnio, FuenteCompra.IVA
            );
            cabeceraRepo.deleteAll(existentes);
        }

        int idxDia = getIdx(col, "DIA");
        int idxDocNro = getIdx(col,
                "DOCUMENTO_NRO", "DOCUMENTO_NO", "DOCUMENTO_N", "DOC_NRO", "DOCUMENTO_NRO_"
        );
        if (idxDocNro < 0) {
            throw new RuntimeException(
                    "No se encontró la columna de número de comprobante (DOCUMENTO NRO / DOC_NRO)."
            );
        }

        int idxRSoc = getIdx(col,
                "R_SOCIAL_APELLIDO_NOMBRE", "RAZON_SOCIAL", "R_SOCIAL_APELLIDO__NOMBRE"
        );
        int idxRuc = getIdx(col, "RUC");
        int idxTipoC = getIdx(col, "T_COMP", "TIPO_COMPROBANTE", "TCOMP", "T_COMP_");
        int idxFPg = getIdx(col, "F_PG", "F_PAGO", "FORMA_DE_PAGO", "F_PG_");

        int idxExentasMonto = getIdx(col, "EXENTAS", "EXENTO", "EXENTOS");
        int idxG10 = getIdx(col, "GRAV_10", "GRV_10", "GRAVADA_10", "GRAVADO_10");
        int idxG5 = getIdx(col, "GRAV_5", "GRV_5", "GRAVADA_5", "GRAVADO_5");
        int idxIva10 = getIdx(col, "IVA_10", "I_V_A_10");
        int idxIva5 = getIdx(col, "IVA_5", "I_V_A_5");
        int idxBase = getIdx(col, "BASE_IMPONIBLE", "BASE");
        int idxTotal = getIdx(col, "TOTAL");

        int idxAfectExentoTxt = getIdx(col, "EXENTO_TXT", "EXENTO");
        int idxAfectGrav10Txt = getIdx(col, "GRV_10_TXT", "GRV_10");
        int idxAfectGrav5Txt = getIdx(col, "GRV_5_TXT", "GRV_5");

        int insertados = 0;
        int actualizados = 0;
        int ignorados = 0;
        int leidas = 0;

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String nro = normalizarNro(readString(row, idxDocNro));
            if (nro == null || nro.isBlank()) {
                break;
            }

            leidas++;

            Integer dia = readInteger(row, idxDia);
            String proveedor = readString(row, idxRSoc);
            String proveedorRuc = sanitizeTxt(readString(row, idxRuc));
            BigDecimal total = readMoney(row, idxTotal);

            boolean vacia = dia == null
                    && (proveedor == null || proveedor.isBlank())
                    && isZeroOrNull(total);
            if (vacia) {
                ignorados++;
                continue;
            }

            DNITCompraCabecera cab = cabeceraRepo
                    .findFirstByUsuarioIdAndPeriodoMesAndPeriodoAnioAndFuenteAndProveedorRucAndNroComprobante(
                            usuarioId,
                            periodoMes,
                            periodoAnio,
                            FuenteCompra.IVA,
                            proveedorRuc,
                            nro
                    )
                    .orElse(null);

            boolean esNueva = cab == null;
            if (cab == null) cab = new DNITCompraCabecera();

            cab.setUsuario(usuario);
            cab.setFuente(FuenteCompra.IVA);
            cab.setPeriodoMes(periodoMes);
            cab.setPeriodoAnio(periodoAnio);
            cab.setPeriodoEmision(periodoEmision);

            int d = dia == null || dia < 1 || dia > periodo.lengthOfMonth() ? 1 : dia;
            cab.setFechaEmision(LocalDate.of(periodoAnio, periodoMes, d));

            cab.setProveedorNombre(sanitizeTxt(proveedor));
            cab.setProveedorRuc(proveedorRuc);
            cab.setNroComprobante(nro);
            cab.setTipoComprobante(sanitizeTxt(readString(row, idxTipoC)));
            cab.setCondicionOperacion(normalizarFormaPago(readString(row, idxFPg)));
            cab.setTimbrado(null);
            cab.setRucTitular(null);
            cab.setRubroGastos(null);
            cab.setObservacion(null);

            BigDecimal g10 = readMoney(row, idxG10);
            BigDecimal g5 = readMoney(row, idxG5);
            BigDecimal i10 = readMoney(row, idxIva10);
            BigDecimal i5 = readMoney(row, idxIva5);
            BigDecimal ex = readMoney(row, idxExentasMonto);
            BigDecimal base = readMoney(row, idxBase);

            cab.setGravada10(g10);
            cab.setGravada5(g5);
            cab.setIva10(i10);
            cab.setIva5(i5);
            cab.setExenta(ex);
            cab.setBaseImponible(base);
            cab.setTotalComprobante(safe(total));

            BigDecimal montoCompra = !isZeroOrNull(base)
                    ? base
                    : safe(g10).add(safe(g5)).add(safe(ex));
            BigDecimal montoIva = safe(i10).add(safe(i5));
            cab.setMontoCompra(montoCompra);
            cab.setMontoIva(montoIva);
            cab.setMontoPagado(safe(total));
            cab.setGastos(safe(total));

            cab.setAfectacionExento(sanitizeTxt(readString(row, idxAfectExentoTxt)));
            cab.setAfectacionGrav10(sanitizeTxt(readString(row, idxAfectGrav10Txt)));
            cab.setAfectacionGrav5(sanitizeTxt(readString(row, idxAfectGrav5Txt)));

            cab.clearAndAddDetalles(crearDetallesIva(
                    g10,
                    g5,
                    i10,
                    i5,
                    ex,
                    cab.getAfectacionExento(),
                    cab.getAfectacionGrav10(),
                    cab.getAfectacionGrav5()
            ));

            cabeceraRepo.save(cab);
            if (esNueva) insertados++; else actualizados++;
        }

        return new ImportResultDTO(
                leidas,
                insertados,
                actualizados,
                ignorados,
                periodoMes,
                periodoAnio,
                FuenteCompra.IVA.name(),
                List.of(periodoEmision)
        );
    }

    private ImportResultDTO importarIrp(Sheet sheet,
                                        int headerRowIdx,
                                        Map<String, Integer> col,
                                        Usuario usuario,
                                        boolean overwrite) {

        int idxFecha = getRequiredIdx(col, "FECHA");
        int idxRucVendedor = getRequiredIdx(col, "RUC_O_CI_DEL_VENDEDOR", "RUC_CI_DEL_VENDEDOR");
        int idxNombreVendedor = getRequiredIdx(col,
                "NOMBRE_Y_APELLIDO_O_RAZON_SOCIAL_DEL_VENDEDOR",
                "NOMBRE_APELLIDO_O_RAZON_SOCIAL_DEL_VENDEDOR",
                "RAZON_SOCIAL_DEL_VENDEDOR"
        );
        int idxDocumento = getRequiredIdx(col, "NUMERO_DEL_DOCUMENTO", "NRO_DEL_DOCUMENTO");
        int idxTimbrado = getIdx(col, "NUMERO_DEL_TIMBRADO", "NRO_DEL_TIMBRADO", "TIMBRADO");
        int idxTipoDoc = getRequiredIdx(col, "TIPO_DEL_DOC", "TIPO_DEL_DOCUMENTO");
        int idxMontoCompra = getRequiredIdx(col, "MONTO_DE_COMPRA", "MONTO_COMPRA");
        int idxMontoIva = getIdx(col, "MONTO_IVA", "MONTO_DE_IVA");
        int idxMontoPagado = getIdx(col, "MONTO_PAGADO");
        int idxTotalComprobante = getRequiredIdx(col, "TOTAL_COMPROBANTE", "TOTAL");
        int idxRucTitular = getIdx(col, "RUC_DEL_TITULAR");
        int idxRubroGastos = getIdx(col, "RUBRO_DE_GASTOS", "RUBRO_GASTOS");
        int idxObservacion = getIdx(col, "OBSERVACION", "OBSERVACIONES");
        int idxGastos = getIdx(col, "GASTOS");

        List<IrpRow> registros = new ArrayList<>();
        int ignorados = 0;
        int leidas = 0;

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            LocalDate fecha = readDate(row, idxFecha);
            String nro = normalizarNro(readString(row, idxDocumento));
            String tipo = sanitizeTxt(readString(row, idxTipoDoc));

            boolean filaSinDatos = fecha == null
                    && (nro == null || nro.isBlank())
                    && (tipo == null || tipo.isBlank());

            if (filaSinDatos) {
                continue;
            }

            leidas++;

            if (tipo != null && "PAGO".equals(normalizeText(tipo))) {
                ignorados++;
                continue;
            }

            if (fecha == null || nro == null || nro.isBlank()) {
                ignorados++;
                continue;
            }

            BigDecimal montoCompra = readMoney(row, idxMontoCompra);
            BigDecimal montoIva = readMoney(row, idxMontoIva);
            BigDecimal montoPagado = readMoney(row, idxMontoPagado);
            BigDecimal total = readMoney(row, idxTotalComprobante);

            if (isZeroOrNull(total)) {
                total = !isZeroOrNull(montoPagado)
                        ? montoPagado
                        : safe(montoCompra).add(safe(montoIva));
            }

            registros.add(new IrpRow(
                    fecha,
                    sanitizeTxt(readString(row, idxRucVendedor)),
                    sanitizeTxt(readString(row, idxNombreVendedor)),
                    nro,
                    sanitizeTxt(readString(row, idxTimbrado)),
                    tipo,
                    montoCompra,
                    montoIva,
                    montoPagado,
                    total,
                    sanitizeTxt(readString(row, idxRucTitular)),
                    sanitizeTxt(readString(row, idxRubroGastos)),
                    sanitizeTxt(readString(row, idxObservacion)),
                    readMoney(row, idxGastos)
            ));
        }

        if (registros.isEmpty()) {
            throw new RuntimeException("El reporte IRP no contiene comprobantes válidos para importar.");
        }

        SortedSet<YearMonth> periodos = new TreeSet<>();
        for (IrpRow reg : registros) {
            periodos.add(YearMonth.from(reg.fecha()));
        }

        if (overwrite) {
            for (YearMonth periodo : periodos) {
                var existentes = cabeceraRepo.findByUsuarioIdAndPeriodoMesAndPeriodoAnioAndFuente(
                        usuario.getId(),
                        periodo.getMonthValue(),
                        periodo.getYear(),
                        FuenteCompra.IRP
                );
                cabeceraRepo.deleteAll(existentes);
            }
        }

        int insertados = 0;
        int actualizados = 0;

        for (IrpRow reg : registros) {
            YearMonth periodo = YearMonth.from(reg.fecha());

            DNITCompraCabecera cab = cabeceraRepo
                    .findFirstByUsuarioIdAndPeriodoMesAndPeriodoAnioAndFuenteAndProveedorRucAndNroComprobante(
                            usuario.getId(),
                            periodo.getMonthValue(),
                            periodo.getYear(),
                            FuenteCompra.IRP,
                            reg.proveedorRuc(),
                            reg.nroComprobante()
                    )
                    .orElse(null);

            boolean esNueva = cab == null;
            if (cab == null) cab = new DNITCompraCabecera();

            cab.setUsuario(usuario);
            cab.setFuente(FuenteCompra.IRP);
            cab.setProveedorRuc(reg.proveedorRuc());
            cab.setProveedorNombre(reg.proveedorNombre());
            cab.setNroComprobante(reg.nroComprobante());
            cab.setTipoComprobante(reg.tipoComprobante());
            cab.setCondicionOperacion(null);
            cab.setTimbrado(reg.timbrado());
            cab.setRucTitular(reg.rucTitular());
            cab.setRubroGastos(reg.rubroGastos());
            cab.setObservacion(reg.observacion());

            cab.setFechaEmision(reg.fecha());
            cab.setPeriodoMes(periodo.getMonthValue());
            cab.setPeriodoAnio(periodo.getYear());
            cab.setPeriodoEmision(String.format("%02d/%d", periodo.getMonthValue(), periodo.getYear()));

            cab.setMontoCompra(safe(reg.montoCompra()));
            cab.setMontoIva(safe(reg.montoIva()));
            cab.setMontoPagado(safe(reg.montoPagado()));
            cab.setGastos(safe(reg.gastos()));
            cab.setTotalComprobante(safe(reg.totalComprobante()));
            cab.setBaseImponible(safe(reg.montoCompra()));

            // El IRP informa IVA total, no discrimina con la estructura IVA 10%/5% del reporte IVA.
            cab.setGravada10(BigDecimal.ZERO);
            cab.setGravada5(BigDecimal.ZERO);
            cab.setIva10(BigDecimal.ZERO);
            cab.setIva5(BigDecimal.ZERO);
            cab.setExenta(BigDecimal.ZERO);

            // Compatibilidad con la app actual: el rubro IRP también queda disponible
            // como categoría genérica hasta que el front use rubroGastos directamente.
            cab.setAfectacionExento(null);
            cab.setAfectacionGrav10(reg.rubroGastos());
            cab.setAfectacionGrav5(null);
            cab.clearAndAddDetalles(Collections.emptyList());

            cabeceraRepo.save(cab);
            if (esNueva) insertados++; else actualizados++;
        }

        List<String> periodosTexto = periodos.stream()
                .map(p -> String.format("%02d/%d", p.getMonthValue(), p.getYear()))
                .toList();

        YearMonth unico = periodos.size() == 1 ? periodos.first() : null;
        int periodoMes = unico != null ? unico.getMonthValue() : 0;
        int periodoAnio = unico != null
                ? unico.getYear()
                : periodos.stream().map(YearMonth::getYear).distinct().count() == 1
                    ? periodos.first().getYear()
                    : 0;

        return new ImportResultDTO(
                leidas,
                insertados,
                actualizados,
                ignorados,
                periodoMes,
                periodoAnio,
                FuenteCompra.IRP.name(),
                periodosTexto
        );
    }

    private static FuenteCompra detectarFuente(Map<String, Integer> col) {
        boolean esIrp = hasAny(col, "FECHA")
                && hasAny(col, "NUMERO_DEL_DOCUMENTO", "NRO_DEL_DOCUMENTO")
                && hasAny(col, "MONTO_DE_COMPRA", "MONTO_COMPRA")
                && hasAny(col, "TOTAL_COMPROBANTE");

        if (esIrp) return FuenteCompra.IRP;

        boolean esIva = hasAny(col, "DIA")
                && hasAny(col, "DOCUMENTO_NRO", "DOCUMENTO_NO", "DOC_NRO");

        if (esIva) return FuenteCompra.IVA;

        throw new RuntimeException(
                "No se pudo identificar el formato. El archivo debe ser Compras IVA o Libro de Egresos IRP."
        );
    }

    private static int encontrarFilaEncabezado(Sheet sheet) {
        for (int r = 0; r <= Math.min(sheet.getLastRowNum(), 80); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            Map<String, Integer> columnas = mapearColumnasNormalizado(row);
            try {
                detectarFuente(columnas);
                return r;
            } catch (RuntimeException ignored) {
                // seguir buscando
            }
        }
        return -1;
    }

    private static List<DNITCompraDetalle> crearDetallesIva(BigDecimal gravada10,
                                                            BigDecimal gravada5,
                                                            BigDecimal iva10,
                                                            BigDecimal iva5,
                                                            BigDecimal exenta,
                                                            String afectacionExento,
                                                            String afectacionGrav10,
                                                            String afectacionGrav5) {
        List<DNITCompraDetalle> detalles = new ArrayList<>();

        if (gtZero(gravada10) || gtZero(iva10)) {
            DNITCompraDetalle d10 = new DNITCompraDetalle();
            d10.setTipoLinea(LineaTipo.IVA10);
            d10.setTasa(10);
            d10.setBase(safe(gravada10));
            d10.setIva(safe(iva10));
            d10.setExento(BigDecimal.ZERO);
            d10.setClasificacion(afectacionGrav10);
            detalles.add(d10);
        }

        if (gtZero(gravada5) || gtZero(iva5)) {
            DNITCompraDetalle d5 = new DNITCompraDetalle();
            d5.setTipoLinea(LineaTipo.IVA5);
            d5.setTasa(5);
            d5.setBase(safe(gravada5));
            d5.setIva(safe(iva5));
            d5.setExento(BigDecimal.ZERO);
            d5.setClasificacion(afectacionGrav5);
            detalles.add(d5);
        }

        if (gtZero(exenta)) {
            DNITCompraDetalle de = new DNITCompraDetalle();
            de.setTipoLinea(LineaTipo.EXENTA);
            de.setTasa(0);
            de.setBase(BigDecimal.ZERO);
            de.setIva(BigDecimal.ZERO);
            de.setExento(safe(exenta));
            de.setClasificacion(afectacionExento);
            detalles.add(de);
        }

        return detalles;
    }

    private static Map<String, Integer> mapearColumnasNormalizado(Row header) {
        Map<String, Integer> map = new HashMap<>();
        if (header == null) return map;

        for (int c = 0; c < header.getLastCellNum(); c++) {
            Cell cell = header.getCell(c);
            String h = readRawHeader(cell);
            if (h == null) continue;

            String key = normalizarHeader(h);
            if (!key.isBlank()) map.put(key, c);
        }
        return map;
    }

    private static String readRawHeader(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> new DecimalFormat("#.###############").format(cell.getNumericCellValue());
            default -> null;
        };
    }

    private static String normalizarHeader(String h) {
        return normalizeText(h)
                .replace("º", "")
                .replace("°", "")
                .replace(".", " ")
                .replace("-", " ")
                .replace("/", " ")
                .replace("%", "")
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static String normalizeText(String s) {
        if (s == null) return "";
        String limpio = s.replace('\u00A0', ' ').trim();
        limpio = Normalizer.normalize(limpio, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return limpio.toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean hasAny(Map<String, Integer> map, String... aliases) {
        return getIdx(map, aliases) >= 0;
    }

    private static int getRequiredIdx(Map<String, Integer> map, String... aliases) {
        int idx = getIdx(map, aliases);
        if (idx < 0) {
            throw new RuntimeException(
                    "Falta una columna obligatoria: " + String.join(" / ", aliases)
            );
        }
        return idx;
    }

    private static int getIdx(Map<String, Integer> map, String... aliases) {
        for (String alias : aliases) {
            Integer idx = map.get(alias);
            if (idx != null) return idx;
        }
        return -1;
    }

    private static String sanitizeTxt(String s) {
        if (s == null) return null;
        String t = s.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizarFormaPago(String fp) {
        if (fp == null) return null;
        String value = normalizeText(fp);
        return switch (value) {
            case "CO", "CONTADO" -> "CONTADO";
            case "CR", "CREDITO" -> "CREDITO";
            default -> value;
        };
    }

    private static String normalizarNro(String nro) {
        if (nro == null) return null;
        String n = nro.trim().replaceAll("\\s+", " ");
        return n.isBlank() ? null : n;
    }

    private static boolean gtZero(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }

    private static boolean isZeroOrNull(BigDecimal v) {
        return v == null || v.compareTo(BigDecimal.ZERO) == 0;
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static Integer readInteger(Row row, int idx) {
        if (idx < 0 || row == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Integer.parseInt(cell.getStringCellValue().trim());
                } catch (Exception e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    private static String readString(Row row, int idx) {
        if (idx < 0 || row == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate().toString();
                }
                yield new DecimalFormat("#.###############").format(cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue().trim();
                } catch (Exception ex) {
                    try {
                        yield new DecimalFormat("#.###############").format(cell.getNumericCellValue());
                    } catch (Exception ignored) {
                        yield null;
                    }
                }
            }
            default -> null;
        };
    }

    private static LocalDate readDate(Row row, int idx) {
        if (idx < 0 || row == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            if (DateUtil.isValidExcelDate(value)) {
                return DateUtil.getLocalDateTime(value).toLocalDate();
            }
        }

        String raw = readString(row, idx);
        if (raw == null || raw.isBlank()) return null;

        List<DateTimeFormatter> formatos = List.of(
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("dd/MM/uuuu"),
                DateTimeFormatter.ofPattern("d-M-uuuu"),
                DateTimeFormatter.ofPattern("dd-MM-uuuu"),
                DateTimeFormatter.ISO_LOCAL_DATE
        );

        for (DateTimeFormatter formatter : formatos) {
            try {
                return LocalDate.parse(raw.trim(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
    }

    private static BigDecimal readMoney(Row row, int idx) {
        if (idx < 0 || row == null) return BigDecimal.ZERO;
        Cell cell = row.getCell(idx);
        if (cell == null) return BigDecimal.ZERO;

        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> parseMoney(cell.getStringCellValue());
            case FORMULA -> {
                try {
                    yield BigDecimal.valueOf(cell.getNumericCellValue());
                } catch (Exception ex) {
                    try {
                        yield parseMoney(cell.getStringCellValue());
                    } catch (Exception ignored) {
                        yield BigDecimal.ZERO;
                    }
                }
            }
            default -> BigDecimal.ZERO;
        };
    }

    private static BigDecimal parseMoney(String s) {
        if (s == null) return BigDecimal.ZERO;

        String v = s.replace("Gs", "")
                .replace("₲", "")
                .replace(" ", "")
                .trim();

        if (v.isEmpty()) return BigDecimal.ZERO;

        if (v.contains(",") && v.contains(".")) {
            if (v.lastIndexOf(',') > v.lastIndexOf('.')) {
                v = v.replace(".", "").replace(",", ".");
            } else {
                v = v.replace(",", "");
            }
        } else if (v.contains(",")) {
            v = v.replace(".", "").replace(",", ".");
        }

        try {
            return new BigDecimal(v);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static YearMonth detectarPeriodoObligatorio(Sheet sheet, String filename) {
        int top = Math.min(sheet.getLastRowNum(), 80);
        Pattern pMes = Pattern.compile(
                "MES\\s*:?\\s*([A-ZÁÉÍÓÚÑ]{3,})",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );
        Pattern pAnio = Pattern.compile(
                "A[NÑ]O\\s*:?\\s*(20\\d{2})",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );

        Map<String, Integer> mapa = Map.ofEntries(
                Map.entry("ENERO", 1), Map.entry("FEBRERO", 2), Map.entry("MARZO", 3), Map.entry("ABRIL", 4),
                Map.entry("MAYO", 5), Map.entry("JUNIO", 6), Map.entry("JULIO", 7), Map.entry("AGOSTO", 8),
                Map.entry("SEPTIEMBRE", 9), Map.entry("SETIEMBRE", 9), Map.entry("OCTUBRE", 10),
                Map.entry("NOVIEMBRE", 11), Map.entry("DICIEMBRE", 12),
                Map.entry("ENE", 1), Map.entry("FEB", 2), Map.entry("MAR", 3), Map.entry("ABR", 4),
                Map.entry("MAY", 5), Map.entry("JUN", 6), Map.entry("JUL", 7), Map.entry("AGO", 8),
                Map.entry("SEP", 9), Map.entry("SET", 9), Map.entry("OCT", 10), Map.entry("NOV", 11), Map.entry("DIC", 12)
        );

        Integer m = null;
        Integer y = null;

        for (int r = 0; r <= top; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                if (cell == null || cell.getCellType() != CellType.STRING) continue;

                String t = cell.getStringCellValue()
                        .toUpperCase(Locale.ROOT)
                        .replace('\u00A0', ' ')
                        .replaceAll("\\s+", " ")
                        .trim();

                Matcher mm = pMes.matcher(t);
                if (mm.find()) m = mapa.get(mm.group(1));

                Matcher yy = pAnio.matcher(t);
                if (yy.find()) y = Integer.parseInt(yy.group(1));

                if (m != null && y != null) return YearMonth.of(y, m);
            }
        }

        if (filename != null) {
            String normalizedFilename = normalizeText(filename);
            for (Map.Entry<String, Integer> entry : mapa.entrySet()) {
                if (normalizedFilename.contains(entry.getKey())) {
                    m = entry.getValue();
                    break;
                }
            }

            Matcher yy = Pattern.compile("(20\\d{2})").matcher(normalizedFilename);
            if (yy.find()) y = Integer.parseInt(yy.group(1));
        }

        if (m != null && y == null) y = LocalDate.now().getYear();
        if (y != null && m == null) m = LocalDate.now().getMonthValue();
        if (m != null && y != null) return YearMonth.of(y, m);

        throw new IllegalStateException("No se pudo detectar el periodo (MES/AÑO) en el archivo IVA.");
    }

    private record IrpRow(
            LocalDate fecha,
            String proveedorRuc,
            String proveedorNombre,
            String nroComprobante,
            String timbrado,
            String tipoComprobante,
            BigDecimal montoCompra,
            BigDecimal montoIva,
            BigDecimal montoPagado,
            BigDecimal totalComprobante,
            String rucTitular,
            String rubroGastos,
            String observacion,
            BigDecimal gastos
    ) {}
}
