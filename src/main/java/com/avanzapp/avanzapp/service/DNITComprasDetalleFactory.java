package com.avanzapp.avanzapp.service;

import com.avanzapp.avanzapp.model.DNITCompraDetalle;
import com.avanzapp.avanzapp.model.LineaTipo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
class DNITComprasDetalleFactory {

    List<DNITCompraDetalle> build(BigDecimal gravada10,
                                  BigDecimal gravada5,
                                  BigDecimal iva10,
                                  BigDecimal iva5,
                                  BigDecimal exenta,
                                  String afectacionExento,
                                  String afectacionGrav10,
                                  String afectacionGrav5) {
        List<DNITCompraDetalle> detalles = new ArrayList<>();

        if (DNITComprasParsingUtils.gtZero(gravada10) || DNITComprasParsingUtils.gtZero(iva10)) {
            DNITCompraDetalle d10 = new DNITCompraDetalle();
            d10.setTipoLinea(LineaTipo.IVA10);
            d10.setTasa(10);
            d10.setBase(DNITComprasParsingUtils.safe(gravada10));
            d10.setIva(DNITComprasParsingUtils.safe(iva10));
            d10.setExento(BigDecimal.ZERO);
            d10.setClasificacion(afectacionGrav10);
            detalles.add(d10);
        }

        if (DNITComprasParsingUtils.gtZero(gravada5) || DNITComprasParsingUtils.gtZero(iva5)) {
            DNITCompraDetalle d5 = new DNITCompraDetalle();
            d5.setTipoLinea(LineaTipo.IVA5);
            d5.setTasa(5);
            d5.setBase(DNITComprasParsingUtils.safe(gravada5));
            d5.setIva(DNITComprasParsingUtils.safe(iva5));
            d5.setExento(BigDecimal.ZERO);
            d5.setClasificacion(afectacionGrav5);
            detalles.add(d5);
        }

        if (DNITComprasParsingUtils.gtZero(exenta)) {
            DNITCompraDetalle de = new DNITCompraDetalle();
            de.setTipoLinea(LineaTipo.EXENTA);
            de.setTasa(0);
            de.setBase(BigDecimal.ZERO);
            de.setIva(BigDecimal.ZERO);
            de.setExento(DNITComprasParsingUtils.safe(exenta));
            de.setClasificacion(afectacionExento);
            detalles.add(de);
        }

        return detalles;
    }
}
