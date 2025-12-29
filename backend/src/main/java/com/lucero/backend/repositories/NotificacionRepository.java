package com.lucero.backend.repositories;

import com.lucero.backend.models.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {

    List<Notificacion> findByEstadoEnvioAndProgramadaParaLessThanEqualOrderByProgramadaParaAsc(
            String estadoEnvio,
            LocalDateTime ahora
    );
}
