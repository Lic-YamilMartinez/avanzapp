package com.avanzapp.avanzapp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dnit_compra_cabecera")
public class DNITCompraCabecera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== RELACIÓN CON USUARIO =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuente", length = 10)
    private FuenteCompra fuente = FuenteCompra.IVA;

    // ===== PROVEEDOR / COMPROBANTE =====
    @Column(name = "proveedor_ruc", length = 25)
    private String proveedorRuc;

    @Column(name = "proveedor_nombre", length = 200)
    private String proveedorNombre;

    @Column(name = "nro_comprobante", length = 60, nullable = false)
    private String nroComprobante;

    @Column(name = "tipo_comprobante", length = 50)
    private String tipoComprobante;

    @Column(name = "condicion_operacion", length = 30)
    private String condicionOperacion;

    @Column(name = "timbrado", length = 30)
    private String timbrado;

    @Column(name = "ruc_titular", length = 25)
    private String rucTitular;

    @Column(name = "rubro_gastos", length = 500)
    private String rubroGastos;

    @Column(name = "observacion", length = 1000)
    private String observacion;

    @Column(name = "observacion_normalizada", length = 500)
    private String observacionNormalizada;

    @Column(name = "observacion_categoria", length = 150)
    private String observacionCategoria;

    @Column(name = "observacion_normalizacion_metodo", length = 30)
    private String observacionNormalizacionMetodo;

    @Column(name = "observacion_normalizacion_confianza", precision = 5, scale = 4)
    private BigDecimal observacionNormalizacionConfianza;

    // ===== FECHAS / PERÍODO =====
    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "periodo_emision", length = 7) // MM/YYYY
    private String periodoEmision;

    @Column(name = "periodo_mes")
    private Integer periodoMes;

    @Column(name = "periodo_anio")
    private Integer periodoAnio;

    // ===== MONTOS =====
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

    @Column(name = "base_imponible", precision = 18, scale = 2)
    private BigDecimal baseImponible = BigDecimal.ZERO;

    @Column(name = "total_comprobante", precision = 18, scale = 2)
    private BigDecimal totalComprobante = BigDecimal.ZERO;

    @Column(name = "monto_compra", precision = 18, scale = 2)
    private BigDecimal montoCompra = BigDecimal.ZERO;

    @Column(name = "monto_iva", precision = 18, scale = 2)
    private BigDecimal montoIva = BigDecimal.ZERO;

    @Column(name = "monto_pagado", precision = 18, scale = 2)
    private BigDecimal montoPagado = BigDecimal.ZERO;

    @Column(name = "gastos", precision = 18, scale = 2)
    private BigDecimal gastos = BigDecimal.ZERO;

    // ===== AFECTACIONES =====
    @Column(name = "afectacion_exento", length = 150)
    private String afectacionExento;

    @Column(name = "afectacion_grav10", length = 150)
    private String afectacionGrav10;

    @Column(name = "afectacion_grav5", length = 150)
    private String afectacionGrav5;

    // ===== DETALLES =====
    @OneToMany(
            mappedBy = "cabecera",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DNITCompraDetalle> detalles = new ArrayList<>();

    public DNITCompraCabecera() {
    }

    // ===== GETTERS / SETTERS =====

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

    public FuenteCompra getFuente() {
        return fuente;
    }

    public void setFuente(FuenteCompra fuente) {
        this.fuente = fuente;
    }

    public String getProveedorRuc() {
        return proveedorRuc;
    }

    public void setProveedorRuc(String proveedorRuc) {
        this.proveedorRuc = proveedorRuc;
    }

    public String getProveedorNombre() {
        return proveedorNombre;
    }

    public void setProveedorNombre(String proveedorNombre) {
        this.proveedorNombre = proveedorNombre;
    }

    public String getNroComprobante() {
        return nroComprobante;
    }

    public void setNroComprobante(String nroComprobante) {
        this.nroComprobante = nroComprobante;
    }

    public String getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(String tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public String getCondicionOperacion() {
        return condicionOperacion;
    }

    public void setCondicionOperacion(String condicionOperacion) {
        this.condicionOperacion = condicionOperacion;
    }

    public String getTimbrado() {
        return timbrado;
    }

    public void setTimbrado(String timbrado) {
        this.timbrado = timbrado;
    }

    public String getRucTitular() {
        return rucTitular;
    }

    public void setRucTitular(String rucTitular) {
        this.rucTitular = rucTitular;
    }

    public String getRubroGastos() {
        return rubroGastos;
    }

    public void setRubroGastos(String rubroGastos) {
        this.rubroGastos = rubroGastos;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getObservacionNormalizada() {
        return observacionNormalizada;
    }

    public void setObservacionNormalizada(String observacionNormalizada) {
        this.observacionNormalizada = observacionNormalizada;
    }

    public String getObservacionCategoria() {
        return observacionCategoria;
    }

    public void setObservacionCategoria(String observacionCategoria) {
        this.observacionCategoria = observacionCategoria;
    }

    public String getObservacionNormalizacionMetodo() {
        return observacionNormalizacionMetodo;
    }

    public void setObservacionNormalizacionMetodo(String observacionNormalizacionMetodo) {
        this.observacionNormalizacionMetodo = observacionNormalizacionMetodo;
    }

    public BigDecimal getObservacionNormalizacionConfianza() {
        return observacionNormalizacionConfianza;
    }

    public void setObservacionNormalizacionConfianza(BigDecimal observacionNormalizacionConfianza) {
        this.observacionNormalizacionConfianza = observacionNormalizacionConfianza;
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

    public BigDecimal getBaseImponible() {
        return baseImponible;
    }

    public void setBaseImponible(BigDecimal baseImponible) {
        this.baseImponible = baseImponible;
    }

    public BigDecimal getTotalComprobante() {
        return totalComprobante;
    }

    public void setTotalComprobante(BigDecimal totalComprobante) {
        this.totalComprobante = totalComprobante;
    }

    public BigDecimal getMontoCompra() {
        return montoCompra;
    }

    public void setMontoCompra(BigDecimal montoCompra) {
        this.montoCompra = montoCompra;
    }

    public BigDecimal getMontoIva() {
        return montoIva;
    }

    public void setMontoIva(BigDecimal montoIva) {
        this.montoIva = montoIva;
    }

    public BigDecimal getMontoPagado() {
        return montoPagado;
    }

    public void setMontoPagado(BigDecimal montoPagado) {
        this.montoPagado = montoPagado;
    }

    public BigDecimal getGastos() {
        return gastos;
    }

    public void setGastos(BigDecimal gastos) {
        this.gastos = gastos;
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

    public List<DNITCompraDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DNITCompraDetalle> detalles) {
        this.detalles = detalles;
    }

    // ===== MÉTODO CLAVE PARA IMPORT =====
    public void clearAndAddDetalles(List<DNITCompraDetalle> nuevos) {
        this.detalles.clear(); // orphanRemoval = true → borra en BD

        if (nuevos != null) {
            for (DNITCompraDetalle d : nuevos) {
                d.setCabecera(this);
                this.detalles.add(d);
            }
        }
    }



}
