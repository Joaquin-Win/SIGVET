# Diccionario de Datos — Base de Datos SIGVET
# Sistema de Gestión Clínica y de Stock para Centros Veterinarios

---

## Convenciones utilizadas

| Símbolo | Significado |
|---------|-------------|
| **PK** | Clave primaria (Primary Key) |
| **FK** | Clave foránea (Foreign Key) |
| **UK** | Clave única (Unique Key) |
| **NN** | Not Null — valor obligatorio |
| **NULL** | Admite valores nulos (campo opcional) |
| **AI** | Auto Increment — se genera automáticamente |
| **DEF** | Valor por defecto |

---

## Tabla: veterinario

**Descripción:** Almacena los datos de los profesionales veterinarios que atienden en la clínica. Cada veterinario define su agenda de disponibilidad y atiende consultas médicas.

**Motor:** InnoDB | **Collation:** utf8mb4_general_ci | **Filas estimadas:** 1-5

| Columna | Tipo | NN | NULL | AI | PK/FK/UK | DEF | Descripción |
|---------|------|:--:|:----:|:--:|----------|-----|-------------|
| id_veterinario | INT | ✅ | ❌ | ✅ | **PK** | — | Identificador único del veterinario. Se genera automáticamente al insertar. |
| nombre | VARCHAR(100) | ✅ | ❌ | ❌ | — | — | Nombre de pila del veterinario. No admite números ni caracteres especiales. |
| apellido | VARCHAR(100) | ✅ | ❌ | ❌ | — | — | Apellido del veterinario. |
| matricula | VARCHAR(30) | ✅ | ❌ | ❌ | **UK** | — | Número de matrícula profesional habilitante. Único en el sistema. Formato: MP-XXXXX (Matrícula Provincial) o MN-XXXXX (Matrícula Nacional). |
| telefono | VARCHAR(20) | ✅ | ❌ | ❌ | — | — | Teléfono de contacto del veterinario. Puede incluir prefijo internacional. |
| email | VARCHAR(150) | ❌ | ✅ | ❌ | — | NULL | Correo electrónico del veterinario. Opcional. |
| estado | ENUM('Activo','Inactivo') | ✅ | ❌ | ❌ | — | 'Activo' | Estado del registro. 'Inactivo' equivale a baja lógica — el veterinario no aparece en la agenda pero sus consultas médicas se preservan. |

**Índices:**

| Nombre | Columna(s) | Tipo |
|--------|-----------|------|
| pk_veterinario | id_veterinario | PRIMARY |
| uk_matricula | matricula | UNIQUE |

---

## Tabla: agenda_disponibilidad

**Descripción:** Define las franjas horarias en las que cada veterinario está disponible para atender consultas. Un veterinario puede tener múltiples franjas (ej.: lunes mañana y lunes tarde). Las franjas no pueden superponerse (RN-11).

**Motor:** InnoDB | **Collation:** utf8mb4_general_ci | **Filas estimadas:** 5-20

| Columna | Tipo | NN | NULL | AI | PK/FK/UK | DEF | Descripción |
|---------|------|:--:|:----:|:--:|----------|-----|-------------|
| id_agenda | INT | ✅ | ❌ | ✅ | **PK** | — | Identificador único de la franja de disponibilidad. |
| id_veterinario | INT | ✅ | ❌ | ❌ | **FK→veterinario** | — | Veterinario al que pertenece esta franja. ON DELETE RESTRICT (no se puede eliminar un veterinario con franjas configuradas). |
| dia_semana | ENUM('Lunes','Martes','Miercoles','Jueves','Viernes','Sabado','Domingo') | ✅ | ❌ | ❌ | — | — | Día de la semana de la franja. Se utiliza 'Miercoles' sin tilde por restricción de ENUM en MySQL. |
| hora_inicio | TIME | ✅ | ❌ | ❌ | — | — | Hora de inicio de la franja de disponibilidad (ej.: '09:00:00'). Debe ser anterior a hora_fin. |
| hora_fin | TIME | ✅ | ❌ | ❌ | — | — | Hora de fin de la franja de disponibilidad (ej.: '13:00:00'). Debe ser posterior a hora_inicio. |

