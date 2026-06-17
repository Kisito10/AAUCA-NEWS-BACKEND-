package com.microservicio.users.entidades;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    // ─── Helper ───────────────────────────────────────────────────────────────

    public boolean esDirector() { return "Director".equalsIgnoreCase(this.nombre); }
    public boolean esCelador()  { return "Celador".equalsIgnoreCase(this.nombre);  }
}