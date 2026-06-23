# Documentacion del Script DDL — Base de Datos SIGVET

## Sistema de Gestion Clinica y de Stock para Centros Veterinarios

---

## 1. Estructura General del Script

El script `sigvet_ddl.sql` se divide en 6 secciones:

| Seccion | Contenido | Cantidad |
|---------|-----------|:--------:|
| 1. Creacion de BD | CREATE DATABASE + CHARSET | 1 |
| 2. Tablas | CREATE TABLE con constraints | 13 |
| 3. Triggers | Reglas de negocio | 12 |
| 4. Procedimientos | SP transaccionales | 10 |
| 5. Funciones | Funciones escalares | 2 |
| 6. Vistas | Vistas auxiliares | 4 |

---

## 2. Orden de Creacion de Tablas

Las tablas se crean respetando el grafo de dependencias de FK:

```
Nivel 0 (sin FK salientes):
  1. veterinario
  2. dueno
  3. especie
  4. medicamento

Nivel 1 (FK -> Nivel 0):
  5. raza          -> especie
  6. mascota       -> dueno, especie, raza
  7. agenda_disponibilidad -> veterinario
  8. stock         -> medicamento
  9. alerta_stock  -> medicamento

Nivel 2 (FK -> Nivel 0-1):
  10. slot_agenda   -> agenda_disponibilidad
  11. turno         -> mascota, slot_agenda

Nivel 3 (FK -> Nivel 0-2):
  12. consulta_medica -> turno, mascota, veterinario

Nivel 4 (FK -> Nivel 3):
  13. item_receta   -> consulta_medica, stock
```

---

## 3. Detalle de Constraints por Tabla

### 3.1 veterinario

| Constraint | Tipo | Columna(s) | Detalle |
|-----------|------|-----------|---------|
| pk_veterinario | PRIMARY KEY | id_veterinario | AUTO_INCREMENT |
| uk_matricula | UNIQUE | matricula | Matricula profesional unica |
| chk_vet_nombre | CHECK | nombre | No vacio tras TRIM |
| chk_vet_apellido | CHECK | apellido | No vacio tras TRIM |
| chk_vet_matricula | CHECK | matricula | No vacio tras TRIM |
| chk_vet_telefono | CHECK | telefono | No vacio tras TRIM |
| chk_vet_email | CHECK | email | Formato email valido (NULL permitido) |

**Indices adicionales:**
- `idx_vet_apellido` (apellido, nombre) — busqueda por nombre

### 3.2 dueno

| Constraint | Tipo | Columna(s) | Detalle |
|-----------|------|-----------|---------|
| pk_dueno | PRIMARY KEY | id_dueno | AUTO_INCREMENT |
| uk_dni | UNIQUE | dni | DNI unico |
| chk_dueno_nombre | CHECK | nombre | No vacio |
| chk_dueno_apellido | CHECK | apellido | No vacio |
| chk_dueno_dni | CHECK | dni | No vacio |
| chk_dueno_telefono | CHECK | telefono | No vacio |
| chk_dueno_email | CHECK | email | Formato email valido |

**Indices adicionales:**
- `idx_dueno_apellido` (apellido, nombre) — busqueda por nombre

### 3.3 especie

| Constraint | Tipo | Columna(s) | Detalle |
|-----------|------|-----------|---------|
| pk_especie | PRIMARY KEY | id_especie | AUTO_INCREMENT |
| uk_nombre_especie | UNIQUE | nombre | Especie sin duplicados |
| chk_especie_nombre | CHECK | nombre | No vacio |

### 3.4 medicamento

| Constraint | Tipo | Columna(s) | Detalle |
|-----------|------|-----------|---------|
| pk_medicamento | PRIMARY KEY | id_medicamento | AUTO_INCREMENT |
| uk_med_dosis | UNIQUE | nombre_comercial, dosis_presentacion | Compuesta |
| chk_med_generico | CHECK | nombre_generico | No vacio |
| chk_med_comercial | CHECK | nombre_comercial | No vacio |
| chk_med_dosis | CHECK | dosis_presentacion | No vacio |
| chk_med_precio | CHECK | precio_venta | > 0 |
| chk_med_stock_min | CHECK | stock_minimo_alerta | >= 0 |

**Indices adicionales:**
- `idx_med_generico` (nombre_generico) — busqueda por generico

### 3.5 raza

