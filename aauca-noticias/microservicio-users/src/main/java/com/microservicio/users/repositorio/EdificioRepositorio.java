package com.microservicio.users.repositorio;

import com.microservicio.users.entidades.Edificio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EdificioRepositorio extends JpaRepository<Edificio, Long> {
    List<Edificio> findByActivoTrueOrderByNombreAsc();
}