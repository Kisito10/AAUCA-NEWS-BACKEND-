package com.microservicio.users.repositorio;

import com.microservicio.users.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);

    // ── Login por teléfono ────────────────────────────────────────────────────
    Optional<Usuario> findByTelefono(String telefono);
    boolean existsByTelefono(String telefono);

    // ── Login por número de habitación (primer acceso sin email/teléfono) ─────
    @Query("""
        SELECT u FROM Usuario u
        WHERE u.habitacion.numero = :numero
          AND u.activo = true
          AND u.expulsado = false
    """)
    List<Usuario> findByHabitacionNumero(@Param("numero") String numero);

    List<Usuario> findByActivoTrue();
    List<Usuario> findByRolId(Long rolId);

    // ── Filtrado por tipo destinatario ────────────────────────────────────────
    List<Usuario> findByGeneroAndActivoTrue(String genero);
    List<Usuario> findByFacultadAndActivoTrue(String facultad);
    List<Usuario> findBySeleccionAndActivoTrue(String seleccion);

    @Query("""
        SELECT u FROM Usuario u
        WHERE u.habitacion.edificioId = :edificioId
          AND u.activo = true
    """)
    List<Usuario> findByEdificioId(@Param("edificioId") Long edificioId);

    @Query("""
        SELECT u FROM Usuario u
        WHERE u.habitacion.id = :habitacionId
          AND u.activo = true
    """)
    List<Usuario> findByHabitacionId(@Param("habitacionId") Long habitacionId);

    // ── Expulsados ────────────────────────────────────────────────────────────
    List<Usuario> findByExpulsadoTrue();

    // ── Residentes que han superado el límite de 4 años ──────────────────────
    @Query("""
        SELECT u FROM Usuario u
        WHERE u.activo = true
          AND u.expulsado = false
          AND u.fechaIngreso IS NOT NULL
          AND u.fechaIngreso < :fechaLimite
          AND u.rol.nombre = 'Residente'
    """)
    List<Usuario> findResidentesQueSuperaronLimite(
            @Param("fechaLimite") LocalDate fechaLimite);

    // ── Valores únicos para filtros ───────────────────────────────────────────
    @Query("SELECT DISTINCT u.facultad FROM Usuario u WHERE u.facultad IS NOT NULL AND u.activo = true")
    List<String> findDistinctFacultades();

    @Query("SELECT DISTINCT u.genero FROM Usuario u WHERE u.genero IS NOT NULL AND u.activo = true")
    List<String> findDistinctGeneros();

    @Query("SELECT DISTINCT u.seleccion FROM Usuario u WHERE u.seleccion IS NOT NULL AND u.activo = true")
    List<String> findDistinctSelecciones();
}