-- ============================================================================
-- SIGVET - Sistema de Informacion para Gestion Veterinaria
-- Script de Consultas SQL
-- ============================================================================
-- Proyecto: AP2 - Trabajo Practico Numero 2
-- Alumno: Winck Joaquin Ezequiel
-- Carrera: Lic. en Informatica
-- Base de Datos: MySQL 8.0+ / InnoDB
-- Fecha: Mayo 2026
-- ============================================================================
-- CONTENIDO:
--   Q01-Q04:  Consultas por Caso de Uso (CU-01 a CU-04)
--   Q05-Q08:  Consultas por Caso de Uso (CU-05 a CU-08)
--   Q09-Q13:  Consultas analiticas y de reporting
--   Q14-Q17:  Consultas con subconsultas y funciones de ventana
--   Q18-Q20:  Consultas de verificacion de integridad
-- ============================================================================

USE sigvet;

-- ============================================================================
-- CU-01: REGISTRAR TURNO
-- ============================================================================

-- Q01: Obtener slots disponibles para un veterinario en una fecha especifica
-- Caso de uso: El recepcionista busca horarios disponibles para la Dra. Rodriguez
-- el lunes 18/05/2026.
SELECT
    sa.id_slot,
    sa.fecha,
    sa.hora,
    sa.estado,
    v.apellido AS apellido_vet,
    v.nombre AS nombre_vet,
    ad.dia_semana
FROM slot_agenda sa
JOIN agenda_disponibilidad ad ON ad.id_agenda = sa.id_agenda
JOIN veterinario v ON v.id_veterinario = ad.id_veterinario
WHERE sa.fecha = '2026-05-18'
  AND v.id_veterinario = 1
  AND sa.estado = 'Disponible'
ORDER BY sa.hora ASC;

-- Q02: Obtener todos los slots disponibles de la semana actual agrupados por veterinario
-- Caso de uso: El recepcionista ve la disponibilidad semanal completa.
SELECT
    v.apellido AS veterinario,
    sa.fecha,
    sa.hora,
    sa.estado
FROM slot_agenda sa
JOIN agenda_disponibilidad ad ON ad.id_agenda = sa.id_agenda
JOIN veterinario v ON v.id_veterinario = ad.id_veterinario
WHERE sa.fecha BETWEEN '2026-05-18' AND '2026-05-22'
  AND sa.estado = 'Disponible'
  AND v.estado = 'Activo'
ORDER BY v.apellido, sa.fecha, sa.hora;

-- Q03: Buscar mascota por nombre o DNI del dueno para asignar al turno
-- Caso de uso: El recepcionista busca la mascota para registrar el turno.
SELECT
    m.id_mascota,
    m.nombre AS nombre_mascota,
    e.nombre AS especie,
    r.nombre AS raza,
    d.dni,
    d.apellido AS apellido_dueno,
    d.nombre AS nombre_dueno,
    d.telefono
FROM mascota m
JOIN dueno d ON d.id_dueno = m.id_dueno
JOIN especie e ON e.id_especie = m.id_especie
JOIN raza r ON r.id_raza = m.id_raza
WHERE m.estado = 'Activo'
  AND (d.dni LIKE '%1234%' OR m.nombre LIKE '%Toby%')
ORDER BY d.apellido, m.nombre;

-- Q04: Consultar turnos de una mascota especifica (historial de turnos)
-- Caso de uso: Ver si la mascota ya tiene turnos programados.
SELECT
    t.id_turno,
    t.motivo,
    t.estado,
    t.fecha_registro,
    sa.fecha AS fecha_turno,
    sa.hora AS hora_turno,
    v.apellido AS veterinario
FROM turno t
JOIN slot_agenda sa ON sa.id_slot = t.id_slot
JOIN agenda_disponibilidad ad ON ad.id_agenda = sa.id_agenda
JOIN veterinario v ON v.id_veterinario = ad.id_veterinario
WHERE t.id_mascota = 1
ORDER BY sa.fecha DESC, sa.hora DESC;


