package com.avanzapp.avanzapp.controller;

import com.avanzapp.avanzapp.model.Usuario;
import com.avanzapp.avanzapp.service.DocumentoClientePdfService;
import com.avanzapp.avanzapp.service.UsuarioService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/pdfs")
@CrossOrigin(origins = "*")
public class DocumentosPdfDownloadController {

    private final DocumentoClientePdfService service;
    private final UsuarioService usuarioService;

    public DocumentosPdfDownloadController(DocumentoClientePdfService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
    }

    /* =========================================================
       LISTAR MIS PDFS
       GET /api/pdfs
       - CLIENT: solo los suyos
       - ADMIN: (opcional) podrías listar todos (pero ahora mantenemos "mis")
    ========================================================= */
    @GetMapping
    public ResponseEntity<?> listarMisPdfs(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "UNAUTHORIZED",
                    "message", "No autenticado"
            ));
        }

        String cedula = userDetails.getUsername();
        Usuario usuario = usuarioService.obtenerPorCedula(cedula);

        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "UNAUTHORIZED",
                    "message", "Usuario no encontrado"
            ));
        }

        return ResponseEntity.ok(service.listarPorUsuario(usuario.getId()));
    }

    /* =========================================================
       DESCARGA PRIVADA (JWT)
       GET /api/pdfs/{id}/download
       - CLIENT solo lo suyo
       - ADMIN todo
    ========================================================= */
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadPrivado(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) throws Exception {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "UNAUTHORIZED",
                    "message", "No autenticado"
            ));
        }

        String cedula = userDetails.getUsername();
        Usuario usuario = usuarioService.obtenerPorCedula(cedula);

        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "UNAUTHORIZED",
                    "message", "Usuario no encontrado"
            ));
        }

        var doc = service.obtener(id);

        // CLIENT: solo lo suyo
        if ("CLIENT".equalsIgnoreCase(usuario.getRol())) {
            if (doc.getUsuario() == null || doc.getUsuario().getId() == null ||
                    !doc.getUsuario().getId().equals(usuario.getId())) {
                return ResponseEntity.status(403).body(Map.of(
                        "error", "FORBIDDEN",
                        "message", "No tenés permisos para este recurso"
                ));
            }
        }

        Path path = service.pathDelDocumento(id);
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists()) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "NOT_FOUND",
                    "message", "Archivo no encontrado"
            ));
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("ngrok-skip-browser-warning", "1")
                // inline: ideal para abrir en navegador/visor del celu
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + doc.getNombreOriginal() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(resource);
    }

    /* =========================================================
       GENERAR LINK TEMPORAL (JWT)
       GET /api/pdfs/{id}/link
       Responde: { token, path }
       Nota: el front hará BASE_URL + path
    ========================================================= */
    @GetMapping("/{id}/link")
    public ResponseEntity<?> linkTemporal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "UNAUTHORIZED",
                    "message", "No autenticado"
            ));
        }

        String cedula = userDetails.getUsername();
        Usuario usuario = usuarioService.obtenerPorCedula(cedula);

        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "UNAUTHORIZED",
                    "message", "Usuario no encontrado"
            ));
        }

        var doc = service.obtener(id);

        // CLIENT: solo lo suyo
        if ("CLIENT".equalsIgnoreCase(usuario.getRol())) {
            if (doc.getUsuario() == null || doc.getUsuario().getId() == null ||
                    !doc.getUsuario().getId().equals(usuario.getId())) {
                return ResponseEntity.status(403).body(Map.of(
                        "error", "FORBIDDEN",
                        "message", "No tenés permisos para este recurso"
                ));
            }
        }

        // ⏱ 2 minutos (podés subir a 600 para pruebas)
        String token = service.crearLinkTemporal(id, 120);
        String path = "/api/pdfs/public/" + token;

        return ResponseEntity.ok(Map.of(
                "token", token,
                "path", path
        ));
    }

    /* =========================================================
       DESCARGA PÚBLICA (TOKEN TEMPORAL)
       GET /api/pdfs/public/{token}
       (sin JWT)
    ========================================================= */
    @GetMapping("/public/{token}")
    public ResponseEntity<?> downloadPublico(@PathVariable String token) throws Exception {

        Long docId = service.validarYObtenerDocId(token);

        if (docId == null) {
            // 403: token inválido/expirado => "no autorizado por token"
            return ResponseEntity.status(403).body(Map.of(
                    "error", "FORBIDDEN",
                    "message", "Token inválido o expirado"
            ));
        }

        var doc = service.obtener(docId);

        Path path = service.pathDelDocumento(docId);
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists()) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "NOT_FOUND",
                    "message", "Archivo no encontrado"
            ));
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("ngrok-skip-browser-warning", "1")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + doc.getNombreOriginal() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(resource);
    }
}
