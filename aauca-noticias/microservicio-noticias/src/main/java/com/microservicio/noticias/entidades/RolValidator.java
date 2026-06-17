package com.microservicio.noticias.seguridad;

import com.microservicio.noticias.entidades.Noticia;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RolValidator {

    // ─── Fuente única de verdad — coincide exactamente con la BD ─────────────
    private static final Set<String> ROLES_DIRECTOR = Set.of("Director", "director", "DIRECTOR");
    private static final Set<String> ROLES_CELADOR  = Set.of("Celador",  "celador",  "CELADOR");

    // ─── API pública ──────────────────────────────────────────────────────────

    public void validarCrud(String rol) {
        if (!esDirector(rol) && !esCelador(rol)) {
            throw new IllegalArgumentException(
                    "No tienes permisos para gestionar noticias.");
        }
    }

    public void validarPublicacion(String rol, Noticia.Prioridad prioridad) {
        if (esDirector(rol)) return;

        if (esCelador(rol) && prioridad == Noticia.Prioridad.URGENTE) return;

        throw new IllegalArgumentException(
                "Esta noticia requiere autorización del director para publicarse.");
    }

    public void validarAutorizacion(String rol, String rolCreador) {
        if (!esDirector(rol)) {
            throw new IllegalArgumentException(
                    "Solo el director puede autorizar noticias.");
        }
        if (rolCreador == null || !esCelador(rolCreador)) {
            throw new IllegalArgumentException(
                    "Solo se pueden autorizar noticias creadas por un celador.");
        }
    }

    // ─── Helpers — tolerantes a mayúsculas/minúsculas ─────────────────────

    public boolean esDirector(String rol) {
        return rol != null && ROLES_DIRECTOR.contains(normalizar(rol));
    }

    public boolean esCelador(String rol) {
        return rol != null && ROLES_CELADOR.contains(normalizar(rol));
    }

    // ─── Normalización central — un solo lugar para cambiar ──────────────────

    private String normalizar(String rol) {
        if (rol == null) return "";
        // Capitaliza primera letra: "director" → "Director"
        String t = rol.trim();
        return t.isEmpty() ? "" : Character.toUpperCase(t.charAt(0)) + t.substring(1).toLowerCase();
    }
}