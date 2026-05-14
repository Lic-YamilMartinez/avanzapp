package com.avanzapp.avanzapp.controller;

import com.avanzapp.avanzapp.dto.DNITCompraCabeceraDTO;
import com.avanzapp.avanzapp.dto.ImportResultDTO;
import com.avanzapp.avanzapp.mapper.DNITCompraMapper;
import com.avanzapp.avanzapp.model.DNITCompraCabecera;
import com.avanzapp.avanzapp.repository.DNITCompraCabeceraRepository;
import com.avanzapp.avanzapp.service.DNITComprasImportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dnit")
@CrossOrigin(origins = "*")
public class DNITComprasController {

    private final DNITCompraCabeceraRepository cabeceraRepo;
    private final DNITComprasImportService importService;

    public DNITComprasController(DNITCompraCabeceraRepository cabeceraRepo,
                                 DNITComprasImportService importService) {
        this.cabeceraRepo = cabeceraRepo;
        this.importService = importService;
    }

    @GetMapping(value = "/{usuarioId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DNITCompraCabeceraDTO>> listar(@PathVariable Long usuarioId) {
        List<DNITCompraCabecera> list = cabeceraRepo.findByUsuarioId(usuarioId);
        return ResponseEntity.ok(list.stream().map(DNITCompraMapper::toDTO).toList());
    }

    @GetMapping(value = "/{usuarioId}/{periodo}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DNITCompraCabeceraDTO>> listarPorPeriodo(
            @PathVariable Long usuarioId,
            @PathVariable String periodo) {
        List<DNITCompraCabecera> list = cabeceraRepo.findByUsuarioIdAndPeriodoEmision(usuarioId, periodo);
        return ResponseEntity.ok(list.stream().map(DNITCompraMapper::toDTO).toList());
    }

    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> importar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("usuarioId") Long usuarioId,
            @RequestParam(value = "overwrite", required = false, defaultValue = "false") boolean overwrite
    ) {
        Map<String, Object> body = new HashMap<>();
        if (file.isEmpty()) {
            body.put("status", "ERROR");
            body.put("mensaje", "El archivo está vacío.");
            body.put("registrosProcesados", 0);
            return ResponseEntity.badRequest().body(body);
        }
        try {
            ImportResultDTO rs = importService.importarCompras(file, usuarioId, overwrite);
            body.put("status", "OK");
            body.put("mensaje", "Importación exitosa");
            body.put("resumen", rs.resumen());
            body.put("periodoMes", rs.getPeriodoMes());
            body.put("periodoAnio", rs.getPeriodoAnio());
            body.put("insertadas", rs.getInsertadas());
            body.put("actualizadas", rs.getActualizadas());
            body.put("ignoradas", rs.getIgnoradas());
            body.put("leidas", rs.getTotalLeidas());
            body.put("registrosProcesados", rs.getInsertadas() + rs.getActualizadas());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            e.printStackTrace();
            body.put("status", "ERROR");
            body.put("mensaje", "Error al procesar el archivo: " + e.getMessage());
            body.put("registrosProcesados", 0);
            return ResponseEntity.internalServerError().body(body);
        }
    }
}
