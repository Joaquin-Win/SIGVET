-- ============================================================================
-- SIGVET - Sistema de Informacion para Gestion Veterinaria
-- Script DML (Data Manipulation Language) - Datos de Prueba
-- ============================================================================
-- Proyecto: AP2 - Trabajo Practico Numero 2
-- Alumno: Winck Joaquin Ezequiel
-- Carrera: Lic. en Informatica
-- Base de Datos: MySQL 8.0+ / InnoDB
-- Fecha: Mayo 2026
-- ============================================================================
-- CONTENIDO:
--   1. Veterinarios (3)
--   2. Duenos (6)
--   3. Especies (7)
--   4. Medicamentos (9)
--   5. Razas (15)
--   6. Mascotas (10)
--   7. Agenda Disponibilidad (6 franjas)
--   8. Stock (18 lotes)
--   9. Slot Agenda (30+ slots)
--  10. Alertas Stock (manuales, las automaticas las generan los triggers)
--  11. Turnos (10)
--  12. Consultas Medicas (7)
--  13. Items Receta (8)
-- ============================================================================
-- NOTAS:
--   - Ejecutar DESPUES de sigvet_ddl.sql
--   - Los triggers generan alertas automaticas al insertar stock
--   - Se incluyen turnos en estados: Pendiente, Atendido, Cancelado
--   - Se incluyen consultas con y sin turno (urgencias)
--   - Las fechas de stock son futuras respecto a Mayo 2026
--   - Los slots se generan para la semana del 18-22 Mayo 2026
-- ============================================================================

USE sigvet;

-- ============================================================================
-- 1. VETERINARIOS (3 profesionales)
-- ============================================================================

INSERT INTO veterinario (nombre, apellido, matricula, telefono, email, estado) VALUES
('Carolina', 'Rodriguez', 'MP-45231', '351-555-0101', 'crodriguez@sigvet.com', 'Activo'),
('Marcos', 'Lombardi', 'MN-78456', '351-555-0102', 'mlombardi@sigvet.com', 'Activo'),
('Elena', 'Vazquez', 'MP-32109', '351-555-0103', 'evazquez@sigvet.com', 'Activo');

-- ============================================================================
-- 2. DUENOS (6 propietarios)
-- ============================================================================

INSERT INTO dueno (dni, nombre, apellido, telefono, direccion, email, estado) VALUES
('30123456', 'Lucia', 'Fernandez', '351-555-1001', 'Av. Colon 1234', 'lucia.fernandez@email.com', 'Activo'),
('28765432', 'Roberto', 'Gomez', '351-555-1002', 'Bv. San Juan 567', 'rgomez@email.com', 'Activo'),
('32456789', 'Ana Maria', 'Lopez', '351-555-1003', 'Calle Rivadavia 890', 'analopez@email.com', 'Activo'),
('35111222', 'Diego', 'Martinez', '351-555-1004', 'Pasaje Italia 45', 'dmartinez@email.com', 'Activo'),
('29887766', 'Carolina', 'Pereyra', '351-555-1005', 'Bv. Illia 234', 'cpereyra@email.com', 'Activo'),
('33445566', 'Fernando', 'Ruiz', '351-555-1006', 'Av. Sabattini 789', 'fruiz@email.com', 'Activo');

-- ============================================================================
-- 3. ESPECIES (7 especies)
-- ============================================================================

INSERT INTO especie (nombre) VALUES
('Canino'),
('Felino'),
('Ave'),
('Roedor'),
('Reptil'),
('Equino'),
('Bovino');

-- ============================================================================
-- 4. MEDICAMENTOS (9 medicamentos del catalogo)
-- ============================================================================
-- Nota: Se incluyen medicamentos con stock_minimo_alerta variado
-- para probar la generacion automatica de alertas (RN-06).

