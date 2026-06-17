package com.microservicio.users.controlador;

import com.microservicio.users.entidades.Solicitud;
import com.microservicio.users.servicios.SolicitudServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudControlador {

    private final SolicitudServicio solicitudServicio;

    @Value("${app.upload.dir:uploads/fotos/}")
    private String uploadDir;

    // ── Crear solicitud sin foto (JSON) — mantiene compatibilidad ─────────────
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Solicitud solicitud) {
        try {
            return ResponseEntity.ok(solicitudServicio.crearSolicitud(solicitud));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ── Crear solicitud CON foto (multipart/form-data) ────────────────────────
    @PostMapping(value = "/con-foto", consumes = "multipart/form-data")
    public ResponseEntity<?> crearConFoto(
            @RequestParam("nombre")                               String nombre,
            @RequestParam("email")                                String email,
            @RequestParam(value = "apellidos",  required = false) String apellidos,
            @RequestParam(value = "genero",     required = false) String genero,
            @RequestParam(value = "facultad",   required = false) String facultad,
            @RequestParam(value = "seleccion",  required = false) String seleccion,
            @RequestParam(value = "edificio",   required = false) String edificio,
            @RequestParam(value = "habitacion", required = false) String habitacion,
            @RequestParam(value = "mensaje",    required = false) String mensaje,
            @RequestParam(value = "foto",       required = false) MultipartFile foto) {
        try {
            Solicitud solicitud = new Solicitud();
            solicitud.setNombre(nombre);
            solicitud.setEmail(email);
            if (apellidos  != null && !apellidos.isBlank())  solicitud.setApellidos(apellidos);
            if (genero     != null && !genero.isBlank())     solicitud.setGenero(genero);
            if (facultad   != null && !facultad.isBlank())   solicitud.setFacultad(facultad);
            if (seleccion  != null && !seleccion.isBlank())  solicitud.setSeleccion(seleccion);
            if (edificio   != null && !edificio.isBlank())   solicitud.setEdificio(edificio);
            if (habitacion != null && !habitacion.isBlank()) solicitud.setHabitacion(habitacion);
            if (mensaje    != null && !mensaje.isBlank())    solicitud.setMensaje(mensaje);

            if (foto != null && !foto.isEmpty()) {
                String fotoUrl = guardarFoto(foto, email);
                solicitud.setFoto(fotoUrl);
            }

            return ResponseEntity.ok(solicitudServicio.crearSolicitud(solicitud));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Error al guardar la foto: " + e.getMessage()));
        }
    }

    // ── Servir foto de solicitud (sin auth) ───────────────────────────────────
    @GetMapping("/foto/{filename:.+}")
    public ResponseEntity<Resource> servirFoto(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable())
                return ResponseEntity.notFound().build();
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";
            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Listar todas ──────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<List<Solicitud>> obtenerTodas() {
        return ResponseEntity.ok(solicitudServicio.obtenerTodas());
    }

    // ── Listar pendientes ─────────────────────────────────────────────────────
    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'CELADOR')")
    public ResponseEntity<List<Solicitud>> obtenerPendientes() {
        return ResponseEntity.ok(solicitudServicio.obtenerPendientes());
    }

    // ── Aprobar ───────────────────────────────────────────────────────────────
    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'CELADOR')")
    public ResponseEntity<?> aprobar(@PathVariable Long id) {
        try {
            solicitudServicio.aprobar(id);
            return ResponseEntity.ok(
                    Map.of("message", "Solicitud aprobada — credenciales enviadas por email"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ── Rechazar ──────────────────────────────────────────────────────────────
    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'CELADOR')")
    public ResponseEntity<?> rechazar(@PathVariable Long id) {
        try {
            solicitudServicio.rechazar(id);
            return ResponseEntity.ok(
                    Map.of("message", "Solicitud rechazada — notificación enviada"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ── NUEVO: Disponibilidad de habitaciones por edificio ────────────────────
    // Devuelve cada habitación del edificio con su número de solicitudes pendientes.
    // El frontend usa esto para pintar en verde/gris el selector visual.
    // Endpoint público — no requiere token (el formulario es para no registrados).
    @GetMapping("/disponibilidad/{edificioId}")
    public ResponseEntity<?> disponibilidadPorEdificio(@PathVariable Long edificioId) {
        try {
            return ResponseEntity.ok(
                    solicitudServicio.disponibilidadPorEdificio(edificioId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ── NUEVO: Comprobar si un email ya tiene solicitud pendiente ─────────────
    // El frontend lo llama cuando el usuario sale del campo email (blur).
    // Responde si tiene solicitud activa para mostrar aviso de rectificación.
    // Endpoint público — el solicitante aún no tiene cuenta.
    @GetMapping("/estado-email")
    public ResponseEntity<?> estadoEmail(@RequestParam String email) {
        try {
            return ResponseEntity.ok(
                    solicitudServicio.estadoEmail(email.trim().toLowerCase()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ── Auxiliar: guardar foto ────────────────────────────────────────────────
    private String guardarFoto(MultipartFile file, String email) throws IOException {
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/"))
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        if (file.getSize() > 3 * 1024 * 1024)
            throw new IllegalArgumentException("La foto no debe superar 3 MB");

        String orig     = file.getOriginalFilename();
        String ext      = (orig != null && orig.contains("."))
                ? orig.substring(orig.lastIndexOf(".")) : ".jpg";
        String fileName = "solicitud_"
                + email.replaceAll("[^a-zA-Z0-9]", "_") + "_"
                + UUID.randomUUID().toString().substring(0, 8) + ext;

        Path path = Paths.get(uploadDir);
        if (!Files.exists(path)) Files.createDirectories(path);
        Files.copy(file.getInputStream(), path.resolve(fileName),
                StandardCopyOption.REPLACE_EXISTING);

        return "/api/solicitudes/foto/" + fileName;
    }
}