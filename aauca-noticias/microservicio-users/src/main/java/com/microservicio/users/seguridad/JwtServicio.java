package com.microservicio.users.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtServicio {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Genera un token con id, nombre, email y rol del usuario */
    public String generarToken(Long id, String nombre, String email, String rol) {
        return Jwts.builder()
                .subject(email)
                .claims(Map.of(
                        "id",     id,
                        "nombre", nombre,
                        "email",  email,
                        "rol",    rol        // ← "Director", "Celador", "Residente"
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    public Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extraerRol(String token)    { return extraerClaims(token).get("rol",    String.class); }
    public String extraerNombre(String token) { return extraerClaims(token).get("nombre", String.class); }
    public Long   extraerId(String token)     { return extraerClaims(token).get("id",     Long.class);   }
    public String extraerEmail(String token)  { return extraerClaims(token).getSubject(); }

    public boolean esValido(String token) {
        try {
            extraerClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        }
    }
}