-- ============================================================================
-- CU-02: REGISTRAR CONSULTA MEDICA
-- ============================================================================

-- Q05: Obtener datos completos de un turno pendiente para atender
-- Caso de uso: El veterinario ve los datos del turno antes de iniciar la consulta.
SELECT
    t.id_turno,
    t.motivo,
    m.nombre AS nombre_mascota,
    e.nombre AS especie,
    r.nombre AS raza,
    fn_calcular_edad_mascota(m.id_mascota) AS edad_anios,
    m.sexo,
    m.color,
    m.senas_particulares,
    d.apellido AS apellido_dueno,
    d.nombre AS nombre_dueno,
    d.telefono AS telefono_dueno,
    sa.fecha AS fecha_turno,
    sa.hora AS hora_turno
FROM turno t
JOIN mascota m ON m.id_mascota = t.id_mascota
JOIN dueno d ON d.id_dueno = m.id_dueno
JOIN especie e ON e.id_especie = m.id_especie
JOIN raza r ON r.id_raza = m.id_raza
JOIN slot_agenda sa ON sa.id_slot = t.id_slot
WHERE t.id_turno = 1
  AND t.estado = 'Pendiente';

-- Q06: Obtener historial clinico previo de una mascota antes de la consulta
-- Caso de uso: El veterinario revisa antecedentes antes de diagnosticar.
SELECT
    cm.fecha,
    v.apellido AS veterinario,
    cm.sintomas,
    cm.diagnostico,
    cm.estado AS estado_consulta,
    GROUP_CONCAT(
        CONCAT(med.nombre_comercial, ' (', ir.dosis, ') x', ir.cantidad)
        SEPARATOR '; '
    ) AS medicamentos_recetados
FROM consulta_medica cm
JOIN veterinario v ON v.id_veterinario = cm.id_veterinario
LEFT JOIN item_receta ir ON ir.id_consulta = cm.id_consulta
LEFT JOIN stock s ON s.id_stock = ir.id_stock
LEFT JOIN medicamento med ON med.id_medicamento = s.id_medicamento
WHERE cm.id_mascota = 1
  AND cm.estado = 'Activa'
GROUP BY cm.id_consulta, cm.fecha, v.apellido, cm.sintomas, cm.diagnostico, cm.estado
ORDER BY cm.fecha DESC;

-- Q07: Buscar medicamentos activos en el catalogo para prescribir
-- Caso de uso: El veterinario busca medicamentos disponibles para la receta.
SELECT
    med.id_medicamento,
    med.nombre_generico,
    med.nombre_comercial,
    med.dosis_presentacion,
    med.precio_venta,
    fn_stock_total_medicamento(med.id_medicamento) AS stock_disponible
FROM medicamento med
WHERE med.estado = 'Activo'
  AND fn_stock_total_medicamento(med.id_medicamento) > 0
ORDER BY med.nombre_generico;

-- Q08: Verificar stock disponible de un medicamento por lote (FIFO)
-- Caso de uso: El sistema muestra lotes disponibles para dispensar.
SELECT
    s.id_stock,
    s.numero_lote,
    s.cantidad,
    s.fecha_vencimiento,
    DATEDIFF(s.fecha_vencimiento, CURDATE()) AS dias_para_vencer,
    med.nombre_comercial,
    med.dosis_presentacion
FROM stock s
JOIN medicamento med ON med.id_medicamento = s.id_medicamento
WHERE s.id_medicamento = 4  -- Enrofloxacina
  AND s.cantidad > 0
  AND s.fecha_vencimiento > CURDATE()
ORDER BY s.fecha_vencimiento ASC;


-- ============================================================================
-- CU-03: REGISTRAR MASCOTA
-- ============================================================================

-- Q09: Obtener catalogo de especies y razas para el formulario de registro
-- Caso de uso: Cargar los combos de especie y raza en la UI.
SELECT
    e.id_especie,
    e.nombre AS especie,
    r.id_raza,
    r.nombre AS raza
