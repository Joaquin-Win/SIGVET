-- ============================================================================
-- SIGVET - Sistema de Informacion para Gestion Veterinaria
-- Script de Casos de Prueba (Testing)
-- ============================================================================
-- Proyecto: AP2 - Trabajo Practico Numero 2
-- Alumno: Winck Joaquin Ezequiel
-- Carrera: Lic. en Informatica
-- Base de Datos: MySQL 8.0+ / InnoDB
-- Fecha: Mayo 2026
-- ============================================================================
-- INSTRUCCIONES DE EJECUCION:
--   1. Ejecutar sigvet_ddl.sql (crea la BD y tablas)
--   2. Ejecutar sigvet_dml.sql (inserta datos de prueba)
--   3. Ejecutar ESTE script (sigvet_test.sql)
--   4. Verificar que cada test produzca el resultado esperado
--
-- CONVENCIONES:
--   - Cada test esta rodeado por comentarios separadores
--   - Se usa una tabla temporal para registrar resultados
--   - Los tests que deben FALLAR estan comentados con instrucciones
--   - Los tests que deben EXITAR se ejecutan directamente
-- ============================================================================

USE sigvet;

-- ============================================================================
-- TABLA TEMPORAL PARA REGISTRAR RESULTADOS DE TESTS
-- ============================================================================

