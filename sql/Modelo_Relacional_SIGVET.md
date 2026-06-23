# Modelo Relacional - Base de Datos SIGVET
# Derivación del Diagrama ER y Verificación de Normalización

---

## 1. Transformación ER → Modelo Relacional

Cada entidad del diagrama ER se transforma en una tabla relacional. Las relaciones se implementan mediante claves foráneas (FK) siguiendo las reglas de transformación estándar.

### Reglas de transformación aplicadas

| Tipo de relación | Regla de transformación |
|------------------|------------------------|
| 1:N (uno a muchos) | La PK del lado "1" se incluye como FK en la tabla del lado "N" |
| 1:1 (uno a uno) | La PK del lado opcional se incluye como FK en la tabla del lado obligatorio |
| 1:0..1 (uno a cero-uno) | La PK del lado "1" se incluye como FK en la tabla del lado "0..1" |
| Composición | Se aplica la regla 1:N estándar; la tabla "parte" tiene FK a la tabla "todo" |
| Agregación | Igual que composición; la diferencia es semántica (ciclo de vida), no estructural |

---

## 2. Esquema Relacional Completo

### Tabla: veterinario

```
veterinario (
    id_veterinario  INT           AUTO_INCREMENT  PRIMARY KEY,
    nombre          VARCHAR(100)  NOT NULL,
    apellido        VARCHAR(100)  NOT NULL,
    matricula       VARCHAR(30)   NOT NULL  UNIQUE,
    telefono        VARCHAR(20)   NOT NULL,
    email           VARCHAR(150)  NULL,
    estado          ENUM('Activo','Inactivo')  NOT NULL  DEFAULT 'Activo'
)
```

**Origen ER:** Entidad VETERINARIO
**Dependencias funcionales:**
- id_veterinario → nombre, apellido, matricula, telefono, email, estado
- matricula → id_veterinario, nombre, apellido, telefono, email, estado (clave candidata)

---

### Tabla: agenda_disponibilidad

```
agenda_disponibilidad (
    id_agenda         INT           AUTO_INCREMENT  PRIMARY KEY,
    id_veterinario    INT           NOT NULL  FOREIGN KEY → veterinario(id_veterinario),
    dia_semana        ENUM('Lunes','Martes','Miercoles','Jueves','Viernes','Sabado','Domingo')  NOT NULL,
    hora_inicio       TIME          NOT NULL,
    hora_fin          TIME          NOT NULL
)
```

**Origen ER:** Entidad AGENDA_DISPONIBILIDAD + Relación "define" (1:N desde VETERINARIO)
**Dependencias funcionales:**
- id_agenda → id_veterinario, dia_semana, hora_inicio, hora_fin
- (id_veterinario, dia_semana, hora_inicio, hora_fin) → id_agenda (clave candidata compuesta)

---

### Tabla: slot_agenda

```
slot_agenda (
    id_slot       INT           AUTO_INCREMENT  PRIMARY KEY,
    id_agenda     INT           NOT NULL  FOREIGN KEY → agenda_disponibilidad(id_agenda),
    fecha         DATE          NOT NULL,
    hora          TIME          NOT NULL,
    estado        ENUM('Disponible','Reservado')  NOT NULL  DEFAULT 'Disponible'
)
```

**Origen ER:** Entidad SLOT_AGENDA + Relación "genera" (1:N desde AGENDA_DISPONIBILIDAD)
**Dependencias funcionales:**
- id_slot → id_agenda, fecha, hora, estado
- (id_agenda, fecha, hora) → id_slot, estado (clave candidata compuesta → UNIQUE constraint)

---

### Tabla: dueno

```
dueno (
    id_dueno      INT           AUTO_INCREMENT  PRIMARY KEY,
    dni           VARCHAR(20)   NOT NULL  UNIQUE,
    nombre        VARCHAR(100)  NOT NULL,
    apellido      VARCHAR(100)  NOT NULL,
    telefono      VARCHAR(20)   NOT NULL,
    direccion     VARCHAR(200)  NULL,
    email         VARCHAR(150)  NULL,
    estado        ENUM('Activo','Inactivo')  NOT NULL  DEFAULT 'Activo'
)
```

**Origen ER:** Entidad DUENO
**Dependencias funcionales:**
- id_dueno → dni, nombre, apellido, telefono, direccion, email, estado
- dni → id_dueno, nombre, apellido, telefono, direccion, email, estado (clave candidata)

---

### Tabla: especie

