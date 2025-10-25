package com.avanzapp.avanzapp.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "dnit_compra_cabecera",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cabecera_usuario_periodo_nro",
                columnNames = {"usuario_id", "periodo_mes", "periodo_anio", "nro_comprobante"}
        )
)
public class DNITCompraCabecera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con el cliente (usuario)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Proveedor
    @Column(name = "proveedor_ruc", length = 25)
    private String proveedorRuc;

    @Column(name = "proveedor_nombre", length = 200)
    private String proveedorNombre;

    // Comprobante
    @Column(name = "nro_comprobante", length = 60)
    private String nroComprobante;

    @Column(name = "tipo_comprobante", length = 20)
    private String tipoComprobante;

    @Column(name = "condicion_operacion", length = 20)
    private String condicionOperacion;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "periodo_emision", length = 7)   // mm/yyyy
    private String periodoEmision;

    @Column(name = "periodo_mes")
    private Integer periodoMes;

    @Column(name = "periodo_anio")
    private Integer periodoAnio;

    // Totales originales del comprobante (como viene del Excel)
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

    @Column(name = "base_imponible", precision = 18, scale = 2)
    private BigDecimal baseImponible = BigDecimal.ZERO;

    // Clasificaciones “texto” del layout
    @Column(name = "afectacion_exento", length = 150)
    private String afectacionExento;

    @Column(name = "afectacion_grav10", length = 150)
    private String afectacionGrav10;

    @Column(name = "afectacion_grav5", length = 150)
    private String afectacionGrav5;

    // Detalles (particiones IVA10/IVA5/EXENTA)
    @OneToMany(
            mappedBy = "cabecera",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DNITCompraDetalle> detalles = new ArrayList<>();

    public DNITCompraCabecera() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getProveedorRuc() { return proveedorRuc; }
    public void setProveedorRuc(String proveedorRuc) { this.proveedorRuc = proveedorRuc; }

    public String getProveedorNombre() { return proveedorNombre; }
    public void setProveedorNombre(String proveedorNombre) { this.proveedorNombre = proveedorNombre; }

    public String getNroComprobante() { return nroComprobante; }
    public void setNroComprobante(String nroComprobante) { this.nroComprobante = nroComprobante; }

    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public String getCondicionOperacion() { return condicionOperacion; }
    public void setCondicionOperacion(String condicionOperacion) { this.condicionOperacion = condicionOperacion; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public String getPeriodoEmision() { return periodoEmision; }
    public void setPeriodoEmision(String periodoEmision) { this.periodoEmision = periodoEmision; }

    public Integer getPeriodoMes() { return periodoMes; }
    public void setPeriodoMes(Integer periodoMes) { this.periodoMes = periodoMes; }

    public Integer getPeriodoAnio() { return periodoAnio; }
    public void setPeriodoAnio(Integer periodoAnio) { this.periodoAnio = periodoAnio; }

    public BigDecimal getGravada10() { return gravada10; }
    public void setGravada10(BigDecimal gravada10) { this.gravada10 = gravada10; }

    public BigDecimal getGravada5() { return gravada5; }
    public void setGravada5(BigDecimal gravada5) { this.gravada5 = gravada5; }

    public BigDecimal getIva10() { return iva10; }
    public void setIva10(BigDecimal iva10) { this.iva10 = iva10; }

    public BigDecimal getIva5() { return iva5; }
    public void setIva5(BigDecimal iva5) { this.iva5 = iva5; }

    public BigDecimal getExenta() { return exenta; }
    public void setExenta(BigDecimal exenta) { this.exenta = exenta; }

    public BigDecimal getTotalComprobante() { return totalComprobante; }
    public void setTotalComprobante(BigDecimal totalComprobante) { this.totalComprobante = totalComprobante; }

    public BigDecimal getBaseImponible() { return baseImponible; }
    public void setBaseImponible(BigDecimal baseImponible) { this.baseImponible = baseImponible; }

    public String getAfectacionExento() { return afectacionExento; }
    public void setAfectacionExento(String afectacionExento) { this.afectacionExento = afectacionExento; }

    public String getAfectacionGrav10() { return afectacionGrav10; }
    public void setAfectacionGrav10(String afectacionGrav10) { this.afectacionGrav10 = afectacionGrav10; }

    public String getAfectacionGrav5() { return afectacionGrav5; }
    public void setAfectacionGrav5(String afectacionGrav5) { this.afectacionGrav5 = afectacionGrav5; }

    public List<DNITCompraDetalle> getDetalles() { return detalles; }
    public void setDetalles(List<DNITCompraDetalle> detalles) { this.detalles = detalles; }

    // helpers
    public void clearAndAddDetalles(List<DNITCompraDetalle> nuevos) {
        this.detalles.clear();
        for (DNITCompraDetalle d : nuevos) {
            d.setCabecera(this);
            this.detalles.add(d);
        }
    }
}