| Constraint | Tipo | Columna(s) | Detalle |
|-----------|------|-----------|---------|
| pk_raza | PRIMARY KEY | id_raza | AUTO_INCREMENT |
| uk_especie_raza | UNIQUE | id_especie, nombre | Raza unica dentro de especie |
| fk_raza_especie | FOREIGN KEY | id_especie | -> especie, RESTRICT/CASCADE |
| chk_raza_nombre | CHECK | nombre | No vacio |

### 3.6 mascota

| Constraint | Tipo | Columna(s) | Detalle |
|-----------|------|-----------|---------|
| pk_mascota | PRIMARY KEY | id_mascota | AUTO_INCREMENT |
| fk_mascota_dueno | FOREIGN KEY | id_dueno | -> dueno, RESTRICT/CASCADE |
| fk_mascota_especie | FOREIGN KEY | id_especie | -> especie, RESTRICT/CASCADE |
| fk_mascota_raza | FOREIGN KEY | id_raza | -> raza, RESTRICT/CASCADE |
| chk_mascota_nombre | CHECK | nombre | No vacio |

### 3.7 agenda_disponibilidad

| Constraint | Tipo | Columna(s) | Detalle |
|-----------|------|-----------|---------|
| pk_agenda | PRIMARY KEY | id_agenda | AUTO_INCREMENT |
| fk_agenda_vet | FOREIGN KEY | id_veterinario | -> veterinario, RESTRICT/CASCADE |
| chk_agenda_horario | CHECK | hora_inicio, hora_fin | hora_inicio < hora_fin |

### 3.8 stock

| Constraint | Tipo | Columna(s) | Detalle |
|-----------|------|-----------|---------|
| pk_stock | PRIMARY KEY | id_stock | AUTO_INCREMENT |
| uk_med_lote | UNIQUE | id_medicamento, numero_lote | Lote unico por medicamento |
| fk_stock_med | FOREIGN KEY | id_medicamento | -> medicamento, RESTRICT/CASCADE |
| chk_stock_cantidad | CHECK | cantidad | >= 0 |
| chk_stock_lote | CHECK | numero_lote | No vacio |
| — | — | fecha_vencimiento | Validado por trigger trg_validar_vencimiento_stock (MySQL no permite CURDATE() en CHECK) |

### 3.9 alerta_stock

| Constraint | Tipo | Columna(s) | Detalle |
|-----------|------|-----------|---------|
| pk_alerta | PRIMARY KEY | id_alerta | AUTO_INCREMENT |
| fk_alerta_med | FOREIGN KEY | id_medicamento | -> medicamento, RESTRICT/CASCADE |
| chk_alerta_mensaje | CHECK | mensaje | No vacio |
| chk_alerta_fechas | CHECK | fecha_resolucion, fecha_generacion | resolucion >= generacion si no es NULL |

### 3.10 slot_agenda

| Constraint | Tipo | Columna(s) | Detalle |
|-----------|------|-----------|---------|
| pk_slot | PRIMARY KEY | id_slot | AUTO_INCREMENT |
| uk_slot_fecha_hora | UNIQUE | id_agenda, fecha, hora | Slot unico |
| fk_slot_agenda | FOREIGN KEY | id_agenda | -> agenda_disponibilidad, RESTRICT/CASCADE |

### 3.11 turno

| Constraint | Tipo | Columna(s) | Detalle |
|-----------|------|-----------|---------|
| pk_turno | PRIMARY KEY | id_turno | AUTO_INCREMENT |
| fk_turno_mascota | FOREIGN KEY | id_mascota | -> mascota, RESTRICT/CASCADE |
| fk_turno_slot | FOREIGN KEY | id_slot | -> slot_agenda, RESTRICT/CASCADE |

### 3.12 consulta_medica

| Constraint | Tipo | Columna(s) | Detalle |
|-----------|------|-----------|---------|
| pk_consulta | PRIMARY KEY | id_consulta | AUTO_INCREMENT |
| fk_consulta_turno | FOREIGN KEY | id_turno | -> turno, SET NULL/CASCADE (NULLABLE) |
| fk_consulta_mascota | FOREIGN KEY | id_mascota | -> mascota, RESTRICT/CASCADE |
| fk_consulta_vet | FOREIGN KEY | id_veterinario | -> veterinario, RESTRICT/CASCADE |
| fk_consulta_vet_mod | FOREIGN KEY | id_veterinario_modif | -> veterinario, SET NULL/CASCADE (NULLABLE) |
| chk_consulta_sintomas | CHECK | sintomas | No vacio |
| chk_consulta_diag | CHECK | diagnostico | No vacio |

