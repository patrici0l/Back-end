package com.lucero.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "firebase_uid", unique = true)
    private String firebaseUid;

    private String nombre;
    private String email;

    @Column(name = "foto_url")
    private String fotoUrl;

    private String rol;

    @Column(name = "password_hash")
    private String passwordHash;

    private boolean activo;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;
}