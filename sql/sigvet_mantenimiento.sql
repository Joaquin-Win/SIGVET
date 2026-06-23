-- ============================================================================
-- SIGVET - Sistema de Informacion para Gestion Veterinaria
-- Script de Mantenimiento (DELETE, UPDATE, Operaciones Batch)
-- ============================================================================
-- Proyecto: AP2 - Trabajo Practico Numero 2
-- Alumno: Winck Joaquin Ezequiel
-- Carrera: Lic. en Informatica
-- Base de Datos: MySQL 8.0+ / InnoDB
-- Fecha: Mayo 2026
-- ============================================================================
-- CONTENIDO:
--   1. Operaciones UPDATE (modificaciones de datos)
--   2. Operaciones DELETE (eliminaciones permitidas)
--   3. Procedimientos de mantenimiento batch
--   4. Operaciones de precio y catalogo
--   5. Transiciones de estado
--   6. Limpieza y archivado periodico
--   7. Regeneracion de slots de agenda
-- ============================================================================
-- NOTAS:
--   - Las consultas medicas NO se pueden eliminar fisicamente (RN-07)
--   - Solo se permite baja logica en consulta_medica
--   - Las FK con ON DELETE RESTRICT impiden eliminacion de registros
--     que tienen dependientes (ej: dueno con mascotas)
--   - Ejecutar DESPUES de sigvet_ddl.sql y sigvet_dml.sql
-- ============================================================================

USE sigvet;

-- ============================================================================
-- 1. OPERACIONES UPDATE - Modificaciones de datos
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1.1 Actualizar datos de un dueno
-- Caso: El dueno cambia su numero de telefono y direccion
-- ----------------------------------------------------------------------------
UPDATE dueno
SET telefono = '351-555-9999',
    direccion = 'Nueva Direccion 456',
    email = 'lucia.fernandez.nuevo@email.com'
WHERE id_dueno = 1
  AND dni = '30123456';

-- ----------------------------------------------------------------------------
-- 1.2 Actualizar datos de un veterinario
-- Caso: La veterinaria actualiza su email institucional
-- ----------------------------------------------------------------------------
UPDATE veterinario
SET email = 'c.rodriguez@sigvet.com.ar',
    telefono = '351-555-0199'
WHERE id_veterinario = 1;

-- ----------------------------------------------------------------------------
-- 1.3 Actualizar datos de una mascota
-- Caso: Se actualiza el color y se agregan senas particulares
-- ----------------------------------------------------------------------------
UPDATE mascota
SET color = 'Negro con pecho blanco',
    senas_particulares = 'Cicatriz quirurgica en abdomen. Microchip 123456789'
WHERE id_mascota = 1;

-- ----------------------------------------------------------------------------
-- 1.4 Actualizar precio de un medicamento
-- Caso: Ajuste de precio por inflacion. Se registra la fecha de actualizacion.
-- ----------------------------------------------------------------------------
UPDATE medicamento
SET precio_venta = 9200.00,
    fecha_actualizacion_precio = CURDATE()
WHERE id_medicamento = 1
  AND nombre_comercial = 'Amoxidal';

-- ----------------------------------------------------------------------------
-- 1.5 Actualizar umbral de stock minimo de alerta
-- Caso: Se aumenta el umbral de alerta por mayor demanda del medicamento
-- ----------------------------------------------------------------------------
UPDATE medicamento
SET stock_minimo_alerta = 15
WHERE id_medicamento = 7
  AND nombre_comercial = 'Bravettes';

-- ----------------------------------------------------------------------------
-- 1.6 Actualizar el motivo de un turno pendiente
-- Caso: El dueno llama para ampliar el motivo de la consulta
-- ----------------------------------------------------------------------------
UPDATE turno
SET motivo = 'Vacunacion anual + Control de desparasitacion'
WHERE id_turno = 1
  AND estado = 'Pendiente';

-- ----------------------------------------------------------------------------
-- 1.7 Actualizar los datos de una consulta medica (modificacion clinica)
-- Caso: El veterinario corrige/agrega informacion al diagnostico
-- Nota: El trigger trg_registro_modificacion_consulta registra la fecha
--        de modificacion automaticamente.
-- ----------------------------------------------------------------------------
UPDATE consulta_medica
SET diagnostico = 'Gastroenteritis aguda. Posible ingesta de alimento en mal estado. Se solicita ecografia abdominal.',
    id_veterinario_modif = 1
