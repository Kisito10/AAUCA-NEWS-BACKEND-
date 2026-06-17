package com.microservicio.users.servicios;

import com.microservicio.users.entidades.Habitacion;
import com.microservicio.users.entidades.Rol;
import com.microservicio.users.entidades.Usuario;
import com.microservicio.users.repositorio.RolRepositorio;
import com.microservicio.users.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioServicios {

    private final UsuarioRepository usuarioRepository;
    private final RolRepositorio    rolRepositorio;
    private final PasswordEncoder   passwordEncoder;

    // ── Creación ──────────────────────────────────────────────────────────────

    @Transactional
    public Usuario crearUsuario(Usuario usuario, String nombreRol) {
        if (usuarioRepository.existsByEmail(usuario.getEmail()))
            throw new IllegalArgumentException("Ya existe un usuario con ese email");

        Rol rol = rolRepositorio.findByNombre(nombreRol)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + nombreRol));

        usuario.setRol(rol);
        usuario.setPasswordHash(passwordEncoder.encode(usuario.getPasswordHash()));
        usuario.setActivo(true);
        return usuarioRepository.save(usuario);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarActivos() {
        return usuarioRepository.findByActivoTrue();
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    // ── Verificación ──────────────────────────────────────────────────────────

    public boolean verificarPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public String generarHash(String password) {
        return passwordEncoder.encode(password);
    }

    // ── Actualización de perfil propio ────────────────────────────────────────
    // Recibe valores primitivos para evitar que @Builder.Default de Lombok
    // inicialice campos en el objeto parcial e interfiera con Hibernate

    @Transactional
    public Usuario actualizarPerfil(Long id,
                                    String nombre,
                                    String apellidos,
                                    String genero,
                                    String facultad,
                                    String seleccion,
                                    String passwordHash,
                                    String foto) {

        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));

        if (nombre    != null && !nombre.isBlank())    existente.setNombre(nombre);
        if (apellidos != null && !apellidos.isBlank()) existente.setApellidos(apellidos);
        if (genero    != null && !genero.isBlank())    existente.setGenero(genero);
        if (facultad  != null && !facultad.isBlank())  existente.setFacultad(facultad);
        if (seleccion != null && !seleccion.isBlank()) existente.setSeleccion(seleccion);
        if (foto      != null && !foto.isBlank())      existente.setFoto(foto);

        if (passwordHash != null && !passwordHash.isBlank()) {
            existente.setPasswordHash(passwordHash);
            existente.setUltimoCambioPass(LocalDateTime.now());
        }

        return usuarioRepository.saveAndFlush(existente);
    }

    // ── Actualización admin ───────────────────────────────────────────────────

    @Transactional
    public Usuario actualizarUsuario(Long id, Usuario datos,
                                     String nombreRol, Habitacion habitacion) {
        Usuario existente = buscarPorId(id);

        if (datos.getNombre()    != null) existente.setNombre(datos.getNombre());
        if (datos.getApellidos() != null) existente.setApellidos(datos.getApellidos());
        if (datos.getEmail()     != null) existente.setEmail(datos.getEmail());
        if (datos.getFacultad()  != null) existente.setFacultad(datos.getFacultad());
        if (datos.getGenero()    != null) existente.setGenero(datos.getGenero());
        if (datos.getSeleccion() != null) existente.setSeleccion(datos.getSeleccion());
        if (datos.getActivo()    != null) existente.setActivo(datos.getActivo());
        if (habitacion           != null) existente.setHabitacion(habitacion);

        if (nombreRol != null && !nombreRol.isBlank()) {
            Rol rol = rolRepositorio.findByNombre(nombreRol)
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + nombreRol));
            existente.setRol(rol);
        }

        if (datos.getPasswordHash() != null && !datos.getPasswordHash().isBlank()) {
            existente.setPasswordHash(passwordEncoder.encode(datos.getPasswordHash()));
            existente.setUltimoCambioPass(LocalDateTime.now());
        }

        return usuarioRepository.saveAndFlush(existente);
    }

    // ── Eliminación / desactivación ───────────────────────────────────────────

    @Transactional
    public void eliminarUsuario(Long id) {
        if (!usuarioRepository.existsById(id))
            throw new RuntimeException("Usuario no encontrado: " + id);
        usuarioRepository.deleteById(id);
    }

    @Transactional
    public void desactivarUsuario(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.setActivo(false);
        usuario.setDeletedAt(LocalDateTime.now());
        usuarioRepository.saveAndFlush(usuario);
    }

    // ── Expulsión definitiva ──────────────────────────────────────────────────
    // El residente queda bloqueado permanentemente:
    //   - activo = false  → no puede iniciar sesión
    //   - expulsado = true → no puede enviar nuevas solicitudes
    // Esta acción no es reversible desde el sistema.

    @Transactional
    public void expulsarUsuario(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.setActivo(false);
        usuario.setExpulsado(true);
        usuario.setDeletedAt(LocalDateTime.now());
        usuarioRepository.saveAndFlush(usuario);
    }

    // ── Registrar fecha de ingreso ────────────────────────────────────────────
    // Se llama desde SolicitudServicio.aprobar() cuando se crea la cuenta.
    // La fecha de ingreso se usa para calcular el límite de 4 años.

    @Transactional
    public void registrarFechaIngreso(Long id, LocalDate fecha) {
        Usuario usuario = buscarPorId(id);
        usuario.setFechaIngreso(fecha);
        usuarioRepository.saveAndFlush(usuario);
    }

    // ── Filtrado por tipo destinatario ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Usuario> filtrarPorTipo(String tipo, String valor) {
        return switch (tipo.toUpperCase()) {
            case "TODOS"              -> usuarioRepository.findByActivoTrue();
            case "GENERO"             -> usuarioRepository.findByGeneroAndActivoTrue(valor);
            case "FACULTAD"           -> usuarioRepository.findByFacultadAndActivoTrue(valor);
            case "SELECCION"          -> usuarioRepository.findBySeleccionAndActivoTrue(valor);
            case "EDIFICIO"           -> usuarioRepository.findByEdificioId(Long.parseLong(valor));
            case "HABITACION"         -> usuarioRepository.findByHabitacionId(Long.parseLong(valor));
            case "USUARIO_ESPECIFICO" -> usuarioRepository.findById(Long.parseLong(valor))
                    .map(List::of).orElse(List.of());
            default                   -> List.of();
        };
    }

    // ── Valores únicos para filtros ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<String> getFacultades() {
        return usuarioRepository.findDistinctFacultades();
    }

    @Transactional(readOnly = true)
    public List<String> getGeneros() {
        return usuarioRepository.findDistinctGeneros();
    }

    @Transactional(readOnly = true)
    public List<String> getSelecciones() {
        return usuarioRepository.findDistinctSelecciones();
    }
}