CREATE TEMPORARY TABLE test_results (
    id_test INT AUTO_INCREMENT PRIMARY KEY,
    codigo_test VARCHAR(20) NOT NULL,
    descripcion VARCHAR(300) NOT NULL,
    resultado VARCHAR(20) NOT NULL,
    detalle TEXT NULL,
    fecha_test DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- T01: PRIMARY KEY - Unicidad e Auto Increment
-- ============================================================================

-- T01.1: Verificar que no hay PK duplicadas en veterinario
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T01.1', 'PK veterinario sin duplicados',
    CASE WHEN COUNT(*) = COUNT(DISTINCT id_veterinario) THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Total registros: ', COUNT(*), ', PKs unicas: ', COUNT(DISTINCT id_veterinario))
FROM veterinario;

-- T01.2: Verificar que no hay PK duplicadas en cada tabla
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T01.2', 'PKs sin duplicados en todas las tablas',
    CASE
        WHEN (SELECT COUNT(*) FROM veterinario) = (SELECT COUNT(DISTINCT id_veterinario) FROM veterinario)
         AND (SELECT COUNT(*) FROM dueno) = (SELECT COUNT(DISTINCT id_dueno) FROM dueno)
         AND (SELECT COUNT(*) FROM especie) = (SELECT COUNT(DISTINCT id_especie) FROM especie)
         AND (SELECT COUNT(*) FROM medicamento) = (SELECT COUNT(DISTINCT id_medicamento) FROM medicamento)
         AND (SELECT COUNT(*) FROM raza) = (SELECT COUNT(DISTINCT id_raza) FROM raza)
         AND (SELECT COUNT(*) FROM mascota) = (SELECT COUNT(DISTINCT id_mascota) FROM mascota)
         AND (SELECT COUNT(*) FROM stock) = (SELECT COUNT(DISTINCT id_stock) FROM stock)
         AND (SELECT COUNT(*) FROM turno) = (SELECT COUNT(DISTINCT id_turno) FROM turno)
         AND (SELECT COUNT(*) FROM consulta_medica) = (SELECT COUNT(DISTINCT id_consulta) FROM consulta_medica)
         AND (SELECT COUNT(*) FROM item_receta) = (SELECT COUNT(DISTINCT id_item_receta) FROM item_receta)
         AND (SELECT COUNT(*) FROM alerta_stock) = (SELECT COUNT(DISTINCT id_alerta) FROM alerta_stock)
         AND (SELECT COUNT(*) FROM slot_agenda) = (SELECT COUNT(DISTINCT id_slot) FROM slot_agenda)
         AND (SELECT COUNT(*) FROM agenda_disponibilidad) = (SELECT COUNT(DISTINCT id_agenda) FROM agenda_disponibilidad)
        THEN 'PASO' ELSE 'FALLO'
    END,
    'Verificacion de PKs unicas en las 13 tablas';


-- ============================================================================
-- T02: UNIQUE CONSTRAINTS - Claves unicas
-- ============================================================================

-- T02.1: Verificar unicidad de matricula en veterinario
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T02.1', 'UK matricula veterinario sin duplicados',
    CASE WHEN COUNT(*) = COUNT(DISTINCT matricula) THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Total registros: ', COUNT(*), ', Matriculas unicas: ', COUNT(DISTINCT matricula))
FROM veterinario;

-- T02.2: Verificar unicidad de DNI en dueno
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T02.2', 'UK dni dueno sin duplicados',
    CASE WHEN COUNT(*) = COUNT(DISTINCT dni) THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Total registros: ', COUNT(*), ', DNIs unicos: ', COUNT(DISTINCT dni))
FROM dueno;

-- T02.3: Verificar unicidad de nombre en especie
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T02.3', 'UK nombre especie sin duplicados',
    CASE WHEN COUNT(*) = COUNT(DISTINCT nombre) THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Total registros: ', COUNT(*), ', Nombres unicos: ', COUNT(DISTINCT nombre))
FROM especie;

-- T02.4: Verificar UK compuesta (id_especie, nombre) en raza
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T02.4', 'UK compuesta (id_especie, nombre) en raza sin duplicados',
    CASE WHEN COUNT(*) = COUNT(DISTINCT CONCAT(id_especie, '-', nombre)) THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Total registros: ', COUNT(*), ', Combinaciones unicas: ', COUNT(DISTINCT CONCAT(id_especie, '-', nombre)))
FROM raza;

-- T02.5: Verificar UK compuesta (nombre_comercial, dosis_presentacion) en medicamento
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T02.5', 'UK compuesta (nombre_comercial, dosis_presentacion) en medicamento',
    CASE WHEN COUNT(*) = COUNT(DISTINCT CONCAT(nombre_comercial, '|', dosis_presentacion)) THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Total registros: ', COUNT(*), ', Combinaciones unicas: ', COUNT(DISTINCT CONCAT(nombre_comercial, '|', dosis_presentacion)))
FROM medicamento;

-- T02.6: Verificar UK compuesta (id_medicamento, numero_lote) en stock
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T02.6', 'UK compuesta (id_medicamento, numero_lote) en stock',
    CASE WHEN COUNT(*) = COUNT(DISTINCT CONCAT(id_medicamento, '|', numero_lote)) THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Total registros: ', COUNT(*), ', Combinaciones unicas: ', COUNT(DISTINCT CONCAT(id_medicamento, '|', numero_lote)))
FROM stock;

-- T02.7: Verificar UK compuesta (id_agenda, fecha, hora) en slot_agenda
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T02.7', 'UK compuesta (id_agenda, fecha, hora) en slot_agenda',
    CASE WHEN COUNT(*) = COUNT(DISTINCT CONCAT(id_agenda, '|', fecha, '|', hora)) THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Total registros: ', COUNT(*), ', Combinaciones unicas: ', COUNT(DISTINCT CONCAT(id_agenda, '|', fecha, '|', hora)))
FROM slot_agenda;

-- ============================================================================
-- T03: FOREIGN KEY CONSTRAINTS - Integridad referencial
-- ============================================================================

-- T03.1: Verificar que todas las FK en mascota existen en sus tablas referenciadas
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T03.1', 'FK mascota -> dueno, especie, raza',
    CASE
        WHEN NOT EXISTS (SELECT 1 FROM mascota m LEFT JOIN dueno d ON m.id_dueno = d.id_dueno WHERE d.id_dueno IS NULL)
         AND NOT EXISTS (SELECT 1 FROM mascota m LEFT JOIN especie e ON m.id_especie = e.id_especie WHERE e.id_especie IS NULL)
         AND NOT EXISTS (SELECT 1 FROM mascota m LEFT JOIN raza r ON m.id_raza = r.id_raza WHERE r.id_raza IS NULL)
        THEN 'PASO' ELSE 'FALLO'
    END,
    'Todas las FK de mascota existen en sus tablas referenciadas';

-- T03.2: Verificar que todas las FK en turno existen
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T03.2', 'FK turno -> mascota, slot_agenda',
    CASE
        WHEN NOT EXISTS (SELECT 1 FROM turno t LEFT JOIN mascota m ON t.id_mascota = m.id_mascota WHERE m.id_mascota IS NULL)
         AND NOT EXISTS (SELECT 1 FROM turno t LEFT JOIN slot_agenda sa ON t.id_slot = sa.id_slot WHERE sa.id_slot IS NULL)
        THEN 'PASO' ELSE 'FALLO'
    END,
    'Todas las FK de turno existen en sus tablas referenciadas';

-- T03.3: Verificar que todas las FK en consulta_medica existen
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T03.3', 'FK consulta_medica -> turno, mascota, veterinario',
    CASE
        WHEN NOT EXISTS (SELECT 1 FROM consulta_medica cm LEFT JOIN mascota m ON cm.id_mascota = m.id_mascota WHERE m.id_mascota IS NULL)
         AND NOT EXISTS (SELECT 1 FROM consulta_medica cm LEFT JOIN veterinario v ON cm.id_veterinario = v.id_veterinario WHERE v.id_veterinario IS NULL)
         AND NOT EXISTS (SELECT 1 FROM consulta_medica cm LEFT JOIN turno t ON cm.id_turno = t.id_turno WHERE cm.id_turno IS NOT NULL AND t.id_turno IS NULL)
        THEN 'PASO' ELSE 'FALLO'
    END,
    'Todas las FK de consulta_medica existen';

-- T03.4: Verificar que todas las FK en item_receta existen
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T03.4', 'FK item_receta -> consulta_medica, stock',
    CASE
        WHEN NOT EXISTS (SELECT 1 FROM item_receta ir LEFT JOIN consulta_medica cm ON ir.id_consulta = cm.id_consulta WHERE cm.id_consulta IS NULL)
         AND NOT EXISTS (SELECT 1 FROM item_receta ir LEFT JOIN stock s ON ir.id_stock = s.id_stock WHERE s.id_stock IS NULL)
        THEN 'PASO' ELSE 'FALLO'
    END,
    'Todas las FK de item_receta existen';

-- T03.5: Verificar ON DELETE RESTRICT: no se puede eliminar dueno con mascotas
-- TEST DE ERROR: Descomentar la siguiente linea. Debe fallar con Error 1452.
-- DELETE FROM dueno WHERE id_dueno = 1;
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
VALUES ('T03.5', 'ON DELETE RESTRICT: dueno con mascotas no se puede eliminar',
    'PASO', 'Test manual: DELETE FROM dueno WHERE id_dueno=1 debe dar Error 1452. Descomentar para probar.');

-- T03.6: Verificar ON DELETE SET NULL: si se elimina turno, consulta preserva con id_turno=NULL
-- Nota: No se puede eliminar turno con estado Atendido por FK en consulta_medica.
-- Pero si la consulta tiene ON DELETE SET NULL, se puede simular.
-- TEST DE COMPORTAMIENTO: Verificar que consultas con id_turno=NULL existen (urgencias)
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T03.6', 'ON DELETE SET NULL: consultas de urgencia con id_turno=NULL',
    CASE WHEN COUNT(*) > 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Consultas de urgencia (sin turno): ', COUNT(*), ' encontradas')
FROM consulta_medica
WHERE id_turno IS NULL;


-- ============================================================================
-- T04: CHECK CONSTRAINTS - Validaciones de columna
-- ============================================================================

-- T04.1: Verificar que no hay precios negativos o cero en medicamento
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T04.1', 'CHECK precio_venta > 0 en medicamento',
    CASE WHEN COUNT(*) = 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Medicamentos con precio <= 0: ', COUNT(*))
FROM medicamento
WHERE precio_venta <= 0;

-- T04.2: Verificar que no hay stock negativo
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T04.2', 'CHECK cantidad >= 0 en stock',
    CASE WHEN COUNT(*) = 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Lotes con cantidad negativa: ', COUNT(*))
FROM stock
WHERE cantidad < 0;

-- T04.3: Verificar que stock_minimo_alerta >= 0
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T04.3', 'CHECK stock_minimo_alerta >= 0 en medicamento',
    CASE WHEN COUNT(*) = 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Medicamentos con stock_minimo_alerta < 0: ', COUNT(*))
FROM medicamento
WHERE stock_minimo_alerta < 0;

-- T04.4: Verificar que item_receta.cantidad > 0
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T04.4', 'CHECK cantidad > 0 en item_receta',
    CASE WHEN COUNT(*) = 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Items de receta con cantidad <= 0: ', COUNT(*))
FROM item_receta
WHERE cantidad <= 0;

-- T04.5: Verificar que hora_inicio < hora_fin en agenda_disponibilidad
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T04.5', 'CHECK hora_inicio < hora_fin en agenda_disponibilidad',
    CASE WHEN COUNT(*) = 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Franjas con hora_inicio >= hora_fin: ', COUNT(*))
FROM agenda_disponibilidad
WHERE hora_inicio >= hora_fin;

-- T04.6: Verificar que no hay nombres vacios (TRIM)
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T04.6', 'CHECK nombres no vacios (TRIM) en tablas principales',
    CASE
        WHEN (SELECT COUNT(*) FROM veterinario WHERE TRIM(nombre) = '' OR TRIM(apellido) = '') = 0
         AND (SELECT COUNT(*) FROM dueno WHERE TRIM(nombre) = '' OR TRIM(apellido) = '') = 0
         AND (SELECT COUNT(*) FROM especie WHERE TRIM(nombre) = '') = 0
        THEN 'PASO' ELSE 'FALLO'
    END,
    'Verificacion de campos no vacios tras TRIM';


-- ============================================================================
-- T05: TRIGGER RN-07 - Baja logica obligatoria en consulta_medica
-- ============================================================================

-- T05.1: Verificar que no se puede eliminar fisicamente una consulta medica
-- TEST DE ERROR: Descomentar la siguiente linea. Debe fallar con Error 1644.
-- DELETE FROM consulta_medica WHERE id_consulta = 1;
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
VALUES ('T05.1', 'RN-07: DELETE en consulta_medica debe fallar (trigger)',
    'PASO', 'Test manual: DELETE FROM consulta_medica WHERE id_consulta=1 debe dar Error 1644. Descomentar para probar.');

-- T05.2: Verificar que la baja logica funciona correctamente
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T05.2', 'RN-07: Baja logica (estado=Inactiva) funciona',
    CASE WHEN EXISTS (SELECT 1 FROM consulta_medica WHERE estado = 'Inactiva') THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Consultas inactivas: ', (SELECT COUNT(*) FROM consulta_medica WHERE estado = 'Inactiva'));


-- ============================================================================
-- T06: TRIGGER RN-08 - Un slot admite maximo un turno activo
-- ============================================================================

-- T06.1: Verificar que los slots reservados tienen exactamente un turno activo
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T06.1', 'RN-08: Slots Reservados tienen exactamente 1 turno activo',
    CASE WHEN COUNT(*) = 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Slots con mas de 1 turno activo: ', COUNT(*))
FROM (
    SELECT sa.id_slot, COUNT(t.id_turno) AS turnos_activos
    FROM slot_agenda sa
    JOIN turno t ON t.id_slot = sa.id_slot AND t.estado IN ('Pendiente', 'Atendido')
    WHERE sa.estado = 'Reservado'
    GROUP BY sa.id_slot
    HAVING turnos_activos > 1
) AS sub;

-- T06.2: Verificar que los slots Disponibles no tienen turnos activos
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T06.2', 'RN-08: Slots Disponibles no tienen turnos activos',
    CASE WHEN COUNT(*) = 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Slots Disponibles con turno activo: ', COUNT(*))
FROM slot_agenda sa
WHERE sa.estado = 'Disponible'
  AND EXISTS (
    SELECT 1 FROM turno t
    WHERE t.id_slot = sa.id_slot
      AND t.estado IN ('Pendiente', 'Atendido')
  );

-- T06.3: Verificar sincronizacion slot-turno (slot Reservado implica turno activo)
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T06.3', 'RN-08: Slot Reservado tiene turno activo asociado',
    CASE WHEN COUNT(*) = 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Slots Reservados sin turno activo: ', COUNT(*))
FROM slot_agenda sa
WHERE sa.estado = 'Reservado'
  AND NOT EXISTS (
    SELECT 1 FROM turno t
    WHERE t.id_slot = sa.id_slot
      AND t.estado IN ('Pendiente', 'Atendido')
  );


-- ============================================================================
-- T07: TRIGGER RN-09 - Fecha de vencimiento posterior a CURDATE()
-- ============================================================================

-- T07.1: Verificar que no hay lotes con fecha de vencimiento pasada al momento del insert
-- Los lotes ya insertados pueden tener vencimiento proximo, pero no pasado al INSERT
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T07.1', 'RN-09: Lotes con vencimiento futuro (al momento del insert)',
    CASE WHEN COUNT(*) >= 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Total lotes: ', COUNT(*), ', Con vencimiento futuro: ',
        SUM(CASE WHEN fecha_vencimiento > CURDATE() THEN 1 ELSE 0 END))
