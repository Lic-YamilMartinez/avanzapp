package com.avanzapp.avanzapp.mapper;

import com.avanzapp.avanzapp.dto.DNITVentasCabeceraDTO;
import com.avanzapp.avanzapp.dto.DNITVentasDetalleDTO;
import com.avanzapp.avanzapp.model.DNITVentasCabecera;
import com.avanzapp.avanzapp.model.DNITVentasDetalle;

import java.util.List;

public final class DNITVentasMapper {

    private DNITVentasMapper() {
    }

    public static DNITVentasCabeceraDTO toDTO(DNITVentasCabecera cabecera) {
        List<DNITVentasDetalleDTO> detalles = cabecera.getDetalles().stream()
                .map(DNITVentasMapper::detalleToDTO)
                .toList();

        return new DNITVentasCabeceraDTO(
                cabecera.getId(),
                cabecera.getUsuario() != null ? cabecera.getUsuario().getId() : null,
                cabecera.getNumeroComprobante(),
                cabecera.getClienteNombre(),
                cabecera.getClienteRuc(),
                cabecera.getDia(),
                cabecera.getFechaEmision() != null ? cabecera.getFechaEmision().toString() : null,
                cabecera.getPeriodoEmision(),
                cabecera.getPeriodoMes(),
                cabecera.getPeriodoAnio(),
                cabecera.getGravada10(),
                cabecera.getGravada5(),
                cabecera.getIva10(),
                cabecera.getIva5(),
                cabecera.getExenta(),
                cabecera.getTotalComprobante(),
                cabecera.getRetenciones(),
                cabecera.getAfectacionExento(),
                cabecera.getAfectacionGrav10(),
                cabecera.getAfectacionGrav5(),
                detalles
        );
    }

    private static DNITVentasDetalleDTO detalleToDTO(DNITVentasDetalle detalle) {
        return new DNITVentasDetalleDTO(
                detalle.getId(),
                detalle.getTipoLinea().name(),
                detalle.getTasa(),
                detalle.getBase(),
                detalle.getIva(),
                detalle.getExento(),
                detalle.getClasificacion()
        );
    }
}
