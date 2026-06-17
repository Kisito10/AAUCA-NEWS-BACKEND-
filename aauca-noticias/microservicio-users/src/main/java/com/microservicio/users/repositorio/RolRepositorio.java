package com.microservicio.users.repositorio;

import com.microservicio.users.entidades.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolRepositorio extends JpaRepository<Rol, Long> {

    /** Busca un rol por nombre exacto, p. ej. findByNombre("Residente"). */
    Optional<Rol> findByNombre(String nombre);
}
