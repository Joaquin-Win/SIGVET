# Documentacion de Casos de Prueba — Base de Datos SIGVET

## Sistema de Gestion Clinica y de Stock para Centros Veterinarios

---

## 1. Resumen de Casos de Prueba

| Codigo | Categoria | Tests | Tipo |
|--------|-----------|:-----:|------|
| T01 | Primary Key | 2 | Verificacion |
| T02 | Unique Constraints | 7 | Verificacion |
| T03 | Foreign Key Constraints | 6 | Verificacion + Error esperado |
| T04 | CHECK Constraints | 6 | Verificacion |
| T05 | Trigger RN-07 (Baja logica) | 2 | Verificacion + Error esperado |
| T06 | Trigger RN-08 (Slot-Turno) | 3 | Verificacion |
| T07 | Trigger RN-09 (Vencimiento) | 2 | Verificacion + Error esperado |
| T08 | Trigger RN-11 (Franjas) | 2 | Verificacion + Error esperado |
| T09 | Trigger RN-12 (Turno Atendido) | 2 | Verificacion + Error esperado |
| T10 | RN-06 (Alertas automaticas) | 4 | Verificacion + Ejecucion |
| T11 | SP Reservar Turno (RN-05) | 3 | Ejecucion + Error esperado |
| T12 | SP Descontar Stock FIFO (RN-10) | 3 | Ejecucion + Verificacion |
| T13 | SP Consulta con Turno (RN-01/12) | 3 | Ejecucion + Verificacion |
| T14 | SP Consulta Urgencia (RN-01) | 2 | Ejecucion + Verificacion |
| T15 | SP Anonimizar Dueno (RN-13) | 3 | Ejecucion + Verificacion |
| T16 | SP Cancelar Turno | 2 | Ejecucion + Verificacion |
| T17 | Funciones Escalares | 3 | Verificacion |
| T18 | Vistas | 4 | Verificacion |
| T19 | Consistencia General | 4 | Verificacion |
| **TOTAL** | | **61** | |

---

## 2. Detalle de Casos de Prueba

### T01: Primary Key

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T01.1 | PK veterinario sin duplicados | PASO si COUNT(*) = COUNT(DISTINCT id) |
| T01.2 | PKs sin duplicados en todas las 13 tablas | PASO si todas las tablas cumplen |

### T02: Unique Constraints

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T02.1 | UK matricula en veterinario sin duplicados | PASO si no hay duplicados |
| T02.2 | UK dni en dueno sin duplicados | PASO si no hay duplicados |
| T02.3 | UK nombre en especie sin duplicados | PASO si no hay duplicados |
| T02.4 | UK compuesta (id_especie, nombre) en raza | PASO si no hay duplicados |
| T02.5 | UK compuesta (nombre_comercial, dosis) en medicamento | PASO si no hay duplicados |
| T02.6 | UK compuesta (id_medicamento, numero_lote) en stock | PASO si no hay duplicados |
| T02.7 | UK compuesta (id_agenda, fecha, hora) en slot_agenda | PASO si no hay duplicados |

### T03: Foreign Key Constraints

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T03.1 | FK mascota -> dueno, especie, raza existen | PASO si no hay FK huerfanas |
| T03.2 | FK turno -> mascota, slot_agenda existen | PASO si no hay FK huerfanas |
| T03.3 | FK consulta_medica -> turno, mascota, veterinario existen | PASO si no hay FK huerfanas |
| T03.4 | FK item_receta -> consulta_medica, stock existen | PASO si no hay FK huerfanas |
| T03.5 | ON DELETE RESTRICT: dueno con mascotas no se puede eliminar | Error 1452 al intentar DELETE |
| T03.6 | ON DELETE SET NULL: consultas de urgencia con id_turno=NULL | PASO si existen urgencias |

### T04: CHECK Constraints

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T04.1 | precio_venta > 0 en medicamento | PASO si no hay precios <= 0 |
| T04.2 | cantidad >= 0 en stock | PASO si no hay cantidades negativas |
| T04.3 | stock_minimo_alerta >= 0 | PASO si no hay umbrales negativos |
| T04.4 | cantidad > 0 en item_receta | PASO si no hay cantidades <= 0 |
| T04.5 | hora_inicio < hora_fin en agenda | PASO si no hay franjas invertidas |
| T04.6 | Nombres no vacios tras TRIM | PASO si no hay nombres vacios |

### T05: Trigger RN-07 (Baja logica obligatoria)

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T05.1 | DELETE en consulta_medica debe fallar | Error 1644 (SIGNAL del trigger) |
| T05.2 | Baja logica (estado=Inactiva) funciona | PASO si existen consultas inactivas |

### T06: Trigger RN-08 (Un slot = un turno activo)

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T06.1 | Slots Reservados tienen exactamente 1 turno activo | PASO si no hay slots con >1 turno |
| T06.2 | Slots Disponibles no tienen turnos activos | PASO si no hay slots libres con turnos |
| T06.3 | Slot Reservado tiene turno activo asociado | PASO si no hay slots huerfanos |

### T07: Trigger RN-09 (Vencimiento futuro)

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T07.1 | Lotes con vencimiento futuro | PASO si los lotes vigentes son >= 0 |
| T07.2 | INSERT stock con fecha pasada debe fallar | Error 1644 (SIGNAL del trigger) |

### T08: Trigger RN-11 (Franjas no superpuestas)

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T08.1 | No hay franjas superpuestas | PASO si COUNT(*) = 0 en el JOIN |
| T08.2 | INSERT franja superpuesta debe fallar | Error 1644 (SIGNAL del trigger) |

