package com.clinica.recordatorios.channel;

import com.clinica.recordatorios.domain.Cita;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailReminderChannel implements ReminderChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailReminderChannel.class);

    @Override
    public void enviar(Cita cita, String mensaje) {
        String logMessage = String.format("[EMAIL a %s] %s", cita.getPacienteContacto(), mensaje);
        log.info(logMessage);
        System.out.println(logMessage);
    }
}

