package com.lucero.backend.controllers;

import com.lucero.backend.models.Disponibilidad;
import com.lucero.backend.repositories.DisponibilidadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/disponibilidades")
@CrossOrigin(origins = "*")
public class DisponibilidadPublicController {

    @Autowired
    private DisponibilidadRepository disponibilidadRepository;

    // ==================================================
    // PÚBLICO: listar disponibilidad ACTIVA de un programador
    // GET /api/disponibilidades/programador/{id}
    // ==================================================
    @GetMapping("/programador/{id}")
    public List<Disponibilidad> listarActivasPorProgramador(@PathVariable UUID id) {
        return disponibilidadRepository.findByProgramadorIdAndActivoTrue(id);
    }
}
