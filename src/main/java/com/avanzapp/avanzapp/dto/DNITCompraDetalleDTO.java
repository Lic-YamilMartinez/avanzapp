package com.avanzapp.avanzapp.dto;

import java.math.BigDecimal;

public record DNITCompraDetalleDTO(
        Long id,
        String tipoLinea,      // "IVA10" | "IVA5" | "EXENTA"
        Integer tasa,          // 10,5,0
        BigDecimal base,
        BigDecimal iva,
        BigDecimal exento,
        String clasificacion
) {}
