package com.avanzapp.avanzapp.repository;

import com.avanzapp.avanzapp.model.DNITVentasCabecera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DNITVentasCabeceraRepository extends JpaRepository<DNITVentasCabecera, Long> {

    List<DNITVentasCabecera> findByUsuarioId(Long usuarioId);

    List<DNITVentasCabecera> findByUsuarioIdAndPeriodoEmision(Long usuarioId, String periodoEmision);

    List<DNITVentasCabecera> findByUsuarioIdAndPeriodoMesAndPeriodoAnio(Long usuarioId, Integer periodoMes, Integer periodoAnio);

    Optional<DNITVentasCabecera> findFirstByUsuarioIdAndPeriodoMesAndPeriodoAnioAndNumeroComprobante(
            Long usuarioId,
            Integer periodoMes,
            Integer periodoAnio,
            String numeroComprobante
    );
}
