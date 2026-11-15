package com.avanzapp.avanzapp.service;

import com.avanzapp.avanzapp.model.DNITCompraCabecera;
import com.avanzapp.avanzapp.model.FuenteCompra;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DNITComprasFuenteSelector {

    /**
     * Regla de negocio AvanzApp:
     * - Si un cliente/mes tiene IRP, IRP es la fuente completa y desplaza IVA.
     * - Si no existe IRP para ese mes, se muestran las compras IVA.
     * - Nunca se suman ambas fuentes para el mismo periodo.
     */
    public List<DNITCompraCabecera> seleccionar(List<DNITCompraCabecera> compras) {
        Map<String, List<DNITCompraCabecera>> porPeriodo = new LinkedHashMap<>();

        compras.stream()
                .sorted(Comparator
                        .comparing(DNITCompraCabecera::getPeriodoAnio, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DNITCompraCabecera::getPeriodoMes, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DNITCompraCabecera::getFechaEmision, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(c -> {
                    String key = String.format(
                            "%04d-%02d",
                            c.getPeriodoAnio() == null ? 0 : c.getPeriodoAnio(),
                            c.getPeriodoMes() == null ? 0 : c.getPeriodoMes()
                    );
                    porPeriodo.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
                });

        List<DNITCompraCabecera> resultado = new ArrayList<>();

        for (List<DNITCompraCabecera> periodo : porPeriodo.values()) {
            boolean tieneIrp = periodo.stream()
                    .anyMatch(c -> c.getFuente() == FuenteCompra.IRP);

            periodo.stream()
                    .filter(c -> tieneIrp
                            ? c.getFuente() == FuenteCompra.IRP
                            : c.getFuente() == null || c.getFuente() == FuenteCompra.IVA)
                    .forEach(resultado::add);
        }

        return resultado;
    }
}