### 3.13 item_receta

| Constraint | Tipo | Columna(s) | Detalle |
|-----------|------|-----------|---------|
| pk_item_receta | PRIMARY KEY | id_item_receta | AUTO_INCREMENT |
| fk_item_consulta | FOREIGN KEY | id_consulta | -> consulta_medica, RESTRICT/CASCADE |
| fk_item_stock | FOREIGN KEY | id_stock | -> stock, RESTRICT/CASCADE |
| chk_item_cantidad | CHECK | cantidad | > 0 |
| chk_item_dosis | CHECK | dosis | No vacio |

---

## 4. Triggers — Trazabilidad con Reglas de Negocio

| # | Trigger | Tabla | Evento | RN | Descripcion |
|---|---------|-------|--------|-----|-------------|
| 1 | trg_prevenir_eliminar_consulta | consulta_medica | BEFORE DELETE | RN-07 | Impide eliminacion fisica de consultas |
| 2 | trg_verificar_slot_disponible | turno | BEFORE INSERT | RN-08 | Verifica slot Disponible antes de reservar |
| 3 | trg_actualizar_slot_al_reservar | turno | AFTER INSERT | RN-08 | Cambia slot a Reservado al crear turno |
| 4 | trg_liberar_slot_al_cancelar | turno | AFTER UPDATE | RN-08 | Libera slot al cancelar turno |
| 5 | trg_validar_vencimiento_stock | stock | BEFORE INSERT | RN-09 | Valida vencimiento > CURDATE() al insertar |
| 6 | trg_validar_vencimiento_stock_update | stock | BEFORE UPDATE | RN-09 | Valida vencimiento > CURDATE() al actualizar |
| 7 | trg_validar_franjas_no_superpuestas | agenda_disponibilidad | BEFORE INSERT | RN-11 | Impide franjas superpuestas |
| 8 | trg_validar_franjas_no_superpuestas_update | agenda_disponibilidad | BEFORE UPDATE | RN-11 | Impide franjas superpuestas al modificar |
| 9 | trg_verificar_turno_atendido | turno | BEFORE UPDATE | RN-12 | Solo Atendido si existe consulta activa |
| 10 | trg_alerta_stock_bajo_insert | stock | AFTER INSERT | RN-06 | Verifica stock bajo al insertar stock |
| 11 | trg_alerta_stock_bajo_update | stock | AFTER UPDATE | RN-06 | Verifica stock bajo al actualizar stock |
| 12 | trg_alerta_vencimiento_insert | stock | AFTER INSERT | RN-06 | Genera alerta vencimiento proximo |
| 13 | trg_registro_modificacion_consulta | consulta_medica | BEFORE UPDATE | — | Registra fecha de modificacion automaticamente |

---

## 5. Procedimientos Almacenados

| # | SP | RN | Transaccional | Descripcion |
|---|-----|-----|:------------:|-------------|
| 1 | sp_verificar_stock_bajo | RN-06 | No | Verifica umbral y genera/resuelve alertas STOCK_BAJO |
| 2 | sp_generar_alerta_vencimiento | RN-06 | No | Genera alerta VENCIMIENTO_PROXIMO para un lote |
| 3 | sp_reservar_turno | RN-05, RN-08 | Si | Reserva turno con bloqueo pesimista (SELECT FOR UPDATE) |
| 4 | sp_cancelar_turno | — | No | Cancela turno y libera slot |
| 5 | sp_descontar_stock_fifo | RN-02, RN-10 | Si | Descuento FIFO por vencimiento, transaccional |
| 6 | sp_registrar_consulta_turno | RN-01, RN-12 | Si | Registra consulta asociada a turno y marca Atendido |
| 7 | sp_registrar_consulta_urgencia | RN-01 | No | Registra consulta sin turno (urgencia) |
| 8 | sp_baja_logica_consulta | RN-07 | No | Baja logica de consulta con registro de modificador |
| 9 | sp_anonimizar_dueno | RN-13 | No | Anonimiza datos personales (Ley 25.326) |
| 10 | sp_ingresar_stock | RN-06 | No | Registra ingreso de stock, dispara verificacion de alertas |

---

## 6. Funciones

| # | Funcion | Retorna | Descripcion |
|---|---------|---------|-------------|
| 1 | fn_stock_total_medicamento(p_id_medicamento) | INT | Stock total disponible (lotes no vencidos) |
| 2 | fn_calcular_edad_mascota(p_id_mascota) | INT | Edad en anos (NULL si no hay fecha nacimiento) |