FROM especie e
LEFT JOIN raza r ON r.id_especie = e.id_especie
ORDER BY e.nombre, r.nombre;

-- Q10: Verificar si ya existe una mascota con el mismo nombre para un dueno
-- Caso de uso: Prevenir registro duplicado de mascota.
SELECT
    m.id_mascota,
    m.nombre,
    e.nombre AS especie,
    m.estado
FROM mascota m
JOIN especie e ON e.id_especie = m.id_especie
WHERE m.id_dueno = 1
  AND m.nombre = 'Toby'
  AND m.estado = 'Activo';


-- ============================================================================
-- CU-04: REGISTRAR DUENO
-- ============================================================================

-- Q11: Verificar si ya existe un dueno con el mismo DNI
-- Caso de uso: Prevenir registro duplicado de dueno.
SELECT id_dueno, dni, nombre, apellido, estado
FROM dueno
WHERE dni = '30123456';

-- Q12: Buscar dueno por apellido para verificar existencia
-- Caso de uso: El recepcionista busca si el dueno ya esta registrado.
SELECT
    id_dueno,
    dni,
    nombre,
    apellido,
    telefono,
    email,
    estado
FROM dueno
WHERE apellido LIKE '%Fer%'
  AND estado = 'Activo'
ORDER BY apellido, nombre;


-- ============================================================================
-- CU-05: CONSULTAR HISTORIAL CLINICO
-- ============================================================================

-- Q13: Historial clinico completo de una mascota (vista)
-- Caso de uso: El veterinario consulta el historial completo.
SELECT *
FROM vw_historial_clinico
WHERE nombre_mascota = 'Toby'
ORDER BY fecha_consulta DESC;

-- Q14: Historial clinico con medicamentos dispensados y detalles de lotes
-- Caso de uso: Trazabilidad completa de medicamentos por mascota.
SELECT
    cm.fecha AS fecha_consulta,
    v.apellido AS veterinario,
    cm.sintomas,
    cm.diagnostico,
    med.nombre_comercial AS medicamento,
    med.nombre_generico,
    ir.cantidad,
    ir.dosis,
    ir.frecuencia,
    ir.duracion,
    ir.dispensado,
    s.numero_lote,
    s.fecha_vencimiento AS vencimiento_lote
FROM consulta_medica cm
JOIN veterinario v ON v.id_veterinario = cm.id_veterinario
LEFT JOIN item_receta ir ON ir.id_consulta = cm.id_consulta
LEFT JOIN stock s ON s.id_stock = ir.id_stock
LEFT JOIN medicamento med ON med.id_medicamento = s.id_medicamento
WHERE cm.id_mascota = 1
  AND cm.estado = 'Activa'
ORDER BY cm.fecha DESC, med.nombre_comercial;

-- Q15: Ultimas 5 consultas de una mascota (resumen para ficha rapida)
-- Caso de uso: Pantalla de inicio del historial.
SELECT
    cm.fecha,
    v.apellido AS veterinario,
    LEFT(cm.sintomas, 50) AS sintomas_resumen,
    LEFT(cm.diagnostico, 50) AS diagnostico_resumen,
    (SELECT COUNT(*) FROM item_receta ir WHERE ir.id_consulta = cm.id_consulta) AS medicamentos
FROM consulta_medica cm
JOIN veterinario v ON v.id_veterinario = cm.id_veterinario
WHERE cm.id_mascota = 1
  AND cm.estado = 'Activa'
ORDER BY cm.fecha DESC
LIMIT 5;


-- ============================================================================
-- CU-06: GESTIONAR STOCK / DISPENSAR MEDICAMENTO
-- ============================================================================

-- Q16: Stock actual de todos los medicamentos con estado de umbral (vista)
-- Caso de uso: Pantalla principal de inventario.
SELECT *
FROM vw_stock_medicamentos
ORDER BY estado_stock ASC, nombre_comercial ASC;

