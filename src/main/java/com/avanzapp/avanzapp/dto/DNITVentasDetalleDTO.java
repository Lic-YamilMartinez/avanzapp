package com.avanzapp.avanzapp.dto;

import java.math.BigDecimal;

public record DNITVentasDetalleDTO(
        Long id,
        String tipoLinea,
        Integer tasa,
        BigDecimal base,
        BigDecimal iva,
        BigDecimal exento,
        String clasificacion
) {
}
