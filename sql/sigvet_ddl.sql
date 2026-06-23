-- ============================================================================
-- SIGVET - Sistema de Informacion para Gestion Veterinaria
-- Script DDL (Data Definition Language)
-- ============================================================================
-- Proyecto: AP2 - Trabajo Practico Numero 2
-- Alumno: Winck Joaquin Ezequiel
-- Carrera: Lic. en Informatica
-- Base de Datos: MySQL 8.0+ / InnoDB
-- Codificacion: utf8mb4
-- Fecha: Mayo 2026
-- ============================================================================
-- CONTENIDO:
--   1. Creacion de la base de datos
--   2. Tablas (orden por dependencia de FK)
--   3. Indices
--   4. Triggers (reglas de negocio)
--   5. Procedimientos almacenados
--   6. Funciones
-- ============================================================================

-- ============================================================================
-- 1. CREACION DE LA BASE DE DATOS
-- ============================================================================

DROP DATABASE IF EXISTS sigvet;

CREATE DATABASE sigvet
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE sigvet;

-- ============================================================================
-- 2. CREACION DE TABLAS (orden por nivel de dependencia FK)
-- ============================================================================
-- Nivel 0 (sin FK salientes): veterinario, dueno, especie, medicamento
-- Nivel 1 (FK -> Nivel 0): agenda_disponibilidad, raza, mascota, stock,
--                           alerta_stock
-- Nivel 2 (FK -> Nivel 0-1): slot_agenda, turno
-- Nivel 3 (FK -> Nivel 0-2): consulta_medica
-- Nivel 4 (FK -> Nivel 3): item_receta
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 2.1 TABLA: veterinario (Nivel 0)
-- Descripcion: Profesionales veterinarios que atienden en la clinica.
-- ----------------------------------------------------------------------------

