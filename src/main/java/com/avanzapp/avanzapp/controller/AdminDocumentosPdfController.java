package com.avanzapp.avanzapp.controller;

import com.avanzapp.avanzapp.dto.DocumentoClientePdfDTO;
import com.avanzapp.avanzapp.model.TipoDocumentoPdf;
import com.avanzapp.avanzapp.service.DocumentoClientePdfService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/admin")
public class AdminDocumentosPdfController {

    private final DocumentoClientePdfService service;

    public AdminDocumentosPdfController(DocumentoClientePdfService service) {
        this.service = service;
    }

    @PostMapping(value = "/clientes/{usuarioId}/pdfs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // @PreAuthorize("hasRole('ADMIN')") // si tenés Spring Security con roles
    public ResponseEntity<DocumentoClientePdfDTO> subirPdf(
            @PathVariable Long usuarioId,
            @RequestParam TipoDocumentoPdf tipo,
            @RequestParam String periodo, // "2025-12"
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        YearMonth ym = YearMonth.parse(periodo); // valida formato YYYY-MM
        var doc = service.subirPdf(usuarioId, tipo, ym, file);

        var dto = new DocumentoClientePdfDTO(
                doc.getId(),
                doc.getUsuario().getId(),
                doc.getTipo(),
                doc.getPeriodo(),
                doc.getNombreOriginal(),
                doc.getSizeBytes(),
                doc.getCreadoEn()
        );

        return ResponseEntity.ok(dto);
    }
}
