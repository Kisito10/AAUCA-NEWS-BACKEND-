package com.microservicio.noticias.repositorio;

import com.microservicio.noticias.entidades.TipoDestinatario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoDestinatarioRepositorio
        extends JpaRepository<TipoDestinatario, Long> {
    List<TipoDestinatario> findByActivoTrue();
}