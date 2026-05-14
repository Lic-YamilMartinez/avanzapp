package com.avanzapp.avanzapp.repository;

import com.avanzapp.avanzapp.model.DNITCompraCabecera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DNITCompraCabeceraRepository extends JpaRepository<DNITCompraCabecera, Long> {

    // 👉 métodos que ya tenías (pueden seguir usándose en otros lados)
    List<DNITCompraCabecera> findByUsuarioId(Long usuarioId);

    List<DNITCompraCabecera> findByUsuarioIdAndPeriodoEmision(Long usuarioId, String periodoEmision);

    List<DNITCompraCabecera> findByUsuarioIdAndPeriodoMesAndPeriodoAnio(
            Long usuarioId,
            Integer mes,
            Integer anio
    );

    Optional<DNITCompraCabecera> findFirstByUsuarioIdAndPeriodoMesAndPeriodoAnioAndNroComprobante(
            Long usuarioId,
            Integer mes,
            Integer anio,
            String nroComprobante
    );

    // ⭐ NUEVO → trae cabeceras + detalles en una sola query
    @Query("""
           select distinct c
           from DNITCompraCabecera c
           left join fetch c.detalles d
           where c.usuario.id = :usuarioId
           """)
    List<DNITCompraCabecera> findByUsuarioIdWithDetalles(@Param("usuarioId") Long usuarioId);
}
