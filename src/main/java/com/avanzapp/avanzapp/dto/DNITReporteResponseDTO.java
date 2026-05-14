package com.avanzapp.avanzapp.dto;

import java.util.List;

public record DNITReporteResponseDTO(
        List<DNITCompraCabeceraDTO> compras,
        List<DNITVentasCabeceraDTO> ventas
) {}
