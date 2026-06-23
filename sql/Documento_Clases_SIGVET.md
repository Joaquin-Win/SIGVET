# Diagrama de Clases - SIGVET
# Documentación Descriptiva

## Catálogo de Clases

### 1. Veterinario
**Estereotipo:** Entidad de dominio | **Tabla BD:** `veterinario`

Representa al profesional médico a cargo de la atención clínica de mascotas. Define su agenda de disponibilidad, atiende consultas médicas, prescribe tratamientos y medicamentos, y documenta las historias clínicas.

| Atributo | Tipo | Restricciones | Descripción |
|----------|------|---------------|-------------|
| idVeterinario | int | PK, AUTO_INCREMENT | Identificador único del veterinario |
| nombre | String | NOT NULL, max 100 | Nombre de pila |
| apellido | String | NOT NULL, max 100 | Apellido |
| matricula | String | NOT NULL, UNIQUE | Número de matrícula profesional |
| telefono | String | NOT NULL, max 20 | Teléfono de contacto |
| email | String | max 150 | Correo electrónico |
| estado | EstadoRegistro | NOT NULL, default 'Activo' | Activo/Inactivo (baja lógica) |

**Métodos clave:**
- `configurarAgenda()`: Define franjas de disponibilidad (CU-01)
- `obtenerTurnosPendientes(fecha)`: Lista turnos del día (CU-03, CU-08)
- `iniciarConsulta(turno)`: Crea consulta médica vinculada al turno (CU-03)

**Relaciones:**
- Composición 1→* con AgendaDisponibilidad (un veterinario define sus franjas)
- Asociación 1→* con ConsultaMedica (un veterinario atiende múltiples consultas)

---

### 2. AgendaDisponibilidad
**Estereotipo:** Entidad de dominio | **Tabla BD:** `agenda_disponibilidad`

Representa una franja horaria de disponibilidad del veterinario para un día de la semana. A partir de cada franja se generan automáticamente los slots individuales de la agenda.

| Atributo | Tipo | Restricciones | Descripción |
|----------|------|---------------|-------------|
| idAgenda | int | PK, AUTO_INCREMENT | Identificador único de la franja |
| idVeterinario | int | FK → veterinario, NOT NULL | Veterinario al que pertenece |
| diaSemana | String | NOT NULL, ENUM('Lunes'...'Domingo') | Día de la semana |
| horaInicio | Time | NOT NULL | Hora de inicio de la franja |
| horaFin | Time | NOT NULL | Hora de fin de la franja |

**Regla de negocio:** RN-11 — No se permiten franjas superpuestas para el mismo veterinario y día.

**Métodos clave:**
- `generarSlots()`: Crea slots individuales de 30 min dentro de la franja
- `validarSuperposicion(franja)`: Verifica que no haya solapamiento
- `tieneTurnosAsociados()`: Verifica si la franja tiene turnos (impide eliminación)

---

### 3. SlotAgenda
**Estereotipo:** Entidad de dominio | **Tabla BD:** `slot_agenda`

Representa un espacio individual de la agenda del veterinario, generado automáticamente a partir de las franjas de disponibilidad. Cada slot corresponde a una fecha y hora específicas y puede estar disponible o reservado.

| Atributo | Tipo | Restricciones | Descripción |
|----------|------|---------------|-------------|
| idSlot | int | PK, AUTO_INCREMENT | Identificador único del slot |
| idAgenda | int | FK → agenda_disponibilidad, NOT NULL | Franja a la que pertenece |
| fecha | Date | NOT NULL | Fecha específica del slot |
| hora | Time | NOT NULL | Hora específica del slot |
| estado | EstadoSlot | NOT NULL, default 'Disponible' | Disponible/Reservado |

**Regla de negocio:** RN-08 — Un slot admite como máximo un turno activo.

**Métodos clave:**
- `reservar()`: Cambia estado a Reservado (dentro de transacción)
- `liberar()`: Cambia estado a Disponible (cancelación/reprogramación)

**Constraint BD:** UNIQUE (id_agenda, fecha, hora) — impide slots duplicados.

---

### 4. Dueno
**Estereotipo:** Entidad de dominio | **Tabla BD:** `dueno`

Representa al propietario de una o más mascotas. Es el tomador de decisiones y el responsable de la mascota en la relación triádica dueño-mascota-profesional identificada en la elicitación.

