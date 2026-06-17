package com.microservicio.noticias.servicios;

import com.microservicio.noticias.entidades.NoticiaDestinatario;
import com.microservicio.noticias.entidades.TipoDestinatario;
import com.microservicio.noticias.repositorio.NoticiaDestinatarioRepositorio;
import com.microservicio.noticias.repositorio.TipoDestinatarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DestinatarioServicio {

    private final TipoDestinatarioRepositorio    tipoRepo;
    private final NoticiaDestinatarioRepositorio destinatarioRepo;

    // ─── Tipos ────────────────────────────────────────────────────────────────

    public List<TipoDestinatario> listarTipos() {
        return tipoRepo.findByActivoTrue();
    }

    // ─── Obtener destinatarios de una noticia ─────────────────────────────────

    public List<NoticiaDestinatario> obtenerPorNoticia(Long noticiaId) {
        return destinatarioRepo.findByNoticiaId(noticiaId);
    }

    // ─── Guardar destinatarios — reemplaza los anteriores ────────────────────

    @Transactional
    public List<NoticiaDestinatario> guardarDestinatarios(
            Long noticiaId,
            List<NoticiaDestinatario> destinatarios) {

        // Borrar los anteriores
        destinatarioRepo.deleteByNoticiaId(noticiaId);

        // Asignar noticiaId y limpiar id para evitar conflictos
        destinatarios.forEach(d -> {
            d.setId(null);
            d.setNoticiaId(noticiaId);
            d.setTipo(null); // evitar conflicto con @ManyToOne
        });

        List<NoticiaDestinatario> saved = destinatarioRepo.saveAll(destinatarios);
        log.info("Guardados {} destinatarios para noticia {}", saved.size(), noticiaId);
        return saved;
    }

    // ─── Eliminar destinatarios de una noticia ────────────────────────────────

    @Transactional
    public void eliminarPorNoticia(Long noticiaId) {
        destinatarioRepo.deleteByNoticiaId(noticiaId);
    }
    public List<TipoDestinatario> listarTodos() {
        return tipoRepo.findAll();
    }

    @Transactional
    public TipoDestinatario crearTipo(TipoDestinatario tipo) {
        tipo.setId(null);
        if (tipo.getActivo() == null) tipo.setActivo(true);
        return tipoRepo.save(tipo);
    }

    @Transactional
    public TipoDestinatario actualizarTipo(Long id, TipoDestinatario datos) {
        TipoDestinatario existente = tipoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo no encontrado: " + id));
        if (datos.getNombre()      != null) existente.setNombre(datos.getNombre());
        if (datos.getDescripcion() != null) existente.setDescripcion(datos.getDescripcion());
        return tipoRepo.save(existente);
    }

    @Transactional
    public TipoDestinatario toggleTipo(Long id) {
        TipoDestinatario tipo = tipoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo no encontrado: " + id));
        tipo.setActivo(!tipo.getActivo());
        return tipoRepo.save(tipo);
    }
}