-- Q17: Detalle de lotes por medicamento con estado de vencimiento
-- Caso de uso: El recepcionista ve los lotes individuales.
SELECT
    med.nombre_comercial,
    s.numero_lote,
    s.cantidad,
    s.fecha_vencimiento,
    s.fecha_ingreso,
    CASE
        WHEN s.fecha_vencimiento <= CURDATE() THEN 'VENCIDO'
        WHEN s.fecha_vencimiento <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN 'PROXIMO A VENCER'
        ELSE 'VIGENTE'
    END AS estado_vencimiento,
    DATEDIFF(s.fecha_vencimiento, CURDATE()) AS dias_para_vencer
FROM stock s
JOIN medicamento med ON med.id_medicamento = s.id_medicamento
WHERE med.estado = 'Activo'
ORDER BY med.nombre_comercial, s.fecha_vencimiento ASC;

-- Q18: Medicamentos con stock bajo (por debajo del umbral minimo)
-- Caso de uso: Reporte de reposicion de stock.
SELECT
    med.id_medicamento,
    med.nombre_comercial,
    med.nombre_generico,
    med.stock_minimo_alerta,
    fn_stock_total_medicamento(med.id_medicamento) AS stock_disponible,
    fn_stock_total_medicamento(med.id_medicamento) - med.stock_minimo_alerta AS deficit
FROM medicamento med
WHERE med.estado = 'Activo'
  AND fn_stock_total_medicamento(med.id_medicamento) < med.stock_minimo_alerta
ORDER BY deficit ASC;

-- Q19: Lotes proximos a vencer (dentro de 30 dias)
-- Caso de uso: Alerta proactiva de vencimiento.
SELECT
    med.nombre_comercial,
    s.numero_lote,
    s.cantidad,
    s.fecha_vencimiento,
    DATEDIFF(s.fecha_vencimiento, CURDATE()) AS dias_para_vencer
FROM stock s
JOIN medicamento med ON med.id_medicamento = s.id_medicamento
WHERE s.fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)
  AND s.cantidad > 0
  AND med.estado = 'Activo'
ORDER BY s.fecha_vencimiento ASC;


-- ============================================================================
-- CU-07: CONSULTAR ALERTAS DE STOCK
-- ============================================================================

-- Q20: Alertas activas (vista)
-- Caso de uso: Pantalla principal de alertas del recepcionista.
SELECT *
FROM vw_alertas_activas
ORDER BY tipo ASC, fecha_generacion ASC;

-- Q21: Historial completo de alertas (incluye resueltas)
-- Caso de uso: Auditoria de alertas pasadas.
SELECT
    a.id_alerta,
    a.tipo,
    a.mensaje,
    a.estado,
    a.fecha_generacion,
    a.fecha_resolucion,
    med.nombre_comercial,
    CASE
        WHEN a.estado = 'Resuelta' THEN TIMESTAMPDIFF(HOUR, a.fecha_generacion, a.fecha_resolucion)
        ELSE NULL
    END AS horas_para_resolver
FROM alerta_stock a
JOIN medicamento med ON med.id_medicamento = a.id_medicamento
ORDER BY a.fecha_generacion DESC;

-- Q22: Cantidad de alertas por tipo y estado (resumen estadistico)
-- Caso de uso: Dashboard de alertas.
SELECT
    tipo,
    estado,
    COUNT(*) AS cantidad
FROM alerta_stock
GROUP BY tipo, estado
ORDER BY tipo, estado;


-- ============================================================================
-- CU-08: GESTIONAR AGENDA
-- ============================================================================

-- Q23: Agenda del dia (vista)
-- Caso de uso: Pantalla principal del recepcionista al inicio del dia.
SELECT *
FROM vw_turnos_del_dia
ORDER BY hora_turno ASC;

