package com.clinica.recordatorios.repository;

import com.clinica.recordatorios.domain.CanalNotificacion;
import com.clinica.recordatorios.domain.Cita;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
class CitaRepositoryTest {

    @Autowired
    private CitaRepository citaRepository;

    @Test
    void findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse_soloRetornaCitasPendientesEnVentana() {
        LocalDateTime desde = LocalDateTime.of(2026, 8, 10, 9, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 8, 11, 9, 0, 0);

        // 1. En ventana pendiente (debe ser retornada)
        Cita citaPendienteEnVentana = Cita.builder()
                .pacienteNombre("Paciente Uno")
                .pacienteContacto("uno@example.com")
                .fechaHoraCita(LocalDateTime.of(2026, 8, 10, 15, 0, 0))
                .canalPreferido(CanalNotificacion.EMAIL)
                .recordatorioEnviado(false)
                .build();

        // 2. En ventana ya enviada (no debe ser retornada)
        Cita citaEnviadaEnVentana = Cita.builder()
                .pacienteNombre("Paciente Dos")
                .pacienteContacto("dos@example.com")
                .fechaHoraCita(LocalDateTime.of(2026, 8, 10, 18, 0, 0))
                .canalPreferido(CanalNotificacion.EMAIL)
                .recordatorioEnviado(true)
                .build();

        // 3. Fuera de ventana pendiente (no debe ser retornada)
        Cita citaFueraDeVentana = Cita.builder()
                .pacienteNombre("Paciente Tres")
                .pacienteContacto("tres@example.com")
                .fechaHoraCita(LocalDateTime.of(2026, 8, 12, 10, 0, 0))
                .canalPreferido(CanalNotificacion.SMS)
                .recordatorioEnviado(false)
                .build();

        citaRepository.saveAll(List.of(citaPendienteEnVentana, citaEnviadaEnVentana, citaFueraDeVentana));

        List<Cita> resultado = citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(desde, hasta);

        assertEquals(1, resultado.size());
        Cita encontrada = resultado.get(0);
        assertEquals("Paciente Uno", encontrada.getPacienteNombre());
        assertFalse(encontrada.isRecordatorioEnviado());
    }
}

