package com.avanzapp.avanzapp.service;

import com.avanzapp.avanzapp.dto.DNITCompraCabeceraDTO;
import com.avanzapp.avanzapp.dto.DNITReporteResponseDTO;
import com.avanzapp.avanzapp.dto.DNITVentasCabeceraDTO;
import com.avanzapp.avanzapp.mapper.DNITCompraMapper;
import com.avanzapp.avanzapp.mapper.DNITVentasMapper;
import com.avanzapp.avanzapp.model.DNITCompraCabecera;
import com.avanzapp.avanzapp.model.DNITVentasCabecera;
import com.avanzapp.avanzapp.model.FuenteCompra;
import com.avanzapp.avanzapp.repository.DNITCompraCabeceraRepository;
import com.avanzapp.avanzapp.repository.DNITVentasCabeceraRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional // readOnly = true para lecturas, si querés podés agregarlo
public class ReportesDNITService {

    private final DNITCompraCabeceraRepository comprasRepo;
    private final DNITVentasCabeceraRepository ventasRepo;

    public ReportesDNITService(DNITCompraCabeceraRepository comprasRepo,
                               DNITVentasCabeceraRepository ventasRepo) {
        this.comprasRepo = comprasRepo;
        this.ventasRepo = ventasRepo;
    }

    public DNITReporteResponseDTO obtenerReporteDnit(Long usuarioId, YearMonth periodo) {

        List<DNITCompraCabecera> compras;
        List<DNITVentasCabecera> ventas;

        if (periodo != null) {
            int mes = periodo.getMonthValue();
            int anio = periodo.getYear();

            compras = comprasRepo.findByUsuarioIdAndPeriodoMesAndPeriodoAnio(usuarioId, mes, anio);
            ventas  = ventasRepo.findByUsuarioIdAndPeriodoMesAndPeriodoAnio(usuarioId, mes, anio);
        } else {
            compras = comprasRepo.findByUsuarioId(usuarioId);
            ventas  = ventasRepo.findByUsuarioId(usuarioId);
        }

        compras = seleccionarFuentePreferida(compras);

        // 🔹 Acá estamos todavía dentro de la transacción,
        // así que podemos tocar `detalles` sin LazyInitializationException
        List<DNITCompraCabeceraDTO> comprasDTO = compras.stream()
                .map(DNITCompraMapper::toDTO)
                .toList();

        List<DNITVentasCabeceraDTO> ventasDTO = ventas.stream()
                .map(DNITVentasMapper::toDTO)
                .toList();

        return new DNITReporteResponseDTO(comprasDTO, ventasDTO);
    }

    /**
     * Regla AvanzApp:
     * - Si un cliente/mes tiene IRP, IRP es la fuente completa y desplaza IVA.
     * - Si no existe IRP para ese mes, se muestran las compras IVA.
     * - Nunca se suman ambas fuentes para el mismo periodo.
     */
    private List<DNITCompraCabecera> seleccionarFuentePreferida(List<DNITCompraCabecera> compras) {
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
