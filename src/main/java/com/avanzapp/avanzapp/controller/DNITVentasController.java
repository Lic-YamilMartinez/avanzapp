package com.avanzapp.avanzapp.controller;

import com.avanzapp.avanzapp.dto.DNITVentasCabeceraDTO;
import com.avanzapp.avanzapp.dto.ImportResultDTO;
import com.avanzapp.avanzapp.dto.ImportResponseDTO;
import com.avanzapp.avanzapp.mapper.DNITVentasMapper;
import com.avanzapp.avanzapp.model.DNITVentasCabecera;
import com.avanzapp.avanzapp.repository.DNITVentasCabeceraRepository;
import com.avanzapp.avanzapp.service.DNITVentasImportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/ventas")
@CrossOrigin(origins = "*")
public class DNITVentasController {

    private final DNITVentasCabeceraRepository cabeceraRepository;
    private final DNITVentasImportService importService;

    public DNITVentasController(DNITVentasCabeceraRepository cabeceraRepository,
                                DNITVentasImportService importService) {
        this.cabeceraRepository = cabeceraRepository;
        this.importService = importService;
    }

    @GetMapping(value = "/{usuarioId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DNITVentasCabeceraDTO>> listar(@PathVariable Long usuarioId) {
        List<DNITVentasCabecera> cabeceras = cabeceraRepository.findByUsuarioId(usuarioId);
        return ResponseEntity.ok(cabeceras.stream().map(DNITVentasMapper::toDTO).toList());
    }

    @GetMapping(value = "/{usuarioId}/{periodo}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DNITVentasCabeceraDTO>> listarPorPeriodo(@PathVariable Long usuarioId,
                                                                        @PathVariable String periodo) {
        YearMonth ym = parsePeriodo(periodo);
        List<DNITVentasCabecera> cabeceras = cabeceraRepository
                .findByUsuarioIdAndPeriodoMesAndPeriodoAnio(usuarioId, ym.getMonthValue(), ym.getYear());
        return ResponseEntity.ok(cabeceras.stream().map(DNITVentasMapper::toDTO).toList());
    }

    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> importar(@RequestParam("file") MultipartFile file,
                                      @RequestParam("usuarioId") Long usuarioId,
                                      @RequestParam(value = "overwrite", defaultValue = "false") boolean overwrite) {
        if (file == null || file.isEmpty()) {
            ImportResponseDTO error = ImportResponseDTO.error("El archivo de ventas no puede estar vacio.")
                    .registrosProcesados(0)
                    .build();
            return ResponseEntity.badRequest().body(error);
        }

        try {
            ImportResultDTO result = importService.importarVentas(file, usuarioId, overwrite);
            ImportResponseDTO okResponse = ImportResponseDTO
                    .fromResult("Importacion de ventas exitosa", result)
                    .build();
            return ResponseEntity.ok(okResponse);
        } catch (Exception e) {
            ImportResponseDTO errorResponse = ImportResponseDTO.error("Error procesando el archivo de ventas: " + e.getMessage())
                    .registrosProcesados(0)
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private YearMonth parsePeriodo(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Periodo invalido");
        }
        String normalized = raw.replace("_", "-").replace(".", "-");
        String[] tokens = normalized.split("[-/]");
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
