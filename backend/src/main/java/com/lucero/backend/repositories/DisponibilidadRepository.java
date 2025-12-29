package com.lucero.backend.repositories;

import com.lucero.backend.models.Disponibilidad;
import com.lucero.backend.models.Programador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, UUID> {

    List<Disponibilidad> findByProgramador(Programador programador);

    List<Disponibilidad> findByProgramadorId(UUID programadorId);

    List<Disponibilidad> findByProgramadorIdAndActivoTrue(UUID programadorId);

}