---

## 7. Vistas

| # | Vista | Proposito | Tablas involucradas |
|---|-------|-----------|-------------------|
| 1 | vw_turnos_del_dia | Turnos del dia actual para pantalla del recepcionista | turno, slot_agenda, mascota, dueno, especie, raza, agenda_disponibilidad, veterinario |
| 2 | vw_historial_clinico | Historial clinico completo de mascotas | consulta_medica, mascota, dueno, especie, raza, veterinario, item_receta, stock, medicamento |
| 3 | vw_stock_medicamentos | Stock actual por medicamento con estado de umbral | medicamento, stock (+ fn_stock_total_medicamento) |
| 4 | vw_alertas_activas | Alertas pendientes/en gestion | alerta_stock, medicamento (+ fn_stock_total_medicamento) |

---

## 8. Convenciones de Nomenclatura

| Elemento | Prefijo | Ejemplo |
|----------|---------|---------|
| Tablas | Sin prefijo | `veterinario`, `consulta_medica` |
| PK | pk_ | `pk_veterinario` |
| FK | fk_ | `fk_mascota_dueno` |
| UK | uk_ | `uk_matricula` |
| CHECK | chk_ | `chk_med_precio` |
| Indices | idx_ | `idx_turno_estado` |
| Triggers | trg_ | `trg_verificar_slot_disponible` |
| Procedimientos | sp_ | `sp_reservar_turno` |
| Funciones | fn_ | `fn_stock_total_medicamento` |
| Vistas | vw_ | `vw_turnos_del_dia` |

---

## 9. Notas de Implementacion

### 9.1 ON DELETE / ON UPDATE
- **RESTRICT**: No se puede eliminar un registro padre si tiene hijos referenciandolo. Aplica a la mayoria de las FK.
- **SET NULL**: Se aplica en `consulta_medica.id_turno` (si se eliminara un turno, la consulta se preserva con turno NULL) y `consulta_medica.id_veterinario_modif` (si se eliminara al veterinario modificador).
- **CASCADE**: Aplica a ON UPDATE en todas las FK para mantener consistencia si cambia una PK.

### 9.2 Estrategia de Bloqueo Pesimista
El procedimiento `sp_reservar_turno` utiliza `SELECT ... FOR UPDATE` sobre la tabla `slot_agenda` dentro de una transaccion explicita. Esto garantiza que dos recepcionistas no puedan reservar el mismo slot simultaneamente (condicion de carrera). El bloqueo se mantiene hasta el COMMIT o ROLLBACK.

### 9.3 Logica FIFO (RN-10)
El procedimiento `sp_descontar_stock_fifo` implementa la logica FIFO por fecha de vencimiento:
1. Selecciona lotes no vencidos con stock disponible, ordenados por `fecha_vencimiento ASC`
2. Descuenta del lote mas proximo a vencer primero
3. Si un lote no alcanza, pasa al siguiente
4. Si el stock total es insuficiente, lanza error y revierte (ROLLBACK)
5. La operacion es atomica: se ejecuta completamente o no se ejecuta nada

### 9.4 Baja Logica (RN-07)
- Las consultas medicas nunca se eliminan fisicamente (trigger `trg_prevenir_eliminar_consulta` lo impide)
- La baja se realiza cambiando `estado` a 'Inactiva' via el procedimiento `sp_baja_logica_consulta`
- Se registra automaticamente `fecha_modificacion` y `id_veterinario_modif`

### 9.5 Verificacion de Vencimiento (RN-09)
Se implementa doble validacion:
1. **Trigger** `trg_validar_vencimiento_stock` con mensaje de error descriptivo

El trigger verifica que `fecha_vencimiento > CURDATE()` al momento del INSERT/UPDATE.

**Nota:** MySQL no permite funciones no deterministas como `CURDATE()` en restricciones CHECK (Error 3814).
Por lo tanto, la unica forma de implementar esta validacion es mediante trigger.

---

## 10. Estadisticas del Script

| Metrica | Valor |
|---------|:-----:|
| Tablas | 13 |
| PK constraints | 13 |
| FK constraints | 16 |
| UK constraints | 7 |
| CHECK constraints | 24 |
| Indices (excluyendo PK/UK) | 16 |
| Triggers | 13 |
| Procedimientos almacenados | 10 |
| Funciones | 2 |
| Vistas | 4 |
| Total de objetos de BD | **105** |