**Regla de negocio RN-11:** No se permiten dos franjas superpuestas para el mismo veterinario y día. Se implementa mediante trigger BEFORE INSERT/UPDATE.

**Índices:**

| Nombre | Columna(s) | Tipo |
|--------|-----------|------|
| pk_agenda | id_agenda | PRIMARY |
| idx_agenda_vet | id_veterinario | INDEX (para JOIN con veterinario) |

---

## Tabla: slot_agenda

**Descripción:** Representa un espacio individual de la agenda del veterinario, generado automáticamente a partir de las franjas de disponibilidad. Cada slot equivale a una consulta de duración estándar (30 minutos). Es la unidad atómica de la agenda.

**Motor:** InnoDB | **Collation:** utf8mb4_general_ci | **Filas estimadas:** 500-2000/mes

| Columna | Tipo | NN | NULL | AI | PK/FK/UK | DEF | Descripción |
|---------|------|:--:|:----:|:--:|----------|-----|-------------|
| id_slot | INT | ✅ | ❌ | ✅ | **PK** | — | Identificador único del slot. |
| id_agenda | INT | ✅ | ❌ | ❌ | **FK→agenda_disponibilidad** | — | Franja de disponibilidad a la que pertenece este slot. ON DELETE RESTRICT. |
| fecha | DATE | ✅ | ❌ | ❌ | — | — | Fecha específica del slot (ej.: '2026-05-15'). Se genera a partir del dia_semana de la franja para cada semana. |
| hora | TIME | ✅ | ❌ | ❌ | — | — | Hora específica del slot (ej.: '09:00:00'). Se genera cada 30 minutos desde hora_inicio hasta hora_fin de la franja. |
| estado | ENUM('Disponible','Reservado') | ✅ | ❌ | ❌ | — | 'Disponible' | Estado del slot. 'Disponible' = libre para reserva. 'Reservado' = tiene un turno activo asignado. |

**Regla de negocio RN-08:** Un slot admite como máximo un turno activo. UNIQUE constraint impide duplicados.

**Índices:**

| Nombre | Columna(s) | Tipo |
|--------|-----------|------|
| pk_slot | id_slot | PRIMARY |
| uk_slot_fecha_hora | id_agenda, fecha, hora | UNIQUE |
| idx_slot_fecha | fecha | INDEX (para consultas de agenda del día) |

---

## Tabla: dueno

**Descripción:** Almacena los datos de los dueños de mascotas. El dueño es el tomador de decisiones y el responsable del paciente veterinario en la relación triádica dueño-mascota-profesional. Los datos personales están amparados por la Ley 25.326 de Protección de Datos Personales (RN-13).

**Motor:** InnoDB | **Collation:** utf8mb4_general_ci | **Filas estimadas:** 100-500

| Columna | Tipo | NN | NULL | AI | PK/FK/UK | DEF | Descripción |
|---------|------|:--:|:----:|:--:|----------|-----|-------------|
| id_dueno | INT | ✅ | ❌ | ✅ | **PK** | — | Identificador único del dueño. |
| dni | VARCHAR(20) | ✅ | ❌ | ❌ | **UK** | — | Documento Nacional de Identidad del dueño. Único en el sistema. Formato: 8 dígitos numéricos (puede incluir puntos como separadores). |
| nombre | VARCHAR(100) | ✅ | ❌ | ❌ | — | — | Nombre de pila del dueño. |
| apellido | VARCHAR(100) | ✅ | ❌ | ❌ | — | — | Apellido del dueño. |
| telefono | VARCHAR(20) | ✅ | ❌ | ❌ | — | — | Teléfono de contacto principal. Puede ser fijo o celular. |
| direccion | VARCHAR(200) | ❌ | ✅ | ❌ | — | NULL | Dirección de residencia del dueño. Opcional. |
| email | VARCHAR(150) | ❌ | ✅ | ❌ | — | NULL | Correo electrónico del dueño. Opcional. |
| estado | ENUM('Activo','Inactivo') | ✅ | ❌ | ❌ | — | 'Activo' | Estado del registro. 'Inactivo' = baja lógica. Los datos se preservan para la trazabilidad de las consultas médicas de sus mascotas. |

