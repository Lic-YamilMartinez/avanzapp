package com.avanzapp.avanzapp.dto;

import com.avanzapp.avanzapp.model.TipoDocumentoPdf;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DocumentoClientePdfDTO {
    private Long id;
    private Long usuarioId;
    private TipoDocumentoPdf tipo;
    private LocalDate periodo;
    private String nombreOriginal;
    private Long sizeBytes;
    private LocalDateTime creadoEn;

    public DocumentoClientePdfDTO(Long id, Long usuarioId, TipoDocumentoPdf tipo, LocalDate periodo,
                                  String nombreOriginal, Long sizeBytes, LocalDateTime creadoEn) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.periodo = periodo;
        this.nombreOriginal = nombreOriginal;
        this.sizeBytes = sizeBytes;
        this.creadoEn = creadoEn;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public TipoDocumentoPdf getTipo() { return tipo; }
    public LocalDate getPeriodo() { return periodo; }
    public String getNombreOriginal() { return nombreOriginal; }
    public Long getSizeBytes() { return sizeBytes; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
