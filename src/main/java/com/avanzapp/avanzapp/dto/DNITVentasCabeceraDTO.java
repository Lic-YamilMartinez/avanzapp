package com.avanzapp.avanzapp.dto;

import java.math.BigDecimal;
import java.util.List;

public record DNITVentasCabeceraDTO(
        Long id,
        Long usuarioId,
        String numeroComprobante,
        String clienteNombre,
        String clienteRuc,
        Integer dia,
        String fechaEmision,
        String periodoEmision,
        Integer periodoMes,
        Integer periodoAnio,
        BigDecimal gravada10,
        BigDecimal gravada5,
        BigDecimal iva10,
        BigDecimal iva5,
        BigDecimal exenta,
        BigDecimal totalComprobante,
        BigDecimal retenciones,
        String afectacionExento,
        String afectacionGrav10,
        String afectacionGrav5,
        List<DNITVentasDetalleDTO> detalles
) {
}
