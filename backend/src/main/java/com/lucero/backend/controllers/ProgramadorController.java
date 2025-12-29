package com.lucero.backend.controllers;

import com.lucero.backend.dto.ProgramadorPublicoDTO;
import com.lucero.backend.models.Disponibilidad;
import com.lucero.backend.models.Programador;
import com.lucero.backend.repositories.AsesoriaRepository;
import com.lucero.backend.repositories.DisponibilidadRepository;
import com.lucero.backend.repositories.ProgramadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/programadores")
@CrossOrigin(origins = "*")
public class ProgramadorController {

    @Autowired
    private ProgramadorRepository programadorRepository;

    @Autowired
    private DisponibilidadRepository disponibilidadRepository; // ✅ Necesario para leer horarios

    @Autowired
    private AsesoriaRepository asesoriaRepository; // ✅ Necesario para ver ocupados

    // 1. LISTAR TODOS
    @GetMapping
    public List<ProgramadorPublicoDTO> obtenerTodos() {
        return programadorRepository.findAll().stream().map(this::convertirADTO).toList();
    }

    // 2. OBTENER UNO
    @GetMapping("/{id}")
    public ProgramadorPublicoDTO obtenerUno(@PathVariable UUID id) {
        Programador p = programadorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Programador no encontrado"));
        return convertirADTO(p);
    }

    // ======================================================================
    // 3. ✅ OBTENER SLOTS (LA MAGIA QUE TE FALTA)
    // ======================================================================
    @GetMapping("/{id}/slots")
    public List<String> obtenerSlots(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        // A. Calcular día de la semana para coincidir con tu BD (0=Domingo, 1=Lunes...)
        int diaJava = fecha.getDayOfWeek().getValue(); // Java da 1 (Lun) a 7 (Dom)
        int diaBD = (diaJava == 7) ? 0 : diaJava; // Convertimos 7 a 0 si es Domingo

        // B. Traer todas las disponibilidades activas de este programador
        List<Disponibilidad> rangos = disponibilidadRepository.findByProgramadorIdAndActivoTrue(id);

        List<String> slotsResultantes = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm"); // Formato "18:00"

        // C. Buscar si hay algún rango configurado para HOY (diaBD)
        for (Disponibilidad rango : rangos) {
            if (rango.getDiaSemana() == diaBD) {

                // D. "Trocear" el rango en horas sueltas
                LocalTime actual = rango.getHoraInicio();
                LocalTime fin = rango.getHoraFin();

                // Mientras la hora actual + 60 mins no se pase del fin...
                while (actual.plusMinutes(60).isBefore(fin) || actual.plusMinutes(60).equals(fin)) {

                    // E. VERIFICAR SI YA ESTÁ OCUPADO
                    // Si existe una cita en esta fecha+hora que NO sea "rechazada", entonces está
                    // ocupado.
                    boolean ocupado = asesoriaRepository.existsByProgramadorIdAndFechaAndHoraAndEstadoNot(
                            id, fecha, actual, "rechazada");

                    if (!ocupado) {
                        slotsResultantes.add(actual.format(formatter)); // Agregamos "18:00"
                    }

                    // Avanzar 1 hora
                    actual = actual.plusMinutes(60);
                }
            }
        }

        return slotsResultantes; // Retorna ["18:00", "19:00", "20:00"]
    }

    // Auxiliar para DTO
    private ProgramadorPublicoDTO convertirADTO(Programador p) {
        return new ProgramadorPublicoDTO(
                p.getId(),
                p.getUsuario().getNombre(),
                p.getUsuario().getFotoUrl(),
                p.getEspecialidad(),
                p.getDescripcion(),
                null, null, null);
    }
}