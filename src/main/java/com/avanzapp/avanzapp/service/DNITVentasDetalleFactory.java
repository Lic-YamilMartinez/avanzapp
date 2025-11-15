package com.avanzapp.avanzapp.service;

import com.avanzapp.avanzapp.model.DNITVentasDetalle;
import com.avanzapp.avanzapp.model.LineaTipo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
class DNITVentasDetalleFactory {

    List<DNITVentasDetalle> build(BigDecimal gravada10,
                                  BigDecimal gravada5,
                                  BigDecimal iva10,
                                  BigDecimal iva5,
                                  BigDecimal exenta,
                                  String afectacionExento,
                                  String afectacionGrav10,
                                  String afectacionGrav5) {
        List<DNITVentasDetalle> detalles = new ArrayList<>();

        if (DNITVentasParsingUtils.gtZero(gravada10) || DNITVentasParsingUtils.gtZero(iva10)) {
            detalles.add(createDetalle(
                    LineaTipo.IVA10,
                    10,
                    DNITVentasParsingUtils.safe(gravada10),
                    DNITVentasParsingUtils.safe(iva10),
                    BigDecimal.ZERO,
                    afectacionGrav10
            ));
        }

        if (DNITVentasParsingUtils.gtZero(gravada5) || DNITVentasParsingUtils.gtZero(iva5)) {
            detalles.add(createDetalle(
                    LineaTipo.IVA5,
                    5,
                    DNITVentasParsingUtils.safe(gravada5),
                    DNITVentasParsingUtils.safe(iva5),
                    BigDecimal.ZERO,
                    afectacionGrav5
            ));
        }

        if (DNITVentasParsingUtils.gtZero(exenta)) {
            detalles.add(createDetalle(
                    LineaTipo.EXENTA,
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    DNITVentasParsingUtils.safe(exenta),
                    afectacionExento
            ));
        }

        return detalles;
    }

    private DNITVentasDetalle createDetalle(LineaTipo tipo,
                                            int tasa,
                                            BigDecimal base,
                                            BigDecimal iva,
                                            BigDecimal exento,
                                            String clasificacion) {
        DNITVentasDetalle detalle = new DNITVentasDetalle();
        detalle.setTipoLinea(tipo);
        detalle.setTasa(tasa);
        detalle.setBase(base);
        detalle.setIva(iva);
        detalle.setExento(exento);
        detalle.setClasificacion(clasificacion);
        return detalle;
    }
}
