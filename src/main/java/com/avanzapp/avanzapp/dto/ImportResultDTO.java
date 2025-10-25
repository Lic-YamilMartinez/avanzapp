package com.avanzapp.avanzapp.dto;

public class ImportResultDTO {

    private int totalLeidas;
    private int insertadas;
    private int actualizadas;
    private int ignoradas;
    private int periodoMes;
    private int periodoAnio;

    public ImportResultDTO() {}

    public ImportResultDTO(int totalLeidas, int insertadas, int actualizadas, int ignoradas, int periodoMes, int periodoAnio) {
        this.totalLeidas = totalLeidas;
        this.insertadas = insertadas;
        this.actualizadas = actualizadas;
        this.ignoradas = ignoradas;
        this.periodoMes = periodoMes;
        this.periodoAnio = periodoAnio;
    }

    public String resumen() {
        return String.format(
                "Periodo %02d/%d → Leídas:%d, Insertadas:%d, Actualizadas:%d, Ignoradas:%d",
                periodoMes, periodoAnio, totalLeidas, insertadas, actualizadas, ignoradas
        );
    }

    // Getters y Setters
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
}