**Regla de negocio RN-13:** Procedimiento `sp_anonimizar_dueno` disponible para cumplimiento de la Ley 25.326 (derecho de supresión).

**Índices:**

| Nombre | Columna(s) | Tipo |
|--------|-----------|------|
| pk_dueno | id_dueno | PRIMARY |
| uk_dni | dni | UNIQUE |
| idx_dueno_apellido | apellido, nombre | INDEX (para búsqueda por nombre) |

---

## Tabla: especie

**Descripción:** Catálogo predefinido de especies veterinarias. Implementa la estandarización de nomenclatura clínica recomendada por la Asociación Mundial Veterinaria (WVA, 2020). Evita el ingreso libre de especies inconsistentes.

**Motor:** InnoDB | **Collation:** utf8mb4_general_ci | **Filas estimadas:** 5-15

| Columna | Tipo | NN | NULL | AI | PK/FK/UK | DEF | Descripción |
|---------|------|:--:|:----:|:--:|----------|-----|-------------|
| id_especie | INT | ✅ | ❌ | ✅ | **PK** | — | Identificador único de la especie. |
| nombre | VARCHAR(50) | ✅ | ❌ | ❌ | **UK** | — | Nombre de la especie (ej.: 'Canino', 'Felino', 'Ave', 'Roedor', 'Reptil'). Único en el sistema. |

**Valores predefinidos sugeridos:** Canino, Felino, Ave, Roedor, Reptil, Bovino, Equino, Peces, Otro.

**Índices:**

| Nombre | Columna(s) | Tipo |
|--------|-----------|------|
| pk_especie | id_especie | PRIMARY |
| uk_nombre_especie | nombre | UNIQUE |

---

## Tabla: raza

**Descripción:** Catálogo predefinido de razas agrupadas por especie. Cada raza pertenece a una única especie.

**Motor:** InnoDB | **Collation:** utf8mb4_general_ci | **Filas estimadas:** 50-200

| Columna | Tipo | NN | NULL | AI | PK/FK/UK | DEF | Descripción |
|---------|------|:--:|:----:|:--:|----------|-----|-------------|
| id_raza | INT | ✅ | ❌ | ✅ | **PK** | — | Identificador único de la raza. |
| id_especie | INT | ✅ | ❌ | ❌ | **FK→especie** | — | Especie a la que pertenece la raza. ON DELETE RESTRICT (no se puede eliminar una especie con razas). |
| nombre | VARCHAR(50) | ✅ | ❌ | ❌ | — | — | Nombre de la raza (ej.: 'Labrador Retriever', 'Siames'). Debe ser único dentro de cada especie. |

**Índices:**

| Nombre | Columna(s) | Tipo |
|--------|-----------|------|
| pk_raza | id_raza | PRIMARY |
| uk_especie_raza | id_especie, nombre | UNIQUE |
| idx_raza_especie | id_especie | INDEX (para filtrar razas por especie) |

---

## Tabla: mascota

**Descripción:** Almacena los datos de los pacientes veterinarios. Toda la trazabilidad clínica del sistema se articula en torno a la mascota. Se vincula a un dueño, una especie y una raza mediante claves foráneas.

**Motor:** InnoDB | **Collation:** utf8mb4_general_ci | **Filas estimadas:** 100-1000