```
especie (
    id_especie    INT           AUTO_INCREMENT  PRIMARY KEY,
    nombre        VARCHAR(50)   NOT NULL  UNIQUE
)
```

**Origen ER:** Entidad ESPECIE
**Dependencias funcionales:**
- id_especie → nombre
- nombre → id_especie (clave candidata)

---

### Tabla: raza

```
raza (
    id_raza       INT           AUTO_INCREMENT  PRIMARY KEY,
    id_especie    INT           NOT NULL  FOREIGN KEY → especie(id_especie),
    nombre        VARCHAR(50)   NOT NULL
)
```

**Origen ER:** Entidad RAZA + Relación "agrupa" (1:N desde ESPECIE)
**Dependencias funcionales:**
- id_raza → id_especie, nombre
- (id_especie, nombre) → id_raza (clave candidata compuesta → UNIQUE constraint)

---

### Tabla: mascota

```
mascota (
    id_mascota            INT           AUTO_INCREMENT  PRIMARY KEY,
    id_dueno              INT           NOT NULL  FOREIGN KEY → dueno(id_dueno),
    nombre                VARCHAR(100)  NOT NULL,
    id_especie            INT           NOT NULL  FOREIGN KEY → especie(id_especie),
    id_raza               INT           NOT NULL  FOREIGN KEY → raza(id_raza),
    fecha_nacimiento      DATE          NULL,
    sexo                  ENUM('M','F') NULL,
    color                 VARCHAR(50)   NULL,
    senas_particulares    VARCHAR(300)  NULL,
    estado                ENUM('Activo','Inactivo')  NOT NULL  DEFAULT 'Activo'
)
```

**Origen ER:** Entidad MASCOTA + Relaciones "posee" (1:N desde DUENO), "clasifica" (N:1 a ESPECIE), "detalla" (N:1 a RAZA)
**Dependencias funcionales:**
- id_mascota → id_dueno, nombre, id_especie, id_raza, fecha_nacimiento, sexo, color, senas_particulares, estado

---

### Tabla: turno

```
turno (
    id_turno          INT           AUTO_INCREMENT  PRIMARY KEY,
    id_mascota        INT           NOT NULL  FOREIGN KEY → mascota(id_mascota),
    id_slot           INT           NOT NULL  FOREIGN KEY → slot_agenda(id_slot),
    motivo            VARCHAR(300)  NULL,
    estado            ENUM('Pendiente','Atendido','Cancelado','Inasistencia')  NOT NULL  DEFAULT 'Pendiente',
    fecha_registro    DATETIME      NOT NULL  DEFAULT CURRENT_TIMESTAMP
)
```

**Origen ER:** Entidad TURNO + Relaciones "solicita" (N:1 a MASCOTA), "contiene" (N:1 a SLOT_AGENDA)
**Dependencias funcionales:**
- id_turno → id_mascota, id_slot, motivo, estado, fecha_registro
- id_slot → id_turno (para turnos activos — implícito por RN-08, no por DF directa)

---

### Tabla: consulta_medica

```
consulta_medica (
    id_consulta               INT           AUTO_INCREMENT  PRIMARY KEY,
    id_turno                  INT           NULL  FOREIGN KEY → turno(id_turno),
    id_mascota                INT           NOT NULL  FOREIGN KEY → mascota(id_mascota),
    id_veterinario            INT           NOT NULL  FOREIGN KEY → veterinario(id_veterinario),
    fecha                     DATETIME      NOT NULL,
    sintomas                  TEXT          NOT NULL,
    diagnostico               TEXT          NOT NULL,
    estado                    ENUM('Activa','Inactiva')  NOT NULL  DEFAULT 'Activa',
    fecha_modificacion        DATETIME      NULL,
    id_veterinario_modif      INT           NULL  FOREIGN KEY → veterinario(id_veterinario)
)
```

**Origen ER:** Entidad CONSULTA_MEDICA + Relaciones "deriva en" (0..1 desde TURNO), "registra" (N:1 a MASCOTA), "atiende" (N:1 a VETERINARIO)
**Dependencias funcionales:**
- id_consulta → id_turno, id_mascota, id_veterinario, fecha, sintomas, diagnostico, estado, fecha_modificacion, id_veterinario_modif
- id_turno → id_consulta (clave candidata parcial — solo para turnos con estado 'Atendido')

**Nota:** id_turno es NULLABLE porque las consultas de urgencia no tienen turno previo (RN-01).

---

### Tabla: medicamento

