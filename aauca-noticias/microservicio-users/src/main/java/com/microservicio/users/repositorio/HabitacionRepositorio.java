package com.microservicio.users.repositorio;

import com.microservicio.users.entidades.Habitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HabitacionRepositorio extends JpaRepository<Habitacion, Long> {

    // ✅ Nombre exacto que usa HabitacionServicio
    List<Habitacion> findByEdificioIdAndActivoTrueOrderByNumeroAsc(Long edificioId);

    // ✅ Para buscar habitación por número al aprobar solicitud
    Optional<Habitacion> findByNumero(String numero);
}