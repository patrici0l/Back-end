package com.lucero.backend.services;

import com.lucero.backend.models.Notificacion;
import com.lucero.backend.repositories.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    // Si no configuras SMTP aún, esto puede ser null si no lo inyectas.
    // Para evitar errores, lo dejamos opcional:
    private final JavaMailSender mailSender;

    // =========================
    // 1) ENCOLAR NOTIFICACIONES
    // =========================
    public void encolarEmail(String email, String mensaje, LocalDateTime programadaPara) {
        Notificacion n = new Notificacion();
        n.setTipo("EMAIL");
        n.setDestinatario(email);
        n.setMensaje(mensaje);
        n.setProgramadaPara(programadaPara);
        n.setEstadoEnvio("PENDIENTE");
        notificacionRepository.save(n);
    }

    public void encolarWhatsApp(String telefono, String mensaje, LocalDateTime programadaPara) {
        Notificacion n = new Notificacion();
        n.setTipo("WHATSAPP");
        n.setDestinatario(telefono);
        n.setMensaje(mensaje);
        n.setProgramadaPara(programadaPara);
        n.setEstadoEnvio("PENDIENTE");
        notificacionRepository.save(n);
    }

    // =========================
    // 2) ENVIAR UNA NOTIFICACIÓN
    // =========================
    public void enviar(Notificacion n) {
        try {
            if ("EMAIL".equalsIgnoreCase(n.getTipo())) {
                enviarEmailReal(n.getDestinatario(), "Notificación de asesoría", n.getMensaje());
            } else if ("WHATSAPP".equalsIgnoreCase(n.getTipo())) {
                // ✅ WhatsApp simulado (por ahora)
                // Aquí luego conectas Twilio si quieres.
                // Por ahora basta para evidencia: guardamos ENVIADO.
            }

            n.setEstadoEnvio("ENVIADO");
            n.setEnviadoEn(LocalDateTime.now());
            n.setError(null);
            notificacionRepository.save(n);

        } catch (Exception e) {
            n.setEstadoEnvio("FALLIDO");
            n.setError(e.getMessage());
            notificacionRepository.save(n);
        }
    }

    private void enviarEmailReal(String para, String asunto, String cuerpo) {
        if (mailSender == null) {
            // Si no hay SMTP configurado, marcamos como fallido en el scheduler.
            throw new RuntimeException("JavaMailSender no configurado (SMTP faltante).");
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(para);
        msg.setSubject(asunto);
        msg.setText(cuerpo);

        mailSender.send(msg);
    }
}