INSERT INTO medicamento (nombre_generico, nombre_comercial, dosis_presentacion, precio_venta, stock_minimo_alerta, estado, fecha_actualizacion_precio) VALUES
('Amoxicilina', 'Amoxidal', '500mg - 16 capsulas', 8500.00, 10, 'Activo', '2026-04-01'),
('Ivermectina', 'Ivomec', '1% - 10ml', 12000.00, 5, 'Activo', '2026-03-15'),
('Meloxicam', 'Meloxivet', '0.5mg/ml - 15ml', 6200.00, 8, 'Activo', '2026-04-10'),
('Enrofloxacina', 'Baytril', '50mg - 10 comprimidos', 9800.00, 5, 'Activo', NULL),
('Cefalexina', 'Rilexine', '300mg - 10 comprimidos', 7500.00, 5, 'Activo', '2026-02-20'),
('Omeprazol', 'Gastrogard', '2mg/ml - 30ml', 5400.00, 3, 'Activo', NULL),
('Dipirona', 'Bravettes', '500mg/ml - 10ml', 3200.00, 10, 'Activo', '2026-05-01'),
('Complejo B', 'Hemopet', '10ml', 4500.00, 5, 'Activo', NULL),
('Suero Fisiologico', 'SF 0.9%', '500ml', 2800.00, 3, 'Activo', NULL);

-- ============================================================================
-- 5. RAZAS (15 razas agrupadas por especie)
-- ============================================================================

-- Canino (id_especie = 1)
INSERT INTO raza (id_especie, nombre) VALUES
(1, 'Labrador Retriever'),
(1, 'Golden Retriever'),
(1, 'Pastor Aleman'),
(1, 'Bulldog Frances'),
(1, 'Poodle');

-- Felino (id_especie = 2)
INSERT INTO raza (id_especie, nombre) VALUES
(2, 'Siames'),
(2, 'Persa'),
(2, 'Maine Coon'),
(2, 'Comun Europeo');

-- Ave (id_especie = 3)
INSERT INTO raza (id_especie, nombre) VALUES
(3, 'Cotorra Argentina'),
(3, 'Canario');

-- Roedor (id_especie = 4)
INSERT INTO raza (id_especie, nombre) VALUES
(4, 'Hamster Sirio');

-- Equino (id_especie = 6)
INSERT INTO raza (id_especie, nombre) VALUES
(6, 'Criollo');

-- Reptil (id_especie = 5)
INSERT INTO raza (id_especie, nombre) VALUES
(5, 'Iguana Verde');

-- ============================================================================
-- 6. MASCOTAS (10 pacientes)
-- ============================================================================
-- FK: id_dueno -> dueno, id_especie -> especie, id_raza -> raza

INSERT INTO mascota (id_dueno, nombre, id_especie, id_raza, fecha_nacimiento, sexo, color, senas_particulares, estado) VALUES
-- Mascotas de Lucia Fernandez (id_dueno = 1)
(1, 'Toby', 1, 1, '2022-03-15', 'M', 'Dorado', 'Mancha blanca en el pecho', 'Activo'),
(1, 'Michi', 2, 8, '2023-06-20', 'F', 'Gris atigrado', NULL, 'Activo'),

-- Mascotas de Roberto Gomez (id_dueno = 2)
(2, 'Rex', 1, 3, '2020-11-08', 'M', 'Negro y fuego', 'Cicatriz en pata trasera derecha', 'Activo'),

-- Mascotas de Ana Maria Lopez (id_dueno = 3)
(3, 'Luna', 1, 2, '2023-01-10', 'F', 'Dorado', NULL, 'Activo'),
(3, 'Pelusa', 2, 7, '2021-09-05', 'F', 'Blanco', 'Ojos azules', 'Activo'),

-- Mascotas de Diego Martinez (id_dueno = 4)
(4, 'Max', 1, 5, '2024-04-22', 'M', 'Blanco', NULL, 'Activo'),

-- Mascotas de Carolina Pereyra (id_dueno = 5)
(5, 'Pepe', 3, 11, '2024-01-15', 'M', 'Verde', NULL, 'Activo'),
(5, 'Buddy', 1, 4, '2022-07-30', 'M', 'Blanco con manchas negras', NULL, 'Activo'),

-- Mascotas de Fernando Ruiz (id_dueno = 6)
(6, 'Simba', 2, 9, '2023-12-01', 'M', 'Marron atigrado', 'Cola muy poblada', 'Activo'),
(6, 'Chiqui', 4, 13, '2025-02-10', 'F', 'Beige', NULL, 'Activo');

