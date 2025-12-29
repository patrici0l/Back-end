package com.lucero.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
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

    // --- AGREGAR ESTOS CAMPOS QUE FALTABAN ---
    private String emailContacto; // Email público (puede ser distinto al del login)
    private String whatsapp;
    private String github;
    private String linkedin;
    private String portafolio;

    // Para guardar texto libre (Ej: "Lunes a Viernes...")
    @Column(columnDefinition = "TEXT")
    private String disponibilidadTexto;

    // Para guardar la lista de horas exactas (Ej: ["09:00", "10:00"])
    @ElementCollection
    @CollectionTable(name = "programador_horas", joinColumns = @JoinColumn(name = "programador_id"))
    @Column(name = "hora")
    private List<String> horasDisponibles;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
    }
}