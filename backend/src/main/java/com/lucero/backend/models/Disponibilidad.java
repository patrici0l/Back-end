package com.lucero.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "disponibilidades")
public class Disponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "programador_id", nullable = false)
    private Programador programador;

    @Column(name = "dia_semana")
    private int diaSemana; // 0=Domingo, 1=Lunes...

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    private String modalidad; // 'virtual' o 'presencial'
    private boolean activo;
}