package com.lucero.backend.services;

import com.lucero.backend.models.Notificacion;
import com.lucero.backend.repositories.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificacionScheduler {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionService notificacionService;

    // cada 60 segundos revisa la cola
    @Scheduled(fixedRate = 60000)
    public void procesarPendientes() {
        List<Notificacion> pendientes = notificacionRepository
                .findByEstadoEnvioAndProgramadaParaLessThanEqualOrderByProgramadaParaAsc(
                        "PENDIENTE",
                        LocalDateTime.now()
                );

        for (Notificacion n : pendientes) {
            notificacionService.enviar(n);
        }
    }
}
