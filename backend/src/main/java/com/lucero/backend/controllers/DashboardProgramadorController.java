package com.lucero.backend.controllers;

import com.lucero.backend.models.Programador;
import com.lucero.backend.models.Usuario;
import com.lucero.backend.repositories.AsesoriaRepository;
import com.lucero.backend.repositories.ProgramadorRepository;
import com.lucero.backend.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/programador/dashboard")
@CrossOrigin(origins = "*")
public class DashboardProgramadorController {

    @Autowired
    private AsesoriaRepository asesoriaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ProgramadorRepository programadorRepository;

    // ======================
    // AUX: programador logueado
    // ======================
    private Programador obtenerProgramadorActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return programadorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RuntimeException("No tienes perfil de programador"));
    }

    // ======================
    // RESUMEN
    // ======================
    @GetMapping("/resumen")
    public Map<String, Object> resumen() {
        Programador p = obtenerProgramadorActual();
        UUID id = p.getId();

        Map<String, Object> resp = new HashMap<>();
        resp.put("total", asesoriaRepository.countByProgramadorId(id));
        resp.put("pendientes", asesoriaRepository.countByProgramadorIdAndEstado(id, "pendiente"));
        resp.put("aprobadas", asesoriaRepository.countByProgramadorIdAndEstado(id, "aprobada"));
        resp.put("rechazadas", asesoriaRepository.countByProgramadorIdAndEstado(id, "rechazada"));

        return resp;
    }

    // ======================
    // SERIE PARA GRÁFICO
    // ======================
    @GetMapping("/serie")
    public List<Map<String, Object>> seriePorFecha() {
        Programador p = obtenerProgramadorActual();
        return asesoriaRepository.countPorFecha(p.getId());
    }
}
