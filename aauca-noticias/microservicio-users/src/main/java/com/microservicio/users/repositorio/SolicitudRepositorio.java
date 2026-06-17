package com.microservicio.users.repositorio;

import com.microservicio.users.entidades.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SolicitudRepositorio extends JpaRepository<Solicitud, Long> {

    List<Solicitud> findByEstado(Solicitud.Estado estado);

    /** ¿Existe alguna solicitud con ese email en cualquier estado? */
    boolean existsByEmail(String email);

    /** ¿Existe solicitud PENDIENTE para ese email? — para rectificación */
    boolean existsByEmailAndEstado(String email, Solicitud.Estado estado);

    /** Busca la solicitud PENDIENTE de un email — para reemplazarla */
    Optional<Solicitud> findByEmailAndEstado(String email, Solicitud.Estado estado);

    /**
     * Cuenta solicitudes PENDIENTES para una habitación concreta.
     * Se usa para bloquear habitaciones llenas (capacidad = 2).
     */
    @Query("SELECT COUNT(s) FROM Solicitud s " +
            "WHERE s.habitacion = :habitacion AND s.estado = 'PENDIENTE'")
    long contarPendientesPorHabitacion(@Param("habitacion") String habitacion);
}