```
medicamento (
    id_medicamento             INT             AUTO_INCREMENT  PRIMARY KEY,
    nombre_generico            VARCHAR(150)    NOT NULL,
    nombre_comercial           VARCHAR(150)    NOT NULL,
    dosis_presentacion         VARCHAR(100)    NOT NULL,
    precio_venta               DECIMAL(10,2)   NOT NULL,
    stock_minimo_alerta        INT             NOT NULL  DEFAULT 5,
    estado                     ENUM('Activo','Inactivo')  NOT NULL  DEFAULT 'Activo',
    fecha_actualizacion_precio DATE            NULL
)
```

**Origen ER:** Entidad MEDICAMENTO
**Dependencias funcionales:**
- id_medicamento → nombre_generico, nombre_comercial, dosis_presentacion, precio_venta, stock_minimo_alerta, estado, fecha_actualizacion_precio
- (nombre_comercial, dosis_presentacion) → id_medicamento, ... (clave candidata compuesta → UNIQUE constraint)

---

### Tabla: stock

```
stock (
    id_stock            INT           AUTO_INCREMENT  PRIMARY KEY,
    id_medicamento      INT           NOT NULL  FOREIGN KEY → medicamento(id_medicamento),
    cantidad             INT           NOT NULL  DEFAULT 0,
    numero_lote          VARCHAR(50)   NOT NULL,
    fecha_vencimiento    DATE          NOT NULL,
    fecha_ingreso        DATE          NOT NULL  DEFAULT (CURDATE())
)
```

**Origen ER:** Entidad STOCK + Relación "tiene lotes" (1:N desde MEDICAMENTO)
**Dependencias funcionales:**
- id_stock → id_medicamento, cantidad, numero_lote, fecha_vencimiento, fecha_ingreso
- (id_medicamento, numero_lote) → id_stock, cantidad, fecha_vencimiento, fecha_ingreso (clave candidata compuesta → UNIQUE constraint)

---

### Tabla: item_receta

```
item_receta (
    id_item_receta      INT           AUTO_INCREMENT  PRIMARY KEY,
    id_consulta         INT           NOT NULL  FOREIGN KEY → consulta_medica(id_consulta),
    id_stock            INT           NOT NULL  FOREIGN KEY → stock(id_stock),
    cantidad             INT           NOT NULL,
    dosis                VARCHAR(100)  NOT NULL,
    frecuencia           VARCHAR(100)  NULL,
    duracion             VARCHAR(100)  NULL,
    dispensado           TINYINT(1)    NOT NULL  DEFAULT 1
)
```

**Origen ER:** Entidad ITEM_RECETA + Relaciones "prescribe" (N:1 a CONSULTA_MEDICA), "se dispensa en" (N:1 a STOCK)
**Dependencias funcionales:**
- id_item_receta → id_consulta, id_stock, cantidad, dosis, frecuencia, duracion, dispensado

---

### Tabla: alerta_stock

```
alerta_stock (
    id_alerta           INT           AUTO_INCREMENT  PRIMARY KEY,
    id_medicamento      INT           NOT NULL  FOREIGN KEY → medicamento(id_medicamento),
    tipo                 ENUM('STOCK_BAJO','VENCIMIENTO_PROXIMO')  NOT NULL,
    mensaje              VARCHAR(300)  NOT NULL,
    estado               ENUM('Pendiente','En Gestion','Resuelta')  NOT NULL  DEFAULT 'Pendiente',
    fecha_generacion     DATETIME      NOT NULL  DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion     DATETIME      NULL
)
```

**Origen ER:** Entidad ALERTA_STOCK + Relación "genera" (N:1 a MEDICAMENTO)
**Dependencias funcionales:**
- id_alerta → id_medicamento, tipo, mensaje, estado, fecha_generacion, fecha_resolucion

---

## 3. Resumen de Claves

### Claves Primarias (PK)

| Tabla | PK | Tipo |
|-------|-----|------|
| veterinario | id_veterinario | INT AUTO_INCREMENT |
| agenda_disponibilidad | id_agenda | INT AUTO_INCREMENT |
| slot_agenda | id_slot | INT AUTO_INCREMENT |
| dueno | id_dueno | INT AUTO_INCREMENT |
| especie | id_especie | INT AUTO_INCREMENT |
| raza | id_raza | INT AUTO_INCREMENT |
| mascota | id_mascota | INT AUTO_INCREMENT |
| turno | id_turno | INT AUTO_INCREMENT |
| consulta_medica | id_consulta | INT AUTO_INCREMENT |
| medicamento | id_medicamento | INT AUTO_INCREMENT |
| stock | id_stock | INT AUTO_INCREMENT |
| item_receta | id_item_receta | INT AUTO_INCREMENT |
| alerta_stock | id_alerta | INT AUTO_INCREMENT |

