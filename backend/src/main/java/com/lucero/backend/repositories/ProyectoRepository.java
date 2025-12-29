package com.lucero.backend.repositories;

import com.lucero.backend.models.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; // <--- ESTO FALTABA
import java.util.UUID;

public interface ProyectoRepository extends JpaRepository<Proyecto, UUID> {
    // Spring Data JPA es inteligente: entiende que buscas por el ID del objeto 'programador'
    List<Proyecto> findByProgramadorId(UUID programadorId);
}