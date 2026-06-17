package com.microservicio.secciones.controlador;

import com.microservicio.secciones.entidades.Seccion;
import com.microservicio.secciones.servicios.SeccionServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/seccion")
@RequiredArgsConstructor
public class SeccionControlador {

    private final SeccionServicio seccionServicio;

    @PostMapping("/crear")
    public ResponseEntity<?> crearSeccion(@RequestBody Seccion seccion) {
        try {
            seccionServicio.crearSeccion(seccion);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Sección creada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al crear sección: " + e.getMessage()));
        }
    }

    @PutMapping("/actualizar/{id}")                                      // ← añadido
    public ResponseEntity<?> actualizarSeccion(@PathVariable Long id,
                                               @RequestBody Seccion seccion) {
        try {
            Seccion actualizada = seccionServicio.actualizarSeccion(id, seccion);
            return ResponseEntity.ok(actualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al actualizar sección: " + e.getMessage()));
        }
    }

    @GetMapping("/listar")
    public ResponseEntity<List<Seccion>> listarSecciones() {
        return ResponseEntity.ok(seccionServicio.listarSecciones());
    }

    @GetMapping("/listar/activas")
    public ResponseEntity<List<Seccion>> listarActivas() {
        return ResponseEntity.ok(seccionServicio.listarActivas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(seccionServicio.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<?> eliminarSeccion(@PathVariable Long id) {
        try {
            seccionServicio.eliminarSeccion(id);
            return ResponseEntity.ok(Map.of("message", "Sección eliminada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al eliminar sección: " + e.getMessage()));
        }
    }

    @PatchMapping("/desactivar/{id}")
    public ResponseEntity<?> desactivarSeccion(@PathVariable Long id) {
        try {
            seccionServicio.desactivarSeccion(id);
            return ResponseEntity.ok(Map.of("message", "Sección desactivada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al desactivar sección: " + e.getMessage()));
        }
    }
}