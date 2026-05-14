package com.avanzapp.avanzapp.controller;

import com.avanzapp.avanzapp.dto.DNITReporteResponseDTO;
import com.avanzapp.avanzapp.service.ReportesDNITService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/reportes")
@CrossOrigin(origins = "*")
public class DNITReportesController {

    private final ReportesDNITService reportesDNITService;

    public DNITReportesController(ReportesDNITService reportesDNITService) {
        this.reportesDNITService = reportesDNITService;
    }

    /**
     * 🔹 Endpoint modo ejecutivo:
     * GET /reportes/dnit/{usuarioId}
     * GET /reportes/dnit/{usuarioId}?periodo=03-2025  (o 03/2025, 03_2025)
     */
    @GetMapping(
            value = "/dnit/{usuarioId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DNITReporteResponseDTO> obtenerReporteDnit(
            @PathVariable Long usuarioId,
            @RequestParam(value = "periodo", required = false) String periodo
    ) {

        YearMonth ym = (periodo != null && !periodo.isBlank())
                ? parsePeriodo(periodo)
                : null;

        DNITReporteResponseDTO body =
                reportesDNITService.obtenerReporteDnit(usuarioId, ym);

        return ResponseEntity.ok(body);
    }

    // Reutilizamos la misma lógica de parseo
    private YearMonth parsePeriodo(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Periodo invalido");
        }
        String normalized = raw.replace("_", "-").replace(".", "-").replace("/", "-");
        String[] tokens = normalized.split("-");
        if (tokens.length != 2) {
            throw new IllegalArgumentException("Formato de periodo invalido. Usar MM-YYYY");
        }
        try {
            int mes = Integer.parseInt(tokens[0]);
            int anio = Integer.parseInt(tokens[1]);
            return YearMonth.of(anio, mes);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Formato de periodo invalido", ex);
        }
    }
}
