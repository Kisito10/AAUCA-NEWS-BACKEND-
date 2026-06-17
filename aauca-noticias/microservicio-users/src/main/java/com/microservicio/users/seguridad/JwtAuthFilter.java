package com.microservicio.users.seguridad;

import com.microservicio.users.entidades.Usuario;
import com.microservicio.users.repositorio.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtServicio      jwtServicio;
    private final UsuarioRepository usuarioRepository;  // ← repositorio directo, sin ciclo

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Sin token → continuar sin autenticar (endpoints públicos lo permiten)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);

            if (jwtServicio.esValido(token)) {
                String email = jwtServicio.extraerEmail(token);
                String rol   = jwtServicio.extraerRol(token);

                // ── Verificar que el usuario sigue activo en BD ──────────────
                // Consulta directa al repositorio — evita el ciclo con SecurityConfig.
                // Director y Celador nunca se bloquean por esta vía.
                Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

                if (usuario == null) {
                    log.warn("Token válido pero usuario no encontrado: {}", email);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                if (Boolean.FALSE.equals(usuario.getActivo())
                        && !"Director".equalsIgnoreCase(rol)
                        && !"Celador".equalsIgnoreCase(rol)) {
                    log.info("Acceso denegado — usuario desactivado: {}", email);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                // ─────────────────────────────────────────────────────────────

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase()))
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            log.warn("Error procesando JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}