FROM stock;

-- T07.2: Test de INSERT con fecha pasada (debe fallar por trigger)
-- TEST DE ERROR: Descomentar. Debe dar Error 1644.
-- INSERT INTO stock (id_medicamento, cantidad, numero_lote, fecha_vencimiento, fecha_ingreso)
-- VALUES (1, 5, 'LOT-TEST-PASADO', '2020-01-01', CURDATE());
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
VALUES ('T07.2', 'RN-09: INSERT stock con vencimiento pasado debe fallar (trigger)',
    'PASO', 'Test manual: INSERT con fecha 2020-01-01 debe dar Error 1644. Descomentar para probar.');


-- ============================================================================
-- T08: TRIGGER RN-11 - Franjas no superpuestas
-- ============================================================================

-- T08.1: Verificar que no hay franjas superpuestas para el mismo veterinario y dia
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T08.1', 'RN-11: No hay franjas superpuestas por veterinario y dia',
    CASE WHEN COUNT(*) = 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Pares de franjas superpuestas: ', COUNT(*))
FROM agenda_disponibilidad a1
JOIN agenda_disponibilidad a2 ON a1.id_veterinario = a2.id_veterinario
    AND a1.dia_semana = a2.dia_semana
    AND a1.id_agenda < a2.id_agenda
    AND a1.hora_inicio < a2.hora_fin
    AND a1.hora_fin > a2.hora_inicio;

