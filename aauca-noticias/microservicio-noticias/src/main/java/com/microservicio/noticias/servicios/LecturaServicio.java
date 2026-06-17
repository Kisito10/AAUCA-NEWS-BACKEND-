package com.microservicio.noticias.servicios;

import com.microservicio.noticias.entidades.NoticiaLectura;
import com.microservicio.noticias.repositorio.LecturaRepositorio;
import com.microservicio.noticias.repositorio.NoticiaDestinatarioRepositorio;
import com.microservicio.noticias.repositorio.NoticiaRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LecturaServicio {

    private final LecturaRepositorio             lecturaRepositorio;
    private final NoticiaDestinatarioRepositorio destinatarioRepositorio;
    private final NoticiaRepositorio             noticiaRepositorio;

    // ── Registrar lectura ─────────────────────────────────────────────────────

    @Transactional
    public void registrarLectura(Long noticiaId, Long usuarioId,
                                 String nombreUsuario, String rolUsuario,
                                 Short duracionSeg) {
        // INSERT ... ON DUPLICATE KEY UPDATE — atómico, sin race condition
        lecturaRepositorio.registrarAtomico(noticiaId, usuarioId, nombreUsuario, rolUsuario);

        // Actualizar duración si se envió (UPDATE solo si ya existía el registro)
        if (duracionSeg != null) {
            lecturaRepositorio.findById(
                    lecturaRepositorio
                            .findByNoticiaIdOrderByFechaLecturaDesc(
                                    noticiaId, PageRequest.of(0, 1))
                            .stream()
                            .filter(l -> l.getUsuarioId().equals(usuarioId))
                            .findFirst()
                            .map(NoticiaLectura::getId)
                            .orElse(-1L)
            ).ifPresent(l -> {
                l.setDuracionSeg(duracionSeg);
                lecturaRepositorio.save(l);
            });
        }

        verificarYArchivarSiCompleta(noticiaId);
    }

    // ── Verificar cobertura total ─────────────────────────────────────────────

    @Transactional
    public void verificarYArchivarSiCompleta(Long noticiaId) {
        // Solo IDs — no carga entidades completas
        List<Long> destinatarios = resolverDestinatariosUsuario(noticiaId);

        if (destinatarios.isEmpty()) {
            log.debug("Noticia {} sin destinatarios específicos — no se archiva automáticamente", noticiaId);
            return;
        }

        long total = destinatarios.size();

        // Una sola query en BD — no carga registros en memoria
        boolean completa = lecturaRepositorio.todosLeyeron(noticiaId, total);

        if (completa) {
            log.info("Todos los destinatarios leyeron la noticia {} — archivando", noticiaId);
            noticiaRepositorio.findById(noticiaId).ifPresent(n -> {
                n.setActivo(false);
                noticiaRepositorio.save(n);
            });
        } else {
            long leidos  = lecturaRepositorio.countByNoticiaId(noticiaId);
            long faltan  = total - leidos;
            log.debug("Noticia {} — {}/{} destinatarios han leído, faltan {}", noticiaId, leidos, total, faltan);
        }
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<NoticiaLectura> lecturasPorNoticia(Long noticiaId, int pagina, int tamano) {
        return lecturaRepositorio
                .findByNoticiaIdOrderByFechaLecturaDesc(noticiaId, PageRequest.of(pagina, tamano));
    }

    @Transactional(readOnly = true)
    public boolean yaLeyo(Long noticiaId, Long usuarioId) {
        return lecturaRepositorio.existsByNoticiaIdAndUsuarioId(noticiaId, usuarioId);
    }

    @Transactional(readOnly = true)
    public long totalLecturas(Long noticiaId) {
        return lecturaRepositorio.countByNoticiaId(noticiaId);
    }

    // ── Helper — resolver solo IDs de destinatarios tipo USUARIO_ESPECIFICO ───

    private List<Long> resolverDestinatariosUsuario(Long noticiaId) {
        return destinatarioRepositorio.findByNoticiaId(noticiaId)
                .stream()
                .filter(d -> d.getValor() != null)
                .map(d -> {
                    try   { return Long.parseLong(d.getValor().trim()); }
                    catch (NumberFormatException e) { return null; }
                })
                .filter(id -> id != null)
                .toList();
    }
}