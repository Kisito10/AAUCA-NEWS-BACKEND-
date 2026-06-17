package com.microservicio.secciones.entidades;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "secciones")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Seccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    @PrePersist
    private void antesDeInsertar() {
        if (this.activo == null) this.activo = true;
        generarSlug();
    }

    @PreUpdate
    private void antesDeActualizar() {
        generarSlug();
    }

    private void generarSlug() {
        if (this.nombre != null) {
            this.slug = this.nombre
                    .toLowerCase()
                    .trim()
                    .replaceAll("[áàäâ]", "a")
                    .replaceAll("[éèëê]", "e")
                    .replaceAll("[íìïî]", "i")
                    .replaceAll("[óòöô]", "o")
                    .replaceAll("[úùüû]", "u")
                    .replaceAll("[ñ]", "n")
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("[\\s]+", "-");
        }
    }
}