| Columna | Tipo | NN | NULL | AI | PK/FK/UK | DEF | Descripción |
|---------|------|:--:|:----:|:--:|----------|-----|-------------|
| id_mascota | INT | ✅ | ❌ | ✅ | **PK** | — | Identificador único de la mascota. |
| id_dueno | INT | ✅ | ❌ | ❌ | **FK→dueno** | — | Dueño responsable de la mascota. ON DELETE RESTRICT (no se puede eliminar un dueño con mascotas). |
| nombre | VARCHAR(100) | ✅ | ❌ | ❌ | — | — | Nombre de la mascota. No es único (pueden existir múltiples mascotas llamadas "Max"). |
| id_especie | INT | ✅ | ❌ | ❌ | **FK→especie** | — | Especie de la mascota. Seleccionada del catálogo predefinido. |
| id_raza | INT | ✅ | ❌ | ❌ | **FK→raza** | — | Raza de la mascota. Seleccionada del catálogo filtrado por especie. |
| fecha_nacimiento | DATE | ❌ | ✅ | ❌ | — | NULL | Fecha de nacimiento estimada de la mascota. Opcional porque muchas veces se desconoce. |
| sexo | ENUM('M','F') | ❌ | ✅ | ❌ | — | NULL | Sexo de la mascota: 'M' = Macho, 'F' = Hembra. Opcional. |
| color | VARCHAR(50) | ❌ | ✅ | ❌ | — | NULL | Color predominante del pelaje/plumaje. Opcional. |
| senas_particulares | VARCHAR(300) | ❌ | ✅ | ❌ | — | NULL | Señas particulares o distintivas de la mascota (manchas, cicatrices, etc.). Opcional. |
| estado | ENUM('Activo','Inactivo') | ✅ | ❌ | ❌ | — | 'Activo' | Estado del registro. 'Inactivo' = baja lógica. Las consultas médicas de la mascota se preservan. |

**Índices:**

| Nombre | Columna(s) | Tipo |
|--------|-----------|------|
| pk_mascota | id_mascota | PRIMARY |
| idx_mascota_dueno | id_dueno | INDEX (para listar mascotas de un dueño) |
| idx_mascota_nombre | nombre | INDEX (para búsqueda por nombre) |
| idx_mascota_especie | id_especie | INDEX (para filtrar por especie) |

---

## Tabla: turno

**Descripción:** Representa la reserva de un espacio temporal en la agenda del veterinario para la atención de una mascota. Diferenciado de la Consulta Médica según RN-01: el turno es la planificación, la consulta es el acto asistencial.

**Motor:** InnoDB | **Collation:** utf8mb4_general_ci | **Filas estimadas:** 500-3000/mes

| Columna | Tipo | NN | NULL | AI | PK/FK/UK | DEF | Descripción |
|---------|------|:--:|:----:|:--:|----------|-----|-------------|
| id_turno | INT | ✅ | ❌ | ✅ | **PK** | — | Identificador único del turno. |
| id_mascota | INT | ✅ | ❌ | ❌ | **FK→mascota** | — | Mascota para la que se reserva el turno. NOT NULL — un turno no puede existir sin mascota (RN-04). ON DELETE RESTRICT. |
| id_slot | INT | ✅ | ❌ | ❌ | **FK→slot_agenda** | — | Espacio de agenda reservado. ON DELETE RESTRICT. |
| motivo | VARCHAR(300) | ❌ | ✅ | ❌ | — | NULL | Motivo de la consulta indicado por el dueño. Opcional. |
| estado | ENUM('Pendiente','Atendido','Cancelado','Inasistencia') | ✅ | ❌ | ❌ | — | 'Pendiente' | Estado del turno: 'Pendiente' = espera atención; 'Atendido' = consulta completada; 'Cancelado' = cancelado por el dueño; 'Inasistencia' = el dueño no se presentó. |
| fecha_registro | DATETIME | ✅ | ❌ | ❌ | — | CURRENT_TIMESTAMP | Fecha y hora en que se registró el turno en el sistema. |