CREATE TABLE veterinario (
    id_veterinario   INT              NOT NULL AUTO_INCREMENT,
    nombre           VARCHAR(100)     NOT NULL,
    apellido         VARCHAR(100)     NOT NULL,
    matricula        VARCHAR(30)      NOT NULL,
    telefono         VARCHAR(20)      NOT NULL,
    email            VARCHAR(150)     NULL,
    estado           ENUM('Activo','Inactivo') NOT NULL DEFAULT 'Activo',

    -- Restricciones de columna
    CONSTRAINT pk_veterinario   PRIMARY KEY (id_veterinario),
    CONSTRAINT uk_matricula     UNIQUE (matricula),

    -- Checks de integridad
    CONSTRAINT chk_vet_nombre      CHECK (LENGTH(TRIM(nombre)) > 0),
    CONSTRAINT chk_vet_apellido    CHECK (LENGTH(TRIM(apellido)) > 0),
    CONSTRAINT chk_vet_matricula   CHECK (LENGTH(TRIM(matricula)) > 0),
    CONSTRAINT chk_vet_telefono    CHECK (LENGTH(TRIM(telefono)) > 0),
    CONSTRAINT chk_vet_email       CHECK (email IS NULL OR email REGEXP '^[A-Za-z0-9._%-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Indice para busqueda por apellido/nombre
CREATE INDEX idx_vet_apellido ON veterinario (apellido, nombre);


-- ----------------------------------------------------------------------------
-- 2.2 TABLA: dueno (Nivel 0)
-- Descripcion: Dueños de mascotas. Datos amparados por Ley 25.326 (RN-13).
-- ----------------------------------------------------------------------------

CREATE TABLE dueno (
    id_dueno      INT              NOT NULL AUTO_INCREMENT,
    dni           VARCHAR(20)      NOT NULL,
    nombre        VARCHAR(100)     NOT NULL,
    apellido      VARCHAR(100)     NOT NULL,
    telefono      VARCHAR(20)      NOT NULL,
    direccion     VARCHAR(200)     NULL,
    email         VARCHAR(150)     NULL,
    estado        ENUM('Activo','Inactivo') NOT NULL DEFAULT 'Activo',

    -- Restricciones de columna
    CONSTRAINT pk_dueno      PRIMARY KEY (id_dueno),
    CONSTRAINT uk_dni        UNIQUE (dni),

    -- Checks de integridad
    CONSTRAINT chk_dueno_nombre    CHECK (LENGTH(TRIM(nombre)) > 0),
    CONSTRAINT chk_dueno_apellido  CHECK (LENGTH(TRIM(apellido)) > 0),
    CONSTRAINT chk_dueno_dni       CHECK (LENGTH(TRIM(dni)) > 0),
    CONSTRAINT chk_dueno_telefono  CHECK (LENGTH(TRIM(telefono)) > 0),
    CONSTRAINT chk_dueno_email     CHECK (email IS NULL OR email REGEXP '^[A-Za-z0-9._%-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Indice para busqueda por apellido y nombre
CREATE INDEX idx_dueno_apellido ON dueno (apellido, nombre);


-- ----------------------------------------------------------------------------
-- 2.3 TABLA: especie (Nivel 0)
-- Descripcion: Catalogo predefinido de especies veterinarias.
-- ----------------------------------------------------------------------------

CREATE TABLE especie (
    id_especie    INT           NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(50)   NOT NULL,

    -- Restricciones de columna
    CONSTRAINT pk_especie           PRIMARY KEY (id_especie),
    CONSTRAINT uk_nombre_especie    UNIQUE (nombre),

    -- Checks de integridad
    CONSTRAINT chk_especie_nombre   CHECK (LENGTH(TRIM(nombre)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- ----------------------------------------------------------------------------
-- 2.4 TABLA: medicamento (Nivel 0)
-- Descripcion: Catalogo maestro de medicamentos (Inventario).
-- Diferenciado de Stock segun RN-02.
-- ----------------------------------------------------------------------------

CREATE TABLE medicamento (
    id_medicamento             INT             NOT NULL AUTO_INCREMENT,
    nombre_generico            VARCHAR(150)    NOT NULL,
    nombre_comercial           VARCHAR(150)    NOT NULL,
    dosis_presentacion         VARCHAR(100)    NOT NULL,
    precio_venta               DECIMAL(10,2)  NOT NULL,
    stock_minimo_alerta        INT             NOT NULL DEFAULT 5,
    estado                     ENUM('Activo','Inactivo') NOT NULL DEFAULT 'Activo',
    fecha_actualizacion_precio DATE            NULL,

    -- Restricciones de columna
    CONSTRAINT pk_medicamento      PRIMARY KEY (id_medicamento),
    CONSTRAINT uk_med_dosis        UNIQUE (nombre_comercial, dosis_presentacion),

    -- Checks de integridad
    CONSTRAINT chk_med_generico    CHECK (LENGTH(TRIM(nombre_generico)) > 0),
    CONSTRAINT chk_med_comercial   CHECK (LENGTH(TRIM(nombre_comercial)) > 0),
    CONSTRAINT chk_med_dosis       CHECK (LENGTH(TRIM(dosis_presentacion)) > 0),
    CONSTRAINT chk_med_precio      CHECK (precio_venta > 0),
    CONSTRAINT chk_med_stock_min   CHECK (stock_minimo_alerta >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Indice para busqueda por nombre generico
CREATE INDEX idx_med_generico ON medicamento (nombre_generico);


-- ----------------------------------------------------------------------------
-- 2.5 TABLA: raza (Nivel 1 -> especie)
-- Descripcion: Catalogo de razas agrupadas por especie.
-- ----------------------------------------------------------------------------

CREATE TABLE raza (
    id_raza       INT           NOT NULL AUTO_INCREMENT,
    id_especie    INT           NOT NULL,
    nombre        VARCHAR(50)   NOT NULL,

    -- Restricciones de columna
    CONSTRAINT pk_raza              PRIMARY KEY (id_raza),
    CONSTRAINT uk_especie_raza      UNIQUE (id_especie, nombre),

    -- Foreign Keys
    CONSTRAINT fk_raza_especie      FOREIGN KEY (id_especie)
        REFERENCES especie (id_especie)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    -- Checks de integridad
    CONSTRAINT chk_raza_nombre      CHECK (LENGTH(TRIM(nombre)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Indice para filtrar razas por especie
CREATE INDEX idx_raza_especie ON raza (id_especie);


-- ----------------------------------------------------------------------------
-- 2.6 TABLA: mascota (Nivel 1 -> dueno, especie, raza)
-- Descripcion: Pacientes veterinarios. Trazabilidad clinica central.
-- ----------------------------------------------------------------------------

CREATE TABLE mascota (
    id_mascota         INT           NOT NULL AUTO_INCREMENT,
    id_dueno           INT           NOT NULL,
    nombre             VARCHAR(100)  NOT NULL,
    id_especie         INT           NOT NULL,
    id_raza            INT           NOT NULL,
    fecha_nacimiento   DATE          NULL,
    sexo               ENUM('M','F') NULL,
    color              VARCHAR(50)   NULL,
    senas_particulares VARCHAR(300)  NULL,
    estado             ENUM('Activo','Inactivo') NOT NULL DEFAULT 'Activo',

    -- Restricciones de columna
    CONSTRAINT pk_mascota        PRIMARY KEY (id_mascota),

    -- Foreign Keys
    CONSTRAINT fk_mascota_dueno  FOREIGN KEY (id_dueno)
        REFERENCES dueno (id_dueno)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_mascota_especie FOREIGN KEY (id_especie)
        REFERENCES especie (id_especie)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_mascota_raza   FOREIGN KEY (id_raza)
        REFERENCES raza (id_raza)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    -- Checks de integridad
    CONSTRAINT chk_mascota_nombre CHECK (LENGTH(TRIM(nombre)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Indices para consultas frecuentes
CREATE INDEX idx_mascota_dueno   ON mascota (id_dueno);
CREATE INDEX idx_mascota_nombre  ON mascota (nombre);
CREATE INDEX idx_mascota_especie ON mascota (id_especie);


-- ----------------------------------------------------------------------------
-- 2.7 TABLA: agenda_disponibilidad (Nivel 1 -> veterinario)
-- Descripcion: Franjas horarias de disponibilidad por veterinario.
-- RN-11: No se permiten franjas superpuestas.
-- ----------------------------------------------------------------------------

CREATE TABLE agenda_disponibilidad (
    id_agenda        INT           NOT NULL AUTO_INCREMENT,
    id_veterinario   INT           NOT NULL,
    dia_semana       ENUM('Lunes','Martes','Miercoles','Jueves','Viernes','Sabado','Domingo') NOT NULL,
    hora_inicio      TIME          NOT NULL,
    hora_fin         TIME          NOT NULL,

    -- Restricciones de columna
    CONSTRAINT pk_agenda    PRIMARY KEY (id_agenda),

    -- Foreign Keys
    CONSTRAINT fk_agenda_vet FOREIGN KEY (id_veterinario)
        REFERENCES veterinario (id_veterinario)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    -- Checks de integridad
    CONSTRAINT chk_agenda_horario CHECK (hora_inicio < hora_fin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Indice para busqueda por veterinario
CREATE INDEX idx_agenda_vet ON agenda_disponibilidad (id_veterinario);


-- ----------------------------------------------------------------------------
-- 2.8 TABLA: stock (Nivel 1 -> medicamento)
-- Descripcion: Unidades fisicas de medicamento por lote y vencimiento.
-- Diferenciado de Inventario segun RN-02.
-- ----------------------------------------------------------------------------

CREATE TABLE stock (
    id_stock            INT           NOT NULL AUTO_INCREMENT,
    id_medicamento      INT           NOT NULL,
    cantidad            INT           NOT NULL DEFAULT 0,
    numero_lote         VARCHAR(50)   NOT NULL,
    fecha_vencimiento   DATE          NOT NULL,
    fecha_ingreso       DATE          NOT NULL DEFAULT (CURDATE()),

    -- Restricciones de columna
    CONSTRAINT pk_stock          PRIMARY KEY (id_stock),
    CONSTRAINT uk_med_lote       UNIQUE (id_medicamento, numero_lote),

    -- Foreign Keys
    CONSTRAINT fk_stock_med      FOREIGN KEY (id_medicamento)
        REFERENCES medicamento (id_medicamento)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    -- Checks de integridad
    CONSTRAINT chk_stock_cantidad     CHECK (cantidad >= 0),
    CONSTRAINT chk_stock_lote         CHECK (LENGTH(TRIM(numero_lote)) > 0)
    -- Nota: La validacion fecha_vencimiento > CURDATE() no se puede implementar
    -- como CHECK constraint porque MySQL no permite funciones no deterministas
    -- como CURDATE() en CHECK. Se implementa mediante el trigger
    -- trg_validar_vencimiento_stock (ver seccion 3.5).
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Indices para consultas frecuentes
CREATE INDEX idx_stock_medicamento  ON stock (id_medicamento);
CREATE INDEX idx_stock_vencimiento  ON stock (fecha_vencimiento);


-- ----------------------------------------------------------------------------
-- 2.9 TABLA: alerta_stock (Nivel 1 -> medicamento)
-- Descripcion: Alertas automaticas de stock bajo y vencimiento proximo.
-- RN-06: Generacion y resolucion automatica de STOCK_BAJO.
-- ----------------------------------------------------------------------------

CREATE TABLE alerta_stock (
    id_alerta         INT           NOT NULL AUTO_INCREMENT,
    id_medicamento    INT           NOT NULL,
    tipo              ENUM('STOCK_BAJO','VENCIMIENTO_PROXIMO') NOT NULL,
    mensaje           VARCHAR(300)  NOT NULL,
    estado            ENUM('Pendiente','En Gestion','Resuelta') NOT NULL DEFAULT 'Pendiente',
    fecha_generacion  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion  DATETIME      NULL,

    -- Restricciones de columna
    CONSTRAINT pk_alerta         PRIMARY KEY (id_alerta),

    -- Foreign Keys
    CONSTRAINT fk_alerta_med     FOREIGN KEY (id_medicamento)
        REFERENCES medicamento (id_medicamento)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    -- Checks de integridad
    CONSTRAINT chk_alerta_mensaje CHECK (LENGTH(TRIM(mensaje)) > 0),
    CONSTRAINT chk_alerta_fechas  CHECK (fecha_resolucion IS NULL OR fecha_resolucion >= fecha_generacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Indices para consultas frecuentes
CREATE INDEX idx_alerta_medicamento ON alerta_stock (id_medicamento);
CREATE INDEX idx_alerta_estado      ON alerta_stock (estado);
CREATE INDEX idx_alerta_tipo        ON alerta_stock (tipo);


-- ----------------------------------------------------------------------------
-- 2.10 TABLA: slot_agenda (Nivel 2 -> agenda_disponibilidad)
-- Descripcion: Espacio individual de la agenda (30 min).
-- Unidad atomica para la reserva de turnos.
-- ----------------------------------------------------------------------------

CREATE TABLE slot_agenda (
    id_slot     INT           NOT NULL AUTO_INCREMENT,
    id_agenda   INT           NOT NULL,
    fecha       DATE          NOT NULL,
    hora        TIME          NOT NULL,
    estado      ENUM('Disponible','Reservado') NOT NULL DEFAULT 'Disponible',

    -- Restricciones de columna
    CONSTRAINT pk_slot              PRIMARY KEY (id_slot),
    CONSTRAINT uk_slot_fecha_hora   UNIQUE (id_agenda, fecha, hora),

    -- Foreign Keys
    CONSTRAINT fk_slot_agenda       FOREIGN KEY (id_agenda)
        REFERENCES agenda_disponibilidad (id_agenda)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Indice para consultas de agenda del dia
CREATE INDEX idx_slot_fecha ON slot_agenda (fecha);


-- ----------------------------------------------------------------------------
-- 2.11 TABLA: turno (Nivel 2 -> mascota, slot_agenda)
-- Descripcion: Reserva de espacio temporal para atencion de mascota.
-- RN-01: Diferenciado de Consulta Medica (planificacion vs acto asistencial).
-- RN-04: id_mascota NOT NULL (turno requiere mascota registrada).
-- RN-05: Bloqueo concurrente (SELECT FOR UPDATE sobre slot).
-- RN-08: Un slot admite maximo un turno activo (verificado por trigger).
-- ----------------------------------------------------------------------------

CREATE TABLE turno (
    id_turno         INT           NOT NULL AUTO_INCREMENT,
    id_mascota       INT           NOT NULL,
    id_slot          INT           NOT NULL,
    motivo           VARCHAR(300)  NULL,
    estado           ENUM('Pendiente','Atendido','Cancelado','Inasistencia') NOT NULL DEFAULT 'Pendiente',
    fecha_registro   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Restricciones de columna
    CONSTRAINT pk_turno    PRIMARY KEY (id_turno),

    -- Foreign Keys
    CONSTRAINT fk_turno_mascota FOREIGN KEY (id_mascota)
        REFERENCES mascota (id_mascota)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_turno_slot    FOREIGN KEY (id_slot)
        REFERENCES slot_agenda (id_slot)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Indices para consultas frecuentes
CREATE INDEX idx_turno_slot     ON turno (id_slot);
CREATE INDEX idx_turno_mascota  ON turno (id_mascota);
CREATE INDEX idx_turno_estado   ON turno (estado);


-- ----------------------------------------------------------------------------
-- 2.12 TABLA: consulta_medica (Nivel 3 -> turno, mascota, veterinario)
-- Descripcion: Acto asistencial documentado por el veterinario.
-- Entidad central de la historia clinica veterinaria.
-- RN-01: id_turno NULLABLE (urgencias sin turno previo).
-- RN-07: Baja logica obligatoria (trigger impide eliminacion fisica).
-- RN-12: Solo pasa a 'Atendido' cuando existe consulta asociada.
-- ----------------------------------------------------------------------------

CREATE TABLE consulta_medica (
    id_consulta            INT           NOT NULL AUTO_INCREMENT,
    id_turno               INT           NULL,
    id_mascota             INT           NOT NULL,
    id_veterinario         INT           NOT NULL,
    fecha                  DATETIME      NOT NULL,
    sintomas               TEXT          NOT NULL,
    diagnostico            TEXT          NOT NULL,
    estado                 ENUM('Activa','Inactiva') NOT NULL DEFAULT 'Activa',
    fecha_modificacion     DATETIME      NULL,
    id_veterinario_modif   INT           NULL,

    -- Restricciones de columna
    CONSTRAINT pk_consulta         PRIMARY KEY (id_consulta),

    -- Foreign Keys
    CONSTRAINT fk_consulta_turno   FOREIGN KEY (id_turno)
        REFERENCES turno (id_turno)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT fk_consulta_mascota FOREIGN KEY (id_mascota)
        REFERENCES mascota (id_mascota)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_consulta_vet     FOREIGN KEY (id_veterinario)
        REFERENCES veterinario (id_veterinario)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_consulta_vet_mod FOREIGN KEY (id_veterinario_modif)
        REFERENCES veterinario (id_veterinario)
        ON DELETE SET NULL ON UPDATE CASCADE,

    -- Checks de integridad
    CONSTRAINT chk_consulta_sintomas  CHECK (LENGTH(TRIM(sintomas)) > 0),
    CONSTRAINT chk_consulta_diag      CHECK (LENGTH(TRIM(diagnostico)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Indices para consultas frecuentes
CREATE INDEX idx_consulta_turno   ON consulta_medica (id_turno);
CREATE INDEX idx_consulta_mascota ON consulta_medica (id_mascota);
CREATE INDEX idx_consulta_vet     ON consulta_medica (id_veterinario);
CREATE INDEX idx_consulta_fecha   ON consulta_medica (fecha);


-- ----------------------------------------------------------------------------
-- 2.13 TABLA: item_receta (Nivel 4 -> consulta_medica, stock)
-- Descripcion: Linea de receta medica. Vincula consulta con lote especifico.
-- Garantiza trazabilidad completa: medicamento-lote-mascota-consulta.
-- ----------------------------------------------------------------------------

CREATE TABLE item_receta (
    id_item_receta   INT           NOT NULL AUTO_INCREMENT,
    id_consulta      INT           NOT NULL,
    id_stock         INT           NOT NULL,
    cantidad          INT           NOT NULL,
    dosis             VARCHAR(100)  NOT NULL,
    frecuencia        VARCHAR(100)  NULL,
    duracion          VARCHAR(100)  NULL,
    dispensado        TINYINT       NOT NULL DEFAULT 1,

    -- Restricciones de columna
    CONSTRAINT pk_item_receta    PRIMARY KEY (id_item_receta),

    -- Foreign Keys
    CONSTRAINT fk_item_consulta  FOREIGN KEY (id_consulta)
        REFERENCES consulta_medica (id_consulta)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_item_stock     FOREIGN KEY (id_stock)
        REFERENCES stock (id_stock)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    -- Checks de integridad
    CONSTRAINT chk_item_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_item_dosis    CHECK (LENGTH(TRIM(dosis)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Indices para consultas frecuentes
CREATE INDEX idx_item_consulta ON item_receta (id_consulta);
CREATE INDEX idx_item_stock    ON item_receta (id_stock);


-- ============================================================================
-- 3. TRIGGERS - Implementacion de Reglas de Negocio
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 3.1 TRIGGER: trg_prevenir_eliminar_consulta (RN-07)
-- Impide la eliminacion fisica de consultas medicas.
-- Solo se permite baja logica (cambiar estado a 'Inactiva').
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE TRIGGER trg_prevenir_eliminar_consulta
BEFORE DELETE ON consulta_medica
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'RN-07: No se permite la eliminacion fisica de consultas medicas. Use baja logica (estado = Inactiva).';
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 3.2 TRIGGER: trg_verificar_slot_disponible (RN-08)
-- Verifica que un slot este 'Disponible' antes de reservar un turno.
-- Se ejecuta antes de INSERT en turno.
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE TRIGGER trg_verificar_slot_disponible
BEFORE INSERT ON turno
FOR EACH ROW
BEGIN
    DECLARE v_estado_slot VARCHAR(20);
    DECLARE v_turno_activo INT;

    -- Verificar que el slot este Disponible
    SELECT estado INTO v_estado_slot
    FROM slot_agenda
    WHERE id_slot = NEW.id_slot;

    IF v_estado_slot IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-08: El slot especificado no existe.';
    END IF;

    IF v_estado_slot != 'Disponible' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-08: El slot ya se encuentra reservado. No se puede asignar otro turno.';
    END IF;

    -- Verificar que no exista otro turno activo para el mismo slot
    SELECT COUNT(*) INTO v_turno_activo
    FROM turno
    WHERE id_slot = NEW.id_slot
      AND estado IN ('Pendiente', 'Atendido');

    IF v_turno_activo > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-08: Ya existe un turno activo para este slot. Un slot admite maximo un turno activo.';
    END IF;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 3.3 TRIGGER: trg_actualizar_slot_al_reservar (RN-08)
-- Cambia el estado del slot a 'Reservado' al insertar un turno.
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE TRIGGER trg_actualizar_slot_al_reservar
AFTER INSERT ON turno
FOR EACH ROW
BEGIN
    UPDATE slot_agenda
    SET estado = 'Reservado'
    WHERE id_slot = NEW.id_slot;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 3.4 TRIGGER: trg_liberar_slot_al_cancelar (RN-08)
-- Libera el slot cuando un turno se cancela o marca inasistencia.
-- Solo libera si no hay otro turno activo para el mismo slot.
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE TRIGGER trg_liberar_slot_al_cancelar
AFTER UPDATE ON turno
FOR EACH ROW
BEGIN
    -- Si el turno pasa a estado cancelado o inasistencia
    IF NEW.estado IN ('Cancelado', 'Inasistencia')
       AND OLD.estado NOT IN ('Cancelado', 'Inasistencia') THEN

        -- Verificar si no quedan turnos activos para este slot
        IF NOT EXISTS (
            SELECT 1 FROM turno
            WHERE id_slot = NEW.id_slot
              AND estado IN ('Pendiente', 'Atendido')
              AND id_turno != NEW.id_turno
        ) THEN
            UPDATE slot_agenda
            SET estado = 'Disponible'
            WHERE id_slot = NEW.id_slot;
        END IF;
    END IF;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 3.5 TRIGGER: trg_validar_vencimiento_stock (RN-09)
-- Verifica que la fecha de vencimiento sea posterior a la fecha actual
-- al registrar un nuevo lote de stock.
-- Nota: Esta validacion NO se puede implementar como CHECK constraint
-- porque MySQL no permite funciones no deterministas (CURDATE) en CHECK.
-- El trigger es el unico mecanismo para validar fecha > CURDATE().
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE TRIGGER trg_validar_vencimiento_stock
BEFORE INSERT ON stock
FOR EACH ROW
BEGIN
    IF NEW.fecha_vencimiento <= CURDATE() THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-09: La fecha de vencimiento del lote debe ser posterior a la fecha actual.';
    END IF;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 3.6 TRIGGER: trg_validar_vencimiento_stock_update (RN-09)
-- Verifica que la fecha de vencimiento sea posterior a la fecha actual
-- al actualizar un registro de stock.
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE TRIGGER trg_validar_vencimiento_stock_update
BEFORE UPDATE ON stock
FOR EACH ROW
BEGIN
    IF NEW.fecha_vencimiento <= CURDATE() THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-09: La fecha de vencimiento del lote debe ser posterior a la fecha actual.';
    END IF;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 3.7 TRIGGER: trg_validar_franjas_no_superpuestas (RN-11)
-- Impide que se superpongan franjas de disponibilidad para el mismo
-- veterinario y dia de la semana.
-- Se ejecuta antes de INSERT en agenda_disponibilidad.
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE TRIGGER trg_validar_franjas_no_superpuestas
BEFORE INSERT ON agenda_disponibilidad
FOR EACH ROW
BEGIN
    DECLARE v_superpuestas INT;

    SELECT COUNT(*) INTO v_superpuestas
    FROM agenda_disponibilidad
    WHERE id_veterinario = NEW.id_veterinario
      AND dia_semana = NEW.dia_semana
      AND (hora_inicio < NEW.hora_fin AND hora_fin > NEW.hora_inicio);

    IF v_superpuestas > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-11: Las franjas de disponibilidad no pueden superponerse para el mismo veterinario y dia.';
    END IF;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 3.8 TRIGGER: trg_validar_franjas_no_superpuestas_update (RN-11)
-- Igual que el anterior pero para UPDATE.
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE TRIGGER trg_validar_franjas_no_superpuestas_update
BEFORE UPDATE ON agenda_disponibilidad
FOR EACH ROW
BEGIN
    DECLARE v_superpuestas INT;

    SELECT COUNT(*) INTO v_superpuestas
    FROM agenda_disponibilidad
    WHERE id_veterinario = NEW.id_veterinario
      AND dia_semana = NEW.dia_semana
      AND id_agenda != NEW.id_agenda
      AND (hora_inicio < NEW.hora_fin AND hora_fin > NEW.hora_inicio);

    IF v_superpuestas > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RN-11: Las franjas de disponibilidad no pueden superponerse para el mismo veterinario y dia.';
    END IF;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 3.9 TRIGGER: trg_verificar_turno_atendido (RN-12)
-- Impide que un turno pase a 'Atendido' si no existe consulta medica
-- asociada a ese turno.
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE TRIGGER trg_verificar_turno_atendido
BEFORE UPDATE ON turno
FOR EACH ROW
BEGIN
    DECLARE v_consulta_existe INT;

    IF NEW.estado = 'Atendido' AND OLD.estado != 'Atendido' THEN
        SELECT COUNT(*) INTO v_consulta_existe
        FROM consulta_medica
        WHERE id_turno = NEW.id_turno
          AND estado = 'Activa';

        IF v_consulta_existe = 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'RN-12: No se puede marcar el turno como Atendido sin una consulta medica activa asociada.';
        END IF;
    END IF;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 3.10 TRIGGER: trg_alerta_stock_bajo (RN-06)
-- Genera alerta automatica cuando el stock total de un medicamento
-- cae por debajo del umbral minimo.
-- Se ejecuta despues de cualquier cambio en la tabla stock.
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE TRIGGER trg_alerta_stock_bajo_insert
AFTER INSERT ON stock
FOR EACH ROW
BEGIN
    CALL sp_verificar_stock_bajo(NEW.id_medicamento);
END //

DELIMITER ;

DELIMITER //

CREATE TRIGGER trg_alerta_stock_bajo_update
AFTER UPDATE ON stock
FOR EACH ROW
BEGIN
    CALL sp_verificar_stock_bajo(NEW.id_medicamento);
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 3.11 TRIGGER: trg_alerta_vencimiento_proximo (RN-06)
-- Genera alerta automatica cuando se registra un lote con vencimiento
-- dentro de los proximos 30 dias.
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE TRIGGER trg_alerta_vencimiento_insert
AFTER INSERT ON stock
FOR EACH ROW
BEGIN
    IF NEW.fecha_vencimiento <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN
        CALL sp_generar_alerta_vencimiento(NEW.id_stock);
    END IF;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 3.12 TRIGGER: trg_registro_modificacion_consulta
-- Registra automaticamente la fecha de modificacion de una consulta.
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE TRIGGER trg_registro_modificacion_consulta
BEFORE UPDATE ON consulta_medica
FOR EACH ROW
BEGIN
    -- Solo registrar modificacion si cambiaron campos clinicos
    IF (NEW.sintomas != OLD.sintomas
        OR NEW.diagnostico != OLD.diagnostico
        OR NEW.estado != OLD.estado) THEN
        SET NEW.fecha_modificacion = NOW();
    END IF;
END //

DELIMITER ;


-- ============================================================================
-- 4. PROCEDIMIENTOS ALMACENADOS
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 4.1 SP: sp_verificar_stock_bajo (RN-06)
-- Verifica si el stock total de un medicamento esta por debajo del umbral.
-- Si es asi, genera una alerta STOCK_BAJO si no existe una pendiente.
-- Si el stock fue repuesto por encima del umbral, resuelve alertas pendientes.
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE PROCEDURE sp_verificar_stock_bajo(
    IN p_id_medicamento INT
)
BEGIN
    DECLARE v_stock_total INT;
    DECLARE v_stock_minimo INT;
    DECLARE v_nombre_comercial VARCHAR(150);
    DECLARE v_alerta_pendiente INT;

    -- Calcular stock total del medicamento
    SELECT COALESCE(SUM(s.cantidad), 0), m.stock_minimo_alerta, m.nombre_comercial
    INTO v_stock_total, v_stock_minimo, v_nombre_comercial
    FROM medicamento m
    LEFT JOIN stock s ON s.id_medicamento = m.id_medicamento
    WHERE m.id_medicamento = p_id_medicamento
    GROUP BY m.id_medicamento;

    -- Verificar si existe alerta STOCK_BAJO pendiente para este medicamento
    SELECT COUNT(*) INTO v_alerta_pendiente
    FROM alerta_stock
    WHERE id_medicamento = p_id_medicamento
      AND tipo = 'STOCK_BAJO'
      AND estado IN ('Pendiente', 'En Gestion');

    IF v_stock_total < v_stock_minimo THEN
        -- Stock bajo: generar alerta si no existe pendiente
        IF v_alerta_pendiente = 0 THEN
            INSERT INTO alerta_stock (id_medicamento, tipo, mensaje, estado)
            VALUES (
                p_id_medicamento,
                'STOCK_BAJO',
                CONCAT('Stock bajo de ', v_nombre_comercial, ': ', v_stock_total, ' unidades (minimo: ', v_stock_minimo, ')'),
                'Pendiente'
            );
        END IF;
    ELSE
        -- Stock repuesto: resolver alertas pendientes automaticamente
        IF v_alerta_pendiente > 0 THEN
            UPDATE alerta_stock
            SET estado = 'Resuelta',
                fecha_resolucion = NOW()
            WHERE id_medicamento = p_id_medicamento
              AND tipo = 'STOCK_BAJO'
              AND estado IN ('Pendiente', 'En Gestion');
        END IF;
    END IF;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 4.2 SP: sp_generar_alerta_vencimiento (RN-06)
-- Genera alerta VENCIMIENTO_PROXIMO para un lote de stock especifico.
-- Verifica que no exista una alerta pendiente para el mismo lote.
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE PROCEDURE sp_generar_alerta_vencimiento(
    IN p_id_stock INT
)
BEGIN
    DECLARE v_id_medicamento INT;
    DECLARE v_numero_lote VARCHAR(50);
    DECLARE v_fecha_vencimiento DATE;
    DECLARE v_nombre_comercial VARCHAR(150);
    DECLARE v_alerta_existente INT;

    -- Obtener datos del lote
    SELECT s.id_medicamento, s.numero_lote, s.fecha_vencimiento, m.nombre_comercial
    INTO v_id_medicamento, v_numero_lote, v_fecha_vencimiento, v_nombre_comercial
    FROM stock s
    JOIN medicamento m ON m.id_medicamento = s.id_medicamento
    WHERE s.id_stock = p_id_stock;

    -- Verificar si ya existe alerta pendiente para este medicamento y lote
    SELECT COUNT(*) INTO v_alerta_existente
    FROM alerta_stock
    WHERE id_medicamento = v_id_medicamento
      AND tipo = 'VENCIMIENTO_PROXIMO'
      AND mensaje LIKE CONCAT('%', v_numero_lote, '%')
      AND estado IN ('Pendiente', 'En Gestion');

    IF v_alerta_existente = 0 THEN
        INSERT INTO alerta_stock (id_medicamento, tipo, mensaje, estado)
        VALUES (
            v_id_medicamento,
            'VENCIMIENTO_PROXIMO',
            CONCAT('Lote ', v_numero_lote, ' de ', v_nombre_comercial, ' vence el ', DATE_FORMAT(v_fecha_vencimiento, '%d/%m/%Y'), ' (dentro de ', DATEDIFF(v_fecha_vencimiento, CURDATE()), ' dias)'),
            'Pendiente'
        );
    END IF;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 4.3 SP: sp_reservar_turno (RN-05, RN-08)
-- Reserva un turno de forma transaccional con bloqueo pesimista.
-- Evita condiciones de carrera en la reserva de slots.
-- Parametros:
--   p_id_mascota: ID de la mascota
--   p_id_slot: ID del slot a reservar
--   p_motivo: Motivo de la consulta (opcional, puede ser NULL)
-- Devuelve: ID del turno creado
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE PROCEDURE sp_reservar_turno(
    IN p_id_mascota INT,
    IN p_id_slot INT,
    IN p_motivo VARCHAR(300),
    OUT p_id_turno INT
)
BEGIN
    DECLARE v_estado_slot VARCHAR(20);
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- Bloqueo pesimista: bloquear el slot para lectura (RN-05)
    SELECT estado INTO v_estado_slot
    FROM slot_agenda
    WHERE id_slot = p_id_slot
    FOR UPDATE;

    -- Verificar que el slot este disponible
    IF v_estado_slot IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El slot especificado no existe.';
    END IF;

    IF v_estado_slot != 'Disponible' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El slot no esta disponible. Ya se encuentra reservado.';
    END IF;

    -- Verificar que no exista otro turno activo para el mismo slot
    IF EXISTS (
        SELECT 1 FROM turno
        WHERE id_slot = p_id_slot
          AND estado IN ('Pendiente', 'Atendido')
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ya existe un turno activo para este slot.';
    END IF;

    -- Insertar el turno
    INSERT INTO turno (id_mascota, id_slot, motivo, estado)
    VALUES (p_id_mascota, p_id_slot, p_motivo, 'Pendiente');

    SET p_id_turno = LAST_INSERT_ID();

    -- Actualizar estado del slot a Reservado
    UPDATE slot_agenda
    SET estado = 'Reservado'
    WHERE id_slot = p_id_slot;

    COMMIT;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 4.4 SP: sp_cancelar_turno
-- Cancela un turno y libera el slot asociado.
-- Parametros:
--   p_id_turno: ID del turno a cancelar
--   p_estado_nuevo: 'Cancelado' o 'Inasistencia'
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE PROCEDURE sp_cancelar_turno(
    IN p_id_turno INT,
    IN p_estado_nuevo VARCHAR(20)
)
BEGIN
    DECLARE v_id_slot INT;
    DECLARE v_estado_actual VARCHAR(20);

    -- Validar estado nuevo
    IF p_estado_nuevo NOT IN ('Cancelado', 'Inasistencia') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Estado invalido. Solo se permite Cancelado o Inasistencia.';
    END IF;

    -- Obtener datos del turno
    SELECT id_slot, estado INTO v_id_slot, v_estado_actual
    FROM turno
    WHERE id_turno = p_id_turno;

    IF v_estado_actual IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El turno especificado no existe.';
    END IF;

    IF v_estado_actual IN ('Cancelado', 'Inasistencia') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El turno ya fue cancelado o marcado como inasistencia.';
    END IF;

    -- Actualizar estado del turno
    UPDATE turno
    SET estado = p_estado_nuevo
    WHERE id_turno = p_id_turno;

    -- Verificar si no quedan turnos activos para este slot
    IF NOT EXISTS (
        SELECT 1 FROM turno
        WHERE id_slot = v_id_slot
          AND estado IN ('Pendiente', 'Atendido')
    ) THEN
        UPDATE slot_agenda
        SET estado = 'Disponible'
        WHERE id_slot = v_id_slot;
    END IF;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 4.5 SP: sp_descontar_stock_fifo (RN-02, RN-10)
-- Descuenta stock de medicamentos usando logica FIFO por fecha de
-- vencimiento (primero en vencer, primero en salir).
-- Operacion transaccional: si no hay stock suficiente, se revierte todo.
-- Parametros:
--   p_id_medicamento: ID del medicamento a dispensar
--   p_cantidad: Cantidad total a descontar
-- Devuelve: Tabla temporal con los lotes afectados y cantidades descontadas
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE PROCEDURE sp_descontar_stock_fifo(
    IN p_id_medicamento INT,
    IN p_cantidad INT
)
BEGIN
    DECLARE v_stock_disponible INT;
    DECLARE v_restante INT;
    DECLARE v_id_stock_act INT;
    DECLARE v_cantidad_lote INT;
    DECLARE v_a_descontar INT;
    DECLARE v_fin INT DEFAULT 0;
    DECLARE v_msg_error VARCHAR(300);
    DECLARE v_lotes_cursor CURSOR FOR
        SELECT id_stock, cantidad
        FROM stock
        WHERE id_medicamento = p_id_medicamento
          AND cantidad > 0
          AND fecha_vencimiento > CURDATE()
        ORDER BY fecha_vencimiento ASC;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_fin = 1;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    -- Validar cantidad positiva
    IF p_cantidad <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La cantidad a descontar debe ser mayor a 0.';
    END IF;

    -- Verificar stock total disponible
    SELECT COALESCE(SUM(cantidad), 0)
    INTO v_stock_disponible
    FROM stock
    WHERE id_medicamento = p_id_medicamento
      AND cantidad > 0
      AND fecha_vencimiento > CURDATE();

    IF v_stock_disponible < p_cantidad THEN
        SET v_msg_error = CONCAT(
            'Stock insuficiente. Disponible: ', v_stock_disponible,
            ' - Solicitado: ', p_cantidad
        );
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = v_msg_error;
    END IF;

    SET v_restante = p_cantidad;

    START TRANSACTION;

    -- Bloquear lotes afectados (bloqueo pesimista)
    -- Se usa SELECT FOR UPDATE en el cursor implicitamente via UPDATE

    OPEN v_lotes_cursor;

    bucle_lotes: LOOP
        FETCH v_lotes_cursor INTO v_id_stock_act, v_cantidad_lote;

        IF v_fin = 1 OR v_restante <= 0 THEN
            LEAVE bucle_lotes;
        END IF;

        -- Calcular cuanto descontar de este lote
        IF v_cantidad_lote >= v_restante THEN
            SET v_a_descontar = v_restante;
        ELSE
            SET v_a_descontar = v_cantidad_lote;
        END IF;

        -- Descontar del lote
        UPDATE stock
        SET cantidad = cantidad - v_a_descontar
        WHERE id_stock = v_id_stock_act;

        SET v_restante = v_restante - v_a_descontar;
    END LOOP;

    CLOSE v_lotes_cursor;

    -- Verificar alerta de stock bajo despues del descuento
    CALL sp_verificar_stock_bajo(p_id_medicamento);

    COMMIT;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 4.6 SP: sp_registrar_consulta_turno (RN-01, RN-07, RN-12)
-- Registra una consulta medica asociada a un turno existente.
-- Cambia automaticamente el estado del turno a 'Atendido'.
-- Operacion transaccional.
-- Parametros:
--   p_id_turno: ID del turno asociado
--   p_id_mascota: ID de la mascota
--   p_id_veterinario: ID del veterinario que atiende
--   p_sintomas: Sintomas observados
--   p_diagnostico: Diagnostico emitido
-- Devuelve: ID de la consulta creada
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE PROCEDURE sp_registrar_consulta_turno(
    IN p_id_turno INT,
    IN p_id_mascota INT,
    IN p_id_veterinario INT,
    IN p_sintomas TEXT,
    IN p_diagnostico TEXT,
    OUT p_id_consulta INT
)
BEGIN
    DECLARE v_estado_turno VARCHAR(20);
    DECLARE v_mascota_turno INT;
    DECLARE v_msg_error VARCHAR(300);
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- Verificar que el turno exista y este Pendiente
    SELECT estado, id_mascota INTO v_estado_turno, v_mascota_turno
    FROM turno
    WHERE id_turno = p_id_turno;

    IF v_estado_turno IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El turno especificado no existe.';
    END IF;

    IF v_estado_turno != 'Pendiente' THEN
        SET v_msg_error = CONCAT('El turno no esta Pendiente. Estado actual: ', v_estado_turno);
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = v_msg_error;
    END IF;

    -- Verificar que la mascota coincida con la del turno
    IF v_mascota_turno != p_id_mascota THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La mascota no coincide con la del turno.';
    END IF;

    -- Insertar la consulta medica
    INSERT INTO consulta_medica (id_turno, id_mascota, id_veterinario, fecha, sintomas, diagnostico, estado)
    VALUES (p_id_turno, p_id_mascota, p_id_veterinario, NOW(), p_sintomas, p_diagnostico, 'Activa');

    SET p_id_consulta = LAST_INSERT_ID();

    -- Actualizar estado del turno a Atendido
    UPDATE turno
    SET estado = 'Atendido'
    WHERE id_turno = p_id_turno;

    COMMIT;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 4.7 SP: sp_registrar_consulta_urgencia (RN-01)
-- Registra una consulta de urgencia (sin turno previo).
-- Parametros:
--   p_id_mascota: ID de la mascota
--   p_id_veterinario: ID del veterinario que atiende
--   p_sintomas: Sintomas observados
--   p_diagnostico: Diagnostico emitido
-- Devuelve: ID de la consulta creada
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE PROCEDURE sp_registrar_consulta_urgencia(
    IN p_id_mascota INT,
    IN p_id_veterinario INT,
    IN p_sintomas TEXT,
    IN p_diagnostico TEXT,
    OUT p_id_consulta INT
)
BEGIN
    INSERT INTO consulta_medica (id_turno, id_mascota, id_veterinario, fecha, sintomas, diagnostico, estado)
    VALUES (NULL, p_id_mascota, p_id_veterinario, NOW(), p_sintomas, p_diagnostico, 'Activa');

    SET p_id_consulta = LAST_INSERT_ID();
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 4.8 SP: sp_baja_logica_consulta (RN-07)
-- Realiza baja logica de una consulta medica.
-- Registra quien realizo la modificacion.
-- Parametros:
--   p_id_consulta: ID de la consulta
--   p_id_veterinario_modif: ID del veterinario que da de baja
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE PROCEDURE sp_baja_logica_consulta(
    IN p_id_consulta INT,
    IN p_id_veterinario_modif INT
)
BEGIN
    DECLARE v_estado_actual VARCHAR(20);

    SELECT estado INTO v_estado_actual
    FROM consulta_medica
    WHERE id_consulta = p_id_consulta;

    IF v_estado_actual IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La consulta especificada no existe.';
    END IF;

    IF v_estado_actual = 'Inactiva' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La consulta ya se encuentra inactiva.';
    END IF;

    UPDATE consulta_medica
    SET estado = 'Inactiva',
        fecha_modificacion = NOW(),
        id_veterinario_modif = p_id_veterinario_modif
    WHERE id_consulta = p_id_consulta;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 4.9 SP: sp_anonimizar_dueno (RN-13)
-- Anonimiza los datos personales de un dueno en cumplimiento de la
-- Ley 25.326 de Proteccion de Datos Personales (derecho de supresion).
-- Preserva el registro para trazabilidad de consultas medicas.
-- Parametros:
--   p_id_dueno: ID del dueno a anonimizar
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE PROCEDURE sp_anonimizar_dueno(
    IN p_id_dueno INT
)
BEGIN
    DECLARE v_estado_actual VARCHAR(20);

    SELECT estado INTO v_estado_actual
    FROM dueno
    WHERE id_dueno = p_id_dueno;

    IF v_estado_actual IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El dueno especificado no existe.';
    END IF;

    UPDATE dueno
    SET dni = CONCAT('ANON-', id_dueno),
        nombre = 'ANONIMIZADO',
        apellido = 'ANONIMIZADO',
        telefono = '0000000000',
        direccion = NULL,
        email = NULL,
        estado = 'Inactivo'
    WHERE id_dueno = p_id_dueno;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 4.10 SP: sp_ingresar_stock
-- Registra un nuevo ingreso de stock y verifica alertas.
-- Si el stock repuesto supera el umbral minimo, resuelve alertas pendientes.
-- Parametros:
--   p_id_medicamento: ID del medicamento
--   p_cantidad: Cantidad de unidades
--   p_numero_lote: Numero de lote del fabricante
--   p_fecha_vencimiento: Fecha de vencimiento del lote
-- Devuelve: ID del registro de stock creado
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE PROCEDURE sp_ingresar_stock(
    IN p_id_medicamento INT,
    IN p_cantidad INT,
    IN p_numero_lote VARCHAR(50),
    IN p_fecha_vencimiento DATE,
    OUT p_id_stock INT
)
BEGIN
    -- Insertar el registro de stock
    INSERT INTO stock (id_medicamento, cantidad, numero_lote, fecha_vencimiento)
    VALUES (p_id_medicamento, p_cantidad, p_numero_lote, p_fecha_vencimiento);

    SET p_id_stock = LAST_INSERT_ID();

    -- Los triggers trg_alerta_stock_bajo_insert y trg_alerta_vencimiento_insert
    -- se ejecutan automaticamente despues del INSERT.
    -- sp_verificar_stock_bajo se llama desde el trigger y resuelve alertas
    -- si el stock supera el umbral.
END //

DELIMITER ;


-- ============================================================================
-- 5. FUNCIONES
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 5.1 FN: fn_stock_total_medicamento
-- Retorna el stock total disponible de un medicamento (suma de cantidades
-- de lotes no vencidos).
-- Parametros:
--   p_id_medicamento: ID del medicamento
-- Retorna: INT con el stock total disponible
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE FUNCTION fn_stock_total_medicamento(
    p_id_medicamento INT
)
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_total INT;

    SELECT COALESCE(SUM(cantidad), 0)
    INTO v_total
    FROM stock
    WHERE id_medicamento = p_id_medicamento
      AND cantidad > 0
      AND fecha_vencimiento > CURDATE();

    RETURN v_total;
END //

DELIMITER ;


-- ----------------------------------------------------------------------------
-- 5.2 FN: fn_calcular_edad_mascota
-- Retorna la edad aproximada de una mascota en anos.
-- Parametros:
--   p_id_mascota: ID de la mascota
-- Retorna: INT con la edad en anos (NULL si no tiene fecha de nacimiento)
-- ----------------------------------------------------------------------------

DELIMITER //

CREATE FUNCTION fn_calcular_edad_mascota(
    p_id_mascota INT
)
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_fecha_nac DATE;
    DECLARE v_edad INT;

    SELECT fecha_nacimiento INTO v_fecha_nac
    FROM mascota
    WHERE id_mascota = p_id_mascota;

    IF v_fecha_nac IS NULL THEN
        RETURN NULL;
    END IF;

    SET v_edad = TIMESTAMPDIFF(YEAR, v_fecha_nac, CURDATE());

    RETURN v_edad;
END //

DELIMITER ;


-- ============================================================================
-- 6. VISTAS AUXILIARES
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 6.1 VISTA: vw_turnos_del_dia
-- Muestra los turnos del dia actual con datos completos del dueno,
-- mascota y veterinario. Util para la pantalla principal del recepcionista.
-- ----------------------------------------------------------------------------

CREATE VIEW vw_turnos_del_dia AS
SELECT
    t.id_turno,
    t.motivo,
    t.estado AS estado_turno,
    t.fecha_registro,
    sa.fecha AS fecha_turno,
    sa.hora AS hora_turno,
    m.nombre AS nombre_mascota,
    m.sexo AS sexo_mascota,
    e.nombre AS especie,
    r.nombre AS raza,
    d.apellido AS apellido_dueno,
    d.nombre AS nombre_dueno,
    d.telefono AS telefono_dueno,
    v.apellido AS apellido_vet,
    v.nombre AS nombre_vet,
    v.matricula AS matricula_vet
FROM turno t
JOIN slot_agenda sa ON sa.id_slot = t.id_slot
JOIN mascota m ON m.id_mascota = t.id_mascota
JOIN dueno d ON d.id_dueno = m.id_dueno
JOIN especie e ON e.id_especie = m.id_especie
JOIN raza r ON r.id_raza = m.id_raza
JOIN agenda_disponibilidad ad ON ad.id_agenda = sa.id_agenda
JOIN veterinario v ON v.id_veterinario = ad.id_veterinario
WHERE sa.fecha = CURDATE()
ORDER BY sa.hora ASC;


-- ----------------------------------------------------------------------------
-- 6.2 VISTA: vw_historial_clinico
-- Muestra el historial clinico completo de las mascotas con datos del
-- veterinario, medicamentos dispensados y lotes.
-- ----------------------------------------------------------------------------

CREATE VIEW vw_historial_clinico AS
SELECT
    cm.id_consulta,
    cm.fecha AS fecha_consulta,
    cm.sintomas,
    cm.diagnostico,
    cm.estado AS estado_consulta,
    m.nombre AS nombre_mascota,
    e.nombre AS especie,
    r.nombre AS raza,
    d.apellido AS apellido_dueno,
    d.nombre AS nombre_dueno,
    v.apellido AS apellido_vet,
    v.nombre AS nombre_vet,
    ir.cantidad AS cantidad_medicamento,
    ir.dosis,
    ir.frecuencia,
    ir.duracion,
    ir.dispensado,
    med.nombre_comercial,
    med.nombre_generico,
    s.numero_lote,
    s.fecha_vencimiento AS vencimiento_lote
FROM consulta_medica cm
JOIN mascota m ON m.id_mascota = cm.id_mascota
JOIN dueno d ON d.id_dueno = m.id_dueno
JOIN especie e ON e.id_especie = m.id_especie
JOIN raza r ON r.id_raza = m.id_raza
JOIN veterinario v ON v.id_veterinario = cm.id_veterinario
LEFT JOIN item_receta ir ON ir.id_consulta = cm.id_consulta
LEFT JOIN stock s ON s.id_stock = ir.id_stock
LEFT JOIN medicamento med ON med.id_medicamento = s.id_medicamento
WHERE cm.estado = 'Activa'
ORDER BY cm.fecha DESC;


-- ----------------------------------------------------------------------------
-- 6.3 VISTA: vw_stock_medicamentos
-- Muestra el stock actual de cada medicamento con el total disponible
-- y el estado respecto al umbral de alerta.
-- ----------------------------------------------------------------------------

CREATE VIEW vw_stock_medicamentos AS
SELECT
    med.id_medicamento,
    med.nombre_generico,
    med.nombre_comercial,
    med.dosis_presentacion,
    med.precio_venta,
    med.stock_minimo_alerta,
    med.estado AS estado_medicamento,
    COALESCE(SUM(s.cantidad), 0) AS stock_total,
    COALESCE(SUM(CASE WHEN s.fecha_vencimiento > CURDATE() THEN s.cantidad ELSE 0 END), 0) AS stock_disponible,
    COALESCE(SUM(CASE WHEN s.fecha_vencimiento <= CURDATE() THEN s.cantidad ELSE 0 END), 0) AS stock_vencido,
    COUNT(s.id_stock) AS total_lotes,
    MIN(s.fecha_vencimiento) AS proximo_vencimiento,
    CASE
        WHEN COALESCE(SUM(CASE WHEN s.fecha_vencimiento > CURDATE() THEN s.cantidad ELSE 0 END), 0) < med.stock_minimo_alerta
        THEN 'STOCK_BAJO'
        ELSE 'OK'
    END AS estado_stock
FROM medicamento med
LEFT JOIN stock s ON s.id_medicamento = med.id_medicamento
WHERE med.estado = 'Activo'
GROUP BY med.id_medicamento, med.nombre_generico, med.nombre_comercial,
         med.dosis_presentacion, med.precio_venta, med.stock_minimo_alerta, med.estado
ORDER BY med.nombre_comercial ASC;


-- ----------------------------------------------------------------------------
-- 6.4 VISTA: vw_alertas_activas
-- Muestra las alertas pendientes o en gestion con datos del medicamento.
-- ----------------------------------------------------------------------------

CREATE VIEW vw_alertas_activas AS
SELECT
    a.id_alerta,
    a.tipo,
    a.mensaje,
    a.estado,
    a.fecha_generacion,
    m.nombre_comercial,
    m.nombre_generico,
    m.stock_minimo_alerta,
    fn_stock_total_medicamento(m.id_medicamento) AS stock_actual
FROM alerta_stock a
JOIN medicamento m ON m.id_medicamento = a.id_medicamento
WHERE a.estado IN ('Pendiente', 'En Gestion')
ORDER BY
    CASE a.tipo
        WHEN 'STOCK_BAJO' THEN 1
        WHEN 'VENCIMIENTO_PROXIMO' THEN 2
    END ASC,
    a.fecha_generacion ASC;


-- ============================================================================
-- FIN DEL SCRIPT DDL - SIGVET
-- ============================================================================
