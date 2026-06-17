package com.microservicio.secciones.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JwtUtil — microservicio-secciones
 *
 * Solo valida y extrae información del token generado por ms-users.
 * Usa la misma clave secreta (jwt.secret) que ms-users.
 * NO genera tokens.
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extraerRol(String token) {
        return extraerClaims(token).get("rol", String.class);
    }

    public boolean esValido(String token) {
        try {
            extraerClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("Token inválido en ms-secciones: {}", e.getMessage());
            return false;
        }
    }
}