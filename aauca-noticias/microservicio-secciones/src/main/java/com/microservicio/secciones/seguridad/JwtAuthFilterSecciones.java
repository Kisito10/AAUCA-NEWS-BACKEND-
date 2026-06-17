package com.microservicio.secciones.seguridad;

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
 * JwtAuthFilterSecciones
 *
 * Valida el token Bearer en cada request y puebla el SecurityContext.
 * Idéntico en funcionamiento al de ms-users y ms-noticias —
 * todos comparten la misma clave secreta JWT.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilterSecciones extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);

            if (jwtUtil.esValido(token)) {
                String rol = jwtUtil.extraerRol(token);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                token,   // principal
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase()))
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("JWT válido en ms-secciones — rol={}", rol);
            }

        } catch (Exception e) {
            log.warn("Error procesando JWT en ms-secciones: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}