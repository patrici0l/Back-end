package com.lucero.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "programadores")
public class Programador {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // RELACIÓN CLAVE: Un programador tiene un Usuario asociado
    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    private String especialidad;
    private String descripcion;
    private String telefono;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;
}