WHERE id_consulta = 2
  AND estado = 'Activa';

-- ----------------------------------------------------------------------------
-- 1.8 Actualizar frecuencia y duracion de un item de receta
-- Caso: El veterinario ajusta la posologia despues de control
-- ----------------------------------------------------------------------------
UPDATE item_receta
SET frecuencia = 'Cada 8 horas',
    duracion = '10 dias'
WHERE id_item_receta = 1;

-- ----------------------------------------------------------------------------
-- 1.9 Actualizar estado de un alerta a "En Gestion"
-- Caso: El recepcionista comienza a gestionar una alerta de stock bajo
-- ----------------------------------------------------------------------------
UPDATE alerta_stock
SET estado = 'En Gestion'
WHERE id_alerta = 1
  AND estado = 'Pendiente';

-- ----------------------------------------------------------------------------
-- 1.10 Actualizar franja horaria de disponibilidad
-- Caso: La Dra. Rodriguez cambia su horario del lunes
-- Nota: El trigger trg_validar_franjas_no_superpuestas_update verifica
--        que la nueva franja no se superponga con otra existente.
-- ----------------------------------------------------------------------------
UPDATE agenda_disponibilidad
SET hora_inicio = '08:30:00',
    hora_fin = '12:30:00'
WHERE id_agenda = 1;


-- ============================================================================
-- 2. OPERACIONES DELETE - Eliminaciones permitidas
-- ============================================================================
-- IMPORTANTE: Las siguientes tablas NO admiten DELETE fisico:
--   - consulta_medica (RN-07: trigger trg_prevenir_eliminar_consulta)
--   - veterinario con agenda configurada (FK RESTRICT)
--   - dueno con mascotas activas (FK RESTRICT)
--   - mascota con turnos/consultas (FK RESTRICT)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 2.1 Eliminar una alerta resuelta antigua (limpieza)
-- Caso: Se eliminan alertas resueltas con mas de 90 dias de antiguedad
-- ----------------------------------------------------------------------------
DELETE FROM alerta_stock
WHERE estado = 'Resuelta'
  AND fecha_resolucion < DATE_SUB(CURDATE(), INTERVAL 90 DAY);

-- ----------------------------------------------------------------------------
-- 2.2 Eliminar slots de agenda de fechas pasadas sin turno
-- Caso: Limpieza periodica de slots no utilizados de semanas anteriores
-- ----------------------------------------------------------------------------
DELETE FROM slot_agenda
WHERE fecha < CURDATE()
  AND estado = 'Disponible'
  AND NOT EXISTS (
    SELECT 1 FROM turno t WHERE t.id_slot = slot_agenda.id_slot
  );

-- ----------------------------------------------------------------------------
-- 2.3 Eliminar un item de receta erroneo (antes de dispensar)
-- Caso: Se cargo un medicamento incorrecto en la receta y aun no se dispensa
-- ----------------------------------------------------------------------------
DELETE FROM item_receta
WHERE id_item_receta = 8
  AND dispensado = 0;

-- Nota: Si el item ya fue dispensado (dispensado = 1), NO se debe eliminar.
-- En su lugar, se debe registrar una devolucion o ajuste de inventario.
-- Ejemplo de intento de eliminacion de item dispensado (sera rechazado):
-- DELETE FROM item_receta WHERE id_item_receta = 1 AND dispensado = 1;
-- => Error FK: no se puede porque el stock ya fue descontado.

-- ----------------------------------------------------------------------------
-- 2.4 Eliminar una especie sin razas asociadas
-- Caso: Se elimina la especie "Bovino" porque no se atiende en la clinica
-- Nota: Si la especie tiene razas asociadas, el DELETE falla por FK RESTRICT
-- ----------------------------------------------------------------------------
DELETE FROM especie
WHERE nombre = 'Bovino'
  AND NOT EXISTS (
    SELECT 1 FROM raza r WHERE r.id_especie = especie.id_especie
  )
  AND NOT EXISTS (
    SELECT 1 FROM mascota m WHERE m.id_especie = especie.id_especie
  );

