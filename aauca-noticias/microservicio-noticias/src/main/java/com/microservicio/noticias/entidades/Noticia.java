package com.microservicio.noticias.entidades;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "noticias")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Noticia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 500)
    private String imagen;

    @Column(name = "seccion_id", nullable = false)
    private Long seccionId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "publicado_por", length = 100)
    private String publicadoPor;

    @Column(name = "autorizado_por", length = 100)
    private String autorizadoPor;

    @Column(name = "fecha_autorizacion")
    private LocalDateTime fechaAutorizacion;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad", nullable = false, length = 20)
    private Prioridad prioridad;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @CreationTimestamp
    @Column(name = "fecha_publicacion", nullable = false, updatable = false)
    private LocalDateTime fechaPublicacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "rol_creador", length = 50)
    private String rolCreador;

    // ✅ Total de lecturas — calculado en el servicio, no guardado en BD
    @Transient
    private Long totalLecturas = 0L;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @PrePersist
    private void antesDeInsertar() {
        if (this.activo       == null) this.activo       = true;
        if (this.estado       == null) this.estado       = "BORRADOR";
        if (this.prioridad    == null) this.prioridad    = Prioridad.NORMAL;
        if (this.publicadoPor == null) this.publicadoPor = "sistema";
    }

    @PreUpdate
    private void antesDeActualizar() {
        if (this.estado != null) this.estado = this.estado.toUpperCase();
    }

    // ─── Enum ─────────────────────────────────────────────────────────────────

    public enum Prioridad {
        NORMAL, URGENTE, DESTACADA
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    @Transient
    public boolean isUrgente() {
        return prioridad == Prioridad.URGENTE || prioridad == Prioridad.DESTACADA;
    }
}