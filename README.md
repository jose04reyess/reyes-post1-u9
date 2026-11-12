# recordatorios-citas-service

**Estudiante:** Reyes  
**Materia / Unidad:** Unidad 9 — *Probar el Diseño: JUnit 5, Mockito, Test Doubles e Integración Continua*  
**Stack Tecnológico:** Java 21 | Spring Boot 3.3.3 | Spring Data JPA | H2 Database | JUnit 5 | Mockito | GitHub Actions

---

## 1. Descripción del Proyecto

El microservicio `recordatorios-citas-service` implementa un motor desacoplado de recordatorios clínicos automáticos y urgentes para citas médicas. Diseñado bajo los principios **SOLID** (especialmente el Principio de Inversión de Dependencias — **DIP**) y patrones de diseño GoF (**Strategy**, **Factory Method** y **Gateway**), permite el procesamiento determinista y verificable de notificaciones a través de múltiples canales (Email y SMS), eliminando acoplamientos rígidos a la infraestructura externa y al reloj del sistema operativo.

---

## 2. Arquitectura y Patrones de Diseño

```
                                  +------------------------+
                                  |   CitaReminderService  |
                                  +-----------+------------+
                                              |
                        +---------------------+---------------------+
                        |                     |                     |
                        v                     v                     v
              +------------------+  +-------------------+  +------------------+
              |  CitaRepository  |  |   ReminderChannel |  |  ReminderChannel |
              |      (JPA)       |  |      Factory      |  |  (canalUrgente)  |
              +------------------+  +---------+---------+  +--------+---------+
                                              |                     |
                                      +-------+-------+             |
                                      |               |             |
                                      v               v             v
                               +-------------+  +--------------------------+
                               |    Email    |  |    SmsReminderChannel    |
                               |   Channel   |  +-------------+------------+
                               +-------------+                |
                                                              v
                                                +--------------------------+
                                                |   NotificadorSmsGateway  |
                                                +-------------+------------+
                                                              |
                                                              v
                                                +--------------------------+
                                                |TwilioNotificadorSmsGatew.|
                                                +--------------------------+
```

### Principales Decisiones de Diseño:
1. **Refactor DIP (Dependency Inversion Principle) y Cero `new`:**
   - La lógica de negocio en `CitaReminderServiceImpl` no instancia dependencias directamente.
   - Recibe vía constructor las abstracciones: `CitaRepository`, `ReminderChannelFactory`, el canal urgente inyectado mediante `@Qualifier("canalUrgente") ReminderChannel` y la abstracción temporal `java.time.Clock`.
2. **Strategy Pattern (`ReminderChannel`):**
   - Define el contrato de notificación `enviar(Cita cita, String mensaje)`.
   - Permite agregar nuevos canales (WhatsApp, Push Notifications, etc.) sin alterar el servicio de citas (Open/Closed Principle).
3. **Gateway Pattern (`NotificadorSmsGateway`):**
   - Aísla a la aplicación de la API del proveedor externo de SMS (ej. Twilio).
   - Facilita la creación de dobles de prueba (`@MockBean`) para pruebas de integración sin consumir saldo ni requerir conexión a internet.
4. **Factory Method (`ReminderChannelFactory`):**
   - Entrega la instancia adecuada según el enum `CanalNotificacion` (`EMAIL` o `SMS`) inyectando componentes administrados por Spring sin instanciación manual.
5. **Composition Root (`CanalUrgenteConfig`):**
   - Configuración declarativa que ensambla el canal urgente inyectando el gateway de SMS correspondiente.
6. **Desacoplamiento Temporal (`ClockConfig` y `java.time.Clock`):**
   - El servicio consume un bean `Clock` inyectado, permitiendo pruebas unitarias y de integración deterministas mediante `Clock.fixed(...)` con fechas exactas fijadas en el tiempo.

---

## 3. Estructura del Proyecto

```text
recordatorios-citas-service/
├── .github/
│   └── workflows/
│       └── ci.yml                             # Pipeline CI/CD para GitHub Actions
├── src/
│   ├── main/
│   │   ├── java/com/clinica/recordatorios/
│   │   │   ├── RecordatoriosCitasServiceApplication.java
│   │   │   ├── channel/
│   │   │   │   ├── ReminderChannel.java            # Interfaz Strategy
│   │   │   │   ├── EmailReminderChannel.java       # Estrategia Email
│   │   │   │   ├── SmsReminderChannel.java         # Estrategia SMS (delega en Gateway)
│   │   │   │   ├── ReminderChannelFactory.java     # Interfaz Factory
│   │   │   │   ├── ReminderChannelFactoryImpl.java # Implementación Factory con switch
│   │   │   │   └── gateway/
│   │   │   │       ├── NotificadorSmsGateway.java      # Interfaz Gateway SMS
│   │   │   │       └── TwilioNotificadorSmsGateway.java# Implementación HTTP simulada
│   │   │   ├── config/
│   │   │   │   ├── ClockConfig.java                # Bean Clock.systemDefaultZone()
│   │   │   │   └── CanalUrgenteConfig.java         # Composition Root canalUrgente
│   │   │   ├── domain/
│   │   │   │   ├── CanalNotificacion.java          # Enum { EMAIL, SMS }
│   │   │   │   └── Cita.java                       # Entidad JPA @Entity con Lombok
│   │   │   ├── repository/
│   │   │   │   └── CitaRepository.java             # Spring Data JPA Repository
│   │   │   └── service/
│   │   │       ├── CitaNoEncontradaException.java  # Excepción de dominio
│   │   │       ├── CitaReminderService.java        # Contrato del servicio
│   │   │       └── CitaReminderServiceImpl.java    # Lógica de recordatorios y DIP
│   │   └── resources/
│   │       └── application.properties              # Configuración H2 y datasource
│   └── test/
│       └── java/com/clinica/recordatorios/
│           ├── CitaReminderServiceIntegrationTest.java # @SpringBootTest completo
│           ├── repository/
│           │   └── CitaRepositoryTest.java             # @DataJpaTest sobre H2
│           └── service/
│               └── CitaReminderServiceImplTest.java    # Pruebas Unitarias aisladas
└── pom.xml
```

