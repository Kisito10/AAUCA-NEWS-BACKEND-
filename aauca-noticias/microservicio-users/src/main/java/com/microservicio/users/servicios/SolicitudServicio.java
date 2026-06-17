package com.microservicio.users.servicios;

import com.microservicio.users.entidades.Habitacion;
import com.microservicio.users.entidades.Rol;
import com.microservicio.users.entidades.Solicitud;
import com.microservicio.users.entidades.Usuario;
import com.microservicio.users.repositorio.HabitacionRepositorio;
import com.microservicio.users.repositorio.RolRepositorio;
import com.microservicio.users.repositorio.SolicitudRepositorio;
import com.microservicio.users.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitudServicio {

    // Capacidad máxima por habitación — 2 personas
    private static final int CAPACIDAD_HABITACION = 2;

    private final SolicitudRepositorio  solicitudRepositorio;
    private final UsuarioRepository     usuarioRepositorio;
    private final RolRepositorio        rolRepositorio;
    private final HabitacionRepositorio habitacionRepositorio;
    private final EmailServicio         emailServicio;
    private final PasswordEncoder       passwordEncoder;

    // ─── Crear / Rectificar solicitud ─────────────────────────────────────────

    @Transactional
    public Solicitud crearSolicitud(Solicitud solicitud) {

        // Validar campos obligatorios
        if (solicitud.getNombre() == null || solicitud.getNombre().isBlank())
            throw new RuntimeException("El nombre es obligatorio");
        if (solicitud.getEmail() == null || solicitud.getEmail().isBlank())
            throw new RuntimeException("El email es obligatorio");

        String emailNorm = solicitud.getEmail().trim().toLowerCase();
        solicitud.setEmail(emailNorm);

        // Verificar si ya existe una cuenta registrada con ese email
        if (usuarioRepositorio.existsByEmail(emailNorm))
            throw new RuntimeException(
                    "Ya existe una cuenta registrada con ese email. " +
                            "Si olvidaste tu contraseña, contacta al Director.");

        // Verificar disponibilidad de la habitación elegida
        if (solicitud.getHabitacion() != null && !solicitud.getHabitacion().isBlank()) {
            long pendientesEnHabitacion = solicitudRepositorio
                    .contarPendientesPorHabitacion(solicitud.getHabitacion().trim());

            if (pendientesEnHabitacion >= CAPACIDAD_HABITACION)
                throw new RuntimeException(
                        "La habitación " + solicitud.getHabitacion() +
                                " ya no está disponible. Por favor elige otra habitación.");
        }

        // ── Rectificación: si tiene solicitud PENDIENTE, reemplazarla ─────────
        Optional<Solicitud> pendiente = solicitudRepositorio
                .findByEmailAndEstado(emailNorm, Solicitud.Estado.PENDIENTE);

        int numRectificaciones = 0;
        if (pendiente.isPresent()) {
            numRectificaciones = pendiente.get().getNumRectificaciones() + 1;
            solicitudRepositorio.delete(pendiente.get());
            solicitudRepositorio.flush();
            log.info("✏️ Rectificación #{} para {}", numRectificaciones, emailNorm);
        }

        solicitud.setEstado(Solicitud.Estado.PENDIENTE);
        solicitud.setNumRectificaciones(numRectificaciones);
        Solicitud guardada = solicitudRepositorio.save(solicitud);

        // Email de confirmación
        try {
            if (numRectificaciones > 0) {
                emailServicio.enviarConfirmacionRectificacion(
                        guardada.getEmail(), guardada.getNombre(), numRectificaciones);
            } else {
                emailServicio.enviarConfirmacionSolicitud(
                        guardada.getEmail(), guardada.getNombre());
            }
        } catch (Exception e) {
            log.error("Error al enviar email de confirmación a {}: {}",
                    guardada.getEmail(), e.getMessage());
        }

        return guardada;
    }

    // ─── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Solicitud> obtenerTodas() {
        return solicitudRepositorio.findAll();
    }

    @Transactional(readOnly = true)
    public List<Solicitud> obtenerPendientes() {
        return solicitudRepositorio.findByEstado(Solicitud.Estado.PENDIENTE);
    }

    // ─── NUEVO: disponibilidad de habitaciones por edificio ───────────────────
    // Devuelve la lista de habitaciones del edificio con su estado:
    //   disponible  → pendientes < 2  (se puede elegir)
    //   llena       → pendientes >= 2 (aparece bloqueada en el selector)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> disponibilidadPorEdificio(Long edificioId) {
        List<Habitacion> habitaciones = habitacionRepositorio
                .findByEdificioIdAndActivoTrueOrderByNumeroAsc(edificioId);

        return habitaciones.stream().map(h -> {
            long pendientes = solicitudRepositorio
                    .contarPendientesPorHabitacion(h.getNumero());
            boolean disponible = pendientes < CAPACIDAD_HABITACION;

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("id",          h.getId());
            info.put("numero",      h.getNumero());
            info.put("piso",        h.getPiso());
            info.put("edificioId",  h.getEdificioId());
            info.put("pendientes",  pendientes);
            info.put("capacidad",   CAPACIDAD_HABITACION);
            info.put("disponible",  disponible);
            // Texto descriptivo para el tooltip del selector
            info.put("estado", disponible
                    ? (pendientes == 0 ? "Libre" : "1 plaza ocupada")
                    : "Completa");
            return info;
        }).toList();
    }

    // ─── NUEVO: estado del email ──────────────────────────────────────────────
    // El frontend llama esto al salir del campo email para avisar al usuario
    // antes de que rellene todo el formulario.
    @Transactional(readOnly = true)
    public Map<String, Object> estadoEmail(String email) {
        Map<String, Object> res = new LinkedHashMap<>();

        // ¿Ya tiene cuenta?
        if (usuarioRepositorio.existsByEmail(email)) {
            res.put("tieneCuenta",    true);
            res.put("tienePendiente", false);
            res.put("mensaje",
                    "Este email ya tiene una cuenta activa. " +
                            "Inicia sesión o contacta al Director si olvidaste tu contraseña.");
            return res;
        }

        // ¿Tiene solicitud pendiente?
        boolean pendiente = solicitudRepositorio
                .existsByEmailAndEstado(email, Solicitud.Estado.PENDIENTE);

        res.put("tieneCuenta",    false);
        res.put("tienePendiente", pendiente);
        res.put("mensaje", pendiente
                ? "Ya tienes una solicitud pendiente de revisión. " +
                "Si envías esta nueva, la anterior se eliminará automáticamente."
                : "");
        return res;
    }

    // ─── Aprobar ──────────────────────────────────────────────────────────────

    @Transactional
    public void aprobar(Long id) {
        Solicitud solicitud = solicitudRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + id));

        if (solicitud.getEstado() != Solicitud.Estado.PENDIENTE)
            throw new RuntimeException("Solo se pueden aprobar solicitudes PENDIENTES");

        if (usuarioRepositorio.existsByEmail(solicitud.getEmail()))
            throw new RuntimeException("Ya existe una cuenta con ese email");

        Rol rolResidente = rolRepositorio.findByNombre("Residente")
                .orElseThrow(() -> new RuntimeException("Rol 'Residente' no encontrado en la BD"));

        Habitacion habitacion = null;
        if (solicitud.getHabitacion() != null && !solicitud.getHabitacion().isBlank()) {
            habitacion = habitacionRepositorio
                    .findByNumero(solicitud.getHabitacion().trim())
                    .orElse(null);
            if (habitacion == null)
                log.warn("Habitación '{}' no encontrada — usuario creado sin habitación",
                        solicitud.getHabitacion());
        }

        String password = "AAUCA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Usuario usuario = Usuario.builder()
                .nombre(solicitud.getNombre())
                .apellidos(solicitud.getApellidos())
                .email(solicitud.getEmail())
                .passwordHash(passwordEncoder.encode(password))
                .rol(rolResidente)
                .facultad(solicitud.getFacultad())
                .genero(solicitud.getGenero())
                .seleccion(solicitud.getSeleccion())
                .habitacion(habitacion)
                .activo(true)
                .coroIglesia(false)
                .tokenVersion(0)
                .intentosFallidos(0)
                .build();

        usuarioRepositorio.save(usuario);
        solicitud.setEstado(Solicitud.Estado.APROBADA);
        solicitudRepositorio.save(solicitud);

        try {
            emailServicio.enviarCredenciales(
                    solicitud.getEmail(), solicitud.getNombre(),
                    solicitud.getEmail(), password);
        } catch (Exception e) {
            log.error("Error al enviar credenciales a {}: {}", solicitud.getEmail(), e.getMessage());
        }
    }

    // ─── Rechazar ─────────────────────────────────────────────────────────────

    @Transactional
    public void rechazar(Long id) {
        Solicitud solicitud = solicitudRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + id));

        if (solicitud.getEstado() != Solicitud.Estado.PENDIENTE)
            throw new RuntimeException("Solo se pueden rechazar solicitudes PENDIENTES");

        solicitud.setEstado(Solicitud.Estado.RECHAZADA);
        solicitudRepositorio.save(solicitud);

        try {
            emailServicio.enviarRechazo(solicitud.getEmail(), solicitud.getNombre());
        } catch (Exception e) {
            log.error("Error al enviar rechazo a {}: {}", solicitud.getEmail(), e.getMessage());
        }
    }
}