package com.lucero.backend.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucero.backend.dto.ProgramadorPublicoDTO;
import com.lucero.backend.models.Programador;
import com.lucero.backend.models.Usuario;
import com.lucero.backend.repositories.ProgramadorRepository;
import com.lucero.backend.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap; // ✅ Import necesario
import java.util.List;
import java.util.Map; // ✅ Import necesario
import java.util.UUID;

@RestController
@RequestMapping("/api/programadores")
@CrossOrigin(origins = "*")
public class ProgramadorController {

    @Autowired
    private ProgramadorRepository programadorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // --- GET METHODS ---
    @GetMapping
    public List<ProgramadorPublicoDTO> obtenerTodos() {
        return programadorRepository.findAll().stream().map(this::convertirADTO).toList();
    }

    @GetMapping("/{id}")
    public ProgramadorPublicoDTO obtenerUno(@PathVariable UUID id) {
        Programador p = programadorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No encontrado"));
        return convertirADTO(p);
    }

    // --- CREAR (POST) ---
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crearProgramador(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("nombre") String nombre,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("especialidad") String especialidad,
            @RequestParam(value = "emailContacto", required = false) String emailContacto,
            @RequestParam(value = "github", required = false) String github,
            @RequestParam(value = "linkedin", required = false) String linkedin,
            @RequestParam(value = "portafolio", required = false) String portafolio,
            @RequestParam(value = "whatsapp", required = false) String whatsapp,
            @RequestParam(value = "disponibilidad", required = false) String disponibilidad,
            @RequestParam(value = "horasDisponibles", required = false) String horasJson) {
        try {
            // 1. VALIDACIÓN
            String emailReal = (emailContacto != null && !emailContacto.isBlank())
                    ? emailContacto
                    : "temp_" + UUID.randomUUID() + "@sistema.com";

            if (usuarioRepository.findByEmail(emailReal).isPresent()) {
                return ResponseEntity.badRequest().body("Error: El email " + emailReal + " ya está registrado.");
            }

            // 2. FOTO
            String urlFoto = null;
            if (file != null && !file.isEmpty()) {
                urlFoto = "https://ui-avatars.com/api/?name=" + nombre.replace(" ", "+");
            }

            // 3. USUARIO
            Usuario usuario = new Usuario();
            usuario.setNombre(nombre);
            usuario.setEmail(emailReal);
            usuario.setPasswordHash("123456");
            usuario.setRol("programador");
            usuario.setActivo(true);
            usuario.setFotoUrl(urlFoto);

            usuario = usuarioRepository.save(usuario);

            // 4. PROGRAMADOR
            Programador p = new Programador();
            p.setUsuario(usuario);
            p.setEspecialidad(especialidad);
            p.setDescripcion(descripcion);
            p.setEmailContacto(emailContacto);
            p.setGithub(github);
            p.setLinkedin(linkedin);
            p.setPortafolio(portafolio);
            p.setWhatsapp(whatsapp);
            p.setDisponibilidadTexto(disponibilidad);

            if (horasJson != null && !horasJson.isEmpty() && !horasJson.equals("undefined")) {
                List<String> horas = objectMapper.readValue(horasJson, new TypeReference<List<String>>() {
                });
                p.setHorasDisponibles(horas);
            } else {
                p.setHorasDisponibles(Collections.emptyList());
            }

            return ResponseEntity.ok(programadorRepository.save(p));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error al crear: " + e.getMessage());
        }
    }

    // --- ACTUALIZAR (PUT) ---
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> actualizarProgramador(
            @PathVariable UUID id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("nombre") String nombre,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("especialidad") String especialidad,
            @RequestParam(value = "emailContacto", required = false) String emailContacto,
            @RequestParam(value = "github", required = false) String github,
            @RequestParam(value = "linkedin", required = false) String linkedin,
            @RequestParam(value = "portafolio", required = false) String portafolio,
            @RequestParam(value = "whatsapp", required = false) String whatsapp,
            @RequestParam(value = "disponibilidad", required = false) String disponibilidad,
            @RequestParam(value = "horasDisponibles", required = false) String horasJson) {
        try {
            Programador p = programadorRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("No existe"));

            p.setDescripcion(descripcion);
            p.setEspecialidad(especialidad);
            p.setEmailContacto(emailContacto);
            p.setGithub(github);
            p.setLinkedin(linkedin);
            p.setPortafolio(portafolio);
            p.setWhatsapp(whatsapp);
            p.setDisponibilidadTexto(disponibilidad);

            if (horasJson != null && !horasJson.isEmpty() && !horasJson.equals("undefined")) {
                List<String> horas = objectMapper.readValue(horasJson, new TypeReference<List<String>>() {
                });
                p.setHorasDisponibles(horas);
            }

            Usuario u = p.getUsuario();
            if (u != null) {
                u.setNombre(nombre);
                if (file != null && !file.isEmpty()) {
                    String nuevaUrl = "https://ui-avatars.com/api/?name=" + nombre;
                    u.setFotoUrl(nuevaUrl);
                }
                usuarioRepository.save(u);
            }

            return ResponseEntity.ok(programadorRepository.save(p));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al editar: " + e.getMessage());
        }
    }

    // --- ✅ ELIMINAR (DELETE) CORREGIDO ---
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProgramador(@PathVariable UUID id) {
        try {
            // 1. Buscar
            Programador p = programadorRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("No existe el programador"));

            Usuario u = p.getUsuario();

            // 2. Borrar (La BD con CASCADE se encarga de los hijos)
            programadorRepository.delete(p);

            // 3. Borrar Usuario
            if (u != null) {
                usuarioRepository.delete(u);
            }

            // ✅ CORRECCIÓN CRÍTICA: Devolver JSON, no String plano
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Programador eliminado correctamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Si hay error, también devolvemos JSON para ser consistentes
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error al eliminar: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // --- CONVERSOR DTO ---
    private ProgramadorPublicoDTO convertirADTO(Programador p) {
        String nombre = (p.getUsuario() != null) ? p.getUsuario().getNombre() : "Sin Nombre";
        String foto = (p.getUsuario() != null) ? p.getUsuario().getFotoUrl() : null;

        return new ProgramadorPublicoDTO(
                p.getId(),
                nombre,
                foto,
                p.getEspecialidad(),
                p.getDescripcion(),
                p.getDisponibilidadTexto(),
                p.getHorasDisponibles(),
                p.getUsuario().getId());
    }
}