package com.microservicio.users.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 150)
    private String apellidos;

    // ── Email — ahora opcional para residentes sin email ──────────────────────
    // unique=true pero nullable — un residente puede no tener email
    @Column(unique = true, length = 255)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(length = 150)
    private String facultad;

    @Column(length = 10)
    private String genero;

    @Column(length = 100)
    private String seleccion;

    // ── Foto de perfil ────────────────────────────────────────────────────────
    @Column(name = "foto", length = 500)
    private String foto;

    // ── Habitación ────────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "habitacion_id")
    private Habitacion habitacion;

    // ── Teléfono — identificador alternativo para residentes sin email ─────────
    // Único cuando no es nulo. Permite login con teléfono + contraseña.
    @Column(name = "telefono", unique = true, length = 30)
    private String telefono;

    // ── Primer acceso — para forzar pantalla de bienvenida ────────────────────
    // true = la cuenta fue creada por carga masiva y aún no ha iniciado sesión
    @Column(name = "primer_acceso", nullable = false)
    @Builder.Default
    private Boolean primerAcceso = false;

    @Column(name = "coro_iglesia", nullable = false)
    @Builder.Default
    private Boolean coroIglesia = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    // ── Expulsado definitivamente ─────────────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private Boolean expulsado = false;

    // ── Fecha de ingreso a la residencia ──────────────────────────────────────
    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    // ── Seguridad ─────────────────────────────────────────────────────────────
    @Column(name = "token_version", nullable = false)
    @Builder.Default
    private Integer tokenVersion = 0;

    @Column(name = "intentos_fallidos", nullable = false)
    @Builder.Default
    private Integer intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "ultimo_login_at")
    private LocalDateTime ultimoLoginAt;

    @Column(name = "ultimo_cambio_pass")
    private LocalDateTime ultimoCambioPass;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public Long getHabitacionId() {
        return habitacion != null ? habitacion.getId() : null;
    }

    /** true si el residente ha superado los 4 años en la residencia */
    public boolean haSuperadoLimiteAnios() {
        if (fechaIngreso == null) return false;
        return LocalDate.now().isAfter(fechaIngreso.plusYears(4));
    }

    /** true si tiene algún identificador para iniciar sesión */
    public boolean tieneIdentificador() {
        return (email != null && !email.isBlank())
                || (telefono != null && !telefono.isBlank());
    }
}