### Claves Foráneas (FK)

| Tabla | FK | Referencia | ON DELETE | ON UPDATE | Nullable |
|-------|-----|-----------|-----------|-----------|----------|
| agenda_disponibilidad | id_veterinario | veterinario(id_veterinario) | RESTRICT | CASCADE | No |
| slot_agenda | id_agenda | agenda_disponibilidad(id_agenda) | RESTRICT | CASCADE | No |
| mascota | id_dueno | dueno(id_dueno) | RESTRICT | CASCADE | No |
| mascota | id_especie | especie(id_especie) | RESTRICT | CASCADE | No |
| mascota | id_raza | raza(id_raza) | RESTRICT | CASCADE | No |
| turno | id_mascota | mascota(id_mascota) | RESTRICT | CASCADE | No |
| turno | id_slot | slot_agenda(id_slot) | RESTRICT | CASCADE | No |
| consulta_medica | id_turno | turno(id_turno) | SET NULL | CASCADE | **Sí** (urgencias) |
| consulta_medica | id_mascota | mascota(id_mascota) | RESTRICT | CASCADE | No |
| consulta_medica | id_veterinario | veterinario(id_veterinario) | RESTRICT | CASCADE | No |
| consulta_medica | id_veterinario_modif | veterinario(id_veterinario) | SET NULL | CASCADE | **Sí** |
| stock | id_medicamento | medicamento(id_medicamento) | RESTRICT | CASCADE | No |
| item_receta | id_consulta | consulta_medica(id_consulta) | RESTRICT | CASCADE | No |
| item_receta | id_stock | stock(id_stock) | RESTRICT | CASCADE | No |
| alerta_stock | id_medicamento | medicamento(id_medicamento) | RESTRICT | CASCADE | No |

### Claves Únicas (UNIQUE)

| Tabla | Constraint | Columnas | Justificación |
|-------|-----------|----------|---------------|
| veterinario | uk_matricula | matricula | Matrícula profesional única |
| dueno | uk_dni | dni | DNI único por dueño |
| especie | uk_nombre_especie | nombre | Especie sin duplicados |
| raza | uk_especie_raza | id_especie, nombre | Raza única dentro de especie |
| medicamento | uk_med_dosis | nombre_comercial, dosis_presentacion | Medicamento único por nombre y dosis |
| slot_agenda | uk_slot_fecha_hora | id_agenda, fecha, hora | Slot único por agenda, fecha y hora |
| stock | uk_med_lote | id_medicamento, numero_lote | Lote único por medicamento |

---

## 4. Verificación de Normalización

### 4.1 Primera Forma Normal (1FN)

**Condición:** Todos los atributos son atómicos (indivisibles), no existen grupos repetitivos ni atributos multivaluados.

| Tabla | Cumple 1FN | Verificación |
|-------|:----------:|--------------|
| veterinario | ✅ | Todos los atributos son atómicos |
| agenda_disponibilidad | ✅ | dia_semana es un solo valor ENUM, no una lista |
| slot_agenda | ✅ | fecha y hora son valores únicos |
| dueno | ✅ | Todos atómicos |
| especie | ✅ | Un solo nombre |
| raza | ✅ | Un solo nombre por especie |
| mascota | ✅ | senas_particulares es un solo campo texto, no una lista |
| turno | ✅ | motivo es un solo texto |
| consulta_medica | ✅ | sintomas y diagnostico son campos TEXT atómicos |
| medicamento | ✅ | dosis_presentacion es un solo campo descripción |
| stock | ✅ | numero_lote es un solo valor |
| item_receta | ✅ | Todos atómicos |
| alerta_stock | ✅ | Todos atómicos |

**Nota sobre composición de PKs:** No existen PKs compuestas en el modelo final. Todas las PKs son surrogadas (AUTO_INCREMENT). Las UNIQUE constraints compuestas (ej. slot_agenda: id_agenda + fecha + hora) no violan 1FN porque son restricciones de unicidad, no PKs funcionales.

---

### 4.2 Segunda Forma Normal (2FN)

**Condición:** Está en 1FN y todo atributo no primo depende completamente de la clave primaria (no hay dependencias parciales).

Dado que todas las PKs son de un solo atributo (surrogadas AUTO_INCREMENT), **no es posible que existan dependencias parciales**. La 2FN se cumple automáticamente para todas las tablas.