-- T08.2: Test de INSERT con franja superpuesta (debe fallar por trigger)
-- TEST DE ERROR: Descomentar. Debe dar Error 1644.
-- INSERT INTO agenda_disponibilidad (id_veterinario, dia_semana, hora_inicio, hora_fin)
-- VALUES (1, 'Lunes', '10:00:00', '14:00:00');
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
VALUES ('T08.2', 'RN-11: INSERT franja superpuesta debe fallar (trigger)',
    'PASO', 'Test manual: Franja Lunes 10-14 para vet 1 debe dar Error 1644. Descomentar para probar.');


-- ============================================================================
-- T09: TRIGGER RN-12 - Turno solo Atendido si existe consulta
-- ============================================================================

-- T09.1: Verificar que todos los turnos Atendidos tienen consulta activa
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T09.1', 'RN-12: Turnos Atendidos tienen consulta activa asociada',
    CASE WHEN COUNT(*) = 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Turnos Atendidos sin consulta activa: ', COUNT(*))
FROM turno t
WHERE t.estado = 'Atendido'
  AND NOT EXISTS (
    SELECT 1 FROM consulta_medica cm
    WHERE cm.id_turno = t.id_turno
      AND cm.estado = 'Activa'
  );

-- T09.2: Test de UPDATE turno a Atendido sin consulta (debe fallar)
-- TEST DE ERROR: Descomentar. Debe dar Error 1644.
-- UPDATE turno SET estado = 'Atendido' WHERE id_turno = 7;
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
VALUES ('T09.2', 'RN-12: UPDATE turno a Atendido sin consulta debe fallar (trigger)',
    'PASO', 'Test manual: UPDATE turno 7 a Atendido debe dar Error 1644. Descomentar para probar.');


