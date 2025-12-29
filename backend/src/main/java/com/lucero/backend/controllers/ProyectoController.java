package com.lucero.backend.controllers;

import com.lucero.backend.models.Programador;
import com.lucero.backend.models.Proyecto;
import com.lucero.backend.models.Usuario;
import com.lucero.backend.repositories.ProgramadorRepository;
import com.lucero.backend.repositories.ProyectoRepository;
import com.lucero.backend.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/proyectos")
@CrossOrigin(origins = "*")
public class ProyectoController {

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProgramadorRepository programadorRepository;

    // --- MÉTODO AUXILIAR PARA OBTENER EL PROGRAMADOR LOGUEADO ---
    private Programador obtenerProgramadorActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return programadorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RuntimeException("No tienes perfil de programador creado."));
    }

    // 1. OBTENER TODOS (Público o restringido según tu SecurityConfig)
    @GetMapping
    public List<Proyecto> obtenerTodos() {
        return proyectoRepository.findAll();
    }

    // 2. OBTENER UN PROYECTO POR ID
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerUno(@PathVariable UUID id) {
        return proyectoRepository.findById(id)
                .map(ResponseEntity::ok) // Si existe, devuelve 200 OK y el proyecto
                .orElse(ResponseEntity.notFound().build()); // Si no, 404 Not Found
    }

    // 3. CREAR PROYECTO (Asignado al Programador logueado)
    @PostMapping
    public ResponseEntity<?> crearProyecto(@RequestBody Proyecto proyecto) {
        try {
            Programador programador = obtenerProgramadorActual(); // Usamos el método auxiliar

            proyecto.setProgramador(programador);
            proyecto.setCreadoEn(LocalDateTime.now());
            // Si el estado viene nulo, ponemos activo por defecto
            if (proyecto.getEstado() == null)
                proyecto.setEstado("activo");

            return ResponseEntity.ok(proyectoRepository.save(proyecto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 4. ACTUALIZAR PROYECTO (¡Con seguridad de propiedad!)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarProyecto(@PathVariable UUID id, @RequestBody Proyecto detalles) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElse(null);

        if (proyecto == null) {
            return ResponseEntity.notFound().build();
        }

        // VERIFICACIÓN DE SEGURIDAD:
        // ¿El usuario que intenta editar es el dueño del proyecto?
        Programador programadorActual = obtenerProgramadorActual();
        if (!proyecto.getProgramador().getId().equals(programadorActual.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tienes permiso para editar este proyecto.");
        }

        // Actualizamos campos
        proyecto.setTitulo(detalles.getTitulo());
        proyecto.setDescripcion(detalles.getDescripcion());
        proyecto.setTecnologias(detalles.getTecnologias());
        proyecto.setUrlRepo(detalles.getUrlRepo());
        proyecto.setUrlDemo(detalles.getUrlDemo());

        if (detalles.getEstado() != null) {
            proyecto.setEstado(detalles.getEstado());
        }

        return ResponseEntity.ok(proyectoRepository.save(proyecto));
    }

    // 5. ELIMINAR PROYECTO (¡Con seguridad de propiedad!)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProyecto(@PathVariable UUID id) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElse(null);

        if (proyecto == null) {
            return ResponseEntity.notFound().build();
        }

        // VERIFICACIÓN DE SEGURIDAD
        Programador programadorActual = obtenerProgramadorActual();
        if (!proyecto.getProgramador().getId().equals(programadorActual.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tienes permiso para eliminar este proyecto.");
        }

        proyectoRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // 6. OBTENER PORTAFOLIO DE UN PROGRAMADOR ESPECÍFICO
    @GetMapping("/programador/{id}")
    public List<Proyecto> obtenerPorProgramador(@PathVariable UUID id) {
        return proyectoRepository.findByProgramadorId(id);
    }
}