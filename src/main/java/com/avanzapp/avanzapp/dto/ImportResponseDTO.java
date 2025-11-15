package com.avanzapp.avanzapp.dto;

public class ImportResponseDTO {

    private final String status;
    private final String mensaje;
    private final String resumen;
    private final Integer periodoMes;
    private final Integer periodoAnio;
    private final Integer insertadas;
    private final Integer actualizadas;
    private final Integer ignoradas;
    private final Integer leidas;
    private final Integer registrosProcesados;

    private ImportResponseDTO(Builder builder) {
        this.status = builder.status;
        this.mensaje = builder.mensaje;
        this.resumen = builder.resumen;
        this.periodoMes = builder.periodoMes;
        this.periodoAnio = builder.periodoAnio;
        this.insertadas = builder.insertadas;
        this.actualizadas = builder.actualizadas;
        this.ignoradas = builder.ignoradas;
        this.leidas = builder.leidas;
        this.registrosProcesados = builder.registrosProcesados;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder fromResult(String mensaje, ImportResultDTO result) {
        return builder()
                .status("OK")
                .mensaje(mensaje)
                .resumen(result.resumen())
                .periodoMes(result.getPeriodoMes())
                .periodoAnio(result.getPeriodoAnio())
                .insertadas(result.getInsertadas())
                .actualizadas(result.getActualizadas())
                .ignoradas(result.getIgnoradas())
                .leidas(result.getTotalLeidas())
                .registrosProcesados(result.getInsertadas() + result.getActualizadas());
    }

    public static Builder error(String mensaje) {
        return builder()
                .status("ERROR")
                .mensaje(mensaje);
    }

    public String getStatus() {
        return status;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getResumen() {
        return resumen;
    }

    public Integer getPeriodoMes() {
        return periodoMes;
    }

    public Integer getPeriodoAnio() {
        return periodoAnio;
    }

    public Integer getInsertadas() {
        return insertadas;
    }

    public Integer getActualizadas() {
        return actualizadas;
    }

    public Integer getIgnoradas() {
        return ignoradas;
    }

    public Integer getLeidas() {
        return leidas;
    }

    public Integer getRegistrosProcesados() {
        return registrosProcesados;
    }

    public static class Builder {
        private String status;
        private String mensaje;
        private String resumen;
        private Integer periodoMes;
        private Integer periodoAnio;
        private Integer insertadas;
        private Integer actualizadas;
        private Integer ignoradas;
        private Integer leidas;
        private Integer registrosProcesados;

        private Builder() {}

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder mensaje(String mensaje) {
            this.mensaje = mensaje;
            return this;
        }

        public Builder resumen(String resumen) {
            this.resumen = resumen;
            return this;
        }

        public Builder periodoMes(Integer periodoMes) {
            this.periodoMes = periodoMes;
            return this;
        }

        public Builder periodoAnio(Integer periodoAnio) {
            this.periodoAnio = periodoAnio;
            return this;
        }

        public Builder insertadas(Integer insertadas) {
            this.insertadas = insertadas;
            return this;
        }

        public Builder actualizadas(Integer actualizadas) {
            this.actualizadas = actualizadas;
            return this;
        }

        public Builder ignoradas(Integer ignoradas) {
            this.ignoradas = ignoradas;
            return this;
        }

        public Builder leidas(Integer leidas) {
            this.leidas = leidas;
            return this;
        }

        public Builder registrosProcesados(Integer registrosProcesados) {
            this.registrosProcesados = registrosProcesados;
            return this;
        }

        public ImportResponseDTO build() {
            return new ImportResponseDTO(this);
        }
    }
}
