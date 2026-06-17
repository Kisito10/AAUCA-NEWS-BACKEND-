package com.microservicio.users.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "habitaciones")
public class Habitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String numero;

    @Column(nullable = false)
    private Integer piso;

    @Column(name = "edificio_id", nullable = false)
    private Long edificioId;

    @Column(nullable = false)
    private Boolean activo = true;
}