| Atributo | Tipo | Restricciones | Descripción |
|----------|------|---------------|-------------|
| idDueno | int | PK, AUTO_INCREMENT | Identificador único |
| dni | String | NOT NULL, UNIQUE, max 20 | Documento Nacional de Identidad |
| nombre | String | NOT NULL, max 100 | Nombre de pila |
| apellido | String | NOT NULL, max 100 | Apellido |
| telefono | String | NOT NULL, max 20 | Teléfono de contacto |
| direccion | String | max 200 | Dirección de residencia |
| email | String | max 150 | Correo electrónico |
| estado | EstadoRegistro | NOT NULL, default 'Activo' | Activo/Inactivo |

**Regla de negocio:** RN-13 — Datos mínimos necesarios según Ley 25.326. Procedimiento de anonimización disponible.

**Métodos clave:**
- `anonimizarDatos()`: Reemplaza datos personales por genéricos (cumplimiento Ley 25.326)
- `tieneConsultasAsociadas()`: Determina si se puede eliminar físicamente o solo baja lógica

---

### 5. Mascota
**Estereotipo:** Entidad de dominio | **Tabla BD:** `mascota`

Representa al paciente veterinario. Toda la trazabilidad clínica se articula en torno a la mascota, pero las transacciones administrativas (turnos) se vinculan con el dueño.

| Atributo | Tipo | Restricciones | Descripción |
|----------|------|---------------|-------------|
| idMascota | int | PK, AUTO_INCREMENT | Identificador único |
| idDueno | int | FK → dueno, NOT NULL | Dueño responsable |
| nombre | String | NOT NULL, max 100 | Nombre de la mascota |
| idEspecie | int | FK → especie, NOT NULL | Especie (perro, gato, etc.) |
| idRaza | int | FK → raza, NOT NULL | Raza |
| fechaNacimiento | Date | nullable | Fecha de nacimiento estimada |
| sexo | String | max 1, ENUM('M','F') | Sexo |
| color | String | max 50 | Color del pelaje |
| senasParticulares | String | max 300 | Señas distintivas |
| estado | EstadoRegistro | NOT NULL, default 'Activo' | Activo/Inactivo |

**Regla de negocio:** RN-04 — Un turno no puede existir sin una mascota registrada (idMascota NOT NULL en turno).

---

### 6. Especie
**Estereotipo:** Catálogo | **Tabla BD:** `especie`

Catálogo predefinido de especies veterinarias. Implementa la estandarización de nomenclatura clínica mencionada en la elicitación (WVA, 2020).

| Atributo | Tipo | Restricciones | Descripción |
|----------|------|---------------|-------------|
| idEspecie | int | PK, AUTO_INCREMENT | Identificador único |
| nombre | String | NOT NULL, UNIQUE, max 50 | Nombre de la especie |

---

### 7. Raza
**Estereotipo:** Catálogo | **Tabla BD:** `raza`

Catálogo predefinido de razas, agrupadas por especie.

| Atributo | Tipo | Restricciones | Descripción |
|----------|------|---------------|-------------|
| idRaza | int | PK, AUTO_INCREMENT | Identificador único |
| idEspecie | int | FK → especie, NOT NULL | Especie a la que pertenece |
| nombre | String | NOT NULL, max 50 | Nombre de la raza |

---

### 8. Turno
**Estereotipo:** Entidad de dominio | **Tabla BD:** `turno`

Representa la reserva de un espacio temporal en la agenda del veterinario. Diferenciado de la Consulta Médica según RN-01: el turno es la planificación, la consulta es el acto asistencial.

| Atributo | Tipo | Restricciones | Descripción |
|----------|------|---------------|-------------|
| idTurno | int | PK, AUTO_INCREMENT | Identificador único |
| idMascota | int | FK → mascota, NOT NULL | Mascota para la que se reserva |
| idSlot | int | FK → slot_agenda, NOT NULL | Espacio de agenda reservado |
| motivo | String | max 300 | Motivo de la consulta |
| estado | EstadoTurno | NOT NULL, default 'Pendiente' | Pendiente/Atendido/Cancelado/Inasistencia |
| fechaRegistro | DateTime | NOT NULL, default NOW() | Fecha y hora de registro del turno |

**Reglas de negocio:**
- RN-04: idMascota NOT NULL (turno requiere mascota registrada)
- RN-05: Bloqueo concurrente al reservar (SELECT FOR UPDATE sobre slot)
- RN-08: Un slot admite máximo un turno activo
- RN-12: Solo puede pasar a "Atendido" si existe consulta asociada

