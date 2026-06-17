package com.microservicio.users.servicios;

import com.microservicio.users.entidades.Edificio;
import com.microservicio.users.entidades.Habitacion;
import com.microservicio.users.repositorio.EdificioRepositorio;
import com.microservicio.users.repositorio.HabitacionRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EdificioServicio {

    private final EdificioRepositorio   edificioRepositorio;
    private final HabitacionRepositorio habitacionRepositorio;

    private static final int PLANTAS                 = 3;
    private static final int HABITACIONES_POR_PLANTA = 12;

    @Transactional(readOnly = true)
    public List<Edificio> listarActivos() {
        return edificioRepositorio.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Edificio> listarTodos() {
        return edificioRepositorio.findAll();
    }

    @Transactional
    public Edificio guardar(Edificio edificio) {
        edificio.setId(null);
        if (edificio.getActivo()     == null) edificio.setActivo(true);
        if (edificio.getNumPlantas() == null) edificio.setNumPlantas(PLANTAS);
        Edificio saved = edificioRepositorio.save(edificio);
        generarHabitaciones(saved);
        return saved;
    }

    @Transactional
    public Edificio actualizar(Long id, Edificio datos) {
        Edificio existente = edificioRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Edificio no encontrado: " + id));
        if (datos.getNombre()     != null) existente.setNombre(datos.getNombre());
        if (datos.getNumPlantas() != null) existente.setNumPlantas(datos.getNumPlantas());
        return edificioRepositorio.save(existente);
    }

    @Transactional
    public Edificio toggle(Long id) {
        Edificio edificio = edificioRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Edificio no encontrado: " + id));
        edificio.setActivo(!edificio.getActivo());
        return edificioRepositorio.save(edificio);
    }

    private void generarHabitaciones(Edificio edificio) {
        int numPlantas = edificio.getNumPlantas() != null
                ? edificio.getNumPlantas() : PLANTAS;

        List<Habitacion> habitaciones = new ArrayList<>();
        for (int planta = 1; planta <= numPlantas; planta++) {
            for (int hab = 1; hab <= HABITACIONES_POR_PLANTA; hab++) {
                int numero = planta * 100 + hab;
                Habitacion h = new Habitacion();
                h.setNumero(edificio.getNombre() + "-" + numero);
                h.setPiso(planta);
                h.setEdificioId(edificio.getId());
                h.setActivo(true);
                habitaciones.add(h);
            }
        }
        habitacionRepositorio.saveAll(habitaciones);
    }
}