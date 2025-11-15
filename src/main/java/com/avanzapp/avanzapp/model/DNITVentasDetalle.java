package com.avanzapp.avanzapp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "dnit_ventas_detalle",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dnit_ventas_detalle_tipo",
                columnNames = {"cabecera_id", "tipo_linea"}
        )
)
public class DNITVentasDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cabecera_id", nullable = false)
    private DNITVentasCabecera cabecera;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_linea", length = 10, nullable = false)
    private LineaTipo tipoLinea;

    @Column(name = "base", precision = 18, scale = 2)
    private BigDecimal base = BigDecimal.ZERO;

    @Column(name = "iva", precision = 18, scale = 2)
    private BigDecimal iva = BigDecimal.ZERO;

    @Column(name = "exento", precision = 18, scale = 2)
    private BigDecimal exento = BigDecimal.ZERO;

    @Column(name = "tasa")
    private Integer tasa;

    @Column(name = "clasificacion", length = 150)
    private String clasificacion;

    public DNITVentasDetalle() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DNITVentasCabecera getCabecera() {
        return cabecera;
    }

    public void setCabecera(DNITVentasCabecera cabecera) {
        this.cabecera = cabecera;
    }

    public LineaTipo getTipoLinea() {
        return tipoLinea;
    }

    public void setTipoLinea(LineaTipo tipoLinea) {
        this.tipoLinea = tipoLinea;
    }

    public BigDecimal getBase() {
        return base;
    }

    public void setBase(BigDecimal base) {
        this.base = base;
    }

    public BigDecimal getIva() {
        return iva;
    }

    public void setIva(BigDecimal iva) {
        this.iva = iva;
    }

    public BigDecimal getExento() {
        return exento;
    }

    public void setExento(BigDecimal exento) {
        this.exento = exento;
    }

    public Integer getTasa() {
        return tasa;
    }

    public void setTasa(Integer tasa) {
        this.tasa = tasa;
    }

    public String getClasificacion() {
        return clasificacion;
    }

    public void setClasificacion(String clasificacion) {
        this.clasificacion = clasificacion;
    }
}