---

### 9. ConsultaMedica
**Estereotipo:** Entidad de dominio | **Tabla BD:** `consulta_medica`

Representa el acto asistencial documentado. Es la entidad central de la historia clínica veterinaria. Diferenciada del Turno según RN-01.

| Atributo | Tipo | Restricciones | Descripción |
|----------|------|---------------|-------------|
| idConsulta | int | PK, AUTO_INCREMENT | Identificador único |
| idTurno | int | FK → turno, NULLABLE | Turno asociado (NULL = urgencia) |
| idMascota | int | FK → mascota, NOT NULL | Mascota atendida |
| idVeterinario | int | FK → veterinario, NOT NULL | Veterinario que atiende |
| fecha | DateTime | NOT NULL | Fecha y hora de la consulta |
| sintomas | String | NOT NULL | Síntomas observados |
| diagnostico | String | NOT NULL | Diagnóstico emitido |
| estado | EstadoConsulta | NOT NULL, default 'Activa' | Activa/Inactiva |
| fechaModificacion | DateTime | NULLABLE | Última fecha de modificación |
| idVeterinarioModificacion | int | FK → veterinario, NULLABLE | Vet que realizó la modificación |

**Reglas de negocio:**
- RN-01: idTurno admite NULL (urgencias sin turno previo)
- RN-07: Baja lógica obligatoria — no se permite DELETE
- RN-12: Turno solo pasa a "Atendido" cuando se guarda la consulta

---

### 10. Medicamento
**Estereotipo:** Catálogo maestro | **Tabla BD:** `medicamento`

Catálogo maestro de medicamentos (Inventario). Diferenciado del Stock según RN-02: el inventario es el catálogo, el stock son las unidades físicas por lote.

| Atributo | Tipo | Restricciones | Descripción |
|----------|------|---------------|-------------|
| idMedicamento | int | PK, AUTO_INCREMENT | Identificador único |
| nombreGenerico | String | NOT NULL, max 150 | Nombre genérico del principio activo |
| nombreComercial | String | NOT NULL, max 150 | Nombre comercial del producto |
| dosisPresentacion | String | NOT NULL, max 100 | Dosis y forma de presentación |
| precioVenta | Decimal | NOT NULL, DECIMAL(10,2) | Precio de venta al público |
| stockMinimoAlerta | int | NOT NULL, default 5 | Umbral mínimo para alerta de stock bajo |
| estado | EstadoRegistro | NOT NULL, default 'Activo' | Activo/Inactivo |
| fechaActualizacionPrecio | Date | NULLABLE | Fecha de última actualización de precio |

**Constraint BD:** UNIQUE (nombreComercial, dosisPresentacion) — impide duplicados.

---

### 11. Stock
**Estereotipo:** Entidad operativa | **Tabla BD:** `stock`

Representa las unidades físicas de un medicamento disponibles en el depósito, identificadas por lote y fecha de vencimiento. Diferenciado del Inventario (catálogo) según RN-02.

| Atributo | Tipo | Restricciones | Descripción |
|----------|------|---------------|-------------|
| idStock | int | PK, AUTO_INCREMENT | Identificador único del lote |
| idMedicamento | int | FK → medicamento, NOT NULL | Medicamento al que pertenece |
| cantidad | int | NOT NULL, >= 0 | Unidades disponibles |
| numeroLote | String | NOT NULL, max 50 | Número de lote del fabricante |
| fechaVencimiento | Date | NOT NULL | Fecha de vencimiento del lote |
| fechaIngreso | Date | NOT NULL, default CURDATE() | Fecha de ingreso al depósito |

**Reglas de negocio:**
- RN-02: Relación N:1 con Medicamento (un medicamento tiene múltiples lotes)
- RN-09: fechaVencimiento debe ser > CURDATE() al registrar
- RN-10: Descuento FIFO por vencimiento (ORDER BY fechaVencimiento ASC)

---

### 12. ItemReceta
**Estereotipo:** Entidad de dominio | **Tabla BD:** `item_receta`

Representa una línea de la receta médica dentro de una consulta. Vincula la consulta con un lote específico de stock, garantizando trazabilidad completa de qué medicamento de qué lote se dispensó a qué mascota.