-- Q24: Agenda semanal completa de un veterinario
-- Caso de uso: El veterinario ve su semana.
SELECT
    sa.fecha,
    sa.hora AS hora_slot,
    sa.estado AS estado_slot,
    t.estado AS estado_turno,
    t.motivo,
    m.nombre AS nombre_mascota,
    e.nombre AS especie,
    d.apellido AS apellido_dueno,
    d.telefono
FROM slot_agenda sa
JOIN agenda_disponibilidad ad ON ad.id_agenda = sa.id_agenda
LEFT JOIN turno t ON t.id_slot = sa.id_slot AND t.estado IN ('Pendiente', 'Atendido')
LEFT JOIN mascota m ON m.id_mascota = t.id_mascota
LEFT JOIN dueno d ON d.id_dueno = m.id_dueno
LEFT JOIN especie e ON e.id_especie = m.id_especie
WHERE ad.id_veterinario = 1
  AND sa.fecha BETWEEN '2026-05-18' AND '2026-05-22'
ORDER BY sa.fecha, sa.hora;

-- Q25: Franjas de disponibilidad de un veterinario
-- Caso de uso: Configurar la agenda del veterinario.
SELECT
    id_agenda,
    dia_semana,
    hora_inicio,
    hora_fin,
    CONCAT(hora_inicio, ' - ', hora_fin) AS franja
FROM agenda_disponibilidad
WHERE id_veterinario = 1
ORDER BY FIELD(dia_semana, 'Lunes','Martes','Miercoles','Jueves','Viernes','Sabado','Domingo');

-- Q26: Turnos cancelados o inasistencia del ultimo mes
-- Caso de uso: Reporte de turnos perdidos para gestion comercial.
SELECT
    t.id_turno,
    t.estado,
    t.motivo,
    t.fecha_registro,
    sa.fecha AS fecha_turno,
    m.nombre AS mascota,
    d.apellido AS dueno,
    d.telefono,
    v.apellido AS veterinario
FROM turno t
JOIN slot_agenda sa ON sa.id_slot = t.id_slot
JOIN mascota m ON m.id_mascota = t.id_mascota
JOIN dueno d ON d.id_dueno = m.id_dueno
JOIN agenda_disponibilidad ad ON ad.id_agenda = sa.id_agenda
JOIN veterinario v ON v.id_veterinario = ad.id_veterinario
WHERE t.estado IN ('Cancelado', 'Inasistencia')
ORDER BY sa.fecha DESC;


-- ============================================================================
-- CONSULTAS ANALITICAS Y DE REPORTING
-- ============================================================================

-- Q27: Cantidad de consultas por veterinario en el ultimo mes
-- Caso de uso: Estadisticas de productividad profesional.
SELECT
    v.apellido AS veterinario,
    COUNT(*) AS consultas_realizadas,
    SUM(CASE WHEN cm.id_turno IS NULL THEN 1 ELSE 0 END) AS urgencias,
    SUM(CASE WHEN cm.id_turno IS NOT NULL THEN 1 ELSE 0 END) AS programadas
FROM consulta_medica cm
JOIN veterinario v ON v.id_veterinario = cm.id_veterinario
WHERE cm.estado = 'Activa'
GROUP BY v.id_veterinario, v.apellido
ORDER BY consultas_realizadas DESC;

-- Q28: Top 5 medicamentos mas recetados
-- Caso de uso: Reporte de medicamentos mas utilizados.
SELECT
    med.nombre_comercial,
    med.nombre_generico,
    COUNT(ir.id_item_receta) AS veces_recetado,
    SUM(ir.cantidad) AS unidades_dispensadas,
    SUM(ir.cantidad * med.precio_venta) AS facturacion_estimada
FROM item_receta ir
JOIN stock s ON s.id_stock = ir.id_stock
JOIN medicamento med ON med.id_medicamento = s.id_medicamento
WHERE ir.dispensado = 1
GROUP BY med.id_medicamento, med.nombre_comercial, med.nombre_generico
ORDER BY veces_recetado DESC
LIMIT 5;

