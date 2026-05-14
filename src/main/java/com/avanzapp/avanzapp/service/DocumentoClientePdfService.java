package com.avanzapp.avanzapp.service;

import com.avanzapp.avanzapp.model.DocumentoClientePdf;
import com.avanzapp.avanzapp.model.TipoDocumentoPdf;
import com.avanzapp.avanzapp.model.Usuario;
import com.avanzapp.avanzapp.repository.DocumentoClientePdfRepository;
import com.avanzapp.avanzapp.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DocumentoClientePdfService {

    @Value("${app.storage.pdf-dir}")
    private String pdfDir;

    private final DocumentoClientePdfRepository pdfRepo;
    private final UsuarioRepository usuarioRepo;

    public DocumentoClientePdfService(DocumentoClientePdfRepository pdfRepo, UsuarioRepository usuarioRepo) {
        this.pdfRepo = pdfRepo;
        this.usuarioRepo = usuarioRepo;
    }

    /* =========================================================
       LINKS TEMPORALES (PUBLIC)
       - Map en memoria (OK en DEV)
       - En PROD conviene token firmado o Redis/DB
    ========================================================= */
    private static class LinkInfo {
        Long docId;
        Instant expiresAt;
        LinkInfo(Long docId, Instant expiresAt) {
            this.docId = docId;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<String, LinkInfo> links = new ConcurrentHashMap<>();

    public String crearLinkTemporal(Long docId, int ttlSeconds) {
        String token = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        links.put(token, new LinkInfo(docId, expiresAt));
        return token;
    }

    public Long validarYObtenerDocId(String token) {
        LinkInfo info = links.get(token);
        if (info == null) return null;

        if (Instant.now().isAfter(info.expiresAt)) {
            links.remove(token);
            return null;
        }
        return info.docId;
    }

    /* =========================================================
       SUBIR / LISTAR / OBTENER PDF
    ========================================================= */
    public DocumentoClientePdf subirPdf(Long usuarioId, TipoDocumentoPdf tipo, YearMonth periodo, MultipartFile file)
            throws IOException {

        validarPdf(file);

        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no existe: " + usuarioId));

        if (usuario.getRol() == null || !usuario.getRol().equalsIgnoreCase("CLIENT")) {
            throw new IllegalArgumentException("El usuario no es CLIENT");
        }

        LocalDate periodoNormalizado = normalizarPeriodo(periodo);

        Optional<DocumentoClientePdf> existenteOpt =
                pdfRepo.findByUsuarioIdAndPeriodoAndTipo(usuarioId, periodoNormalizado, tipo);

        DocumentoClientePdf doc = existenteOpt.orElseGet(DocumentoClientePdf::new);

        Path baseDir = Paths.get(pdfDir);
        Files.createDirectories(baseDir);

        String original = safeOriginalName(file.getOriginalFilename());
        String stored = UUID.randomUUID() + "-" + original.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
        Path destino = baseDir.resolve(stored);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        }

        // borrar anterior si existía
        if (doc.getId() != null && doc.getRuta() != null) {
            borrarArchivoSilencioso(doc.getRuta());
        }

        doc.setUsuario(usuario);
        doc.setTipo(tipo);
        doc.setPeriodo(periodoNormalizado);
        doc.setNombreOriginal(original);
        doc.setNombreAlmacenado(stored);
        doc.setContentType("application/pdf");
        doc.setSizeBytes(file.getSize());
        doc.setRuta(destino.toAbsolutePath().toString());

        return pdfRepo.save(doc);
    }

    public List<DocumentoClientePdf> listarPorUsuario(Long usuarioId) {
        return pdfRepo.findByUsuarioId(usuarioId);
    }

    public List<DocumentoClientePdf> listarPorUsuarioYPeriodo(Long usuarioId, YearMonth periodo) {
        LocalDate p = normalizarPeriodo(periodo);
        return pdfRepo.findByUsuarioIdAndPeriodo(usuarioId, p);
    }

    public DocumentoClientePdf obtener(Long id) {
        return pdfRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Documento no existe: " + id));
    }

    public Path pathDelDocumento(Long id) {
        DocumentoClientePdf doc = obtener(id);
        return Paths.get(doc.getRuta());
    }

    /* =========================================================
       HELPERS
    ========================================================= */
    private LocalDate normalizarPeriodo(YearMonth ym) {
        return ym.atDay(1);
    }

    private void validarPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Archivo vacío");

        String ct = file.getContentType();
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();

        boolean parecePdf = "application/pdf".equalsIgnoreCase(ct) || name.endsWith(".pdf");
        if (!parecePdf) throw new IllegalArgumentException("Solo se permite PDF");
    }

    private String safeOriginalName(String original) {
        if (original == null || original.isBlank()) return "documento.pdf";
        String o = original.trim();
        if (!o.toLowerCase().endsWith(".pdf")) o += ".pdf";
        return o;
    }

    private void borrarArchivoSilencioso(String ruta) {
        try { Files.deleteIfExists(Paths.get(ruta)); } catch (Exception ignored) {}
    }
}
