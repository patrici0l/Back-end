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

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    private String especialidad;
    private String descripcion;
    private String telefono;

    // Campos nuevos necesarios
    private String emailContacto;
    private String whatsapp;
    private String github;
    private String linkedin;
    private String portafolio;
    
    @Column(columnDefinition = "TEXT") 
    private String disponibilidadTexto; 

    // Lista de horas
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