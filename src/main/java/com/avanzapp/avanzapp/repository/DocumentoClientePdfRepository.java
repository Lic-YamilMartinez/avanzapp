package com.avanzapp.avanzapp.repository;

import com.avanzapp.avanzapp.model.DocumentoClientePdf;
import com.avanzapp.avanzapp.model.TipoDocumentoPdf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DocumentoClientePdfRepository
        extends JpaRepository<DocumentoClientePdf, Long> {

    List<DocumentoClientePdf> findByUsuarioIdAndPeriodo(
            Long usuarioId,
            LocalDate periodo
    );

    Optional<DocumentoClientePdf> findByUsuarioIdAndPeriodoAndTipo(
            Long usuarioId,
            LocalDate periodo,
            TipoDocumentoPdf tipo
    );

    List<DocumentoClientePdf> findByUsuarioId(Long usuarioId);
}
