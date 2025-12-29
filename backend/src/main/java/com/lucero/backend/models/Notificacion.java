package com.lucero.backend.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String tipo; // EMAIL | WHATSAPP

    @Column(nullable = false)
    private String destinatario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "estado_envio", nullable = false)
    private String estadoEnvio; // PENDIENTE | ENVIADO | FALLIDO

    @Column(name = "programada_para", nullable = false)
    private LocalDateTime programadaPara;

    @Column(name = "enviado_en")
    private LocalDateTime enviadoEn;

    @Column(columnDefinition = "TEXT")
    private String error;

    @PrePersist
    protected void onCreate() {
        if (estadoEnvio == null) estadoEnvio = "PENDIENTE";
        if (programadaPara == null) programadaPara = LocalDateTime.now();
    }
}
