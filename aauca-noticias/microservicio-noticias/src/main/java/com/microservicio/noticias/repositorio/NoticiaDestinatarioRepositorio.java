package com.microservicio.noticias.repositorio;

import com.microservicio.noticias.entidades.NoticiaDestinatario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticiaDestinatarioRepositorio
        extends JpaRepository<NoticiaDestinatario, Long> {
    List<NoticiaDestinatario> findByNoticiaId(Long noticiaId);
    void deleteByNoticiaId(Long noticiaId);
}