| Tabla | Cumple 2FN | Justificación |
|-------|:----------:|---------------|
| Todas | ✅ | PKs de un solo atributo → no existen dependencias parciales |

---

### 4.3 Tercera Forma Normal (3FN)

**Condición:** Está en 2FN y ningún atributo no primo depende transitivamente de la PK (no hay dependencias A→B→C donde A es la PK).

| Tabla | Cumple 3FN | Análisis de dependencias transitivas |
|-------|:----------:|--------------------------------------|
| veterinario | ✅ | id_veterinario → todos los atributos directamente. matricula es clave candidata pero no crea dependencia transitiva. |
| agenda_disponibilidad | ✅ | id_agenda → todos los atributos directamente. id_veterinario es FK, no genera dependencia transitiva. |
| slot_agenda | ✅ | id_slot → todos los atributos directamente. id_agenda es FK hacia tabla independiente. |
| dueno | ✅ | id_dueno → todos los atributos directamente. dni es clave candidata sin dependencia transitiva. |
| especie | ✅ | id_especie → nombre. Sin atributos adicionales para transitividad. |
| raza | ✅ | id_raza → id_especie, nombre. id_especie es FK, no genera atributo dependiente. |
| mascota | ✅ | id_mascota → todos los atributos. Los FK (id_especie, id_raza) referencian tablas independientes. **No se incluye nombre_especie ni nombre_raza** porque están en sus tablas respectivas (se accede por JOIN). |
| turno | ✅ | id_turno → todos los atributos. Los FK referencian tablas independientes. |
| consulta_medica | ✅ | id_consulta → todos los atributos. id_veterinario_modif es FK independiente. |
| medicamento | ✅ | id_medicamento → todos los atributos directamente. |
| stock | ✅ | id_stock → todos los atributos. id_medicamento es FK hacia tabla independiente. |
| item_receta | ✅ | id_item_receta → todos los atributos. Los FK son referencias a tablas independientes. |
| alerta_stock | ✅ | id_alerta → todos los atributos. id_medicamento es FK hacia tabla independiente. |

**Caso de análisis especial — mascota:** En el modelo AS-IS del AP1, las fichas de papel incluían "especie" y "raza" como texto libre dentro de la ficha de la mascota, lo que generaba duplicación e inconsistencias. En el modelo propuesto, especie y raza se normalizan en tablas independientes con catálogos predefinidos, y la mascota solo almacena las FK `id_especie` e `id_raza`. Esto elimina la dependencia transitiva (id_mascota → id_especie → nombre_especie se resuelve separando especie en su propia tabla).

---

### 4.4 Conclusión de normalización

**El modelo relacional propuesto cumple con 1FN, 2FN y 3FN en todas sus tablas.**

No se identifican dependencias funcionales parciales (2FN) ni transitivas (3FN). Las decisiones de diseño que garantizan la normalización son:

1. **PKs surrogadas**: Evitan dependencias parciales de forma natural.
2. **Catálogos separados**: Especie, Raza y Medicamento están en tablas independientes, eliminando datos redundantes.
3. **Separación Inventario/Stock**: La tabla `medicamento` (catálogo) y `stock` (lotes) están separadas, evitando que datos maestros se repitan por cada lote.
4. **Baja lógica**: Los campos `estado` en vez de `DELETE` evitan la pérdida de datos que podría generar inconsistencias referenciales.
5. **Sin atributos calculados almacenados**: El stock total de un medicamento se calcula dinámicamente (`SUM(cantidad)`) y no se almacena como campo, evitando anomalías de actualización.

---

## 5. Grafo de Dependencias entre Tablas

Orden de creación de tablas respetando restricciones de FK (de independientes a dependientes):

```
Nivel 0 (sin FK salientes):
    veterinario, dueno, especie, medicamento

Nivel 1 (FK hacia Nivel 0):
    agenda_disponibilidad → veterinario
    raza → especie
    mascota → dueno, especie, raza
    stock → medicamento
    alerta_stock → medicamento

Nivel 2 (FK hacia Nivel 0-1):
    slot_agenda → agenda_disponibilidad
    turno → mascota, slot_agenda

Nivel 3 (FK hacia Nivel 0-2):
    consulta_medica → turno, mascota, veterinario

Nivel 4 (FK hacia Nivel 3):
    item_receta → consulta_medica, stock
```

**Orden de creación para el script DDL:**
1. veterinario
2. dueno
3. especie
4. raza
5. medicamento
6. mascota
7. agenda_disponibilidad
8. slot_agenda
9. stock
10. alerta_stock
11. turno
12. consulta_medica
13. item_receta