-- Q29: Cantidad de mascotas registradas por especie
-- Caso de uso: Estadisticas demograficas del paciente.
SELECT
    e.nombre AS especie,
    COUNT(m.id_mascota) AS cantidad_mascotas,
    ROUND(COUNT(m.id_mascota) * 100.0 / (SELECT COUNT(*) FROM mascota WHERE estado = 'Activo'), 1) AS porcentaje
FROM especie e
LEFT JOIN mascota m ON m.id_especie = e.id_especie AND m.estado = 'Activo'
GROUP BY e.id_especie, e.nombre
ORDER BY cantidad_mascotas DESC;

-- Q30: Ingresos estimados por consultas y medicamentos dispensados
-- Caso de uso: Reporte financiero basico.
SELECT
    DATE_FORMAT(cm.fecha, '%Y-%m') AS mes,
    COUNT(DISTINCT cm.id_consulta) AS total_consultas,
    COUNT(DISTINCT cm.id_consulta) * 5000 AS ingresos_consultas_est,
    SUM(CASE WHEN ir.dispensado = 1 THEN ir.cantidad * med.precio_venta ELSE 0 END) AS ingresos_medicamentos
FROM consulta_medica cm
LEFT JOIN item_receta ir ON ir.id_consulta = cm.id_consulta
LEFT JOIN stock s ON s.id_stock = ir.id_stock
LEFT JOIN medicamento med ON med.id_medicamento = s.id_medicamento
WHERE cm.estado = 'Activa'
GROUP BY DATE_FORMAT(cm.fecha, '%Y-%m')
ORDER BY mes DESC;


-- ============================================================================
-- CONSULTAS CON SUBCONSULTAS Y FUNCIONES DE VENTANA
-- ============================================================================

-- Q31: Mascotas con mas consultas (ranking)
-- Caso de uso: Identificar pacientes frecuentes.
SELECT
    m.nombre AS mascota,
    e.nombre AS especie,
    d.apellido AS dueno,
    COUNT(cm.id_consulta) AS total_consultas
FROM mascota m
JOIN dueno d ON d.id_dueno = m.id_dueno
JOIN especie e ON e.id_especie = m.id_especie
JOIN consulta_medica cm ON cm.id_mascota = m.id_mascota AND cm.estado = 'Activa'
WHERE m.estado = 'Activo'
GROUP BY m.id_mascota, m.nombre, e.nombre, d.apellido
HAVING total_consultas > 0
ORDER BY total_consultas DESC;

-- Q32: Veterinario con mayor carga de turnos en la semana
-- Caso de uso: Distribucion de carga de trabajo.
SELECT
    v.apellido AS veterinario,
    COUNT(t.id_turno) AS turnos_asignados,
    SUM(CASE WHEN t.estado = 'Pendiente' THEN 1 ELSE 0 END) AS pendientes,
    SUM(CASE WHEN t.estado = 'Atendido' THEN 1 ELSE 0 END) AS atendidos,
    SUM(CASE WHEN t.estado = 'Cancelado' THEN 1 ELSE 0 END) AS cancelados
FROM veterinario v
JOIN agenda_disponibilidad ad ON ad.id_veterinario = v.id_veterinario
JOIN slot_agenda sa ON sa.id_agenda = ad.id_agenda
LEFT JOIN turno t ON t.id_slot = sa.id_slot
WHERE sa.fecha BETWEEN '2026-05-18' AND '2026-05-22'
GROUP BY v.id_veterinario, v.apellido
ORDER BY turnos_asignados DESC;

-- Q33: Medicamentos que nunca fueron recetados
-- Caso de uso: Identificar medicamentos sin rotacion para posible discontinuacion.
SELECT
    med.id_medicamento,
    med.nombre_comercial,
    med.nombre_generico,
    med.estado,
    fn_stock_total_medicamento(med.id_medicamento) AS stock_actual
