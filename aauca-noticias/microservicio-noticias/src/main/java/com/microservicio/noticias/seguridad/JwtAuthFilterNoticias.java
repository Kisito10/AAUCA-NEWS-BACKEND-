package com.microservicio.noticias.seguridad;

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

/**
 * JwtAuthFilterNoticias
 *
 * Extrae el token Bearer de cada request, lo valida con JwtUtil
 * (misma clave secreta que ms-users) y puebla el SecurityContext
 * con el email y el rol del usuario.
 *
 * Si el token no existe o es inválido, la request pasa sin autenticar
 * y Spring Security aplicará las reglas del SecurityConfig
 * (la mayoría requiere autenticación → devolverá 401).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilterNoticias extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Sin token → continuar sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);

            if (jwtUtil.esValido(token)) {
                String rol   = jwtUtil.extraerRol(token);
                Long   id    = jwtUtil.extraerUsuarioId(token);

                // Principal = id del usuario (útil en lecturas para saber quién lee)
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                id,       // principal — usado en LecturaControlador
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase()))
                        );

                // Guardar también el header original para que los controladores
                // puedan seguir usando extraerDatosJwt(authHeader) sin cambios
                auth.setDetails(authHeader);

                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("JWT válido — userId={} rol={}", id, rol);
            }

        } catch (Exception e) {
            log.warn("Error procesando JWT en ms-noticias: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}