-- ----------------------------------------------------------------------------
-- 2.5 Eliminar un medicamento inactivo sin stock ni items de receta
-- Caso: Se discontinua un medicamento y se limpia del catalogo
-- Primero se da de baja logica, y luego de un tiempo se puede eliminar.
-- ----------------------------------------------------------------------------
-- Paso 1: Dar de baja logica
UPDATE medicamento
SET estado = 'Inactivo'
WHERE id_medicamento = 9
  AND estado = 'Activo';

-- Paso 2: Eliminar (solo si no tiene stock con items de receta asociados)
DELETE FROM stock
WHERE id_medicamento = 9
  AND cantidad = 0
  AND NOT EXISTS (
    SELECT 1 FROM item_receta ir WHERE ir.id_stock = stock.id_stock
  );

DELETE FROM medicamento
WHERE id_medicamento = 9
  AND estado = 'Inactivo'
  AND NOT EXISTS (SELECT 1 FROM stock s WHERE s.id_medicamento = 9)
  AND NOT EXISTS (SELECT 1 FROM alerta_stock a WHERE a.id_medicamento = 9);


-- ============================================================================
-- 3. BAJA LOGICA - Operaciones de baja sin eliminacion fisica
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 3.1 Baja logica de una consulta medica (RN-07)
-- Caso: Se desactiva una consulta por error de registro
-- Se utiliza el procedimiento almacenado sp_baja_logica_consulta
-- ----------------------------------------------------------------------------
CALL sp_baja_logica_consulta(7, 3);

-- ----------------------------------------------------------------------------
-- 3.2 Baja logica de un veterinario
-- Caso: La veterinaria se va de la clinica pero se preservan sus consultas
-- ----------------------------------------------------------------------------
UPDATE veterinario
SET estado = 'Inactivo'
WHERE id_veterinario = 3
  AND estado = 'Activo';

-- ----------------------------------------------------------------------------
-- 3.3 Baja logica de un dueno (anonimizacion - Ley 25.326, RN-13)
-- Caso: El dueno ejerce su derecho de supresion de datos personales
-- Se utiliza el procedimiento almacenado sp_anonimizar_dueno
-- ----------------------------------------------------------------------------
CALL sp_anonimizar_dueno(6);

-- ----------------------------------------------------------------------------
-- 3.4 Baja logica de una mascota
-- Caso: Fallecimiento de la mascota. Se preserva el historial clinico.
-- Nota: Solo se da de baja si no tiene turnos pendientes.
-- ----------------------------------------------------------------------------
UPDATE mascota
SET estado = 'Inactivo'
WHERE id_mascota = 10
  AND estado = 'Activo'
  AND NOT EXISTS (
    SELECT 1 FROM turno t
    WHERE t.id_mascota = mascota.id_mascota
      AND t.estado = 'Pendiente'
  );

-- ----------------------------------------------------------------------------
-- 3.5 Reactivar un veterinario dado de baja
-- Caso: La veterinaria regresa a la clinica
-- ----------------------------------------------------------------------------
UPDATE veterinario
SET estado = 'Activo'
WHERE id_veterinario = 3
  AND estado = 'Inactivo';


-- ============================================================================
-- 4. TRANSICIONES DE ESTADO DE TURNOS
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 4.1 Marcar turno como Inasistencia
-- Caso: El dueno no se presento a la consulta
-- Se utiliza el procedimiento almacenado que libera el slot automaticamente
-- ----------------------------------------------------------------------------
CALL sp_cancelar_turno(10, 'Inasistencia');

-- ----------------------------------------------------------------------------
-- 4.2 Cancelar un turno pendiente
-- Caso: El dueno llama para cancelar su turno
-- ----------------------------------------------------------------------------
CALL sp_cancelar_turno(5, 'Cancelado');

-- ----------------------------------------------------------------------------
-- 4.3 Revertir una cancelacion erronea (reactivar turno)
-- Caso: Se cancelo por error y se necesita reactivar
-- Precondicion: El slot debe estar Disponible
-- ----------------------------------------------------------------------------
START TRANSACTION;

-- Reactivar el turno
UPDATE turno
SET estado = 'Pendiente'
WHERE id_turno = 5
  AND estado = 'Cancelado';

-- Reservar el slot nuevamente
UPDATE slot_agenda
SET estado = 'Reservado'
WHERE id_slot = 27
  AND estado = 'Disponible';

