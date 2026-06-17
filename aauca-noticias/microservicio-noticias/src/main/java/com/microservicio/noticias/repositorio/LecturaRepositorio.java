package com.microservicio.noticias.repositorio;

import com.microservicio.noticias.entidades.NoticiaLectura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LecturaRepositorio extends JpaRepository<NoticiaLectura, Long> {

    // ── Escritura atómica — elimina race condition del existsBy + save ────────
    @Modifying
    @Query(value = """
        INSERT INTO noticia_lecturas
            (noticia_id, usuario_id, nombre_usuario, rol_usuario, fecha_lectura)
        VALUES
            (:noticiaId, :usuarioId, :nombreUsuario, :rolUsuario, NOW(6))
        ON DUPLICATE KEY UPDATE noticia_id = noticia_id
    """, nativeQuery = true)
    void registrarAtomico(
            @Param("noticiaId")     Long   noticiaId,
            @Param("usuarioId")     Long   usuarioId,
            @Param("nombreUsuario") String nombreUsuario,
            @Param("rolUsuario")    String rolUsuario
    );

    // ── Panel admin — paginado, no carga todo en memoria ──────────────────────
    Page<NoticiaLectura> findByNoticiaIdOrderByFechaLecturaDesc(Long noticiaId, Pageable pageable);

    // ── Verificación sin cargar entidades — solo IDs ──────────────────────────
    @Query("SELECT l.usuarioId FROM NoticiaLectura l WHERE l.noticiaId = :noticiaId")
    List<Long> findUsuarioIdsByNoticiaId(@Param("noticiaId") Long noticiaId);

    // ── Verificar si un usuario específico ya leyó — sin cargar nada más ──────
    boolean existsByNoticiaIdAndUsuarioId(Long noticiaId, Long usuarioId);

    // ── Conteo directo en BD ──────────────────────────────────────────────────
    long countByNoticiaId(Long noticiaId);

    // ── Verificar cobertura total sin cargar registros ────────────────────────
    @Query("""
        SELECT COUNT(DISTINCT l.usuarioId) = :totalDestinatarios
        FROM NoticiaLectura l
        WHERE l.noticiaId = :noticiaId
    """)
    boolean todosLeyeron(
            @Param("noticiaId")          Long noticiaId,
            @Param("totalDestinatarios") long totalDestinatarios
    );
}