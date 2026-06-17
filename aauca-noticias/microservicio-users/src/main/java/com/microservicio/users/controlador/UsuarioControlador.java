package com.microservicio.users.controlador;

import com.microservicio.users.entidades.Edificio;
import com.microservicio.users.entidades.Habitacion;
import com.microservicio.users.entidades.Usuario;
import com.microservicio.users.seguridad.JwtServicio;
import com.microservicio.users.servicios.EdificioServicio;
import com.microservicio.users.servicios.EmailServicio;
import com.microservicio.users.servicios.HabitacionServicio;
import com.microservicio.users.servicios.UsuarioServicios;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UsuarioControlador {

    private final UsuarioServicios   usuarioServicios;
    private final JwtServicio        jwtServicio;
    private final EdificioServicio   edificioServicio;
    private final HabitacionServicio habitacionServicio;
    private final EmailServicio      emailServicio;

    @Value("${app.upload.dir:uploads/fotos/}")
    private String uploadDir;

    // ── Helper ────────────────────────────────────────────────────────────────

    private Map<String, Object> usuarioAMapa(Usuario u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",           u.getId());
        m.put("nombre",       u.getNombre());
        m.put("apellidos",    u.getApellidos());
        m.put("email",        u.getEmail());
        m.put("rol",          u.getRol() != null ? u.getRol().getNombre() : null);
        m.put("activo",       u.getActivo());
        m.put("expulsado",    u.getExpulsado());
        m.put("fechaIngreso", u.getFechaIngreso() != null
                ? u.getFechaIngreso().toString() : null);
        m.put("genero",       u.getGenero());
        m.put("facultad",     u.getFacultad());
        m.put("seleccion",    u.getSeleccion());
        m.put("foto",         u.getFoto());
        m.put("habitacionId", u.getHabitacionId());
        m.put("edificioId",   u.getHabitacion() != null
                ? u.getHabitacion().getEdificioId() : null);
        m.put("habitacion",   u.getHabitacion() != null
                ? Map.of(
                "id",         u.getHabitacion().getId(),
                "numero",     u.getHabitacion().getNumero(),
                "piso",       u.getHabitacion().getPiso() != null
                        ? u.getHabitacion().getPiso() : 0,
                "edificioId", u.getHabitacion().getEdificioId()
        )
                : null);
        return m;
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email    = credentials.get("email");
        String password = credentials.get("password");
        try {
            Usuario usuario = usuarioServicios.buscarPorEmail(email);

            if (usuario == null)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Credenciales incorrectas."));

            if (Boolean.TRUE.equals(usuario.getExpulsado()))
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message",
                                "Tu acceso ha sido revocado definitivamente. " +
                                        "Contacta con la administración si crees que es un error."));

            String nombreRol = usuario.getRol() != null
                    ? usuario.getRol().getNombre() : "Residente";
            boolean esResidente = "Residente".equalsIgnoreCase(nombreRol);

            if (esResidente && usuario.haSuperadoLimiteAnios()) {
                if (Boolean.TRUE.equals(usuario.getActivo())) {
                    usuarioServicios.desactivarUsuario(usuario.getId());
                    try {
                        emailServicio.enviarNotificacionBloqueoAnios(
                                usuario.getEmail(), usuario.getNombre());
                    } catch (Exception ignored) {}
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message",
                                "Tu acceso ha sido desactivado al completar 4 años " +
                                        "en la residencia. Contacta con la administración."));
            }

            if (Boolean.FALSE.equals(usuario.getActivo()))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message",
                                "Tu cuenta está desactivada. " +
                                        "Contacta con la administración."));

            if (!usuarioServicios.verificarPassword(password, usuario.getPasswordHash()))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Contraseña incorrecta."));

            String token = jwtServicio.generarToken(
                    usuario.getId(), usuario.getNombre(),
                    usuario.getEmail(), nombreRol);

            return ResponseEntity.ok(Map.of("token", token, "user", usuarioAMapa(usuario)));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    // ── Listar ────────────────────────────────────────────────────────────────

    @GetMapping("/listar")
    public ResponseEntity<?> listarUsuarios() {
        try {
            return ResponseEntity.ok(
                    usuarioServicios.listarUsuarios().stream()
                            .map(this::usuarioAMapa).toList());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    // ── GET por ID ────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerUsuario(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(usuarioAMapa(usuarioServicios.buscarPorId(id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado"));
        }
    }

    // ── Servir foto de perfil (sin auth) ──────────────────────────────────────

    @GetMapping("/foto/{filename:.+}")
    public ResponseEntity<Resource> servirFoto(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable())
                return ResponseEntity.notFound().build();
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";
            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Crear usuario ─────────────────────────────────────────────────────────

    @PostMapping("/crear")
    public ResponseEntity<?> crearUsuario(@RequestBody Map<String, Object> body) {
        try {
            Usuario nuevo = new Usuario();
            nuevo.setNombre((String) body.get("nombre"));
            nuevo.setApellidos((String) body.get("apellidos"));
            nuevo.setEmail((String) body.get("email"));
            nuevo.setPasswordHash((String) body.get("password"));
            nuevo.setGenero((String) body.get("genero"));
            nuevo.setFacultad((String) body.get("facultad"));
            nuevo.setSeleccion((String) body.get("seleccion"));

            Object activoObj = body.get("activo");
            nuevo.setActivo(activoObj == null || Boolean.parseBoolean(activoObj.toString()));

            String rolNombre = body.get("rolNombre") != null
                    ? (String) body.get("rolNombre") : "Residente";

            Object habId = body.get("habitacionId");
            if (habId != null) {
                Habitacion h = habitacionServicio.buscarPorId(Long.parseLong(habId.toString()));
                nuevo.setHabitacion(h);
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(usuarioAMapa(usuarioServicios.crearUsuario(nuevo, rolNombre)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al crear usuario: " + e.getMessage()));
        }
    }

    // ── Actualizar usuario (admin) ────────────────────────────────────────────

    @PutMapping("/actualizar/{id}")
    public ResponseEntity<?> actualizarUsuario(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            Usuario datos = new Usuario();
            datos.setNombre((String) body.get("nombre"));
            datos.setApellidos((String) body.get("apellidos"));
            datos.setEmail((String) body.get("email"));
            datos.setGenero((String) body.get("genero"));
            datos.setFacultad((String) body.get("facultad"));
            datos.setSeleccion((String) body.get("seleccion"));

            Object activoObj = body.get("activo");
            if (activoObj != null)
                datos.setActivo(Boolean.parseBoolean(activoObj.toString()));

            Object pwObj = body.get("password");
            if (pwObj != null && !pwObj.toString().isBlank())
                datos.setPasswordHash(pwObj.toString());

            String rolNombre = (String) body.get("rolNombre");

            Habitacion habitacion = null;
            Object habId = body.get("habitacionId");
            if (habId != null)
                habitacion = habitacionServicio.buscarPorId(Long.parseLong(habId.toString()));

            return ResponseEntity.ok(usuarioAMapa(
                    usuarioServicios.actualizarUsuario(id, datos, rolNombre, habitacion)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al actualizar usuario: " + e.getMessage()));
        }
    }

    // ── Actualizar perfil propio (multipart con foto) ─────────────────────────

    @PutMapping(value = "/perfil/{id}", consumes = "multipart/form-data")
    public ResponseEntity<?> actualizarPerfil(
            @PathVariable Long id,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellidos,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) String facultad,
            @RequestParam(required = false) String seleccion,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) MultipartFile foto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer "))
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Token no proporcionado"));

            Long userIdFromToken = jwtServicio.extraerId(authHeader.substring(7));
            if (!userIdFromToken.equals(id))
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "No autorizado"));

            String fotoUrl      = (foto != null && !foto.isEmpty()) ? guardarFoto(foto, id) : null;
            String passwordHash = (password != null && !password.isBlank())
                    ? usuarioServicios.generarHash(password) : null;

            Usuario actualizado = usuarioServicios.actualizarPerfil(
                    id, nombre, apellidos, genero, facultad, seleccion, passwordHash, fotoUrl);

            return ResponseEntity.ok(usuarioAMapa(actualizado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al actualizar perfil: " + e.getMessage()));
        }
    }

    // ── Desactivar ────────────────────────────────────────────────────────────

    @PatchMapping("/desactivar/{id}")
    public ResponseEntity<?> desactivarUsuario(@PathVariable Long id) {
        try {
            usuarioServicios.desactivarUsuario(id);
            return ResponseEntity.ok(Map.of("message", "Usuario desactivado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    // ── Expulsar definitivamente ──────────────────────────────────────────────

    @PatchMapping("/expulsar/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<?> expulsarUsuario(@PathVariable Long id) {
        try {
            Usuario usuario = usuarioServicios.buscarPorId(id);
            String rol = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
            if (!"Residente".equalsIgnoreCase(rol))
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Solo se puede expulsar a residentes."));

            usuarioServicios.expulsarUsuario(id);
            try {
                emailServicio.enviarNotificacionExpulsion(
                        usuario.getEmail(), usuario.getNombre());
            } catch (Exception ignored) {}

            return ResponseEntity.ok(Map.of(
                    "message", "Residente expulsado definitivamente.",
                    "usuario", usuarioAMapa(usuarioServicios.buscarPorId(id))
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    // ── Eliminar ──────────────────────────────────────────────────────────────

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        try {
            usuarioServicios.eliminarUsuario(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    // ── Valores filtro ────────────────────────────────────────────────────────

    @GetMapping("/valores-filtro")
    public ResponseEntity<?> valoresFiltro() {
        try {
            return ResponseEntity.ok(Map.of(
                    "facultades",  usuarioServicios.getFacultades(),
                    "generos",     usuarioServicios.getGeneros(),
                    "selecciones", usuarioServicios.getSelecciones()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    // ── Emails por tipo de destinatario ───────────────────────────────────────
    // Llamado desde microservicio-noticias al publicar una noticia.
    // Devuelve [{email, nombre}] de los residentes activos del tipo indicado.
    // Endpoint interno — no requiere token pero solo accesible desde la red local.

    @GetMapping("/emails-destinatario")
    public ResponseEntity<?> emailsPorTipoDestinatario(
            @RequestParam String tipo,
            @RequestParam(required = false) String valor) {
        try {
            List<Usuario> usuarios = usuarioServicios.filtrarPorTipo(
                    tipo != null ? tipo.toUpperCase() : "TODOS",
                    valor);

            List<Map<String, String>> resultado = usuarios.stream()
                    .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                    .filter(u -> !Boolean.TRUE.equals(u.getExpulsado()))
                    .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                    .map(u -> Map.of(
                            "email",  u.getEmail(),
                            "nombre", u.getNombre() != null ? u.getNombre() : "Residente"
                    ))
                    .toList();

            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    // ── Edificios ─────────────────────────────────────────────────────────────

    @GetMapping("/edificios")
    public ResponseEntity<List<Edificio>> getEdificios() {
        return ResponseEntity.ok(edificioServicio.listarActivos());
    }

    @GetMapping("/edificios/todos")
    public ResponseEntity<List<Edificio>> todosEdificios() {
        return ResponseEntity.ok(edificioServicio.listarTodos());
    }

    @PostMapping("/edificios")
    public ResponseEntity<?> crearEdificio(@RequestBody Edificio edificio) {
        try {
            return ResponseEntity.ok(edificioServicio.guardar(edificio));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/edificios/{id}")
    public ResponseEntity<?> actualizarEdificio(
            @PathVariable Long id, @RequestBody Edificio datos) {
        try {
            return ResponseEntity.ok(edificioServicio.actualizar(id, datos));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/edificios/{id}/toggle")
    public ResponseEntity<?> toggleEdificio(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(edificioServicio.toggle(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ── Habitaciones ──────────────────────────────────────────────────────────

    @GetMapping("/habitaciones/{edificioId}")
    public ResponseEntity<List<Habitacion>> getHabitaciones(@PathVariable Long edificioId) {
        return ResponseEntity.ok(habitacionServicio.listarPorEdificio(edificioId));
    }

    // ── Generar hash ──────────────────────────────────────────────────────────

    @GetMapping("/generar-hash")
    public String generarHash(@RequestParam String password) {
        return usuarioServicios.generarHash(password);
    }

    // ── Auxiliar: guardar foto ────────────────────────────────────────────────

    private String guardarFoto(MultipartFile file, Long userId) throws IOException {
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/"))
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        if (file.getSize() > 3 * 1024 * 1024)
            throw new IllegalArgumentException("La foto no debe superar 3 MB");

        String orig     = file.getOriginalFilename();
        String ext      = (orig != null && orig.contains("."))
                ? orig.substring(orig.lastIndexOf(".")) : ".jpg";
        String fileName = "user_" + userId + "_"
                + UUID.randomUUID().toString().substring(0, 8) + ext;

        Path path = Paths.get(uploadDir);
        if (!Files.exists(path)) Files.createDirectories(path);
        Files.copy(file.getInputStream(), path.resolve(fileName),
                StandardCopyOption.REPLACE_EXISTING);

        return "/api/user/foto/" + fileName;
    }
}