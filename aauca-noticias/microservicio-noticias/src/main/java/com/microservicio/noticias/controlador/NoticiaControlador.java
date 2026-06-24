package com.microservicio.noticias.controlador;

import com.microservicio.noticias.entidades.Noticia;
import com.microservicio.noticias.seguridad.JwtUtil;
import com.microservicio.noticias.servicios.NoticiaServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/noticia")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "https://aauca-news.vercel.app"})
public class NoticiaControlador {

    private final NoticiaServicio noticiaServicio;
    private final JwtUtil         jwtUtil;

    @Value("${app.upload.dir:uploads/noticias/}")
    private String uploadDir;

    private String[] extraerDatosJwt(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return new String[]{"celador", "sistema"};
        String token = authHeader.substring(7);
        if (!jwtUtil.esValido(token))
            return new String[]{"celador", "sistema"};
        return new String[]{ jwtUtil.extraerRol(token), jwtUtil.extraerNombre(token) };
    }

    @PostMapping(value = "/crear", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> crearNoticiaJson(
            @RequestBody Noticia noticia,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String[] datos = extraerDatosJwt(authHeader);
            if (noticia.getImagen() != null && noticia.getImagen().startsWith("data:image"))
                noticia.setImagen(saveBase64Image(noticia.getImagen()));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(noticiaServicio.crearNoticia(noticia, datos[0], datos[1]));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al crear la noticia: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/crear", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crearNoticiaMultipart(
            @RequestParam("titulo")                                        String titulo,
            @RequestParam("descripcion")                                   String descripcion,
            @RequestParam(value = "seccionId",   required = false)         Long seccionId,
            @RequestParam(value = "usuarioId",   required = false)         Long usuarioId,
            @RequestParam(value = "prioridad",   required = false,
                    defaultValue = "NORMAL")                               String prioridad,
            @RequestParam(value = "estado",      required = false,
                    defaultValue = "BORRADOR")                             String estado,
            @RequestParam(value = "imagen",      required = false)         MultipartFile imagen,
            @RequestParam(value = "imagenUrl",   required = false)         String imagenUrl,
            @RequestHeader(value = "Authorization", required = false)      String authHeader) {
        try {
            String[] datos = extraerDatosJwt(authHeader);
            Noticia noticia = new Noticia();
            noticia.setTitulo(titulo);
            noticia.setDescripcion(descripcion);
            noticia.setSeccionId(seccionId);
            noticia.setUsuarioId(usuarioId);
            noticia.setEstado(estado);
            try {
                noticia.setPrioridad(Noticia.Prioridad.valueOf(prioridad.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                noticia.setPrioridad(Noticia.Prioridad.NORMAL);
            }
            if (imagen != null && !imagen.isEmpty())
                noticia.setImagen(saveMultipartFile(imagen));
            else if (imagenUrl != null && !imagenUrl.isBlank())
                noticia.setImagen(imagenUrl);

            Noticia saved = noticiaServicio.crearNoticia(noticia, datos[0], datos[1]);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Noticia creada", "noticia", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/actualizar/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> actualizarNoticiaJson(
            @PathVariable Long id,
            @RequestBody Noticia noticia,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String[] datos = extraerDatosJwt(authHeader);
            if (noticia.getImagen() != null && noticia.getImagen().startsWith("data:image"))
                noticia.setImagen(saveBase64Image(noticia.getImagen()));
            return ResponseEntity.ok(noticiaServicio.actualizarNoticia(id, noticia, datos[0]));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping(value = "/actualizar/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> actualizarNoticiaMultipart(
            @PathVariable Long id,
            @RequestParam("titulo")                                        String titulo,
            @RequestParam("descripcion")                                   String descripcion,
            @RequestParam(value = "seccionId",   required = false)         Long seccionId,
            @RequestParam(value = "prioridad",   required = false,
                    defaultValue = "NORMAL")                               String prioridad,
            @RequestParam(value = "estado",      required = false,
                    defaultValue = "BORRADOR")                             String estado,
            @RequestParam(value = "imagen",      required = false)         MultipartFile imagen,
            @RequestParam(value = "imagenUrl",   required = false)         String imagenUrl,
            @RequestHeader(value = "Authorization", required = false)      String authHeader) {
        try {
            String[] datos = extraerDatosJwt(authHeader);
            Noticia datos_noticia = new Noticia();
            datos_noticia.setTitulo(titulo);
            datos_noticia.setDescripcion(descripcion);
            datos_noticia.setSeccionId(seccionId);
            datos_noticia.setEstado(estado);
            try {
                datos_noticia.setPrioridad(Noticia.Prioridad.valueOf(prioridad.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                datos_noticia.setPrioridad(Noticia.Prioridad.NORMAL);
            }
            if (imagen != null && !imagen.isEmpty())
                datos_noticia.setImagen(saveMultipartFile(imagen));
            else if (imagenUrl != null && !imagenUrl.isBlank())
                datos_noticia.setImagen(imagenUrl);

            Noticia saved = noticiaServicio.actualizarNoticia(id, datos_noticia, datos[0]);
            return ResponseEntity.ok(Map.of("message", "Noticia actualizada", "noticia", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @PatchMapping(value = "/actualizar/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> patchNoticiaEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> cambios,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String[] datos = extraerDatosJwt(authHeader);
            Noticia parcial = new Noticia();
            if (cambios.containsKey("estado")) parcial.setEstado(cambios.get("estado"));
            return ResponseEntity.ok(noticiaServicio.actualizarNoticia(id, parcial, datos[0]));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/publicar/{id}")
    public ResponseEntity<?> publicarNoticia(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String[] datos = extraerDatosJwt(authHeader);
            return ResponseEntity.ok(Map.of(
                    "message", "Noticia publicada",
                    "noticia", noticiaServicio.publicarNoticia(id, datos[0], datos[1])));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/autorizar/{id}")
    public ResponseEntity<?> autorizarNoticia(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String[] datos = extraerDatosJwt(authHeader);
            return ResponseEntity.ok(Map.of(
                    "message", "Noticia autorizada y publicada",
                    "noticia", noticiaServicio.autorizarNoticia(id, datos[0], datos[1])));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/listar")
    public ResponseEntity<List<Noticia>> listarNoticias() {
        return ResponseEntity.ok(noticiaServicio.listarTodas());
    }

    @GetMapping("/listar/publicadas")
    public ResponseEntity<List<Noticia>> listarPublicadas() {
        return ResponseEntity.ok(noticiaServicio.listarPublicadas());
    }

    @GetMapping("/listar/urgentes")
    public ResponseEntity<List<Noticia>> listarUrgentes() {
        return ResponseEntity.ok(noticiaServicio.listarUrgentes());
    }

    @GetMapping("/listar/destacadas")
    public ResponseEntity<List<Noticia>> listarDestacadas() {
        return ResponseEntity.ok(noticiaServicio.listarDestacadas());
    }

    @GetMapping("/listar/para-usuario")
    public ResponseEntity<List<Noticia>> listarParaUsuario(
            @RequestParam                       Long   usuarioId,
            @RequestParam(required = false)     String genero,
            @RequestParam(required = false)     String facultad,
            @RequestParam(required = false)     String seleccion,
            @RequestParam(required = false)     Long   habitacionId,
            @RequestParam(required = false)     Long   edificioId) {
        return ResponseEntity.ok(
                noticiaServicio.listarPublicadasParaUsuario(
                        usuarioId, genero, facultad, seleccion, habitacionId, edificioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(noticiaServicio.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<?> eliminarNoticia(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String[] datos = extraerDatosJwt(authHeader);
            noticiaServicio.eliminarNoticia(id, datos[0]);
            return ResponseEntity.ok(Map.of("message", "Noticia eliminada"));
        } catch (IllegalStateException e) {
            // ✅ Noticia con lecturas — mensaje claro
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/archivar/{id}")
    public ResponseEntity<?> archivarNoticia(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String[] datos = extraerDatosJwt(authHeader);
            noticiaServicio.archivarNoticia(id, datos[0]);
            return ResponseEntity.ok(Map.of("message", "Noticia archivada"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    private String saveBase64Image(String base64Image) throws IOException {
        String[] parts = base64Image.split(",");
        if (parts.length < 2) throw new IllegalArgumentException("Formato base64 inválido");
        String ext = ".jpg";
        if (parts[0].contains("png"))       ext = ".png";
        else if (parts[0].contains("gif"))  ext = ".gif";
        else if (parts[0].contains("webp")) ext = ".webp";
        byte[] bytes = java.util.Base64.getDecoder().decode(parts[1]);
        if (bytes.length > 5 * 1024 * 1024)
            throw new IllegalArgumentException("La imagen no debe superar 5 MB");
        String fileName = UUID.randomUUID() + ext;
        Path path = Paths.get(uploadDir);
        if (!Files.exists(path)) Files.createDirectories(path);
        Files.write(path.resolve(fileName), bytes);
        return "/" + uploadDir + fileName;
    }

    private String saveMultipartFile(MultipartFile file) throws IOException {
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/"))
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        if (file.getSize() > 5 * 1024 * 1024)
            throw new IllegalArgumentException("La imagen no debe superar 5 MB");
        String orig = file.getOriginalFilename();
        String ext  = (orig != null && orig.contains("."))
                ? orig.substring(orig.lastIndexOf(".")) : ".jpg";
        String fileName = UUID.randomUUID() + ext;
        Path path = Paths.get(uploadDir);
        if (!Files.exists(path)) Files.createDirectories(path);
        Files.copy(file.getInputStream(), path.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        return "/" + uploadDir + fileName;
    }
}