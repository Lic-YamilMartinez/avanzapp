package com.avanzapp.avanzapp.service;

import com.avanzapp.avanzapp.dto.ImportResultDTO;
import com.avanzapp.avanzapp.model.*;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DNITComprasImportService {

    private final DNITCompraCabeceraRepository cabeceraRepo;
    private final UsuarioRepository usuarioRepository;

    public DNITComprasImportService(DNITCompraCabeceraRepository cabeceraRepo, UsuarioRepository usuarioRepository) {
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

            YearMonth periodo = detectarPeriodoObligatorio(sheet, filename);
            int periodoMes = periodo.getMonthValue();
            int periodoAnio = periodo.getYear();
            String periodoEmision = String.format("%02d/%d", periodoMes, periodoAnio);

            if (overwrite) {
                var existentes = cabeceraRepo.findByUsuarioIdAndPeriodoMesAndPeriodoAnio(usuarioId, periodoMes, periodoAnio);
                System.out.printf("[DNIT-COMPRAS] overwrite=true → eliminando %d cabeceras %02d/%d (detalles por cascade)%n",
                        existentes.size(), periodoMes, periodoAnio);
                cabeceraRepo.deleteAll(existentes);
            }

            int headerRowIdx = encontrarFilaEncabezado(sheet);
            if (headerRowIdx < 0) {
                throw new RuntimeException("No se encontró la fila de encabezados (columna 'DIA').");
            }

            Row header = sheet.getRow(headerRowIdx);
            Map<String, Integer> col = mapearColumnasNormalizado(header);

            // === columnas principales ===
            int idxDia    = getIdx(col, "DIA");
            int idxDocNro = getIdx(col,
                    "DOCUMENTO_NRO","DOCUMENTO_NO","DOCUMENTO_Nº","DOC_NRO","DOCUMENTO_NRO."
            );
            if (idxDocNro < 0) {
                throw new RuntimeException(
                        "No se encontró la columna de número de comprobante (DOCUMENTO NRO / DOC_NRO)."
                );
            }

            int idxRSoc   = getIdx(col, "R_SOCIAL_APELLIDO_NOMBRE","RAZON_SOCIAL","R_SOCIAL_APELLIDO__NOMBRE");
            int idxRuc    = getIdx(col, "RUC");
            int idxTipoC  = getIdx(col, "T_COMP","TIPO_COMPROBANTE","TCOMP","T_COMP.");
            int idxFPg    = getIdx(col, "F_PG","F_PAGO","FORMA_DE_PAGO","F_PG.");

            int idxExentasMonto = getIdx(col, "EXENTAS","EXENTO","EXENTOS");
            int idxG10   = getIdx(col, "GRAV_10","GRV_10","GRAVADA_10","GRAVADO_10","GRAV_10%");
            int idxG5    = getIdx(col, "GRAV_5","GRV_5","GRAVADA_5","GRAVADO_5","GRAV_5%");
            int idxIva10 = getIdx(col, "IVA_10","I_V_A_10","IVA_10%");
            int idxIva5  = getIdx(col, "IVA_5","I_V_A_5","IVA_5%");
            int idxBase  = getIdx(col, "BASE_IMPONIBLE","BASE");
            int idxTotal = getIdx(col, "TOTAL");

            int idxAfectExentoTxt = getIdx(col, "EXENTO_TXT","EXENTO");
            int idxAfectGrav10Txt = getIdx(col, "GRV_10_TXT","GRV_10");
            int idxAfectGrav5Txt  = getIdx(col, "GRV_5_TXT","GRV_5");

            int insertados = 0, actualizados = 0, ignorados = 0, leidas = 0;

            // Recorremos filas de detalle:
            for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                // === REGLA CLAVE: cortar cuando ya no haya DOCUMENTO NRO ===
                String rawNro = readString(row, idxDocNro);
                String nro = normalizarNro(rawNro);
                if (nro == null || nro.isBlank()) {
                    // A partir de aquí el reporte son totales / basura para nuestro módulo
                    System.out.printf("[DNIT-COMPRAS] Corte en fila %d: DOCUMENTO NRO vacío. Fin de datos útiles.%n", r);
                    break;
                }

                leidas++;

                Integer dia = readInteger(row, idxDia);
                String proveedor = readString(row, idxRSoc);
                BigDecimal total = readMoney(row, idxTotal);

                boolean vacia = (dia == null) &&
                        (proveedor == null || proveedor.isBlank()) &&
                        isZeroOrNull(total);
                if (vacia) {
                    ignorados++;
                    continue;
                }

                DNITCompraCabecera cab = cabeceraRepo
                        .findFirstByUsuarioIdAndPeriodoMesAndPeriodoAnioAndNroComprobante(
                                usuarioId, periodoMes, periodoAnio, nro
                        )
                        .orElse(null);

                boolean esNueva = (cab == null);
                if (cab == null) {
                    cab = new DNITCompraCabecera();
                }

                // ---- Cabecera ----
                cab.setUsuario(usuario);
                cab.setPeriodoMes(periodoMes);
                cab.setPeriodoAnio(periodoAnio);
                cab.setPeriodoEmision(periodoEmision);

                int d = (dia == null || dia < 1 || dia > 31) ? 1 : dia;
                cab.setFechaEmision(LocalDate.of(periodoAnio, periodoMes, d));

                cab.setProveedorNombre(proveedor);
                cab.setProveedorRuc(readString(row, idxRuc));
                cab.setNroComprobante(nro);
                cab.setTipoComprobante(readString(row, idxTipoC));
                cab.setCondicionOperacion(normalizarFormaPago(readString(row, idxFPg)));

                BigDecimal g10 = readMoney(row, idxG10);
                BigDecimal g5  = readMoney(row, idxG5);
                BigDecimal i10 = readMoney(row, idxIva10);
                BigDecimal i5  = readMoney(row, idxIva5);
                BigDecimal ex  = readMoney(row, idxExentasMonto);

                cab.setGravada10(g10);
                cab.setGravada5(g5);
                cab.setIva10(i10);
                cab.setIva5(i5);
                cab.setExenta(ex);
                cab.setBaseImponible(readMoney(row, idxBase));
                cab.setTotalComprobante(safe(total));

                cab.setAfectacionExento(sanitizeTxt(readString(row, idxAfectExentoTxt)));
                cab.setAfectacionGrav10(sanitizeTxt(readString(row, idxAfectGrav10Txt)));
                cab.setAfectacionGrav5(sanitizeTxt(readString(row, idxAfectGrav5Txt)));

                // ---- Detalles (0..3 líneas) ----
                List<DNITCompraDetalle> nuevos = new ArrayList<>();

                if (gtZero(g10) || gtZero(i10)) {
                    DNITCompraDetalle d10 = new DNITCompraDetalle();
                    d10.setTipoLinea(LineaTipo.IVA10);
                    d10.setTasa(10);
                    d10.setBase(safe(g10));
                    d10.setIva(safe(i10));
                    d10.setExento(BigDecimal.ZERO);
                    d10.setClasificacion(cab.getAfectacionGrav10());
                    nuevos.add(d10);
                }

                if (gtZero(g5) || gtZero(i5)) {
                    DNITCompraDetalle d5 = new DNITCompraDetalle();
                    d5.setTipoLinea(LineaTipo.IVA5);
                    d5.setTasa(5);
                    d5.setBase(safe(g5));
                    d5.setIva(safe(i5));
                    d5.setExento(BigDecimal.ZERO);
                    d5.setClasificacion(cab.getAfectacionGrav5());
                    nuevos.add(d5);
                }

                if (gtZero(ex)) {
                    DNITCompraDetalle de = new DNITCompraDetalle();
                    de.setTipoLinea(LineaTipo.EXENTA);
                    de.setTasa(0);
                    de.setBase(BigDecimal.ZERO);
                    de.setIva(BigDecimal.ZERO);
                    de.setExento(safe(ex));
                    de.setClasificacion(cab.getAfectacionExento());
                    nuevos.add(de);
                }

                // Guardado: reemplazamos detalles para garantizar idempotencia
                cab.clearAndAddDetalles(nuevos);

                // Persistir (insert/update)
                cabeceraRepo.save(cab);
                if (esNueva) insertados++; else actualizados++;
            }

            System.out.printf("[DNIT-COMPRAS] Resultado usuario=%d periodo=%02d/%d → Leídas=%d, Insertadas=%d, Actualizadas=%d, Ignoradas=%d%n",
                    usuarioId, periodoMes, periodoAnio, leidas, insertados, actualizados, ignorados);

            return new ImportResultDTO(leidas, insertados, actualizados, ignorados, periodoMes, periodoAnio);
        }
    }

    // ================= Helpers =================

    private static int encontrarFilaEncabezado(Sheet sheet) {
        for (int r = 0; r <= Math.min(sheet.getLastRowNum(), 60); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    String val = normalizeText(cell.getStringCellValue());
                    if ("DIA".equals(val)) return r;
                }
            }
        }
        return -1;
    }

    private static Map<String, Integer> mapearColumnasNormalizado(Row header) {
        Map<String, Integer> map = new HashMap<>();
        if (header == null) return map;
        for (int c = 0; c < header.getLastCellNum(); c++) {
            Cell cell = header.getCell(c);
            String h = (cell != null && cell.getCellType() == CellType.STRING) ? cell.getStringCellValue() : null;
            if (h == null) continue;
            String key = normalizarHeader(h);
            if (!key.isBlank()) map.put(key, c);
        }
        return map;
    }

    private static String normalizarHeader(String h) {
        String t = normalizeText(h)
                .replace("º","").replace("°","")
                .replace("."," ").replace("-"," ")
                .replace("/"," ")
                .replace("%","")
                .trim();
        return t.replaceAll("\\s+","_");
    }

    private static String normalizeText(String s) {
        if (s == null) return "";
        return s.replace('\u00A0',' ')
                .toUpperCase()
                .replace("Ó","O").replace("É","E").replace("Í","I").replace("Á","A").replace("Ú","U").replace("Ñ","N")
                .replaceAll("\\s+"," ")
                .trim();
    }

    private static int getIdx(Map<String,Integer> map, String... aliases) {
        for (String a : aliases) {
            Integer idx = map.get(a);
            if (idx != null) return idx;
        }
        return -1;
    }

    private static String sanitizeTxt(String s) {
        if (s == null) return null;
        String t = s.replace('\u00A0',' ').replaceAll("\\s+", " ").trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizarFormaPago(String fp) {
        if (fp == null) return null;
        fp = fp.trim().toUpperCase();
        return switch (fp) {
            case "CO", "CONTADO" -> "CONTADO";
            case "CR", "CREDITO", "CRÉDITO" -> "CREDITO";
            default -> fp;
        };
    }

    private static String normalizarNro(String nro) {
        if (nro == null) return null;
        return nro.trim().replaceAll("\\s+", " ");
    }

    private static boolean gtZero(BigDecimal v) { return v != null && v.compareTo(BigDecimal.ZERO) > 0; }
    private static boolean isZeroOrNull(BigDecimal v) { return v == null || v.compareTo(BigDecimal.ZERO) == 0; }
    private static BigDecimal safe(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static Integer readInteger(Row row, int idx) {
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

    private static String readString(Row row, int idx) {
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

    private static BigDecimal readMoney(Row row, int idx) {
        if (idx < 0 || row == null) return BigDecimal.ZERO;
        Cell cell = row.getCell(idx);
        if (cell == null) return BigDecimal.ZERO;
        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> parseMoney(cell.getStringCellValue());
            default -> BigDecimal.ZERO;
        };
    }

    private static BigDecimal parseMoney(String s) {
        if (s == null) return BigDecimal.ZERO;
        String v = s.replace("Gs", "")
                .replace("₲", "")
                .replace(".", "")
                .replace(",", ".")
                .trim();
        if (v.isEmpty()) return BigDecimal.ZERO;
        try { return new BigDecimal(v); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private static YearMonth detectarPeriodoObligatorio(Sheet sheet, String filename) {
        int top = Math.min(sheet.getLastRowNum(), 80);
        Pattern pMes = Pattern.compile("MES\\s*:?\\s*([A-ZÁÉÍÓÚÑ]{3,})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Pattern pAnio= Pattern.compile("A[NÑ]O\\s*:?\\s*(20\\d{2})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

        Map<String,Integer> mapa = Map.ofEntries(
                Map.entry("ENERO",1), Map.entry("FEBRERO",2), Map.entry("MARZO",3), Map.entry("ABRIL",4),
                Map.entry("MAYO",5), Map.entry("JUNIO",6), Map.entry("JULIO",7), Map.entry("AGOSTO",8),
                Map.entry("SEPTIEMBRE",9), Map.entry("SETIEMBRE",9), Map.entry("OCTUBRE",10),
                Map.entry("NOVIEMBRE",11), Map.entry("DICIEMBRE",12),
                Map.entry("ENE",1), Map.entry("FEB",2), Map.entry("MAR",3), Map.entry("ABR",4),
                Map.entry("MAY",5), Map.entry("JUN",6), Map.entry("JUL",7), Map.entry("AGO",8),
                Map.entry("SEP",9), Map.entry("SET",9), Map.entry("OCT",10), Map.entry("NOV",11), Map.entry("DIC",12)
        );

        Integer m=null,y=null;
        for (int r=0;r<=top;r++){
            Row row=sheet.getRow(r); if (row==null) continue;
            for (int c=0;c<row.getLastCellNum();c++){
                Cell cell=row.getCell(c); if (cell==null) continue;
                if (cell.getCellType()!=CellType.STRING) continue;
                String t=cell.getStringCellValue().toUpperCase().replace('\u00A0',' ').replaceAll("\\s+"," ").trim();
                Matcher mm=pMes.matcher(t); if (mm.find()) { m = mapa.get(mm.group(1)); }
                Matcher yy=pAnio.matcher(t); if (yy.find()) { y = Integer.parseInt(yy.group(1)); }
                if (m!=null && y!=null) return YearMonth.of(y,m);
            }
        }
        if (m!=null && y==null) y = LocalDate.now().getYear();
        if (y!=null && m==null) m = LocalDate.now().getMonthValue();
        if (m!=null && y!=null) return YearMonth.of(y,m);
        throw new IllegalStateException("No se pudo detectar el periodo (MES/AÑO) en el archivo.");
    }
}
