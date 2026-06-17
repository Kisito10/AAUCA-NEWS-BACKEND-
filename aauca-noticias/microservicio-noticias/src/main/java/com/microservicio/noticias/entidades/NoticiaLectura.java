package com.microservicio.noticias.entidades;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "noticia_lecturas",
        uniqueConstraints = @UniqueConstraint(
                name        = "uk_lectura_noticia_usuario",
                columnNames = {"noticia_id", "usuario_id"}
        ),
        indexes = {
                @Index(name = "idx_lectura_noticia_id", columnList = "noticia_id"),
                @Index(name = "idx_lectura_usuario_id", columnList = "usuario_id"),
                @Index(name = "idx_lectura_fecha",      columnList = "fecha_lectura")
        }
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoticiaLectura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "noticia_id", nullable = false)
    private Long noticiaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    // Snapshot — para el panel sin joins al microservicio de usuarios
    @Column(name = "nombre_usuario", length = 150)
    private String nombreUsuario;

    @Column(name = "rol_usuario", length = 50)
    private String rolUsuario;

    @CreationTimestamp
    @Column(name = "fecha_lectura", nullable = false, updatable = false)
    private LocalDateTime fechaLectura;

    @Column(name = "duracion_seg")
    private Short duracionSeg;
}