-- ============================================================================
-- 7. AGENDA DISPONIBILIDAD (6 franjas horarias)
-- ============================================================================
-- Dra. Rodriguez (id_veterinario = 1): Lunes y Jueves manana, Miercoles tarde
-- Dr. Lombardi (id_veterinario = 2): Martes y Viernes manana
-- Dra. Vazquez (id_veterinario = 3): Martes y Jueves tarde

INSERT INTO agenda_disponibilidad (id_veterinario, dia_semana, hora_inicio, hora_fin) VALUES
(1, 'Lunes', '09:00:00', '13:00:00'),
(1, 'Jueves', '09:00:00', '13:00:00'),
(1, 'Miercoles', '14:00:00', '18:00:00'),
(2, 'Martes', '09:00:00', '13:00:00'),
(2, 'Viernes', '09:00:00', '13:00:00'),
(3, 'Martes', '14:00:00', '18:00:00');

-- ============================================================================
-- 8. STOCK (18 lotes de medicamentos)
-- ============================================================================
-- Nota: Todos los lotes tienen fecha_vencimiento > CURDATE() (requerido por trigger RN-09).
-- Se incluyen:
--   - Lotes con stock suficiente (no generan alerta)
--   - Lotes con stock bajo para Amoxicilina (genera alerta STOCK_BAJO automatica)
--   - Un lote de Dipirona venciendo en Junio 2026 (genera alerta VENCIMIENTO_PROXIMO)

INSERT INTO stock (id_medicamento, cantidad, numero_lote, fecha_vencimiento, fecha_ingreso) VALUES
-- Amoxicilina (id_medicamento = 1) - STOCK BAJO: total 8 < stock_minimo_alerta 10
(1, 5,  'LOT-2025-A001', '2027-03-15', '2025-08-20'),
(1, 3,  'LOT-2026-A012', '2027-06-30', '2026-02-10'),

-- Ivermectina (id_medicamento = 2) - Stock suficiente: total 22 > 5
(2, 10, 'LOT-2025-I003', '2027-01-20', '2025-09-15'),
(2, 12, 'LOT-2026-I008', '2027-08-10', '2026-01-05'),

-- Meloxicam (id_medicamento = 3) - Stock suficiente: total 15 > 8
(3, 15, 'LOT-2026-M005', '2027-05-22', '2026-03-01'),

-- Enrofloxacina (id_medicamento = 4) - Stock suficiente: total 18 > 5
(4, 8,  'LOT-2025-E002', '2026-12-18', '2025-07-10'),
(4, 10, 'LOT-2026-E009', '2027-09-30', '2026-04-15'),

-- Cefalexina (id_medicamento = 5) - Stock suficiente: total 20 > 5
(5, 20, 'LOT-2026-C006', '2027-04-10', '2026-01-20'),

-- Omeprazol (id_medicamento = 6) - Stock suficiente: total 7 > 3
(6, 7,  'LOT-2026-O010', '2027-07-15', '2026-03-10'),

-- Dipirona (id_medicamento = 7) - VENCIMIENTO PROXIMO + stock bajo
-- Lote 1: vence en Junio 2026 (dentro de 30 dias) -> genera alerta VENCIMIENTO_PROXIMO
(7, 4,  'LOT-2025-D004', '2026-06-10', '2025-06-01'),
-- Lote 2: vence mas adelante
(7, 3,  'LOT-2026-D011', '2027-02-28', '2026-01-15'),

-- Complejo B (id_medicamento = 8) - Stock suficiente: total 12 > 5
(8, 12, 'LOT-2026-B007', '2027-11-05', '2026-02-28'),

-- Suero Fisiologico (id_medicamento = 9) - Stock suficiente: total 9 > 3
(9, 9,  'LOT-2026-S013', '2027-10-20', '2026-04-01');

-- ============================================================================
--  NOTA SOBRE ALERTAS AUTOMATICAS:
--  Al insertar el stock anterior, los triggers habran generado:
--  - 1 alerta STOCK_BAJO para Amoxicilina (total 8 < minimo 10)
--  - 1 alerta VENCIMIENTO_PROXIMO para lote LOT-2025-D004 de Dipirona
--  - Posible alerta STOCK_BAJO para Dipirona (total 7 < minimo 10)
--  NO insertar manualmente estas alertas para evitar duplicados.
-- ============================================================================

