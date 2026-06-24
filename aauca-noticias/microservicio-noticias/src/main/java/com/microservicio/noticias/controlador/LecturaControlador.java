package com.microservicio.noticias.controlador;

import com.microservicio.noticias.entidades.NoticiaLectura;
import com.microservicio.noticias.seguridad.JwtUtil;
import com.microservicio.noticias.servicios.LecturaServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/lectura")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "https://aauca-news.vercel.app"})
public class LecturaControlador {

    private final LecturaServicio lecturaServicio;
    private final JwtUtil         jwtUtil;

    // ── Registrar lectura ─────────────────────────────────────────────────────

    @PostMapping("/{noticiaId}")
    public ResponseEntity<?> registrar(
            @PathVariable                                             Long   noticiaId,
            @RequestParam(required = false)                           Short  duracion,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (!tokenValido(authHeader))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token requerido"));

            String token        = authHeader.substring(7);
            Long   usuarioId    = jwtUtil.extraerUsuarioId(token);
            String nombre       = jwtUtil.extraerNombre(token);
            String rol          = jwtUtil.extraerRol(token);

            lecturaServicio.registrarLectura(noticiaId, usuarioId, nombre, rol, duracion);

            return ResponseEntity.ok(Map.of(
                    "message",   "Lectura registrada",
                    "noticiaId", noticiaId,
                    "usuarioId", usuarioId
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ── ¿Ya leyó este usuario? ────────────────────────────────────────────────

    @GetMapping("/{noticiaId}/leida")
    public ResponseEntity<?> yaLeyo(
            @PathVariable                                             Long   noticiaId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (!tokenValido(authHeader))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token requerido"));

            Long usuarioId = jwtUtil.extraerUsuarioId(authHeader.substring(7));
            return ResponseEntity.ok(Map.of(
                    "leida",     lecturaServicio.yaLeyo(noticiaId, usuarioId),
                    "noticiaId", noticiaId,
                    "usuarioId", usuarioId
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ── Panel admin — tabla paginada de lectores ──────────────────────────────

    @GetMapping("/{noticiaId}/lectores")
    public ResponseEntity<Map<String, Object>> lectores(
            @PathVariable                      Long noticiaId,
            @RequestParam(defaultValue = "0")  int  pagina,
            @RequestParam(defaultValue = "20") int  tamano) {

        Page<NoticiaLectura> page = lecturaServicio.lecturasPorNoticia(noticiaId, pagina, tamano);

        return ResponseEntity.ok(Map.of(
                "noticiaId",    noticiaId,
                "totalLectores", page.getTotalElements(),
                "totalPaginas",  page.getTotalPages(),
                "pagina",        pagina,
                "lecturas",      page.getContent()
        ));
    }

    // ── Contador simple ───────────────────────────────────────────────────────

    @GetMapping("/{noticiaId}/total")
    public ResponseEntity<?> total(@PathVariable Long noticiaId) {
        return ResponseEntity.ok(Map.of(
                "noticiaId", noticiaId,
                "total",     lecturaServicio.totalLecturas(noticiaId)
        ));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private boolean tokenValido(String authHeader) {
        return authHeader != null
                && authHeader.startsWith("Bearer ")
                && jwtUtil.esValido(authHeader.substring(7));
    }
}