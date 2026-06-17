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
        name = "noticia_vistas",
        uniqueConstraints = @UniqueConstraint(
                name  = "uk_vista_noticia_usuario",
                columnNames = {"noticia_id", "usuario_id"}
        ),
        indexes = {
                @Index(name = "idx_vista_noticia_id", columnList = "noticia_id"),
                @Index(name = "idx_vista_usuario_id", columnList = "usuario_id"),
                @Index(name = "idx_vista_fecha",      columnList = "fecha_vista")
        }
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoticiaVista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "noticia_id", nullable = false)
    private Long noticiaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    // Snapshot en el momento de la vista — evita joins al microservicio de usuarios
    @Column(name = "nombre_usuario", length = 150)
    private String nombreUsuario;

    @Column(name = "rol_usuario", length = 50)
    private String rolUsuario;

    @CreationTimestamp
    @Column(name = "fecha_vista", nullable = false, updatable = false)
    private LocalDateTime fechaVista;
}