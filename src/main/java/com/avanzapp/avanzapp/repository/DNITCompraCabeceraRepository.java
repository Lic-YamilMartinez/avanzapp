package com.avanzapp.avanzapp.repository;

import com.avanzapp.avanzapp.model.DNITCompraCabecera;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DNITCompraCabeceraRepository extends JpaRepository<DNITCompraCabecera, Long> {
    List<DNITCompraCabecera> findByUsuarioId(Long usuarioId);
    List<DNITCompraCabecera> findByUsuarioIdAndPeriodoEmision(Long usuarioId, String periodoEmision);
    List<DNITCompraCabecera> findByUsuarioIdAndPeriodoMesAndPeriodoAnio(Long usuarioId, Integer mes, Integer anio);

    Optional<DNITCompraCabecera> findFirstByUsuarioIdAndPeriodoMesAndPeriodoAnioAndNroComprobante(
            Long usuarioId, Integer mes, Integer anio, String nroComprobante
    );
}
