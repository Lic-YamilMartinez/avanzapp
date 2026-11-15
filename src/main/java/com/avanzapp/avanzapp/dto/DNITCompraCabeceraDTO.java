package com.avanzapp.avanzapp.dto;

import java.math.BigDecimal;
import java.util.List;

public record DNITCompraCabeceraDTO(
        Long id,
        Long usuarioId,
        String fuente,
        String proveedorRuc,
        String proveedorNombre,
        String nroComprobante,
        String tipoComprobante,
        String condicionOperacion,
        String timbrado,
        String rucTitular,
        String rubroGastos,
        String observacion,
        String observacionNormalizada,
        String observacionCategoria,
        String observacionNormalizacionMetodo,
        BigDecimal observacionNormalizacionConfianza,
        String fechaEmision,     // yyyy-MM-dd
        String periodoEmision,   // mm/yyyy
        Integer periodoMes,
        Integer periodoAnio,
        BigDecimal gravada10,
        BigDecimal gravada5,
        BigDecimal iva10,
        BigDecimal iva5,
        BigDecimal exenta,
        BigDecimal totalComprobante,
        BigDecimal baseImponible,
        BigDecimal montoCompra,
        BigDecimal montoIva,
        BigDecimal montoPagado,
        BigDecimal gastos,
        String afectacionExento,
        String afectacionGrav10,
        String afectacionGrav5,
        List<DNITCompraDetalleDTO> detalles
) {}
