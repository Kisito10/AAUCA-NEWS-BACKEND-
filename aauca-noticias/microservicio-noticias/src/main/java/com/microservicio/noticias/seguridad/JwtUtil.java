package com.microservicio.noticias.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

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

    public String extraerRol(String token)    { return extraerClaims(token).get("rol",    String.class); }
    public String extraerNombre(String token) { return extraerClaims(token).get("nombre", String.class); }
    public Long   extraerId(String token)     { return extraerClaims(token).get("id",     Long.class);   }

    public boolean esValido(String token) {
        try { extraerClaims(token); return true; }
        catch (Exception e) { log.warn("Token inválido: {}", e.getMessage()); return false; }
    }
    public Long extraerUsuarioId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Object id = claims.get("id");
        if (id instanceof Integer) return ((Integer) id).longValue();
        if (id instanceof Long)    return (Long) id;
        return Long.parseLong(id.toString());
    }
}