COMMIT;


-- ============================================================================
-- 5. OPERACIONES DE STOCK
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 5.1 Ingresar nuevo lote de stock
-- Caso: Llega un nuevo lote de Amoxicilina al deposito
-- Se utiliza el procedimiento que dispara verificacion de alertas automaticas
-- ----------------------------------------------------------------------------
CALL sp_ingresar_stock(1, 20, 'LOT-2026-A018', '2027-12-15', @id_nuevo_stock);
SELECT @id_nuevo_stock AS id_stock_creado;

-- Nota: Al ingresar 20 unidades de Amoxicilina, el stock total sube a 28,
-- que supera el stock_minimo_alerta (10). El trigger automaticamente
-- resuelve la alerta STOCK_BAJO pendiente.

-- ----------------------------------------------------------------------------
-- 5.2 Ajustar cantidad de stock (inventario fisico)
-- Caso: Durante el conteo fisico se detecta que hay 2 unidades menos
-- ----------------------------------------------------------------------------
UPDATE stock
SET cantidad = cantidad - 2
WHERE id_stock = 1
  AND numero_lote = 'LOT-2025-A001';

-- Nota: El trigger trg_alerta_stock_bajo_update verificara si el ajuste
-- genera una nueva alerta de stock bajo.

-- ----------------------------------------------------------------------------
-- 5.3 Descuento de stock FIFO para dispensa de medicamentos
-- Caso: Se dispensa Amoxicilina desde el stock usando logica FIFO
-- El procedimiento descontara primero del lote mas proximo a vencer
-- ----------------------------------------------------------------------------
CALL sp_descontar_stock_fifo(1, 5);

-- ----------------------------------------------------------------------------
-- 5.4 Marcar item de receta como dispensado
-- Caso: El recepcionista entrega el medicamento al dueno
-- ----------------------------------------------------------------------------
UPDATE item_receta
SET dispensado = 1
WHERE id_item_receta = 3
  AND dispensado = 0;

-- ----------------------------------------------------------------------------
-- 5.5 Resolver manualmente una alerta de vencimiento
-- Caso: Se retira el lote proximo a vencer del deposito
-- ----------------------------------------------------------------------------
UPDATE alerta_stock
SET estado = 'Resuelta',
    fecha_resolucion = NOW()
WHERE tipo = 'VENCIMIENTO_PROXIMO'
  AND estado IN ('Pendiente', 'En Gestion')
  AND id_medicamento = 7;


-- ============================================================================
-- 6. PROCEDIMIENTOS DE MANTENIMIENTO BATCH
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 6.1 SP: Generar slots de agenda para una semana completa
-- Crea slots de 30 minutos para todas las franjas de disponibilidad
-- que correspondan a los dias de la semana especificada.
-- Parametros:
--   p_fecha_inicio: Lunes de la semana a generar (formato DATE)
-- ----------------------------------------------------------------------------
DELIMITER //