**Reglas de negocio:**
- RN-04: id_mascota NOT NULL (turno requiere mascota registrada)
- RN-05: Bloqueo concurrente al reservar (SELECT FOR UPDATE sobre slot)
- RN-08: Un slot admite máximo un turno activo (verificado por trigger)
- RN-12: Solo pasa a 'Atendido' cuando existe consulta asociada

**Índices:**

| Nombre | Columna(s) | Tipo |
|--------|-----------|------|
| pk_turno | id_turno | PRIMARY |
| idx_turno_slot | id_slot | INDEX (para verificar disponibilidad del slot) |
| idx_turno_mascota | id_mascota | INDEX (para listar turnos de una mascota) |
| idx_turno_estado | estado | INDEX (para filtrar por estado) |

---

## Tabla: consulta_medica

**Descripción:** Representa el acto asistencial documentado por el veterinario. Es la entidad central de la historia clínica veterinaria. Vincula la mascota, el veterinario, y opcionalmente el turno. No puede ser eliminada — solo se admite baja lógica (RN-07).

**Motor:** InnoDB | **Collation:** utf8mb4_general_ci | **Filas estimadas:** 500-3000/mes

| Columna | Tipo | NN | NULL | AI | PK/FK/UK | DEF | Descripción |
|---------|------|:--:|:----:|:--:|----------|-----|-------------|
| id_consulta | INT | ✅ | ❌ | ✅ | **PK** | — | Identificador único de la consulta médica. |
| id_turno | INT | ❌ | ✅ | ❌ | **FK→turno** | NULL | Turno asociado. NULLABLE porque las urgencias no tienen turno previo (RN-01). ON DELETE SET NULL (si se eliminara el turno, la consulta se preserva). |
| id_mascota | INT | ✅ | ❌ | ❌ | **FK→mascota** | — | Mascota atendida. Siempre NOT NULL — toda consulta es de una mascota. ON DELETE RESTRICT. |
| id_veterinario | INT | ✅ | ❌ | ❌ | **FK→veterinario** | — | Veterinario que realizó la consulta. ON DELETE RESTRICT. |
| fecha | DATETIME | ✅ | ❌ | ❌ | — | — | Fecha y hora en que se realizó/documentó la consulta. |
| sintomas | TEXT | ✅ | ❌ | ❌ | — | — | Síntomas observados durante la consulta. Texto libre con formato largo. NOT NULL — toda consulta debe registrar síntomas. |
| diagnostico | TEXT | ✅ | ❌ | ❌ | — | — | Diagnóstico emitido por el veterinario. NOT NULL — toda consulta debe registrar un diagnóstico. |
| estado | ENUM('Activa','Inactiva') | ✅ | ❌ | ❌ | — | 'Activa' | Estado de la consulta: 'Activa' = vigente; 'Inactiva' = baja lógica. Nunca se elimina físicamente (RN-07). |
| fecha_modificacion | DATETIME | ❌ | ✅ | ❌ | — | NULL | Fecha y hora de la última modificación. NULL si nunca se modificó. |
| id_veterinario_modif | INT | ❌ | ✅ | ❌ | **FK→veterinario** | NULL | Veterinario que realizó la última modificación. NULL si nunca se modificó. ON DELETE SET NULL. |

**Reglas de negocio:**
- RN-01: id_turno NULLABLE (urgencias sin turno)
- RN-07: Baja lógica obligatoria — trigger BEFORE DELETE impide eliminación física
- RN-12: Trigger BEFORE UPDATE impide 'Atendido' en turno sin consulta

**Índices:**

| Nombre | Columna(s) | Tipo |
|--------|-----------|------|
| pk_consulta | id_consulta | PRIMARY |
| idx_consulta_turno | id_turno | INDEX (para vincular con turno) |
| idx_consulta_mascota | id_mascota | INDEX (para historial clínico) |
| idx_consulta_vet | id_veterinario | INDEX (para consultas por veterinario) |
| idx_consulta_fecha | fecha | INDEX (para ordenamiento cronológico) |

