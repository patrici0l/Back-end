package com.lucero.backend.controllers;

import com.lucero.backend.models.Asesoria;
import com.lucero.backend.models.Programador;
import com.lucero.backend.models.Usuario;
import com.lucero.backend.repositories.AsesoriaRepository;
import com.lucero.backend.repositories.ProgramadorRepository;
import com.lucero.backend.repositories.UsuarioRepository;
import com.lucero.backend.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/asesorias")
@CrossOrigin(origins = "*")
public class AsesoriaController {

    @Autowired
    private AsesoriaRepository asesoriaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ProgramadorRepository programadorRepository;
    @Autowired
    private EmailService emailService;

    // --- MÉTODOS DE APOYO ---

    private Usuario obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private Programador obtenerProgramadorActual() {
        Usuario u = obtenerUsuarioActual();
        return programadorRepository.findByUsuarioId(u.getId())
                .orElseThrow(() -> new RuntimeException("No tienes perfil de programador"));
    }

    @PostMapping("/publica")
    public ResponseEntity<?> crearPublica(@RequestBody Map<String, Object> body) {
        try {
            // 1. Validar Programador
            UUID idProgramador = UUID.fromString((String) body.get("idProgramador"));
            Programador prog = programadorRepository.findById(idProgramador)
                    .orElseThrow(() -> new RuntimeException("Programador no existe"));

            Asesoria a = new Asesoria();
            a.setProgramador(prog);
            a.setNombreSolicitante((String) body.get("nombreSolicitante"));

            // 2. OBTENER EMAIL
            String email = (String) body.get("emailSolicitante");
            a.setEmailSolicitante(email);

            // =================================================================
            // 🔥 MAGIA AQUÍ: VINCULACIÓN AUTOMÁTICA
            // Buscamos si ya existe un usuario con ese email y lo conectamos.
            // Así aparecerá en "Mis Asesorías" inmediatamente.
            // =================================================================
            usuarioRepository.findByEmail(email).ifPresent(usuarioEncontrado -> {
                a.setUsuario(usuarioEncontrado);
            });

            // 3. Resto de datos
            a.setComentario((String) body.getOrDefault("comentario", ""));
            a.setFecha(LocalDate.parse((String) body.get("fecha")));
            a.setHora(LocalTime.parse((String) body.get("hora")));
            a.setEstado("pendiente");

            return ResponseEntity.ok(asesoriaRepository.save(a));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Requerido por el componente de Angular para marcar horas como ocupadas
     */
    @GetMapping("/ocupadas/{idProgramador}/{fecha}")
    public List<Asesoria> getOcupadas(@PathVariable UUID idProgramador, @PathVariable String fecha) {
        LocalDate ld = LocalDate.parse(fecha);
        // Retorna asesorías que no estén rechazadas para esa fecha
        return asesoriaRepository.findByProgramadorIdAndFechaAndEstadoNot(idProgramador, ld, "rechazada");
    }

    // --- ENDPOINTS PRIVADOS ---

    @GetMapping("/programador")
    public List<Asesoria> asesoriasDelProgramador() {
        Programador p = obtenerProgramadorActual();
        return asesoriaRepository.findByProgramadorId(p.getId());
    }

    @GetMapping("/mis")
    public List<Asesoria> misAsesoriasComoUsuario() {
        Usuario u = obtenerUsuarioActual();
        return asesoriaRepository.findByUsuarioId(u.getId());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarAsesoria(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        Asesoria asesoria = asesoriaRepository.findById(id).orElse(null);
        if (asesoria == null)
            return ResponseEntity.notFound().build();

        Programador programadorActual = obtenerProgramadorActual();

        // 1. Validación de Propiedad
        if (!asesoria.getProgramador().getId().equals(programadorActual.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado");
        }

        // 2. ✅ NUEVA VALIDACIÓN: Si ya no es pendiente, no se toca.
        if (!asesoria.getEstado().equalsIgnoreCase("pendiente")) {
            return ResponseEntity.badRequest()
                    .body("Esta asesoría ya fue procesada (" + asesoria.getEstado() + ") y no puede modificarse.");
        }

        // 3. ✅ NUEVA VALIDACIÓN: Bloqueo de fechas pasadas
        if (asesoria.getFecha().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest()
                    .body("No se puede gestionar una asesoría de una fecha pasada.");
        }

        // --- INICIO DE ACTUALIZACIÓN ---

        String estadoRaw = (String) body.get("estado");
        String estado = (estadoRaw != null) ? estadoRaw.toLowerCase().trim() : null;
        String respuesta = (String) body.get("respuestaProgramador");

        if (estado != null)
            asesoria.setEstado(estado);
        if (respuesta != null)
            asesoria.setRespuestaProgramador(respuesta);

        Asesoria guardada = asesoriaRepository.save(asesoria);

        // --- ENVÍO DE EMAIL ---
        if (estado != null && (estado.equals("aprobada") || estado.equals("rechazada"))) {

            if (guardada.getEmailSolicitante() == null || guardada.getEmailSolicitante().isBlank()) {
                return ResponseEntity.ok(Map.of(
                        "asesoria", guardada,
                        "warning", "Estado actualizado, pero la asesoría no tiene un email de contacto."));
            }

            try {
                String asunto = estado.equals("aprobada") ? "✅ Tu asesoría fue aprobada"
                        : "❌ Tu asesoría fue rechazada";
                String mensaje = (respuesta != null && !respuesta.isBlank())
                        ? respuesta
                        : "Tu asesoría para el día " + guardada.getFecha() + " ha sido: " + estado;

                emailService.enviarCorreo(guardada.getEmailSolicitante(), asunto, mensaje);
            } catch (Exception e) {
                return ResponseEntity.ok(Map.of(
                        "asesoria", guardada,
                        "warning", "Estado guardado, pero falló el servidor de correo: " + e.getMessage()));
            }
        }

        return ResponseEntity.ok(guardada);
    }
    // ... resto de tu código existente ...

    // --- NUEVO ENDPOINT PARA FILTROS ---

    @GetMapping("/programador/filtradas")
    public ResponseEntity<?> asesoriasFiltradas(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        try {
            Programador p = obtenerProgramadorActual();

            // Caso 1: sin filtros (devuelve todo lo del programador)
            if (estado == null && desde == null && hasta == null) {
                return ResponseEntity.ok(
                        asesoriaRepository.findByProgramadorId(p.getId()));
            }

            // Caso 2: solo estado
            if (estado != null && desde == null && hasta == null) {
                return ResponseEntity.ok(
                        asesoriaRepository.findByProgramadorIdAndEstado(p.getId(), estado));
            }

            // Caso 3: rango de fechas (sin importar estado)
            if (estado == null && desde != null && hasta != null) {
                return ResponseEntity.ok(
                        asesoriaRepository.findByProgramadorIdAndFechaBetween(
                                p.getId(),
                                LocalDate.parse(desde),
                                LocalDate.parse(hasta)));
            }

            // Caso 4: estado + rango de fechas
            if (estado != null && desde != null && hasta != null) {
                return ResponseEntity.ok(
                        asesoriaRepository.findByProgramadorIdAndEstadoAndFechaBetween(
                                p.getId(),
                                estado,
                                LocalDate.parse(desde),
                                LocalDate.parse(hasta)));
            }

            return ResponseEntity.badRequest().body("Combinación de filtros no válida (ej. falta fecha inicio o fin)");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al filtrar: " + e.getMessage());
        }
    }

} // Fin de la clase AsesoriaController