-- ============================================================================
-- 9. SLOT AGENDA (slots de 30 minutos)
-- ============================================================================
-- Se generan slots para la semana del 18-22 de Mayo 2026.
-- Dra. Rodriguez: Lunes 18/05 y Jueves 21/05 (09:00-13:00), Miercoles 20/05 (14:00-18:00)
-- Dr. Lombardi: Martes 19/05 y Viernes 22/05 (09:00-13:00)
-- Dra. Vazquez: Martes 19/05 (14:00-18:00)
--
-- Cada franja de 4 horas genera 8 slots de 30 minutos.

-- Dra. Rodriguez - Lunes 18/05 (id_agenda = 1: Lunes 09:00-13:00)
INSERT INTO slot_agenda (id_agenda, fecha, hora, estado) VALUES
(1, '2026-05-18', '09:00:00', 'Disponible'),
(1, '2026-05-18', '09:30:00', 'Disponible'),
(1, '2026-05-18', '10:00:00', 'Disponible'),
(1, '2026-05-18', '10:30:00', 'Disponible'),
(1, '2026-05-18', '11:00:00', 'Disponible'),
(1, '2026-05-18', '11:30:00', 'Disponible'),
(1, '2026-05-18', '12:00:00', 'Disponible'),
(1, '2026-05-18', '12:30:00', 'Disponible');

-- Dra. Rodriguez - Jueves 21/05 (id_agenda = 2: Jueves 09:00-13:00)
INSERT INTO slot_agenda (id_agenda, fecha, hora, estado) VALUES
(2, '2026-05-21', '09:00:00', 'Disponible'),
(2, '2026-05-21', '09:30:00', 'Disponible'),
(2, '2026-05-21', '10:00:00', 'Disponible'),
(2, '2026-05-21', '10:30:00', 'Disponible'),
(2, '2026-05-21', '11:00:00', 'Disponible'),
(2, '2026-05-21', '11:30:00', 'Disponible'),
(2, '2026-05-21', '12:00:00', 'Disponible'),
(2, '2026-05-21', '12:30:00', 'Disponible');

-- Dra. Rodriguez - Miercoles 20/05 (id_agenda = 3: Miercoles 14:00-18:00)
INSERT INTO slot_agenda (id_agenda, fecha, hora, estado) VALUES
(3, '2026-05-20', '14:00:00', 'Disponible'),
(3, '2026-05-20', '14:30:00', 'Disponible'),
(3, '2026-05-20', '15:00:00', 'Disponible'),
(3, '2026-05-20', '15:30:00', 'Disponible'),
(3, '2026-05-20', '16:00:00', 'Disponible'),
(3, '2026-05-20', '16:30:00', 'Disponible'),
(3, '2026-05-20', '17:00:00', 'Disponible'),
(3, '2026-05-20', '17:30:00', 'Disponible');

-- Dr. Lombardi - Martes 19/05 (id_agenda = 4: Martes 09:00-13:00)
INSERT INTO slot_agenda (id_agenda, fecha, hora, estado) VALUES
(4, '2026-05-19', '09:00:00', 'Disponible'),
(4, '2026-05-19', '09:30:00', 'Disponible'),
(4, '2026-05-19', '10:00:00', 'Disponible'),
(4, '2026-05-19', '10:30:00', 'Disponible'),
(4, '2026-05-19', '11:00:00', 'Disponible'),
(4, '2026-05-19', '11:30:00', 'Disponible'),
(4, '2026-05-19', '12:00:00', 'Disponible'),
(4, '2026-05-19', '12:30:00', 'Disponible');

-- Dr. Lombardi - Viernes 22/05 (id_agenda = 5: Viernes 09:00-13:00)
INSERT INTO slot_agenda (id_agenda, fecha, hora, estado) VALUES
(5, '2026-05-22', '09:00:00', 'Disponible'),
(5, '2026-05-22', '09:30:00', 'Disponible'),
(5, '2026-05-22', '10:00:00', 'Disponible'),
(5, '2026-05-22', '10:30:00', 'Disponible'),
(5, '2026-05-22', '11:00:00', 'Disponible'),
(5, '2026-05-22', '11:30:00', 'Disponible'),
(5, '2026-05-22', '12:00:00', 'Disponible'),
(5, '2026-05-22', '12:30:00', 'Disponible');