---

## Tabla: medicamento

**Descripción:** Catálogo maestro de medicamentos (Inventario). Diferenciado del Stock según RN-02: el inventario es el catálogo de productos, el stock son las unidades físicas por lote. Un medicamento puede existir en el catálogo pero tener stock cero.

**Motor:** InnoDB | **Collation:** utf8mb4_general_ci | **Filas estimadas:** 50-200

| Columna | Tipo | NN | NULL | AI | PK/FK/UK | DEF | Descripción |
|---------|------|:--:|:----:|:--:|----------|-----|-------------|
| id_medicamento | INT | ✅ | ❌ | ✅ | **PK** | — | Identificador único del medicamento. |
| nombre_generico | VARCHAR(150) | ✅ | ❌ | ❌ | — | — | Nombre genérico del principio activo (ej.: 'Amoxicilina'). |
| nombre_comercial | VARCHAR(150) | ✅ | ❌ | ❌ | **UK(comp)** | — | Nombre comercial del producto (ej.: 'Amoxidal'). Parte de la clave única compuesta con dosis_presentacion. |
| dosis_presentacion | VARCHAR(100) | ✅ | ❌ | ❌ | **UK(comp)** | — | Dosis y forma de presentación (ej.: '500mg - 16 cápsulas'). Parte de la clave única compuesta con nombre_comercial. |
| precio_venta | DECIMAL(10,2) | ✅ | ❌ | ❌ | — | — | Precio de venta al público en pesos argentinos. Ej.: 4500.50 |
| stock_minimo_alerta | INT | ✅ | ❌ | ❌ | — | 5 | Umbral mínimo de unidades para generar alerta de stock bajo (RN-06). Cuando el stock total disponible cae por debajo de este valor, se genera una alerta. |
| estado | ENUM('Activo','Inactivo') | ✅ | ❌ | ❌ | — | 'Activo' | Estado del medicamento en el catálogo: 'Activo' = se prescribe y vende; 'Inactivo' = discontinuado (no se prescribe pero se mantiene en el catálogo para trazabilidad). |
| fecha_actualizacion_precio | DATE | ❌ | ✅ | ❌ | — | NULL | Fecha de la última actualización del precio de venta. NULL si nunca se actualizó desde el alta. |

**Índices:**

| Nombre | Columna(s) | Tipo |
|--------|-----------|------|
| pk_medicamento | id_medicamento | PRIMARY |
| uk_med_dosis | nombre_comercial, dosis_presentacion | UNIQUE |
| idx_med_generico | nombre_generico | INDEX (para búsqueda por nombre genérico) |

---

## Tabla: stock

**Descripción:** Registra las unidades físicas de un medicamento disponibles en el depósito, identificadas por lote y fecha de vencimiento. Diferenciado del Inventario (catálogo) según RN-02. Un medicamento puede tener múltiples registros de stock (diferentes lotes con distintos vencimientos).

**Motor:** InnoDB | **Collation:** utf8mb4_general_ci | **Filas estimadas:** 100-500

