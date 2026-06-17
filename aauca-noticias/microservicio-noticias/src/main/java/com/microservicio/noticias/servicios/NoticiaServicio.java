package com.microservicio.noticias.servicios;

import com.microservicio.noticias.entidades.Noticia;
import com.microservicio.noticias.repositorio.LecturaRepositorio;
import com.microservicio.noticias.repositorio.NoticiaRepositorio;
import com.microservicio.noticias.seguridad.RolValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticiaServicio {

    private final NoticiaRepositorio noticiaRepositorio;
    private final LecturaRepositorio lecturaRepositorio;
    private final RolValidator       rolValidator;

    private List<Noticia> conLecturas(List<Noticia> noticias) {
        noticias.forEach(n ->
                n.setTotalLecturas(lecturaRepositorio.countByNoticiaId(n.getId()))
        );
        return noticias;
    }

    @Transactional
    public Noticia crearNoticia(Noticia noticia, String rol, String nombreUsuario) {
        rolValidator.validarCrud(rol);
        validarSeccion(noticia);
        normalizarPrioridad(noticia);
        noticia.setPublicadoPor(resolverNombre(nombreUsuario, noticia.getUsuarioId()));
        noticia.setRolCreador(rol.toLowerCase());

        boolean quierePublicar = "PUBLICADO".equalsIgnoreCase(noticia.getEstado());
        if (quierePublicar) {
            rolValidator.validarPublicacion(rol, noticia.getPrioridad());
            noticia.setEstado("PUBLICADO");
            noticia.setAutorizadoPor(nombreUsuario);
            noticia.setFechaAutorizacion(LocalDateTime.now());
        } else {
            noticia.setEstado("BORRADOR");
        }

        return noticiaRepositorio.save(noticia);
    }

    @Transactional
    public Noticia publicarNoticia(Long id, String rol, String nombreUsuario) {
        Noticia noticia = obtenerPorId(id);
        if ("PUBLICADO".equals(noticia.getEstado()))
            throw new IllegalArgumentException("La noticia ya está publicada.");
        rolValidator.validarPublicacion(rol, noticia.getPrioridad());
        noticia.setEstado("PUBLICADO");
        noticia.setAutorizadoPor(nombreUsuario);
        noticia.setFechaAutorizacion(LocalDateTime.now());
        return noticiaRepositorio.save(noticia);
    }

    @Transactional
    public Noticia autorizarNoticia(Long id, String rol, String nombreUsuario) {
        Noticia noticia = obtenerPorId(id);
        rolValidator.validarAutorizacion(rol, noticia.getRolCreador());
        if ("PUBLICADO".equals(noticia.getEstado()))
            throw new IllegalArgumentException("La noticia ya está publicada.");
        noticia.setEstado("PUBLICADO");
        noticia.setAutorizadoPor(nombreUsuario);
        noticia.setFechaAutorizacion(LocalDateTime.now());
        return noticiaRepositorio.save(noticia);
    }

    @Transactional(readOnly = true)
    public List<Noticia> listarTodas() {
        return conLecturas(noticiaRepositorio.findAll());
    }

    @Transactional(readOnly = true)
    public List<Noticia> listarActivas() {
        return conLecturas(noticiaRepositorio.findByActivoTrue());
    }

    @Transactional(readOnly = true)
    public List<Noticia> listarPublicadas() {
        return conLecturas(noticiaRepositorio.findByEstadoAndActivoTrue("PUBLICADO"));
    }

    @Transactional(readOnly = true)
    public List<Noticia> listarUrgentes() {
        return conLecturas(noticiaRepositorio.findByPrioridadAndEstado(
                Noticia.Prioridad.URGENTE, "PUBLICADO"));
    }

    @Transactional(readOnly = true)
    public List<Noticia> listarDestacadas() {
        return conLecturas(noticiaRepositorio.findByPrioridadAndEstado(
                Noticia.Prioridad.DESTACADA, "PUBLICADO"));
    }

    @Transactional(readOnly = true)
    public Noticia obtenerPorId(Long id) {
        Noticia n = noticiaRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Noticia no encontrada: " + id));
        n.setTotalLecturas(lecturaRepositorio.countByNoticiaId(id));
        return n;
    }

    @Transactional(readOnly = true)
    public List<Noticia> listarPublicadasParaUsuario(
            Long   usuarioId,
            String genero,
            String facultad,
            String seleccion,
            Long   habitacionId,
            Long   edificioId) {

        return conLecturas(noticiaRepositorio.findPublicadasParaUsuario(
                usuarioId    != null ? String.valueOf(usuarioId)    : null,
                trimOrNull(genero),
                trimOrNull(facultad),
                trimOrNull(seleccion),
                habitacionId != null ? String.valueOf(habitacionId) : null,
                edificioId   != null ? String.valueOf(edificioId)   : null
        ));
    }

    @Transactional
    public Noticia actualizarNoticia(Long id, Noticia datos, String rol) {
        rolValidator.validarCrud(rol);
        Noticia existente = obtenerPorId(id);

        if (datos.getTitulo()       != null) existente.setTitulo(datos.getTitulo());
        if (datos.getDescripcion()  != null) existente.setDescripcion(datos.getDescripcion());
        if (datos.getImagen()       != null) existente.setImagen(datos.getImagen());
        if (datos.getSeccionId()    != null) existente.setSeccionId(datos.getSeccionId());
        if (datos.getUsuarioId()    != null) existente.setUsuarioId(datos.getUsuarioId());
        if (datos.getPublicadoPor() != null) existente.setPublicadoPor(datos.getPublicadoPor());
        if (datos.getPrioridad()    != null) existente.setPrioridad(datos.getPrioridad());

        if (datos.getEstado() != null && !datos.getEstado().isBlank()) {
            String estadoNorm = datos.getEstado().toUpperCase();
            validarValorEstado(estadoNorm);
            existente.setEstado(estadoNorm);
        }

        return noticiaRepositorio.save(existente);
    }

    @Transactional
    public void eliminarNoticia(Long id, String rol) {
        rolValidator.validarCrud(rol);
        if (!noticiaRepositorio.existsById(id))
            throw new RuntimeException("Noticia no encontrada: " + id);

        // ✅ Verificar si tiene lecturas antes de eliminar
        long totalLecturas = lecturaRepositorio.countByNoticiaId(id);
        if (totalLecturas > 0)
            throw new IllegalStateException(
                    "No puedes eliminar esta noticia porque ya ha sido leída por " +
                            totalLecturas + " residente(s). Puedes archivarla en su lugar."
            );

        noticiaRepositorio.deleteById(id);
    }

    @Transactional
    public void archivarNoticia(Long id, String rol) {
        rolValidator.validarCrud(rol);
        Noticia noticia = obtenerPorId(id);
        noticia.setActivo(false);
        noticiaRepositorio.save(noticia);
    }

    private void validarSeccion(Noticia noticia) {
        if (noticia.getSeccionId() == null)
            throw new IllegalArgumentException("Debes seleccionar una sección.");
    }

    private void validarValorEstado(String estado) {
        if (!estado.equals("PUBLICADO") && !estado.equals("BORRADOR"))
            throw new IllegalArgumentException("El estado debe ser PUBLICADO o BORRADOR.");
    }

    private void normalizarPrioridad(Noticia noticia) {
        if (noticia.getPrioridad() == null)
            noticia.setPrioridad(Noticia.Prioridad.NORMAL);
    }

    private String resolverNombre(String nombre, Long usuarioId) {
        if (nombre != null && !nombre.isBlank()) return nombre;
        if (usuarioId != null) return "usuario-" + usuarioId;
        return "sistema";
    }

    private String trimOrNull(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }
}