package com.microservicio.secciones.repositorio;

import com.microservicio.secciones.entidades.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ISeccion extends JpaRepository<Seccion, Long> {

    List<Seccion> findByActivoTrue();

    boolean existsByNombre(String nombre);  // ← valida nombre duplicado

    boolean existsBySlug(String slug);      // ← valida slug duplicado
}