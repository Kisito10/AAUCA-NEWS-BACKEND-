package com.microservicio.users.servicios;

import com.microservicio.users.entidades.Habitacion;
import com.microservicio.users.repositorio.HabitacionRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitacionServicio {

    private final HabitacionRepositorio habitacionRepositorio;

    @Transactional(readOnly = true)
    public List<Habitacion> listarPorEdificio(Long edificioId) {
        return habitacionRepositorio
                .findByEdificioIdAndActivoTrueOrderByNumeroAsc(edificioId);
    }

    @Transactional(readOnly = true)
    public Habitacion buscarPorId(Long id) {
        return habitacionRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Habitación no encontrada: " + id));
    }
}