CREATE PROCEDURE sp_generar_slots_semana(
    IN p_fecha_inicio DATE
)
BEGIN
    DECLARE v_id_agenda INT;
    DECLARE v_dia_semana VARCHAR(20);
    DECLARE v_hora_inicio TIME;
    DECLARE v_hora_fin TIME;
    DECLARE v_hora_actual TIME;
    DECLARE v_fecha_slot DATE;
    DECLARE v_fin INT DEFAULT 0;
    DECLARE v_existe INT;

    DECLARE cur_agenda CURSOR FOR
        SELECT id_agenda, dia_semana, hora_inicio, hora_fin
        FROM agenda_disponibilidad
        WHERE id_veterinario IN (
            SELECT id_veterinario FROM veterinario WHERE estado = 'Activo'
        );

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_fin = 1;

    OPEN cur_agenda;

    bucle_agenda: LOOP
        FETCH cur_agenda INTO v_id_agenda, v_dia_semana, v_hora_inicio, v_hora_fin;

        IF v_fin = 1 THEN
            LEAVE bucle_agenda;
        END IF;

        -- Calcular la fecha del dia de la semana correspondiente
        SET v_fecha_slot = CASE v_dia_semana
            WHEN 'Lunes' THEN DATE_ADD(p_fecha_inicio, INTERVAL 0 DAY)
            WHEN 'Martes' THEN DATE_ADD(p_fecha_inicio, INTERVAL 1 DAY)
            WHEN 'Miercoles' THEN DATE_ADD(p_fecha_inicio, INTERVAL 2 DAY)
            WHEN 'Jueves' THEN DATE_ADD(p_fecha_inicio, INTERVAL 3 DAY)
            WHEN 'Viernes' THEN DATE_ADD(p_fecha_inicio, INTERVAL 4 DAY)
            WHEN 'Sabado' THEN DATE_ADD(p_fecha_inicio, INTERVAL 5 DAY)
            WHEN 'Domingo' THEN DATE_ADD(p_fecha_inicio, INTERVAL 6 DAY)
        END;

        -- Generar slots de 30 minutos
        SET v_hora_actual = v_hora_inicio;

        bucle_slots: LOOP
            IF v_hora_actual >= v_hora_fin THEN
                LEAVE bucle_slots;
            END IF;

            -- Verificar si ya existe el slot (evitar duplicados)
            SELECT COUNT(*) INTO v_existe
            FROM slot_agenda
            WHERE id_agenda = v_id_agenda
              AND fecha = v_fecha_slot
              AND hora = v_hora_actual;

            IF v_existe = 0 THEN
                INSERT INTO slot_agenda (id_agenda, fecha, hora, estado)
                VALUES (v_id_agenda, v_fecha_slot, v_hora_actual, 'Disponible');
            END IF;

            SET v_hora_actual = DATE_ADD(v_hora_actual, INTERVAL 30 MINUTE);
        END LOOP;

    END LOOP;

    CLOSE cur_agenda;
END //

DELIMITER ;

-- Ejemplo de uso: generar slots para la semana del 25 al 29 de Mayo 2026
-- CALL sp_generar_slots_semana('2026-05-25');


-- ----------------------------------------------------------------------------
-- 6.2 SP: Limpiar slots pasados sin turno
-- Elimina slots de fechas anteriores a hoy que esten en estado Disponible
-- y no tengan turno asociado. Mantiene los slots reservados con turno.
-- ----------------------------------------------------------------------------
DELIMITER //

CREATE PROCEDURE sp_limpiar_slots_pasados()
BEGIN
    DELETE FROM slot_agenda
    WHERE fecha < CURDATE()
      AND estado = 'Disponible'
      AND NOT EXISTS (
        SELECT 1 FROM turno t WHERE t.id_slot = slot_agenda.id_slot
      );
END //

DELIMITER ;

-- Ejemplo de uso:
-- CALL sp_limpiar_slots_pasados();


-- ----------------------------------------------------------------------------
-- 6.3 SP: Marcar turnos pasados como Inasistencia
-- Los turnos que quedaron en estado 'Pendiente' y cuya fecha ya paso
-- se marcan automaticamente como 'Inasistencia' y se liberan los slots.
-- Se ejecuta como tarea nocturna (cron job).
-- ----------------------------------------------------------------------------
DELIMITER //

CREATE PROCEDURE sp_marcar_inasistencias()
BEGIN
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_id_turno_act INT;
    DECLARE v_id_slot_act INT;

    DECLARE cur_turnos CURSOR FOR
        SELECT t.id_turno, t.id_slot
        FROM turno t
        JOIN slot_agenda sa ON sa.id_slot = t.id_slot
        WHERE t.estado = 'Pendiente'
          AND sa.fecha < CURDATE();

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    OPEN cur_turnos;

    bucle: LOOP
        FETCH cur_turnos INTO v_id_turno_act, v_id_slot_act;

        IF v_done = 1 THEN
            LEAVE bucle;
        END IF;

        -- Marcar turno como Inasistencia
        UPDATE turno
        SET estado = 'Inasistencia'
        WHERE id_turno = v_id_turno_act;

        -- Liberar el slot
        UPDATE slot_agenda
        SET estado = 'Disponible'
        WHERE id_slot = v_id_slot_act;
    END LOOP;

    CLOSE cur_turnos;
END //

DELIMITER ;

-- Ejemplo de uso:
-- CALL sp_marcar_inasistencias();


