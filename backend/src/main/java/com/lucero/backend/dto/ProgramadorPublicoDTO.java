package com.lucero.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ProgramadorPublicoDTO {

    private UUID id;
    private String nombre;
    private String foto;
    private String especialidad;
    private String descripcion;
    private String disponibilidad;
    private List<String> horasDisponibles;
    private UUID usuarioId;

    public ProgramadorPublicoDTO(UUID id, String nombre, String foto, String especialidad,
            String descripcion, String disponibilidad,
            List<String> horasDisponibles, UUID usuarioId) {
        this.id = id;
        this.nombre = nombre;
        this.foto = foto;
        this.especialidad = especialidad;
        this.descripcion = descripcion;
        this.disponibilidad = disponibilidad;
        this.horasDisponibles = horasDisponibles;
        this.usuarioId = usuarioId;
    }
}