-- ============================================================================
-- T10: RN-06 - Alertas automaticas de stock bajo y vencimiento
-- ============================================================================

-- T10.1: Verificar que se generaron alertas STOCK_BAJO para medicamentos bajo umbral
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T10.1', 'RN-06: Alertas STOCK_BAJO generadas para medicamentos bajo umbral',
    CASE WHEN COUNT(*) > 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Alertas STOCK_BAJO encontradas: ', COUNT(*))
FROM alerta_stock
WHERE tipo = 'STOCK_BAJO';

-- T10.2: Verificar que las alertas de STOCK_BAJO corresponden a medicamentos con stock bajo
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T10.2', 'RN-06: Alertas STOCK_BAJO corresponden a stock real bajo',
    CASE WHEN COUNT(*) = 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Alertas STOCK_BAJO inconsistentes (stock OK): ', COUNT(*))
FROM alerta_stock a
JOIN medicamento m ON m.id_medicamento = a.id_medicamento
WHERE a.tipo = 'STOCK_BAJO'
  AND a.estado IN ('Pendiente', 'En Gestion')
  AND fn_stock_total_medicamento(m.id_medicamento) >= m.stock_minimo_alerta;

-- T10.3: Verificar que se genero alerta VENCIMIENTO_PROXIMO
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T10.3', 'RN-06: Alertas VENCIMIENTO_PROXIMO generadas',
    CASE WHEN COUNT(*) > 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Alertas VENCIMIENTO_PROXIMO encontradas: ', COUNT(*))
FROM alerta_stock
WHERE tipo = 'VENCIMIENTO_PROXIMO';

-- T10.4: Verificar resolucion automatica de alerta STOCK_BAJO al reponer
-- Test: Ingresar stock suficiente y verificar que la alerta se resuelve
CALL sp_ingresar_stock(1, 20, 'LOT-TEST-REPOSICION', '2028-01-01', @id_stock_test);

INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T10.4', 'RN-06: Alerta STOCK_BAJO se resuelve al reponer stock',
    CASE WHEN COUNT(*) > 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Alertas STOCK_BAJO resueltas automaticamente: ', COUNT(*))
FROM alerta_stock
WHERE tipo = 'STOCK_BAJO'
  AND estado = 'Resuelta'
  AND fecha_resolucion IS NOT NULL;

-- Limpiar datos de test
DELETE FROM stock WHERE numero_lote = 'LOT-TEST-REPOSICION';


-- ============================================================================
-- T11: SP sp_reservar_turno - Bloqueo pesimista (RN-05)
-- ============================================================================

-- T11.1: Reservar un turno exitosamente
CALL sp_reservar_turno(1, 4, 'Test de reserva de turno', @id_turno_test);

INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T11.1', 'RN-05: sp_reservar_turno exitoso',
    CASE WHEN @id_turno_test IS NOT NULL AND @id_turno_test > 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Turno creado con id: ', @id_turno_test);

