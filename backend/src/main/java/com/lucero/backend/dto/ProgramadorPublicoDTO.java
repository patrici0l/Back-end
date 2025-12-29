package com.lucero.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramadorPublicoDTO {
    private UUID id;
    private String nombre;
    private String foto;
    private String especialidad;
    private String descripcion;

    // Estos campos no están en tu modelo actual, pero tu HTML los usa.
    // Por ahora van nulos (opcional) hasta que los agregues a BD si el PDF lo pide.
    private String disponibilidad;
    private String github;
    private String linkedin;
}
