package com.avanzapp.avanzapp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "dnit_ventas_cabecera",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dnit_ventas_usuario_periodo_numero",
                columnNames = {"usuario_id", "periodo_mes", "periodo_anio", "numero_comprobante"}
        )
)
public class DNITVentasCabecera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "numero_comprobante", length = 60, nullable = false)
    private String numeroComprobante;

    @Column(name = "cliente_nombre", length = 200)
    private String clienteNombre;

    @Column(name = "cliente_ruc", length = 25)
    private String clienteRuc;

    @Column(name = "dia")
    private Integer dia;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "periodo_emision", length = 7)
    private String periodoEmision;

    @Column(name = "periodo_mes")
    private Integer periodoMes;

    @Column(name = "periodo_anio")
    private Integer periodoAnio;

    @Column(name = "gravada_10", precision = 18, scale = 2)
    private BigDecimal gravada10 = BigDecimal.ZERO;

    @Column(name = "gravada_5", precision = 18, scale = 2)
    private BigDecimal gravada5 = BigDecimal.ZERO;

    @Column(name = "iva_10", precision = 18, scale = 2)
    private BigDecimal iva10 = BigDecimal.ZERO;

    @Column(name = "iva_5", precision = 18, scale = 2)
    private BigDecimal iva5 = BigDecimal.ZERO;

    @Column(name = "exenta", precision = 18, scale = 2)
    private BigDecimal exenta = BigDecimal.ZERO;

    @Column(name = "total_comprobante", precision = 18, scale = 2)
    private BigDecimal totalComprobante = BigDecimal.ZERO;

    @Column(name = "retenciones", precision = 18, scale = 2)
    private BigDecimal retenciones = BigDecimal.ZERO;

    @Column(name = "afectacion_exento", length = 150)
    private String afectacionExento;

    @Column(name = "afectacion_grav10", length = 150)
    private String afectacionGrav10;

    @Column(name = "afectacion_grav5", length = 150)
    private String afectacionGrav5;

    @OneToMany(
            mappedBy = "cabecera",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DNITVentasDetalle> detalles = new ArrayList<>();

    public DNITVentasCabecera() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public void setNumeroComprobante(String numeroComprobante) {
        this.numeroComprobante = numeroComprobante;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getClienteRuc() {
        return clienteRuc;
    }

    public void setClienteRuc(String clienteRuc) {
        this.clienteRuc = clienteRuc;
    }

    public Integer getDia() {
        return dia;
    }

    public void setDia(Integer dia) {
        this.dia = dia;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getPeriodoEmision() {
        return periodoEmision;
    }

    public void setPeriodoEmision(String periodoEmision) {
        this.periodoEmision = periodoEmision;
    }

    public Integer getPeriodoMes() {
        return periodoMes;
    }

    public void setPeriodoMes(Integer periodoMes) {
        this.periodoMes = periodoMes;
    }

    public Integer getPeriodoAnio() {
        return periodoAnio;
    }

    public void setPeriodoAnio(Integer periodoAnio) {
        this.periodoAnio = periodoAnio;
    }

    public BigDecimal getGravada10() {
        return gravada10;
    }

    public void setGravada10(BigDecimal gravada10) {
        this.gravada10 = gravada10;
    }

    public BigDecimal getGravada5() {
        return gravada5;
    }

    public void setGravada5(BigDecimal gravada5) {
        this.gravada5 = gravada5;
    }

    public BigDecimal getIva10() {
        return iva10;
    }

    public void setIva10(BigDecimal iva10) {
        this.iva10 = iva10;
    }

    public BigDecimal getIva5() {
        return iva5;
    }

    public void setIva5(BigDecimal iva5) {
        this.iva5 = iva5;
    }

    public BigDecimal getExenta() {
        return exenta;
    }

    public void setExenta(BigDecimal exenta) {
        this.exenta = exenta;
    }

    public BigDecimal getTotalComprobante() {
        return totalComprobante;
    }

    public void setTotalComprobante(BigDecimal totalComprobante) {
        this.totalComprobante = totalComprobante;
    }

    public BigDecimal getRetenciones() {
        return retenciones;
    }

    public void setRetenciones(BigDecimal retenciones) {
        this.retenciones = retenciones;
    }

    public String getAfectacionExento() {
        return afectacionExento;
    }

    public void setAfectacionExento(String afectacionExento) {
        this.afectacionExento = afectacionExento;
    }

    public String getAfectacionGrav10() {
        return afectacionGrav10;
    }

    public void setAfectacionGrav10(String afectacionGrav10) {
        this.afectacionGrav10 = afectacionGrav10;
    }

    public String getAfectacionGrav5() {
        return afectacionGrav5;
    }

    public void setAfectacionGrav5(String afectacionGrav5) {
        this.afectacionGrav5 = afectacionGrav5;
    }

    public List<DNITVentasDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DNITVentasDetalle> detalles) {
        this.detalles = detalles;
    }

    public void clearAndAddDetalles(List<DNITVentasDetalle> nuevos) {
        this.detalles.clear();
        for (DNITVentasDetalle detalle : nuevos) {
            detalle.setCabecera(this);
            this.detalles.add(detalle);
        }
    }
}
