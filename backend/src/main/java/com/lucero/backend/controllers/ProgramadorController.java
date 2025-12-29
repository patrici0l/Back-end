package com.lucero.backend.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucero.backend.dto.ProgramadorPublicoDTO;
import com.lucero.backend.models.Programador;
import com.lucero.backend.models.Usuario; // Asegúrate de importar esto
import com.lucero.backend.repositories.ProgramadorRepository;
import com.lucero.backend.repositories.UsuarioRepository; // Necesitas este repo
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/programadores")
@CrossOrigin(origins = "*")
public class ProgramadorController {

    @Autowired
    private ProgramadorRepository programadorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository; // ✅ NECESARIO para crear el usuario base

    @Autowired
    private ObjectMapper objectMapper;

    // --- GET METHODS (Igual que antes) ---
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
            // 1. GESTIÓN DEL ARCHIVO (FOTO)
            String urlFoto = null;
            if (file != null && !file.isEmpty()) {
                // AQUÍ VA TU LÓGICA DE SUBIDA REAL.
                // Por ahora simulamos una URL si no tienes servicio de storage configurado
                urlFoto = "https://ui-avatars.com/api/?name=" + nombre.replace(" ", "+");
                // TODO: Reemplazar línea anterior con tu fileService.subir(file)
            }

            // 2. CREAR EL USUARIO ASOCIADO (Obligatorio por tu modelo)
            Usuario usuario = new Usuario();
            usuario.setNombre(nombre);
            usuario.setEmail(emailContacto != null ? emailContacto : "temp_" + UUID.randomUUID() + "@sistema.com");
            usuario.setPasswordHash("123456"); // Contraseña por defecto o generada
            usuario.setRol("PROGRAMADOR");
            usuario.setActivo(true);
            usuario.setFotoUrl(urlFoto);

            usuario = usuarioRepository.save(usuario); // Guardamos el usuario primero

            // 3. CREAR EL PROGRAMADOR Y VINCULARLO
            Programador p = new Programador();
            p.setUsuario(usuario); // <--- VINCULACIÓN IMPORTANTE
            p.setEspecialidad(especialidad);
            p.setDescripcion(descripcion);
            p.setEmailContacto(emailContacto);
            p.setGithub(github);
            p.setLinkedin(linkedin);
            p.setPortafolio(portafolio);
            p.setWhatsapp(whatsapp);
            p.setDisponibilidadTexto(disponibilidad);

            // Convertir horas JSON a List
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
            @RequestParam("nombre") String nombre, // Viene del form, actualizamos el Usuario
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

            // 1. ACTUALIZAR DATOS PROPIOS DEL PROGRAMADOR
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

            // 2. ACTUALIZAR DATOS DEL USUARIO VINCULADO (Nombre y Foto)
            Usuario u = p.getUsuario();
            if (u != null) {
                u.setNombre(nombre);

                if (file != null && !file.isEmpty()) {
                    // TODO: Reemplazar con tu lógica de subida real
                    String nuevaUrl = "https://ui-avatars.com/api/?name=" + nombre;
                    u.setFotoUrl(nuevaUrl);
                }
                usuarioRepository.save(u); // Guardar cambios del usuario
            }

            return ResponseEntity.ok(programadorRepository.save(p));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al editar: " + e.getMessage());
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
                p.getDisponibilidadTexto(), // Ahora sí existe en el modelo
                p.getHorasDisponibles(), // Ahora sí existe en el modelo
                p.getUsuario().getId());
    }
}