| Atributo | Tipo | Restricciones | Descripción |
|----------|------|---------------|-------------|
| idItemReceta | int | PK, AUTO_INCREMENT | Identificador único |
| idConsulta | int | FK → consulta_medica, NOT NULL | Consulta donde se prescribe |
| idStock | int | FK → stock, NOT NULL | Lote de stock dispensado |
| cantidad | int | NOT NULL, > 0 | Cantidad recetada |
| dosis | String | NOT NULL, max 100 | Dosis indicada |
| frecuencia | String | max 100 | Frecuencia de administración |
| duracion | String | max 100 | Duración del tratamiento |
| dispensado | boolean | NOT NULL, default TRUE | Si se dispensó desde stock de la clínica |

---

### 13. AlertaStock
**Estereotipo:** Entidad del sistema | **Tabla BD:** `alerta_stock`

Registra las alertas generadas automáticamente por el sistema cuando el stock de un medicamento cae por debajo del umbral mínimo o cuando un lote está próximo a vencer.

| Atributo | Tipo | Restricciones | Descripción |
|----------|------|---------------|-------------|
| idAlerta | int | PK, AUTO_INCREMENT | Identificador único |
| idMedicamento | int | FK → medicamento, NOT NULL | Medicamento asociado |
| tipo | TipoAlerta | NOT NULL | STOCK_BAJO / VENCIMIENTO_PROXIMO |
| mensaje | String | NOT NULL, max 300 | Descripción de la alerta |
| estado | EstadoAlerta | NOT NULL, default 'Pendiente' | Pendiente/En Gestión/Resuelta |
| fechaGeneracion | DateTime | NOT NULL, default NOW() | Fecha de generación |
| fechaResolucion | DateTime | NULLABLE | Fecha de resolución |

**Regla de negocio:** RN-06 — Generación automática de alertas. Resolución automática al reponer stock.

---

## Enumeraciones

### EstadoTurno
- `PENDIENTE` — Turno registrado, espera atención
- `ATENDIDO` — Consulta médica completada
- `CANCELADO` — Turno cancelado por el dueño
- `INASISTENCIA` — El dueño no se presentó

### EstadoConsulta
- `ACTIVA` — Consulta vigente
- `INACTIVA` — Baja lógica (no eliminable, RN-07)

### EstadoSlot
- `DISPONIBLE` — Libre para reserva
- `RESERVADO` — Ocupado por un turno

### EstadoAlerta
- `PENDIENTE` — Generada, sin gestión
- `EN_GESTION` — En proceso de reposición
- `RESUELTA` — Stock repuesto o lote retirado

### TipoAlerta
- `STOCK_BAJO` — Stock total < umbral mínimo
- `VENCIMIENTO_PROXIMO` — Lote vence en <= 30 días

### EstadoRegistro
- `ACTIVO` — Registro vigente
- `INACTIVO` — Baja lógica (preserva datos para auditoría)

---

## Resumen de Relaciones

| Origen | Destino | Tipo | Cardinalidad | Regla de Negocio |
|--------|---------|------|:---:|------------------|
| Veterinario | AgendaDisponibilidad | Composición | 1:* | Las franjas pertenecen al veterinario |
| AgendaDisponibilidad | SlotAgenda | Composición | 1:* | Los slots se generan desde la franja |
| Dueno | Mascota | Composición | 1:* | Las mascotas pertenecen al dueño |
| Especie | Raza | Composición | 1:* | Las razas se agrupan por especie |
| Mascota | Especie | Asociación | *:1 | Una mascota es de una especie |
| Mascota | Raza | Asociación | *:1 | Una mascota es de una raza |
| Mascota | Turno | Asociación | 1:* | Una mascota tiene múltiples turnos |
| SlotAgenda | Turno | Agregación | 1:0..1 | Un slot tiene máximo un turno activo (RN-08) |
| Turno | ConsultaMedica | Asociación | 0..1:0..1 | Relación opcional bidireccional (RN-01) |
| Mascota | ConsultaMedica | Asociación | 1:* | Una mascota tiene múltiples consultas |
| Veterinario | ConsultaMedica | Asociación | 1:* | Un veterinario atiende múltiples consultas |
| ConsultaMedica | ItemReceta | Composición | 1:* | La receta pertenece a la consulta |
| Medicamento | Stock | Composición | 1:* | Un medicamento tiene múltiples lotes (RN-02) |
| Stock | ItemReceta | Asociación | 1:* | Un lote se dispensa en múltiples recetas |
| Medicamento | AlertaStock | Asociación | 1:* | Un medicamento genera múltiples alertas (RN-06) |