---

## 4. Estrategia de Pruebas Automatizadas

La solución cuenta con **8 pruebas automatizadas** organizadas en tres niveles de la pirámide de pruebas:

### A. Pruebas Unitarias Aisladas (`CitaReminderServiceImplTest`)
Uso exclusivo de **Mockito** (`@ExtendWith(MockitoExtension.class)`) y Fake Clock:
- `clockFijo = Clock.fixed(Instant.parse("2026-08-10T09:00:00Z"), ZoneId.of("UTC"))`.

| Método de Prueba | Condición y Verificación |
|---|---|
| `enviarRecordatoriosPendientes_citaEnVentana_usaElCanalPreferido` | Valida obtención del canal preferido desde la factory, invocación a `enviar`, marcado de `recordatorioEnviado = true` y persistencia con `save`. |
| `enviarRecordatoriosPendientes_ventanaDeterministaSegunClockFijo_consultaLosLimitesExactos` | Captura los argumentos temporales pasados al repositorio con `ArgumentCaptor<LocalDateTime>` y valida que los límites sean exactamente `2026-08-10T09:00:00` y `2026-08-11T09:00:00`. |
| `enviarRecordatoriosPendientes_sinCitasPendientes_noConsultaLaFactory` | Si no hay citas en ventana, verifica `verifyNoInteractions(reminderChannelFactory)` y que no se invoque `save`. |
| `enviarRecordatorioUrgente_citaExistente_usaCanalUrgenteInyectado` | Valida que se invoque `canalUrgente.enviar(...)` con el mensaje urgente, se marque `recordatorioEnviado = true` y se persista. |
| `enviarRecordatorioUrgente_gatewayFalla_noMarcaComoEnviado` | Simula falla con `doThrow` en el canal urgente, comprueba `assertThrows(RuntimeException.class)`, confirma que no se marque como enviada y que nunca se invoque `save`. |
| `enviarRecordatorioUrgente_citaInexistente_lanzaCitaNoEncontradaException` | Al buscar un ID inexistente, valida el lanzamiento de `CitaNoEncontradaException` y `verifyNoInteractions(canalUrgenteMock)`. |

### B. Pruebas de Persistencia (`CitaRepositoryTest`)
Prueba de corte (`@DataJpaTest`) sobre base de datos H2 en memoria:
- `findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse_soloRetornaCitasPendientesEnVentana`: Inserta 3 citas de prueba (en ventana pendiente, en ventana ya enviada, fuera de ventana) y valida que la consulta derive exactamente la única cita pendiente dentro de la ventana de 24 horas.

### C. Pruebas de Integración (`CitaReminderServiceIntegrationTest`)
Prueba de contexto completo (`@SpringBootTest`):
- Registra una configuración de prueba (`@TestConfiguration`) con un bean `@Primary Clock` determinista (`2026-08-10T09:00:00Z`).
- Reemplaza el gateway de SMS con un `@MockBean private NotificadorSmsGateway notificadorSmsGateway`.
- Inserta una cita real en H2, ejecuta `enviarRecordatoriosPendientes()` y verifica que el SMS haya sido despachado a través del gateway y que la entidad quede actualizada en la base de datos con `recordatorioEnviado = true`.

---

## 5. Pipeline de Integración Continua (CI/CD)

El archivo `.github/workflows/ci.yml` define el flujo de trabajo automático para GitHub Actions ante cada `push` o `pull_request` a la rama `main`:

```yaml
name: CI — Pruebas y Cobertura

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout del repositorio
        uses: actions/checkout@v4

      - name: Configurar JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"
          cache: maven

      - name: Compilar y ejecutar todas las pruebas
        run: mvn -B verify --no-transfer-progress
```

---

## 6. Compilación y Ejecución Local

Para compilar el proyecto y ejecutar la suite completa de pruebas:

```bash
# Limpiar y verificar (compilación, pruebas unitarias e integración, empaquetado JAR)
mvn clean verify

# Ejecutar únicamente las pruebas automatizadas
mvn test
```

### Salida esperada de las pruebas:
```text
[INFO] Running com.clinica.recordatorios.CitaReminderServiceIntegrationTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.clinica.recordatorios.repository.CitaRepositoryTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.clinica.recordatorios.service.CitaReminderServiceImplTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```
