package com.avanzapp.avanzapp.service;

import com.avanzapp.avanzapp.dto.DNITCompraCabeceraDTO;
import com.avanzapp.avanzapp.dto.DNITReporteResponseDTO;
import com.avanzapp.avanzapp.dto.DNITVentasCabeceraDTO;
import com.avanzapp.avanzapp.mapper.DNITCompraMapper;
import com.avanzapp.avanzapp.mapper.DNITVentasMapper;
import com.avanzapp.avanzapp.model.DNITCompraCabecera;
import com.avanzapp.avanzapp.model.DNITVentasCabecera;
import com.avanzapp.avanzapp.repository.DNITCompraCabeceraRepository;
import com.avanzapp.avanzapp.repository.DNITVentasCabeceraRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

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
}
