package com.microservicio.secciones.servicios;

import com.microservicio.secciones.entidades.Seccion;
import com.microservicio.secciones.repositorio.ISeccion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeccionServicio {

    private final ISeccion seccionRepositorio;

    public Seccion crearSeccion(Seccion seccion) {
        if (seccionRepositorio.existsByNombre(seccion.getNombre())) {
            throw new RuntimeException("Ya existe una sección con el nombre: " + seccion.getNombre());
        }
        return seccionRepositorio.save(seccion);
    }

    public Seccion actualizarSeccion(Long id, Seccion datosNuevos) {  // ← añadido
        Seccion seccion = obtenerPorId(id);

        if (!seccion.getNombre().equals(datosNuevos.getNombre()) &&
                seccionRepositorio.existsByNombre(datosNuevos.getNombre())) {
            throw new RuntimeException("Ya existe una sección con el nombre: " + datosNuevos.getNombre());
        }

        seccion.setNombre(datosNuevos.getNombre());
        seccion.setDescripcion(datosNuevos.getDescripcion());

        return seccionRepositorio.save(seccion);  // @PreUpdate genera el slug automáticamente
    }

    public List<Seccion> listarSecciones() {
        return seccionRepositorio.findAll();
    }

    public List<Seccion> listarActivas() {
        return seccionRepositorio.findByActivoTrue();
    }

    public Seccion obtenerPorId(Long id) {
        return seccionRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Sección no encontrada con ID: " + id));
    }

    public void eliminarSeccion(Long id) {
        Seccion seccion = obtenerPorId(id);
        seccionRepositorio.delete(seccion);
    }

    public void desactivarSeccion(Long id) {
        Seccion seccion = obtenerPorId(id);
        seccion.setActivo(false);
        seccionRepositorio.save(seccion);
    }
}