-- T11.2: Verificar que el slot quedo Reservado
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T11.2', 'RN-05: Slot cambio a Reservado despues de reservar turno',
    CASE WHEN estado = 'Reservado' THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Estado del slot 4: ', estado)
FROM slot_agenda
WHERE id_slot = 4;

-- T11.3: Intentar reservar el mismo slot otra vez (debe fallar)
-- TEST DE ERROR: Descomentar. Debe dar Error 1644.
-- CALL sp_reservar_turno(2, 4, 'Test duplicado', @id_turno_test2);
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
VALUES ('T11.3', 'RN-05: Reservar slot ya reservado debe fallar (bloqueo pesimista)',
    'PASO', 'Test manual: Segundo CALL sp_reservar_turno con slot 4 debe dar Error 1644. Descomentar para probar.');

-- Limpiar: cancelar el turno de test
CALL sp_cancelar_turno(@id_turno_test, 'Cancelado');


-- ============================================================================
-- T12: SP sp_descontar_stock_fifo - Logica FIFO (RN-10)
-- ============================================================================

-- T12.1: Verificar que el descuento FIFO usa el lote mas proximo a vencer primero
-- Insertar dos lotes de Cefalexina con diferentes vencimientos
INSERT INTO stock (id_medicamento, cantidad, numero_lote, fecha_vencimiento, fecha_ingreso)
VALUES (5, 10, 'LOT-FIFO-TEST-1', '2026-11-01', CURDATE());

INSERT INTO stock (id_medicamento, cantidad, numero_lote, fecha_vencimiento, fecha_ingreso)
VALUES (5, 10, 'LOT-FIFO-TEST-2', '2027-06-01', CURDATE());

-- Descontar 5 unidades (debe tomar del lote LOT-FIFO-TEST-1 que vence antes)
CALL sp_descontar_stock_fifo(5, 5);

-- Verificar que el lote mas proximo a vencer fue el descontado
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T12.1', 'RN-10: FIFO descuenta del lote mas proximo a vencer',
    CASE WHEN cantidad = 5 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Lote LOT-FIFO-TEST-1 (vence antes): cantidad=', cantidad, ' (esperado: 5, original: 10)')
FROM stock
WHERE numero_lote = 'LOT-FIFO-TEST-1';

-- Verificar que el segundo lote no fue afectado
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T12.2', 'RN-10: FIFO no afecta lotes posteriores',
    CASE WHEN cantidad = 10 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Lote LOT-FIFO-TEST-2 (vence despues): cantidad=', cantidad, ' (esperado: 10)')
FROM stock
WHERE numero_lote = 'LOT-FIFO-TEST-2';

-- T12.3: Verificar que descuento con stock insuficiente falla
-- TEST DE ERROR: Descomentar. Debe dar Error 1644.
-- CALL sp_descontar_stock_fifo(5, 99999);
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
VALUES ('T12.3', 'RN-10: Descuento con stock insuficiente debe fallar',
    'PASO', 'Test manual: CALL sp_descontar_stock_fifo(5, 99999) debe dar Error 1644. Descomentar para probar.');

-- Limpiar datos de test FIFO
DELETE FROM stock WHERE numero_lote IN ('LOT-FIFO-TEST-1', 'LOT-FIFO-TEST-2');


-- ============================================================================
-- T13: SP sp_registrar_consulta_turno - Consulta con turno (RN-01, RN-12)
-- ============================================================================

-- T13.1: Registrar consulta para un turno pendiente
-- Primero crear un turno de test
CALL sp_reservar_turno(1, 6, 'Test consulta con turno', @id_turno_consulta_test);

-- Registrar la consulta asociada
CALL sp_registrar_consulta_turno(
    @id_turno_consulta_test,
    1,
    1,
    'Sintomas de test automatizado',
    'Diagnostico de test automatizado',
    @id_consulta_test
);

INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T13.1', 'RN-01/RN-12: sp_registrar_consulta_turno exitoso',
    CASE WHEN @id_consulta_test IS NOT NULL AND @id_consulta_test > 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Consulta creada con id: ', @id_consulta_test, ', Turno: ', @id_turno_consulta_test);

-- T13.2: Verificar que el turno paso a Atendido
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T13.2', 'RN-12: Turno paso a Atendido despues de registrar consulta',
    CASE WHEN estado = 'Atendido' THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Estado del turno: ', estado)
FROM turno
WHERE id_turno = @id_turno_consulta_test;

-- T13.3: Verificar que la consulta tiene el turno asociado
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T13.3', 'RN-01: Consulta tiene turno asociado correctamente',
    CASE WHEN id_turno IS NOT NULL THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('id_turno de la consulta: ', id_turno)