| Columna | Tipo | NN | NULL | AI | PK/FK/UK | DEF | Descripción |
|---------|------|:--:|:----:|:--:|----------|-----|-------------|
| id_stock | INT | ✅ | ❌ | ✅ | **PK** | — | Identificador único del registro de stock. |
| id_medicamento | INT | ✅ | ❌ | ❌ | **FK→medicamento** | — | Medicamento al que pertenece este lote. ON DELETE RESTRICT. |
| cantidad | INT | ✅ | ❌ | ❌ | — | 0 | Unidades físicas disponibles en el depósito para este lote. Puede ser 0 (lote agotado pero conservado para trazabilidad). No admite valores negativos. |
| numero_lote | VARCHAR(50) | ✅ | ❌ | ❌ | **UK(comp)** | — | Número de lote del fabricante (ej.: 'LOT-2026-A0042'). Parte de la clave única compuesta con id_medicamento. Permite trazabilidad en caso de retiro del mercado por alertas sanitarias de SENASA. |
| fecha_vencimiento | DATE | ✅ | ❌ | ❌ | — | — | Fecha de vencimiento del lote. Debe ser posterior a la fecha actual al momento del registro (RN-09). Los lotes vencidos no se ofrecen para dispensa pero se mantienen en el registro. |
| fecha_ingreso | DATE | ✅ | ❌ | ❌ | — | CURDATE() | Fecha en que el lote ingresó al depósito. |

**Reglas de negocio:**
- RN-02: Relación N:1 con Medicamento (un medicamento tiene múltiples lotes)
- RN-09: fecha_vencimiento > CURDATE() al registrar (trigger BEFORE INSERT)
- RN-10: Descuento FIFO por vencimiento (ORDER BY fecha_vencimiento ASC)

**Índices:**

| Nombre | Columna(s) | Tipo |
|--------|-----------|------|
| pk_stock | id_stock | PRIMARY |
| uk_med_lote | id_medicamento, numero_lote | UNIQUE |
| idx_stock_medicamento | id_medicamento | INDEX (para listar stock de un medicamento) |
| idx_stock_vencimiento | fecha_vencimiento | INDEX (para consultas de vencimientos próximos) |

---

## Tabla: item_receta

**Descripción:** Representa una línea de la receta médica dentro de una consulta. Vincula la consulta con un lote específico de stock, garantizando trazabilidad completa: qué medicamento de qué lote se dispensó a qué mascota en qué consulta.

**Motor:** InnoDB | **Collation:** utf8mb4_general_ci | **Filas estimadas:** 500-2000/mes

| Columna | Tipo | NN | NULL | AI | PK/FK/UK | DEF | Descripción |
|---------|------|:--:|:----:|:--:|----------|-----|-------------|
| id_item_receta | INT | ✅ | ❌ | ✅ | **PK** | — | Identificador único del item de receta. |
| id_consulta | INT | ✅ | ❌ | ❌ | **FK→consulta_medica** | — | Consulta médica donde se prescribe este medicamento. ON DELETE RESTRICT (no se puede eliminar una consulta con items de receta). |
| id_stock | INT | ✅ | ❌ | ❌ | **FK→stock** | — | Lote de stock del que se dispensa el medicamento. Permite trazabilidad al lote específico. ON DELETE RESTRICT. |
| cantidad | INT | ✅ | ❌ | ❌ | — | — | Cantidad de unidades recetadas. Debe ser > 0. |
| dosis | VARCHAR(100) | ✅ | ❌ | ❌ | — | — | Dosis indicada por el veterinario (ej.: '1 cápsula'). |
| frecuencia | VARCHAR(100) | ❌ | ✅ | ❌ | — | NULL | Frecuencia de administración (ej.: 'Cada 8 horas'). Opcional. |
| duracion | VARCHAR(100) | ❌ | ✅ | ❌ | — | NULL | Duración del tratamiento (ej.: '7 días'). Opcional. |
| dispensado | TINYINT(1) | ✅ | ❌ | ❌ | — | 1 | Indica si el medicamento fue dispensado desde el stock de la clínica (1 = sí, 0 = no). Si el dueño lo adquiere externamente, se registra la receta pero dispensado = 0. |

**Índices:**

| Nombre | Columna(s) | Tipo |
|--------|-----------|------|
| pk_item_receta | id_item_receta | PRIMARY |
| idx_item_consulta | id_consulta | INDEX (para listar items de una consulta) |
| idx_item_stock | id_stock | INDEX (para trazabilidad de lotes) |

---

## Tabla: alerta_stock