-- Dra. Vazquez - Martes 19/05 (id_agenda = 6: Martes 14:00-18:00)
INSERT INTO slot_agenda (id_agenda, fecha, hora, estado) VALUES
(6, '2026-05-19', '14:00:00', 'Disponible'),
(6, '2026-05-19', '14:30:00', 'Disponible'),
(6, '2026-05-19', '15:00:00', 'Disponible'),
(6, '2026-05-19', '15:30:00', 'Disponible'),
(6, '2026-05-19', '16:00:00', 'Disponible'),
(6, '2026-05-19', '16:30:00', 'Disponible'),
(6, '2026-05-19', '17:00:00', 'Disponible'),
(6, '2026-05-19', '17:30:00', 'Disponible');

-- ============================================================================
-- 10. ALERTAS STOCK (manuales - las automaticas ya fueron generadas por triggers)
-- ============================================================================
-- Las alertas STOCK_BAJO para Amoxicilina y Dipirona, y VENCIMIENTO_PROXIMO
-- para Dipirona fueron generadas automaticamente por los triggers al insertar stock.
-- Se agregan alertas manuales para estados 'En Gestion' y 'Resuelta'.

INSERT INTO alerta_stock (id_medicamento, tipo, mensaje, estado, fecha_generacion, fecha_resolucion) VALUES
-- Alerta resuelta: Cefalexina habia estado en stock bajo pero ya se repuso
(5, 'STOCK_BAJO', 'Stock bajo de Rilexine: 2 unidades (minimo: 5)', 'Resuelta', '2026-04-20 10:30:00', '2026-04-25 14:00:00');

-- ============================================================================
-- 11. TURNOS (10 turnos)
-- ============================================================================
-- Los turnos se insertan como 'Pendiente'. Los que tengan consulta asociada
-- se actualizaran a 'Atendido' despues de insertar las consultas.
--
-- IMPORTANTE: El trigger trg_verificar_slot_disponible verifica que el slot
-- este 'Disponible' antes de insertar. El trigger trg_actualizar_slot_al_reservar
-- cambia el slot a 'Reservado' automaticamente.
--
-- Mapeo de slots (id_slot se genera con AUTO_INCREMENT):
--   Slots 1-8:   Dra. Rodriguez - Lun 18/05 (09:00-12:30)
--   Slots 9-16:  Dra. Rodriguez - Jue 21/05 (09:00-12:30)
--   Slots 17-24: Dra. Rodriguez - Mie 20/05 (14:00-17:30)
--   Slots 25-32: Dr. Lombardi   - Mar 19/05 (09:00-12:30)
--   Slots 33-40: Dr. Lombardi   - Vie 22/05 (09:00-12:30)
--   Slots 41-48: Dra. Vazquez   - Mar 19/05 (14:00-17:30)

-- Turno 1: Toby - Dra. Rodriguez - Lun 18/05 09:00
INSERT INTO turno (id_mascota, id_slot, motivo, estado) VALUES
(1, 1, 'Vacunacion anual', 'Pendiente');

-- Turno 2: Rex - Dr. Lombardi - Mar 19/05 09:00
INSERT INTO turno (id_mascota, id_slot, motivo, estado) VALUES
(3, 25, 'Control postoperatorio', 'Pendiente');

-- Turno 3: Luna - Dra. Rodriguez - Lun 18/05 10:00
INSERT INTO turno (id_mascota, id_slot, motivo, estado) VALUES
(4, 3, 'Vomitos y diarrea', 'Pendiente');

-- Turno 4: Pelusa - Dra. Vazquez - Mar 19/05 14:00
INSERT INTO turno (id_mascota, id_slot, motivo, estado) VALUES
(5, 41, 'Desparasitacion', 'Pendiente');

-- Turno 5: Max - Dr. Lombardi - Mar 19/05 10:00
INSERT INTO turno (id_mascota, id_slot, motivo, estado) VALUES
(6, 27, 'Revision general', 'Pendiente');