FROM medicamento med
WHERE med.id_medicamento NOT IN (
    SELECT DISTINCT s.id_medicamento
    FROM item_receta ir
    JOIN stock s ON s.id_stock = ir.id_stock
)
AND med.estado = 'Activo'
ORDER BY med.nombre_comercial;

-- Q34: Dueños con mas de una mascota registrada
-- Caso de uso: Identificar clientes frecuentes para programas de fidelidad.
SELECT
    d.id_dueno,
    d.apellido,
    d.nombre,
    d.telefono,
    COUNT(m.id_mascota) AS cantidad_mascotas
FROM dueno d
JOIN mascota m ON m.id_dueno = d.id_dueno AND m.estado = 'Activo'
WHERE d.estado = 'Activo'
GROUP BY d.id_dueno, d.apellido, d.nombre, d.telefono
HAVING cantidad_mascotas > 1
ORDER BY cantidad_mascotas DESC, d.apellido;

-- Q35: Consultas por dia de la semana (patron de demanda)
-- Caso de uso: Planificacion de recursos humanos.
SELECT
    DAYNAME(cm.fecha) AS dia_semana,
    COUNT(*) AS consultas
FROM consulta_medica cm
WHERE cm.estado = 'Activa'
GROUP BY DAYNAME(cm.fecha)
ORDER BY FIELD(dia_semana, 'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday');


-- ============================================================================
-- CONSULTAS DE VERIFICACION DE INTEGRIDAD
-- ============================================================================

-- Q36: Verificar que no hay slots reservados sin turno activo
-- Caso de uso: Auditoria de consistencia slot-turno.
SELECT
    sa.id_slot,
    sa.fecha,
    sa.hora,
    sa.estado
FROM slot_agenda sa
WHERE sa.estado = 'Reservado'
  AND NOT EXISTS (
    SELECT 1 FROM turno t
    WHERE t.id_slot = sa.id_slot
      AND t.estado IN ('Pendiente', 'Atendido')
  );

-- Q37: Verificar que no hay consultas con turno 'Atendido' sin consulta asociada
-- Caso de uso: Auditoria RN-12.
SELECT
    t.id_turno,
    t.estado,
    t.id_mascota
FROM turno t
WHERE t.estado = 'Atendido'
  AND NOT EXISTS (
    SELECT 1 FROM consulta_medica cm
    WHERE cm.id_turno = t.id_turno
      AND cm.estado = 'Activa'
  );

-- Q38: Verificar que no hay stock negativo
-- Caso de uso: Auditoria de integridad de inventario.
SELECT
    s.id_stock,
    s.numero_lote,
    s.cantidad,
    med.nombre_comercial
FROM stock s
JOIN medicamento med ON med.id_medicamento = s.id_medicamento
WHERE s.cantidad < 0;

-- Q39: Verificar integridad referencial: mascotas con dueno inactivo
-- Caso de uso: Verificar consistencia de baja logica.
SELECT
    m.id_mascota,
    m.nombre AS mascota,
    m.estado AS estado_mascota,
    d.apellido AS dueno,
    d.estado AS estado_dueno
FROM mascota m
JOIN dueno d ON d.id_dueno = m.id_dueno
WHERE m.estado = 'Activo'
  AND d.estado = 'Inactivo';

-- Q40: Verificar que todas las consultas de urgencia no tienen turno
-- Caso de uso: Verificar RN-01 (id_turno NULLABLE para urgencias).
SELECT
    cm.id_consulta,
    cm.fecha,
    cm.sintomas,
    m.nombre AS mascota,
    v.apellido AS veterinario,
    cm.id_turno
FROM consulta_medica cm
JOIN mascota m ON m.id_mascota = cm.id_mascota
JOIN veterinario v ON v.id_veterinario = cm.id_veterinario
WHERE cm.id_turno IS NULL
  AND cm.estado = 'Activa'
ORDER BY cm.fecha DESC;


-- ============================================================================
-- FIN DEL SCRIPT DE CONSULTAS - SIGVET
-- ============================================================================