-- ----------------------------------------------------------------------------
-- 6.4 SP: Verificar vencimientos proximos y generar alertas
-- Recorre todos los lotes con vencimiento dentro de los proximos 30 dias
-- y genera alertas VENCIMIENTO_PROXIMO si no existen.
-- Se ejecuta como tarea diaria (cron job).
-- ----------------------------------------------------------------------------
DELIMITER //

CREATE PROCEDURE sp_verificar_vencimientos()
BEGIN
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_id_stock_act INT;

    DECLARE cur_lotes CURSOR FOR
        SELECT s.id_stock
        FROM stock s
        JOIN medicamento m ON m.id_medicamento = s.id_medicamento
        WHERE s.fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)
          AND s.cantidad > 0
          AND m.estado = 'Activo';

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    OPEN cur_lotes;

    bucle: LOOP
        FETCH cur_lotes INTO v_id_stock_act;

        IF v_done = 1 THEN
            LEAVE bucle;
        END IF;

        CALL sp_generar_alerta_vencimiento(v_id_stock_act);
    END LOOP;

    CLOSE cur_lotes;
END //

DELIMITER ;

-- Ejemplo de uso:
-- CALL sp_verificar_vencimientos();


-- ----------------------------------------------------------------------------
-- 6.5 SP: Actualizacion masiva de precios por porcentaje
-- Aplica un porcentaje de ajuste a todos los medicamentos activos.
-- Registra la fecha de actualizacion.
-- Parametros:
--   p_porcentaje: Porcentaje de aumento (ej: 15.0 para 15%)
-- ----------------------------------------------------------------------------
DELIMITER //

CREATE PROCEDURE sp_actualizar_precios(
    IN p_porcentaje DECIMAL(5,2)
)
BEGIN
    IF p_porcentaje <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El porcentaje debe ser mayor a 0.';
    END IF;

    IF p_porcentaje > 100 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El porcentaje no puede superar el 100%.';
    END IF;

    UPDATE medicamento
    SET precio_venta = ROUND(precio_venta * (1 + p_porcentaje / 100), 2),
        fecha_actualizacion_precio = CURDATE()
    WHERE estado = 'Activo';
END //

DELIMITER ;

-- Ejemplo de uso: aplicar 12% de aumento
-- CALL sp_actualizar_precios(12.0);


-- ----------------------------------------------------------------------------
-- 6.6 SP: Generar resumen mensual de actividad
-- Genera un reporte con estadisticas del mes especificado.
-- Parametros:
--   p_anio: Anio (ej: 2026)
--   p_mes: Mes (1-12)
-- ----------------------------------------------------------------------------
DELIMITER //

CREATE PROCEDURE sp_resumen_mensual(
    IN p_anio INT,
    IN p_mes INT
)
BEGIN
    DECLARE v_fecha_inicio DATE;
    DECLARE v_fecha_fin DATE;

    SET v_fecha_inicio = DATE(CONCAT(p_anio, '-', LPAD(p_mes, 2, '0'), '-01'));
    SET v_fecha_fin = LAST_DAY(v_fecha_inicio);

    -- Consultas del mes
    SELECT 'Consultas realizadas' AS metrica, COUNT(*) AS valor
    FROM consulta_medica
    WHERE fecha BETWEEN v_fecha_inicio AND v_fecha_fin
      AND estado = 'Activa'

    UNION ALL

    -- Urgencias del mes
    SELECT 'Urgencias atendidas', COUNT(*)
    FROM consulta_medica
    WHERE fecha BETWEEN v_fecha_inicio AND v_fecha_fin
      AND estado = 'Activa'
      AND id_turno IS NULL

    UNION ALL

    -- Turnos programados
    SELECT 'Turnos programados', COUNT(*)
    FROM turno t
    JOIN slot_agenda sa ON sa.id_slot = t.id_slot
    WHERE sa.fecha BETWEEN v_fecha_inicio AND v_fecha_fin

    UNION ALL

    -- Turnos cancelados
    SELECT 'Turnos cancelados', COUNT(*)
    FROM turno t
    JOIN slot_agenda sa ON sa.id_slot = t.id_slot
    WHERE sa.fecha BETWEEN v_fecha_inicio AND v_fecha_fin
      AND t.estado = 'Cancelado'

    UNION ALL

    -- Inasistencias
    SELECT 'Inasistencias', COUNT(*)
    FROM turno t
    JOIN slot_agenda sa ON sa.id_slot = t.id_slot
    WHERE sa.fecha BETWEEN v_fecha_inicio AND v_fecha_fin
      AND t.estado = 'Inasistencia'

    UNION ALL

    -- Medicamentos dispensados
    SELECT 'Medicamentos dispensados', SUM(ir.cantidad)
    FROM item_receta ir
    JOIN consulta_medica cm ON cm.id_consulta = ir.id_consulta
    WHERE cm.fecha BETWEEN v_fecha_inicio AND v_fecha_fin
      AND ir.dispensado = 1

    UNION ALL

    -- Mascotas nuevas registradas
    SELECT 'Mascotas nuevas', COUNT(*)
    FROM mascota
    WHERE fecha_registro BETWEEN v_fecha_inicio AND v_fecha_fin
      AND estado = 'Activo'

    UNION ALL

    -- Alertas generadas
    SELECT 'Alertas generadas', COUNT(*)
    FROM alerta_stock
    WHERE fecha_generacion BETWEEN v_fecha_inicio AND v_fecha_fin;