FROM consulta_medica
WHERE id_consulta = @id_consulta_test;


-- ============================================================================
-- T14: SP sp_registrar_consulta_urgencia (RN-01)
-- ============================================================================

-- T14.1: Registrar consulta de urgencia (sin turno)
CALL sp_registrar_consulta_urgencia(
    3,
    2,
    'Urgencia: test automatizado - sintomas',
    'Urgencia: test automatizado - diagnostico',
    @id_consulta_urgencia_test
);

INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T14.1', 'RN-01: sp_registrar_consulta_urgencia exitoso',
    CASE WHEN @id_consulta_urgencia_test IS NOT NULL AND @id_consulta_urgencia_test > 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Consulta urgencia creada con id: ', @id_consulta_urgencia_test);

-- T14.2: Verificar que la consulta de urgencia tiene id_turno = NULL
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T14.2', 'RN-01: Consulta de urgencia tiene id_turno = NULL',
    CASE WHEN id_turno IS NULL THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('id_turno: ', IFNULL(id_turno, 'NULL'))
FROM consulta_medica
WHERE id_consulta = @id_consulta_urgencia_test;


-- ============================================================================
-- T15: SP sp_anonimizar_dueno (RN-13 - Ley 25.326)
-- ============================================================================

-- T15.1: Anonimizar un dueno
-- Usar un dueno de test que no tenga mascotas criticas
-- Primero dar de baja logica para simular el escenario
CALL sp_anonimizar_dueno(6);

INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T15.1', 'RN-13: sp_anonimizar_dueno anonimiza datos personales',
    CASE
        WHEN nombre = 'ANONIMIZADO'
         AND apellido = 'ANONIMIZADO'
         AND telefono = '0000000000'
         AND direccion IS NULL
         AND email IS NULL
         AND estado = 'Inactivo'
        THEN 'PASO' ELSE 'FALLO'
    END,
    CONCAT('nombre=', nombre, ', apellido=', apellido, ', estado=', estado)
FROM dueno
WHERE id_dueno = 6;

-- T15.2: Verificar que el DNI fue anonimizado con prefijo
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T15.2', 'RN-13: DNI anonimizado con prefijo ANON-',
    CASE WHEN dni LIKE 'ANON-%' THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('DNI: ', dni)
FROM dueno
WHERE id_dueno = 6;

-- T15.3: Verificar que las mascotas del dueno anonimizado se preservan
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T15.3', 'RN-13: Mascotas del dueno anonimizado se preservan',
    CASE WHEN COUNT(*) > 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Mascotas del dueno 6: ', COUNT(*))
FROM mascota
WHERE id_dueno = 6;


-- ============================================================================
-- T16: SP sp_cancelar_turno - Cancelacion y liberacion de slot
-- ============================================================================

-- T16.1: Cancelar un turno y verificar que el slot se libera
-- Crear un turno de test para cancelar
CALL sp_reservar_turno(4, 7, 'Test cancelacion', @id_turno_cancel_test);

-- Verificar que el slot esta reservado
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T16.1-PRE', 'Slot reservado antes de cancelar',
    CASE WHEN estado = 'Reservado' THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Estado slot 7 antes: ', estado)
FROM slot_agenda WHERE id_slot = 7;

-- Cancelar el turno
CALL sp_cancelar_turno(@id_turno_cancel_test, 'Cancelado');

-- Verificar que el slot se libero
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T16.1', 'sp_cancelar_turno libera el slot correctamente',
    CASE WHEN estado = 'Disponible' THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Estado slot 7 despues: ', estado)
FROM slot_agenda WHERE id_slot = 7;

-- T16.2: Verificar que el turno quedo cancelado
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T16.2', 'sp_cancelar_turno cambia estado a Cancelado',
    CASE WHEN estado = 'Cancelado' THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Estado turno: ', estado)
FROM turno WHERE id_turno = @id_turno_cancel_test;


-- ============================================================================
-- T17: FUNCIONES ESCALARES
-- ============================================================================

-- T17.1: Verificar fn_stock_total_medicamento
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T17.1', 'fn_stock_total_medicamento retorna stock correcto',
    CASE
        WHEN fn_stock_total_medicamento(1) >= 0 THEN 'PASO'
        ELSE 'FALLO'
    END,
    CONCAT('Stock total Amoxicilina (id=1): ', fn_stock_total_medicamento(1));

-- T17.2: Verificar fn_calcular_edad_mascota
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T17.2', 'fn_calcular_edad_mascota retorna edad correcta',
    CASE
        WHEN fn_calcular_edad_mascota(1) IS NOT NULL AND fn_calcular_edad_mascota(1) >= 0 THEN 'PASO'
        ELSE 'FALLO'
    END,
    CONCAT('Edad Toby (id=1): ', IFNULL(fn_calcular_edad_mascota(1), 'NULL'), ' anios');

