package com.lucero.backend.controllers;

import com.lucero.backend.models.Asesoria;
import com.lucero.backend.models.Programador;
import com.lucero.backend.models.Usuario;
import com.lucero.backend.repositories.AsesoriaRepository;
import com.lucero.backend.repositories.ProgramadorRepository;
import com.lucero.backend.repositories.UsuarioRepository;
import com.lucero.backend.services.EmailService; // ✅ IMPORTANTE: Importar el servicio
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/programador/asesorias")
@CrossOrigin(origins = "*")
public class AsesoriaProgramadorController {

    @Autowired
    private AsesoriaRepository asesoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProgramadorRepository programadorRepository;

    @Autowired
    private EmailService emailService; // ✅ ESTO FALTABA: Inyección del servicio de Email

    // Método auxiliar para obtener el programador logueado
    private Programador obtenerProgramadorActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return programadorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RuntimeException("No tienes perfil de programador creado."));
    }

    // 1) LISTAR ASESORÍAS DEL PROGRAMADOR LOGUEADO
    @GetMapping
    public List<Asesoria> listarMias() {
        Programador p = obtenerProgramadorActual();
        return asesoriaRepository.findByProgramadorId(p.getId());
    }

    // 2) APROBAR / RECHAZAR + RESPUESTA (CON LOGS DE DEBUG)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarEstado(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        System.out.println("--- INICIO DEBUG ACTUALIZACIÓN ---");

        Programador actual = obtenerProgramadorActual();
        Asesoria asesoria = asesoriaRepository.findById(id).orElse(null);

        if (asesoria == null)
            return ResponseEntity.notFound().build();

        // Seguridad: Verificar que la asesoría pertenece a este programador
        if (!asesoria.getProgramador().getId().equals(actual.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado");
        }

        String estado = body.get("estado");
        String respuesta = body.get("respuestaProgramador");

        // Guardamos en BD
        asesoria.setEstado(estado);
        asesoria.setRespuestaProgramador(respuesta);
        Asesoria guardada = asesoriaRepository.save(asesoria);
        System.out.println("✅ BD Actualizada. Estado: " + estado);

        // --- LÓGICA DE CORREO CON DEBUG ---
        System.out.println("🔍 Datos para envío:");
        System.out.println("   -> Estado nuevo: " + estado);
        System.out.println("   -> Email destino: " + guardada.getEmailSolicitante());

        if (estado != null && (estado.equals("aprobada") || estado.equals("rechazada"))) {

            if (guardada.getEmailSolicitante() == null || guardada.getEmailSolicitante().isBlank()) {
                System.out.println("⚠️ ALERTA: Email nulo o vacío. Cancelando envío.");
            } else {
                System.out.println("🚀 Intentando enviar correo SMTP...");
                try {
                    String asunto = estado.equals("aprobada") ? "✅ Asesoría Aprobada" : "❌ Asesoría Rechazada";
                    // Enviar correo real
                    emailService.enviarCorreo(guardada.getEmailSolicitante(), asunto, respuesta);
                    System.out.println("✨ ÉXITO: Método enviarCorreo ejecutado sin error.");
                } catch (Exception e) {
                    System.err.println("❌ ERROR CRÍTICO enviando correo: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("ℹ️ No se envía correo (estado no es aprobada/rechazada).");
        }

        System.out.println("--- FIN DEBUG ---");
        return ResponseEntity.ok(guardada);
    }
}