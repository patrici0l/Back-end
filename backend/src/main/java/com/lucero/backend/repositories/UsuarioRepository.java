package com.lucero.backend.repositories;

import com.lucero.backend.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional; // Importar esto
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    // Método necesario para el Login
    Optional<Usuario> findByEmail(String email);
}