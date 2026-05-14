package com.avanzapp.avanzapp.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "documentos_cliente_pdf",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_usuario_periodo_tipo",
                        columnNames = {"usuario_id", "periodo", "tipo"}
                )
        }
)
public class DocumentoClientePdf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CLIENTE
    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoDocumentoPdf tipo;

    /**
     * Representa el mes (siempre día 1)
     * Ej: 2025-09-01
     */
    @Column(nullable = false)
    private LocalDate periodo;

    @Column(nullable = false)
    private String nombreOriginal;

    @Column(nullable = false)
    private String nombreAlmacenado;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long sizeBytes;

    @Column(nullable = false)
    private String ruta;

    @Column(nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    // ===== Getters y Setters =====

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public TipoDocumentoPdf getTipo() {
        return tipo;
    }

    public void setTipo(TipoDocumentoPdf tipo) {
        this.tipo = tipo;
    }

    public LocalDate getPeriodo() {
        return periodo;
    }

    public void setPeriodo(LocalDate periodo) {
        this.periodo = periodo;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public void setNombreOriginal(String nombreOriginal) {
        this.nombreOriginal = nombreOriginal;
    }

    public String getNombreAlmacenado() {
        return nombreAlmacenado;
    }

    public void setNombreAlmacenado(String nombreAlmacenado) {
        this.nombreAlmacenado = nombreAlmacenado;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