-- Turno 6: Buddy - Dra. Rodriguez - Mie 20/05 14:00
INSERT INTO turno (id_mascota, id_slot, motivo, estado) VALUES
(8, 17, 'Dificultad respiratoria', 'Pendiente');

-- Turno 7: Simba - Dr. Lombardi - Vie 22/05 09:00
INSERT INTO turno (id_mascota, id_slot, motivo, estado) VALUES
(9, 33, 'Vacunacion', 'Pendiente');

-- Turno 8: Michi - Dra. Rodriguez - Jue 21/05 09:00
INSERT INTO turno (id_mascota, id_slot, motivo, estado) VALUES
(2, 9, 'Control de peso', 'Pendiente');

-- Turno 9: Pepe - Dr. Lombardi - Vie 22/05 10:30 (se cancelara despues)
INSERT INTO turno (id_mascota, id_slot, motivo, estado) VALUES
(7, 36, 'Revision alas', 'Pendiente');

-- Turno 10: Chiqui - Dra. Vazquez - Mar 19/05 15:00
INSERT INTO turno (id_mascota, id_slot, motivo, estado) VALUES
(10, 43, 'Control general', 'Pendiente');

-- ============================================================================
-- 12. CONSULTAS MEDICAS (7 consultas)
-- ============================================================================
-- Se insertan consultas para algunos de los turnos anteriores.
-- Consultas 1-5: con turno asociado (se actualizaran los turnos a 'Atendido')
-- Consultas 6-7: URGENCIAS sin turno (id_turno = NULL, RN-01)

INSERT INTO consulta_medica (id_turno, id_mascota, id_veterinario, fecha, sintomas, diagnostico, estado) VALUES
-- Consulta 1: Atiende turno 1 (Toby - Vacunacion)
(1, 1, 1, '2026-05-18 09:15:00', 'Paciente asintomatico. Control de vacunacion anual al dia.', 'Paciente sano. Se aplica vacuna hexavalente.', 'Activa'),

-- Consulta 2: Atiende turno 3 (Luna - Vomitos y diarrea)
(3, 4, 1, '2026-05-18 10:10:00', 'Vomitos intermitentes desde hace 2 dias. Diarrea liquida. Inapetencia.', 'Gastroenteritis aguda. Posible ingesta de alimento en mal estado.', 'Activa'),

-- Consulta 3: Atiende turno 2 (Rex - Control postoperatorio)
(2, 3, 2, '2026-05-19 09:20:00', 'Control postoperatorio de esterilizacion. Herida quirurgica en buen estado.', 'Evolucion postquirurgica satisfactoria. Retirar puntos en 10 dias.', 'Activa'),

-- Consulta 4: Atiende turno 4 (Pelusa - Desparasitacion)
(4, 5, 3, '2026-05-19 14:15:00', 'Paciente con parasitos intestinales visibles en heces. Letargia leve.', 'Parasitosis intestinal. Se prescribe desparasitante.', 'Activa'),

-- Consulta 5: Atiende turno 6 (Buddy - Dificultad respiratoria)
(6, 8, 1, '2026-05-20 14:10:00', 'Disnea, tos seca y letargia desde hace 3 dias. Murmullo vesicular disminuido.', 'Neumonia bacteriana. Requiere antibioticos y antiinflamatorios.', 'Activa'),

-- Consulta 6: URGENCIA - Toby ingresa de urgencia por intoxicacion (sin turno)
(NULL, 1, 2, '2026-05-19 16:30:00', 'Ingesta sospechosa de rodenticida hace 2 horas. Salivacion excesiva, temblores.', 'Intoxicacion por rodenticida (sospecha). Tratamiento antitoxico inmediato.', 'Activa'),

-- Consulta 7: URGENCIA - Rex con herida (sin turno)
(NULL, 3, 3, '2026-05-20 11:00:00', 'Herida profunda en pata delantera izquierda por pelea con otro perro.', 'Herida por mordedura. Limpieza quirurgica y sutura. Profilaxis antirrabica.', 'Activa');

-- ============================================================================
-- Actualizar turnos atendidos a estado 'Atendido'
-- ============================================================================
-- Solo se actualizan los turnos que tienen consulta medica asociada.
-- El trigger trg_verificar_turno_atendido verifica que exista la consulta.