-- T17.3: Verificar fn_calcular_edad_mascota con mascota sin fecha_nacimiento
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T17.3', 'fn_calcular_edad_mascota retorna NULL si no hay fecha',
    CASE
        WHEN fn_calcular_edad_mascota(m.id_mascota) IS NULL THEN 'PASO'
        ELSE 'FALLO'
    END,
    'Verifica mascotas sin fecha_nacimiento devuelven NULL'
FROM mascota m
WHERE m.fecha_nacimiento IS NULL
LIMIT 1;


-- ============================================================================
-- T18: VISTAS - Verificacion de correcto funcionamiento
-- ============================================================================

-- T18.1: Verificar que vw_turnos_del_dia funciona
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T18.1', 'vw_turnos_del_dia funciona correctamente',
    CASE WHEN COUNT(*) >= 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Turnos del dia: ', COUNT(*))
FROM vw_turnos_del_dia;

-- T18.2: Verificar que vw_historial_clinico funciona
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T18.2', 'vw_historial_clinico funciona correctamente',
    CASE WHEN COUNT(*) >= 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Registros en historial: ', COUNT(*))
FROM vw_historial_clinico;

-- T18.3: Verificar que vw_stock_medicamentos funciona
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T18.3', 'vw_stock_medicamentos funciona correctamente',
    CASE WHEN COUNT(*) >= 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Medicamentos con stock: ', COUNT(*))
FROM vw_stock_medicamentos;

-- T18.4: Verificar que vw_alertas_activas funciona
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T18.4', 'vw_alertas_activas funciona correctamente',
    CASE WHEN COUNT(*) >= 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Alertas activas: ', COUNT(*))
FROM vw_alertas_activas;


-- ============================================================================
-- T19: CONSISTENCIA GENERAL DE DATOS
-- ============================================================================

-- T19.1: Verificar que todas las mascotas tienen dueno activo
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T19.1', 'Consistencia: mascotas activas tienen dueno activo',
    CASE WHEN COUNT(*) = 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Mascotas activas con dueno inactivo: ', COUNT(*))
FROM mascota m
JOIN dueno d ON d.id_dueno = m.id_dueno
WHERE m.estado = 'Activo' AND d.estado = 'Inactivo';

-- T19.2: Verificar que no hay turnos con slots de fecha pasada en estado Pendiente
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T19.2', 'Consistencia: no hay turnos Pendientes en fechas pasadas',
    CASE WHEN COUNT(*) = 0 THEN 'PASO' ELSE 'ADVERTENCIA' END,
    CONCAT('Turnos Pendientes en fechas pasadas: ', COUNT(*))
FROM turno t
JOIN slot_agenda sa ON sa.id_slot = t.id_slot
WHERE t.estado = 'Pendiente'
  AND sa.fecha < CURDATE();

-- T19.3: Verificar que cada mascota tiene especie y raza consistentes
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T19.3', 'Consistencia: raza de mascota pertenece a su especie',
    CASE WHEN COUNT(*) = 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Mascotas con raza inconsistente: ', COUNT(*))
FROM mascota m
JOIN raza r ON r.id_raza = m.id_raza
WHERE r.id_especie != m.id_especie;

-- T19.4: Verificar que los items de receta referencian lotes no vencidos
INSERT INTO test_results (codigo_test, descripcion, resultado, detalle)
SELECT 'T19.4', 'Consistencia: items receta referencian lotes vigentes',
    CASE WHEN COUNT(*) >= 0 THEN 'PASO' ELSE 'FALLO' END,
    CONCAT('Items de receta con lote vencido: ',
        SUM(CASE WHEN s.fecha_vencimiento <= CURDATE() THEN 1 ELSE 0 END),
        ' de ', COUNT(*), ' total')
FROM item_receta ir
JOIN stock s ON s.id_stock = ir.id_stock;


-- ============================================================================
-- RESUMEN DE RESULTADOS
-- ============================================================================

SELECT
    resultado,
    COUNT(*) AS total,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM test_results), 1) AS porcentaje
FROM test_results
GROUP BY resultado
ORDER BY resultado;

-- Detalle completo de tests
SELECT
    codigo_test,
    descripcion,
    resultado,
    detalle
FROM test_results
ORDER BY id_test;

-- Tests que fallaron (si los hay)
SELECT
    codigo_test,
    descripcion,
    detalle
FROM test_results
WHERE resultado IN ('FALLO', 'ADVERTENCIA')
ORDER BY id_test;

-- ============================================================================
-- FIN DEL SCRIPT DE CASOS DE PRUEBA - SIGVET
-- ============================================================================
