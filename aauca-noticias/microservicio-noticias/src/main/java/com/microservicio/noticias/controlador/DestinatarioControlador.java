package com.microservicio.noticias.controlador;

import com.microservicio.noticias.entidades.NoticiaDestinatario;
import com.microservicio.noticias.entidades.TipoDestinatario;
import com.microservicio.noticias.seguridad.JwtUtil;
import com.microservicio.noticias.servicios.DestinatarioServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/destinatario")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class DestinatarioControlador {

    private final DestinatarioServicio destinatarioServicio;
    private final JwtUtil              jwtUtil;

    // ─── Tipos disponibles ────────────────────────────────────────────────────

    @GetMapping("/tipos")
    public ResponseEntity<List<TipoDestinatario>> listarTipos() {
        return ResponseEntity.ok(destinatarioServicio.listarTipos());
    }

    // ─── Obtener destinatarios de una noticia ─────────────────────────────────

    @GetMapping("/noticia/{noticiaId}")
    public ResponseEntity<List<NoticiaDestinatario>> getByNoticia(
            @PathVariable Long noticiaId) {
        return ResponseEntity.ok(
                destinatarioServicio.obtenerPorNoticia(noticiaId));
    }

    // ─── Guardar destinatarios ────────────────────────────────────────────────

    @PostMapping("/noticia/{noticiaId}")
    public ResponseEntity<?> setDestinatarios(
            @PathVariable Long noticiaId,
            @RequestBody List<NoticiaDestinatario> destinatarios,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (!tokenValido(authHeader)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token requerido o inválido"));
            }
            String rol = jwtUtil.extraerRol(authHeader.substring(7));
            if (!rol.equalsIgnoreCase("Director") && !rol.equalsIgnoreCase("Celador")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Sin permisos"));
            }
            List<NoticiaDestinatario> saved =
                    destinatarioServicio.guardarDestinatarios(noticiaId, destinatarios);
            return ResponseEntity.ok(Map.of(
                    "message", "Destinatarios guardados correctamente",
                    "destinatarios", saved
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    // ─── Eliminar destinatarios ───────────────────────────────────────────────

    @DeleteMapping("/noticia/{noticiaId}")
    public ResponseEntity<?> deleteByNoticia(
            @PathVariable Long noticiaId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (!tokenValido(authHeader)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token requerido o inválido"));
            }
            destinatarioServicio.eliminarPorNoticia(noticiaId);
            return ResponseEntity.ok(Map.of("message", "Destinatarios eliminados"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private boolean tokenValido(String authHeader) {
        return authHeader != null
                && authHeader.startsWith("Bearer ")
                && jwtUtil.esValido(authHeader.substring(7));
    }
    // ─── CRUD Tipos ───────────────────────────────────────────────────────────────

    @PostMapping("/tipos")
    public ResponseEntity<?> crearTipo(
            @RequestBody TipoDestinatario tipo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (!tokenValido(authHeader)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Token requerido"));
            String rol = jwtUtil.extraerRol(authHeader.substring(7));
            if (!rol.equalsIgnoreCase("Director")) return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Solo el Director puede gestionar tipos"));
            TipoDestinatario saved = destinatarioServicio.crearTipo(tipo);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/tipos/{id}")
    public ResponseEntity<?> actualizarTipo(
            @PathVariable Long id,
            @RequestBody TipoDestinatario tipo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (!tokenValido(authHeader)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Token requerido"));
            String rol = jwtUtil.extraerRol(authHeader.substring(7));
            if (!rol.equalsIgnoreCase("Director")) return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Solo el Director puede gestionar tipos"));
            TipoDestinatario updated = destinatarioServicio.actualizarTipo(id, tipo);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/tipos/{id}/toggle")
    public ResponseEntity<?> toggleTipo(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (!tokenValido(authHeader)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Token requerido"));
            TipoDestinatario updated = destinatarioServicio.toggleTipo(id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/tipos/todos")
    public ResponseEntity<List<TipoDestinatario>> listarTodosTipos(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!tokenValido(authHeader)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(destinatarioServicio.listarTodos());
    }
}