**Descripción:** Registra las alertas generadas automáticamente por el sistema cuando el stock de un medicamento cae por debajo del umbral mínimo (STOCK_BAJO) o cuando un lote está próximo a vencer dentro de los 30 días (VENCIMIENTO_PROXIMO). Las alertas de stock bajo se resuelven automáticamente cuando un ingreso de stock supera el umbral (RN-06).

**Motor:** InnoDB | **Collation:** utf8mb4_general_ci | **Filas estimadas:** 20-100

| Columna | Tipo | NN | NULL | AI | PK/FK/UK | DEF | Descripción |
|---------|------|:--:|:----:|:--:|----------|-----|-------------|
| id_alerta | INT | ✅ | ❌ | ✅ | **PK** | — | Identificador único de la alerta. |
| id_medicamento | INT | ✅ | ❌ | ❌ | **FK→medicamento** | — | Medicamento asociado a la alerta. ON DELETE RESTRICT. |
| tipo | ENUM('STOCK_BAJO','VENCIMIENTO_PROXIMO') | ✅ | ❌ | ❌ | — | — | Tipo de alerta: 'STOCK_BAJO' = stock total < umbral mínimo; 'VENCIMIENTO_PROXIMO' = lote vence en <= 30 días. |
| mensaje | VARCHAR(300) | ✅ | ❌ | ❌ | — | — | Descripción legible de la alerta (ej.: 'Stock de Amoxicilina: 3 unidades (mínimo: 10)'). Generado automáticamente por el sistema. |
| estado | ENUM('Pendiente','En Gestion','Resuelta') | ✅ | ❌ | ❌ | — | 'Pendiente' | Estado de la alerta: 'Pendiente' = generada sin gestión; 'En Gestion' = el recepcionista la está procesando; 'Resuelta' = stock repuesto o lote retirado. Las alertas STOCK_BAJO pueden resolverse automáticamente al ingresar stock. |
| fecha_generacion | DATETIME | ✅ | ❌ | ❌ | — | CURRENT_TIMESTAMP | Fecha y hora en que se generó la alerta. |
| fecha_resolucion | DATETIME | ❌ | ✅ | ❌ | — | NULL | Fecha y hora en que se resolvió la alerta. NULL si aún está pendiente o en gestión. |

**Regla de negocio RN-06:** Generación automática. Resolución automática de STOCK_BAJO al reponer.

**Índices:**

| Nombre | Columna(s) | Tipo |
|--------|-----------|------|
| pk_alerta | id_alerta | PRIMARY |
| idx_alerta_medicamento | id_medicamento | INDEX (para listar alertas de un medicamento) |
| idx_alerta_estado | estado | INDEX (para filtrar alertas pendientes) |
| idx_alerta_tipo | tipo | INDEX (para filtrar por tipo de alerta) |

---

## Resumen General del Diccionario

| Tabla | Columnas | PK | FK | UK | Índices |
|-------|:--------:|:--:|:--:|:--:|:-------:|
| veterinario | 7 | 1 | 0 | 1 | 2 |
| agenda_disponibilidad | 5 | 1 | 1 | 0 | 2 |
| slot_agenda | 5 | 1 | 1 | 1 | 3 |
| dueno | 8 | 1 | 0 | 1 | 3 |
| especie | 2 | 1 | 0 | 1 | 2 |
| raza | 3 | 1 | 1 | 1 | 3 |
| mascota | 10 | 1 | 3 | 0 | 4 |
| turno | 6 | 1 | 2 | 0 | 4 |
| consulta_medica | 10 | 1 | 4 | 0 | 5 |
| medicamento | 8 | 1 | 0 | 1 | 3 |
| stock | 6 | 1 | 1 | 1 | 4 |
| item_receta | 8 | 1 | 2 | 0 | 3 |
| alerta_stock | 7 | 1 | 1 | 0 | 4 |
| **TOTAL** | **85** | **13** | **16** | **7** | **41** |
