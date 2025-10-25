package com.avanzapp.avanzapp.mapper;

import com.avanzapp.avanzapp.dto.*;
import com.avanzapp.avanzapp.model.*;

import java.util.List;

public class DNITCompraMapper {

    public static DNITCompraCabeceraDTO toDTO(DNITCompraCabecera c) {
        List<DNITCompraDetalleDTO> dets = c.getDetalles().stream().map(d ->
                new DNITCompraDetalleDTO(
                        d.getId(),
                        d.getTipoLinea().name(),
                        d.getTasa(),
                        d.getBase(),
                        d.getIva(),
                        d.getExento(),
                        d.getClasificacion()
                )
        ).toList();

        return new DNITCompraCabeceraDTO(
                c.getId(),
                c.getUsuario() != null ? c.getUsuario().getId() : null,
                c.getProveedorRuc(),
                c.getProveedorNombre(),
                c.getNroComprobante(),
                c.getTipoComprobante(),
                c.getCondicionOperacion(),
                c.getFechaEmision() != null ? c.getFechaEmision().toString() : null,
                c.getPeriodoEmision(),
                c.getPeriodoMes(),
                c.getPeriodoAnio(),
                c.getGravada10(),
                c.getGravada5(),
                c.getIva10(),
                c.getIva5(),
                c.getExenta(),
                c.getTotalComprobante(),
                c.getBaseImponible(),
                c.getAfectacionExento(),
                c.getAfectacionGrav10(),
                c.getAfectacionGrav5(),
                dets
        );
    }
}
