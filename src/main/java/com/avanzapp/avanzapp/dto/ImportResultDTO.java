package com.avanzapp.avanzapp.dto;

import java.util.ArrayList;
import java.util.List;

public class ImportResultDTO {

    private int totalLeidas;
    private int insertadas;
    private int actualizadas;
    private int ignoradas;
    private int periodoMes;
    private int periodoAnio;
    private String fuente;
    private List<String> periodos = new ArrayList<>();

    public ImportResultDTO() {}

    public ImportResultDTO(int totalLeidas,
                           int insertadas,
                           int actualizadas,
                           int ignoradas,
                           int periodoMes,
                           int periodoAnio) {
        this(totalLeidas, insertadas, actualizadas, ignoradas, periodoMes, periodoAnio, "IVA",
                periodoMes > 0 && periodoAnio > 0
                        ? List.of(String.format("%02d/%d", periodoMes, periodoAnio))
                        : List.of());
    }

    public ImportResultDTO(int totalLeidas,
                           int insertadas,
                           int actualizadas,
                           int ignoradas,
                           int periodoMes,
                           int periodoAnio,
                           String fuente,
                           List<String> periodos) {
        this.totalLeidas = totalLeidas;
        this.insertadas = insertadas;
        this.actualizadas = actualizadas;
        this.ignoradas = ignoradas;
        this.periodoMes = periodoMes;
        this.periodoAnio = periodoAnio;
        this.fuente = fuente;
        this.periodos = periodos == null ? new ArrayList<>() : new ArrayList<>(periodos);
    }

    public String resumen() {
        String periodoTexto;
        if (periodos != null && !periodos.isEmpty()) {
            periodoTexto = String.join(", ", periodos);
        } else if (periodoMes > 0 && periodoAnio > 0) {
            periodoTexto = String.format("%02d/%d", periodoMes, periodoAnio);
        } else {
            periodoTexto = "sin periodo";
        }

        return String.format(
                "%s • %s → Leídas:%d, Insertadas:%d, Actualizadas:%d, Ignoradas:%d",
                fuente == null ? "COMPRAS" : fuente,
                periodoTexto,
                totalLeidas,
                insertadas,
                actualizadas,
                ignoradas
        );
    }

    public int getTotalLeidas() { return totalLeidas; }
    public void setTotalLeidas(int totalLeidas) { this.totalLeidas = totalLeidas; }

    public int getInsertadas() { return insertadas; }
    public void setInsertadas(int insertadas) { this.insertadas = insertadas; }

    public int getActualizadas() { return actualizadas; }
    public void setActualizadas(int actualizadas) { this.actualizadas = actualizadas; }

    public int getIgnoradas() { return ignoradas; }
    public void setIgnoradas(int ignoradas) { this.ignoradas = ignoradas; }

    public int getPeriodoMes() { return periodoMes; }
    public void setPeriodoMes(int periodoMes) { this.periodoMes = periodoMes; }

    public int getPeriodoAnio() { return periodoAnio; }
    public void setPeriodoAnio(int periodoAnio) { this.periodoAnio = periodoAnio; }

    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }

    public List<String> getPeriodos() { return periodos; }
    public void setPeriodos(List<String> periodos) {
        this.periodos = periodos == null ? new ArrayList<>() : new ArrayList<>(periodos);
    }
}
