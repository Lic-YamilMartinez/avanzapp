package com.avanzapp.avanzapp.service;

import com.avanzapp.avanzapp.model.DNITCompraCabecera;
import com.avanzapp.avanzapp.model.FuenteCompra;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DNITComprasFuenteSelectorTest {

    private final DNITComprasFuenteSelector selector = new DNITComprasFuenteSelector();

    @Test
    void priorizaIrpEnMesConAmbasFuentesYMantieneIvaCuandoNoHayIrp() {
        DNITCompraCabecera eneroIva = compra(FuenteCompra.IVA, 1, "IVA-ENE");
        DNITCompraCabecera eneroIrp = compra(FuenteCompra.IRP, 1, "IRP-ENE");
        DNITCompraCabecera febreroIva = compra(FuenteCompra.IVA, 2, "IVA-FEB");

        List<DNITCompraCabecera> resultado = selector.seleccionar(
                List.of(eneroIva, eneroIrp, febreroIva)
        );

        assertEquals(2, resultado.size());
        assertEquals("IRP-ENE", resultado.get(0).getNroComprobante());
        assertEquals("IVA-FEB", resultado.get(1).getNroComprobante());
    }

    private DNITCompraCabecera compra(FuenteCompra fuente, int mes, String numero) {
        DNITCompraCabecera compra = new DNITCompraCabecera();
        compra.setFuente(fuente);
        compra.setPeriodoMes(mes);
        compra.setPeriodoAnio(2026);
        compra.setPeriodoEmision(String.format("%02d/2026", mes));
        compra.setFechaEmision(LocalDate.of(2026, mes, 5));
        compra.setNroComprobante(numero);
        return compra;
    }
}
