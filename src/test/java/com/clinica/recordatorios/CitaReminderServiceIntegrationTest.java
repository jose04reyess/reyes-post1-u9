package com.clinica.recordatorios;

import com.clinica.recordatorios.channel.gateway.NotificadorSmsGateway;
import com.clinica.recordatorios.domain.CanalNotificacion;
import com.clinica.recordatorios.domain.Cita;
import com.clinica.recordatorios.repository.CitaRepository;
import com.clinica.recordatorios.service.CitaReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
class CitaReminderServiceIntegrationTest {

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        public Clock clock() {
            return Clock.fixed(Instant.parse("2026-08-10T09:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @MockBean
    private NotificadorSmsGateway notificadorSmsGateway;

    @Autowired
    private CitaReminderService citaReminderService;

    @Autowired
    private CitaRepository citaRepository;

    @BeforeEach
    void setUp() {
        citaRepository.deleteAll();
    }

    @Test
    void enviarRecordatoriosPendientes_contextoCompletoDeSpring_persisteYNotificaCitasEnVentana() {
        LocalDateTime fechaCita = LocalDateTime.of(2026, 8, 10, 15, 0, 0);
        Cita cita = Cita.builder()
                .pacienteNombre("Ana Torres")
                .pacienteContacto("3119876543")
                .fechaHoraCita(fechaCita)
                .canalPreferido(CanalNotificacion.SMS)
                .recordatorioEnviado(false)
                .build();

        Cita citaGuardada = citaRepository.save(cita);

        int procesados = citaReminderService.enviarRecordatoriosPendientes();

        assertEquals(1, procesados);
        String mensajeEsperado = "Recordatorio: tiene una cita el " + fechaCita;
        verify(notificadorSmsGateway).enviarSms(eq("3119876543"), eq(mensajeEsperado));

        Cita citaActualizada = citaRepository.findById(citaGuardada.getId()).orElseThrow();
        assertTrue(citaActualizada.isRecordatorioEnviado());
    }
}