END //

DELIMITER ;

-- Ejemplo de uso: resumen de Mayo 2026
-- CALL sp_resumen_mensual(2026, 5);


-- ============================================================================
-- 7. OPERACIONES DE RECUPERACION Y CORRECCION
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 7.1 Corregir mascota asignada a especie/raza incorrecta
-- Caso: Se registro un gato como canino por error
-- ----------------------------------------------------------------------------
UPDATE mascota
SET id_especie = 2,  -- Felino
    id_raza = 8      -- Comun Europeo
WHERE id_mascota = 2
  AND nombre = 'Michi';

-- ----------------------------------------------------------------------------
-- 7.2 Reasignar un turno a otro slot (cambio de horario)
-- Caso: El dueno pide cambiar el horario de su turno
-- Requiere: Liberar slot original y reservar nuevo slot
-- ----------------------------------------------------------------------------
START TRANSACTION;

-- Liberar slot original
UPDATE slot_agenda
SET estado = 'Disponible'
WHERE id_slot = 3;  -- Slot original (Lun 18/05 10:00)

-- Actualizar turno al nuevo slot
UPDATE turno
SET id_slot = 5     -- Nuevo slot (Lun 18/05 11:00)
WHERE id_turno = 3
  AND estado = 'Pendiente';

-- Reservar nuevo slot
UPDATE slot_agenda
SET estado = 'Reservado'
WHERE id_slot = 5;

COMMIT;

-- ----------------------------------------------------------------------------
-- 7.3 Corregir lote de stock asignado a item de receta
-- Caso: Se dispensa del lote equivocado; se corrige antes de cerrar la receta
-- Precondicion: El item no debe haber sido dispensado aun
-- ----------------------------------------------------------------------------
UPDATE item_receta
SET id_stock = 7  -- Corregir al lote FIFO correcto
WHERE id_item_receta = 4
  AND dispensado = 1;  -- Solo si aun no se confirmo la dispensa


-- ============================================================================
-- 8. SCRIPT DE TAREAS PROGRAMADAS (Cron Jobs sugeridos)
-- ============================================================================
-- Estas sentencias se ejecutarian desde un script externo (crontab de Linux
-- o Programador de Tareas de Windows) conectandose a MySQL.
--
-- Tarea diaria (00:00 hs): Marcar inasistencias y verificar vencimientos
--   mysql -u usuario -p -e "USE sigvet; CALL sp_marcar_inasistencias(); CALL sp_verificar_vencimientos();"
--
-- Tarea semanal (domingo 01:00 hs): Generar slots y limpiar pasados
--   mysql -u usuario -p -e "USE sigvet; CALL sp_limpiar_slots_pasados(); CALL sp_generar_slots_semana(DATE_ADD(CURDATE(), INTERVAL 1 DAY));"
--
-- Tarea mensual (1ro de cada mes): Resumen mensual
--   mysql -u usuario -p -e "USE sigvet; CALL sp_resumen_mensual(YEAR(CURDATE()), MONTH(CURDATE()));"
-- ============================================================================


-- ============================================================================
-- FIN DEL SCRIPT DE MANTENIMIENTO - SIGVET
-- ============================================================================