UPDATE turno SET estado = 'Atendido' WHERE id_turno = 1;
UPDATE turno SET estado = 'Atendido' WHERE id_turno = 2;
UPDATE turno SET estado = 'Atendido' WHERE id_turno = 3;
UPDATE turno SET estado = 'Atendido' WHERE id_turno = 4;
UPDATE turno SET estado = 'Atendido' WHERE id_turno = 6;

-- ============================================================================
-- Cancelar turno 9 (Pepe - Revision alas)
-- ============================================================================
-- Se utiliza el procedimiento almacenado para cancelar y liberar el slot.

CALL sp_cancelar_turno(9, 'Cancelado');

-- ============================================================================
-- 13. ITEMS RECETA (8 items de receta)
-- ============================================================================
-- Vinculan las consultas con lotes especificos de stock.
-- Los medicamentos se dispensan con logica FIFO (RN-10).

INSERT INTO item_receta (id_consulta, id_stock, cantidad, dosis, frecuencia, duracion, dispensado) VALUES
-- Receta Consulta 2 (Luna - Gastroenteritis): Enrofloxacina + Omeprazol
-- Enrofloxacina FIFO: LOT-2025-E002 (id_stock=6, vence 2026-12-18, primero en vencer)
(2, 6, 10, '1 comprimido', 'Cada 12 horas', '7 dias', 1),
-- Omeprazol (LOT-2026-O010, id_stock=9)
(2, 9, 7, '1 ml', 'Cada 24 horas', '7 dias', 1),

-- Receta Consulta 4 (Pelusa - Parasitosis): Ivermectina
-- Ivermectina FIFO: LOT-2025-I003 (id_stock=3, vence 2027-01-20, primero en vencer)
(4, 3, 1, '0.1 ml por kg', 'Dosis unica', NULL, 1),

-- Receta Consulta 5 (Buddy - Neumonia): Enrofloxacina + Meloxicam + Suero
-- Enrofloxacina (LOT-2025-E002, id_stock=6) - segundo descuento del mismo lote
(5, 6, 10, '1 comprimido', 'Cada 12 horas', '10 dias', 1),
-- Meloxicam (LOT-2026-M005, id_stock=5)
(5, 5, 1, '0.1 ml por kg', 'Cada 24 horas', '5 dias', 1),
-- Suero Fisiologico (LOT-2026-S013, id_stock=13)
(5, 13, 2, '500ml por via IV', 'Dosis unica', NULL, 1),

-- Receta Consulta 6 (Toby - Intoxicacion): Complejo B + Suero
-- Complejo B (LOT-2026-B007, id_stock=12)
(6, 12, 2, '1 ml por via IM', 'Cada 24 horas', '3 dias', 1),
-- Suero Fisiologico (LOT-2026-S013, id_stock=13)
(6, 13, 3, '500ml por via IV', 'Dosis unica', NULL, 1);

-- ============================================================================
-- VERIFICACION DE DATOS INSERTADOS
-- ============================================================================

SELECT 'veterinario' AS tabla, COUNT(*) AS registros FROM veterinario
UNION ALL
SELECT 'dueno', COUNT(*) FROM dueno
UNION ALL
SELECT 'especie', COUNT(*) FROM especie
UNION ALL
SELECT 'medicamento', COUNT(*) FROM medicamento
UNION ALL
SELECT 'raza', COUNT(*) FROM raza
UNION ALL
SELECT 'mascota', COUNT(*) FROM mascota
UNION ALL
SELECT 'agenda_disponibilidad', COUNT(*) FROM agenda_disponibilidad
UNION ALL
SELECT 'stock', COUNT(*) FROM stock
UNION ALL
SELECT 'slot_agenda', COUNT(*) FROM slot_agenda
UNION ALL
SELECT 'alerta_stock', COUNT(*) FROM alerta_stock
UNION ALL
SELECT 'turno', COUNT(*) FROM turno
UNION ALL
SELECT 'consulta_medica', COUNT(*) FROM consulta_medica
UNION ALL
SELECT 'item_receta', COUNT(*) FROM item_receta;

-- ============================================================================
-- FIN DEL SCRIPT DML - SIGVET
-- ============================================================================
