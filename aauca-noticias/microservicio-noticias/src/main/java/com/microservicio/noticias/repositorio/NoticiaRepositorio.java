package com.microservicio.noticias.repositorio;

import com.microservicio.noticias.entidades.Noticia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticiaRepositorio extends JpaRepository<Noticia, Long> {

    List<Noticia> findByActivoTrue();

    List<Noticia> findByEstadoAndActivoTrue(String estado);

    List<Noticia> findByPrioridadAndEstado(Noticia.Prioridad prioridad, String estado);

    @Query(value = """
        SELECT DISTINCT n.* FROM noticias n
        WHERE n.estado = 'PUBLICADO'
          AND n.activo = true
          AND (
              -- Noticia sin destinatarios → visible para todos
              NOT EXISTS (
                  SELECT 1 FROM noticia_destinatarios d
                  WHERE d.noticia_id = n.id
              )
              OR EXISTS (
                  SELECT 1 FROM noticia_destinatarios d
                  INNER JOIN tipos_destinatario t ON t.id = d.tipo_id
                  WHERE d.noticia_id = n.id
                  AND t.activo = true
                  AND (
                      -- Para todos
                      t.nombre = 'TODOS'

                      -- Usuario específico
                      OR (
                          t.nombre = 'USUARIO_ESPECIFICO'
                          AND :usuarioId IS NOT NULL
                          AND TRIM(d.valor) = TRIM(:usuarioId)
                      )

                      -- Por habitación
                      OR (
                          t.nombre = 'HABITACION'
                          AND :habitacionId IS NOT NULL
                          AND :habitacionId != ''
                          AND TRIM(d.valor) = TRIM(:habitacionId)
                      )

                      -- Por género
                      OR (
                          t.nombre = 'GENERO'
                          AND :genero IS NOT NULL
                          AND :genero != ''
                          AND LOWER(TRIM(d.valor)) = LOWER(TRIM(:genero))
                      )

                      -- Por facultad
                      OR (
                          t.nombre = 'FACULTAD'
                          AND :facultad IS NOT NULL
                          AND :facultad != ''
                          AND LOWER(TRIM(d.valor)) = LOWER(TRIM(:facultad))
                      )

                      -- Por selección deportiva
                      OR (
                          t.nombre = 'SELECCION'
                          AND :seleccion IS NOT NULL
                          AND :seleccion != ''
                          AND LOWER(TRIM(d.valor)) = LOWER(TRIM(:seleccion))
                      )

                      -- Por edificio
                      OR (
                          t.nombre = 'EDIFICIO'
                          AND :edificioId IS NOT NULL
                          AND :edificioId != ''
                          AND TRIM(d.valor) = TRIM(:edificioId)
                      )
                  )
              )
          )
        ORDER BY n.fecha_publicacion DESC
    """, nativeQuery = true)
    List<Noticia> findPublicadasParaUsuario(
            @Param("usuarioId")    String usuarioId,
            @Param("genero")       String genero,
            @Param("facultad")     String facultad,
            @Param("seleccion")    String seleccion,
            @Param("habitacionId") String habitacionId,
            @Param("edificioId")   String edificioId
    );
}