### T09: Trigger RN-12 (Turno Atendido solo con consulta)

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T09.1 | Turnos Atendidos tienen consulta activa | PASO si no hay turnos sin consulta |
| T09.2 | UPDATE turno a Atendido sin consulta debe fallar | Error 1644 (SIGNAL del trigger) |

### T10: RN-06 (Alertas automaticas)

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T10.1 | Alertas STOCK_BAJO generadas automaticamente | PASO si existen alertas |
| T10.2 | Alertas STOCK_BAJO corresponden a stock real bajo | PASO si no hay inconsistencias |
| T10.3 | Alertas VENCIMIENTO_PROXIMO generadas | PASO si existen alertas |
| T10.4 | Alerta STOCK_BAJO se resuelve al reponer | PASO si hay alertas resueltas con fecha |

### T11: SP sp_reservar_turno (RN-05)

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T11.1 | Reserva exitosa crea turno y devuelve ID | PASO si ID > 0 |
| T11.2 | Slot cambia a Reservado | PASO si estado = 'Reservado' |
| T11.3 | Reservar mismo slot otra vez debe fallar | Error 1644 (bloqueo pesimista) |

### T12: SP sp_descontar_stock_fifo (RN-10)

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T12.1 | FIFO descuenta del lote mas proximo a vencer | PASO si lote1.cantidad disminuye |
| T12.2 | FIFO no afecta lotes posteriores | PASO si lote2.cantidad permanece igual |
| T12.3 | Descuento con stock insuficiente falla | Error 1644 (SIGNAL del SP) |

### T13: SP sp_registrar_consulta_turno (RN-01, RN-12)

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T13.1 | Registro exitoso crea consulta y devuelve ID | PASO si ID > 0 |
| T13.2 | Turno cambia a Atendido automaticamente | PASO si estado = 'Atendido' |
| T13.3 | Consulta tiene turno asociado | PASO si id_turno no es NULL |

### T14: SP sp_registrar_consulta_urgencia (RN-01)

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T14.1 | Registro exitoso crea consulta sin turno | PASO si ID > 0 |
| T14.2 | Consulta de urgencia tiene id_turno = NULL | PASO si id_turno IS NULL |

### T15: SP sp_anonimizar_dueno (RN-13)

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T15.1 | Datos personales anonimizados | PASO si nombre='ANONIMIZADO', email=NULL |
| T15.2 | DNI anonimizado con prefijo | PASO si dni LIKE 'ANON-%' |
| T15.3 | Mascotas del dueno se preservan | PASO si COUNT(*) > 0 |

### T16: SP sp_cancelar_turno

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T16.1 | Slot se libera al cancelar turno | PASO si estado = 'Disponible' |
| T16.2 | Turno queda en estado Cancelado | PASO si estado = 'Cancelado' |

### T17: Funciones Escalares

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T17.1 | fn_stock_total_medicamento retorna valor >= 0 | PASO si resultado >= 0 |
| T17.2 | fn_calcular_edad_mascota retorna edad > 0 | PASO si resultado IS NOT NULL |
| T17.3 | fn_calcular_edad_mascota retorna NULL si no hay fecha | PASO si resultado IS NULL |

### T18: Vistas

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T18.1 | vw_turnos_del_dia funciona | PASO si retorna filas sin error |
| T18.2 | vw_historial_clinico funciona | PASO si retorna filas sin error |
| T18.3 | vw_stock_medicamentos funciona | PASO si retorna filas sin error |
| T18.4 | vw_alertas_activas funciona | PASO si retorna filas sin error |

### T19: Consistencia General

| Codigo | Descripcion | Resultado esperado |
|--------|-------------|-------------------|
| T19.1 | Mascotas activas tienen dueno activo | PASO si COUNT(*) = 0 |
| T19.2 | No hay turnos Pendientes en fechas pasadas | PASO o ADVERTENCIA |
| T19.3 | Raza de mascota pertenece a su especie | PASO si COUNT(*) = 0 |
| T19.4 | Items receta referencian lotes vigentes | PASO si verificacion OK |

---

## 3. Clasificacion de Tests

### Tests de Verificacion (no modifican datos)
- T01, T02, T03.1-T03.4, T04, T06, T07.1, T08.1, T09.1, T10.1-T10.3, T17, T18, T19

### Tests de Ejecucion (modifican datos y verifican resultados)
- T10.4, T11.1-T11.2, T12.1-T12.2, T13, T14, T15, T16

### Tests de Error Esperado (deben fallar con error especifico)
- T03.5 (Error 1452), T03.6 (verificacion), T05.1 (Error 1644), T07.2 (Error 1644), T08.2 (Error 1644), T09.2 (Error 1644), T11.3 (Error 1644), T12.3 (Error 1644)

### Tests con Limpieza
- T10.4 (elimina stock de test), T12 (elimina lotes FIFO de test)

---

## 4. Instrucciones de Ejecucion

1. Ejecutar `sigvet_ddl.sql` (crea la base de datos y tablas)
2. Ejecutar `sigvet_dml.sql` (inserta datos de prueba)
3. Ejecutar `sigvet_test.sql` (ejecuta los tests automaticos)
4. Revisar los resultados en la tabla temporal `test_results`
5. Para los tests de error esperado: descomentar las lineas marcadas y ejecutar individualmente

---

## 5. Criterios de Aceptacion

| Resultado | Significado |
|-----------|-------------|
| **PASO** | El test se ejecuto correctamente y el resultado es el esperado |
| **FALLO** | El test fallo - hay un problema de integridad o logica |
| **ADVERTENCIA** | El test encontro una situacion no critica pero que requiere atencion |
| **Error 1452** | FK constraint violada (esperado en tests de DELETE) |
| **Error 1644** | SIGNAL SQLSTATE del trigger/SP (esperado en tests de reglas de negocio) |
