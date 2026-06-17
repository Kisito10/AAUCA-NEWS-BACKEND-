package com.microservicio.users.entidades;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_registro")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 100)
    private String apellidos;

    // ⚠️ Sin @Column(unique=true) — un email puede rectificar su solicitud
    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 10)
    private String genero;

    @Column(length = 100)
    private String facultad;

    @Column(length = 100)
    private String seleccion;

    @Column(length = 50)
    private String habitacion;

    @Column(length = 100)
    private String edificio;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    @Column(length = 255)
    private String foto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Estado estado = Estado.PENDIENTE;

    /**
     * Número de veces que el solicitante ha rectificado su solicitud.
     * 0 = primera vez, 1 = primera rectificación, etc.
     * El frontend usa este valor para mostrar el mensaje correcto.
     */
    @Column(name = "num_rectificaciones", nullable = false)
    @Builder.Default
    private Integer numRectificaciones = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Estado { PENDIENTE, APROBADA, RECHAZADA }
}