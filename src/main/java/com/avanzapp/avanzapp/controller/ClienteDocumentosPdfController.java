package com.avanzapp.avanzapp.controller;

import com.avanzapp.avanzapp.dto.DocumentoClientePdfDTO;
import com.avanzapp.avanzapp.dto.UserDTO;
import com.avanzapp.avanzapp.service.DocumentoClientePdfService;
import com.avanzapp.avanzapp.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteDocumentosPdfController {

    private final DocumentoClientePdfService pdfService;
    private final UsuarioService usuarioService;

    public ClienteDocumentosPdfController(
            DocumentoClientePdfService pdfService,
            UsuarioService usuarioService
    ) {
        this.pdfService = pdfService;
        this.usuarioService = usuarioService;
    }

    /**
     * CLIENTE: listar MIS PDFs del mes
     * GET /api/clientes/me/pdfs?periodo=2025-12
     */
    @GetMapping("/me/pdfs")
    public ResponseEntity<List<DocumentoClientePdfDTO>> listarMisPdfs(
            @RequestParam String periodo,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        // username = cédula (según tu implementación)
        String cedula = userDetails.getUsername();

        UserDTO user = usuarioService.obtenerDTOporCedula(cedula);

        YearMonth ym = YearMonth.parse(periodo);

        var lista = pdfService.listarPorUsuarioYPeriodo(user.getId(), ym);

        var dtos = lista.stream()
                .map(d -> new DocumentoClientePdfDTO(
                        d.getId(),
                        d.getUsuario().getId(),
                        d.getTipo(),
                        d.getPeriodo(),
                        d.getNombreOriginal(),
                        d.getSizeBytes(),
                        d.getCreadoEn()
                ))
                .toList();

        return ResponseEntity.ok(dtos);
    }
}
