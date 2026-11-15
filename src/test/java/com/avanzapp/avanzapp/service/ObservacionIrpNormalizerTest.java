package com.avanzapp.avanzapp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObservacionIrpNormalizerTest {

    private final ObservacionIrpNormalizer normalizer = new ObservacionIrpNormalizer();

    @Test
    void agrupaVariantesDeConsumicion() {
        assertCategoria("Consumicion", "Consumición");
        assertCategoria("Consumiciones", "Consumición");
        assertCategoria("Consumisiones", "Consumición");
        assertCategoria("  consumición   ", "Consumición");
    }

    @Test
    void reconoceCategoriasFrecuentesDelArchivoReal() {
        assertCategoria("Combustibles", "Combustible");
        assertCategoria("Gastos Bancarios", "Gastos bancarios");
        assertCategoria("Mant. de Jardin", "Mantenimiento de jardín");
        assertCategoria("Articulos Alimenticios", "Artículos alimenticios");
        assertCategoria("Seguro Medico", "Seguro médico");
    }

    @Test
    void textoDeFacturaSeNormalizaPeroNoSeInventaCategoria() {
        var r = normalizer.normalizar("Compra segun Factura Nro.: 001-001-1364088");
        assertEquals("Compra según factura", r.normalizada());
        assertNull(r.categoria());
        assertEquals("REGLA_FACTURA", r.metodo());
        assertEquals(1.0, r.confianza());
    }

    @Test
    void textoDesconocidoSeConservaSinClasificacionForzada() {
        var r = normalizer.normalizar("Servicio especial del proveedor");
        assertEquals("Servicio especial del proveedor", r.normalizada());
        assertNull(r.categoria());
        assertEquals("LIMPIEZA", r.metodo());
        assertTrue(r.confianza() < 0.50);
    }

    private void assertCategoria(String original, String esperada) {
        var r = normalizer.normalizar(original);
        assertEquals(esperada, r.normalizada());
        assertEquals(esperada, r.categoria());
        assertTrue(r.confianza() >= 0.84);
    }
}
