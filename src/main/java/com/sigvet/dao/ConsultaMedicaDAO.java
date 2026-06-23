package com.sigvet.dao;

import com.sigvet.model.*;
import com.sigvet.model.dto.HistorialClinicoDTO;
import com.sigvet.model.enums.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad {@link ConsultaMedica}.
 *
 * <p>Gestiona la creación y consulta de consultas médicas veterinarias.
 * Las operaciones de registro se delegan a los SPs:
 * <ul>
 *   <li>{@code sp_registrar_consulta_turno} – con turno previo</li>
 *   <li>{@code sp_registrar_consulta_urgencia} – sin turno (urgencias)</li>
 *   <li>{@code sp_baja_logica_consulta} – baja lógica (nunca DELETE)</li>
 * </ul>
 *
 * <p><strong>RN-07:</strong> NUNCA se implementa un método {@code eliminar()}.
 * El trigger {@code trg_prevenir_eliminar_consulta} también lo impide en BD.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see ConsultaMedica
 * @see BaseDAO
 */
public class ConsultaMedicaDAO extends BaseDAO<ConsultaMedica> {

    /** SQL base con JOINs para construir la consulta completa. */
    private static final String SQL_SELECT_COMPLETO =
        "SELECT cm.id_consulta, cm.fecha AS fecha_consulta, cm.sintomas, cm.diagnostico, "
      + "       cm.estado AS estado_consulta, cm.fecha_modificacion, "
      + "       cm.id_turno, "
      + "       m.id_mascota, m.nombre AS nombre_mascota, m.fecha_nacimiento, "
      + "       m.sexo, m.color, m.senas_particulares, m.estado AS estado_mascota, "
      + "       d.id_dueno, d.dni, d.nombre AS nombre_dueno, d.apellido AS apellido_dueno, "
      + "       d.telefono AS telefono_dueno, d.direccion, d.email AS email_dueno, "
      + "       d.estado AS estado_dueno, "
      + "       e.id_especie, e.nombre AS nombre_especie, "
      + "       r.id_raza, r.nombre AS nombre_raza, "
      + "       v.id_veterinario, v.nombre AS nombre_vet, v.apellido AS apellido_vet, "
      + "       v.matricula, v.telefono AS telefono_vet, v.email AS email_vet, "
      + "       v.estado AS estado_vet "
      + "FROM consulta_medica cm "
      + "JOIN mascota m ON m.id_mascota = cm.id_mascota "
      + "JOIN dueno d ON d.id_dueno = m.id_dueno "
      + "JOIN especie e ON e.id_especie = m.id_especie "
      + "JOIN raza r ON r.id_raza = m.id_raza "
      + "JOIN veterinario v ON v.id_veterinario = cm.id_veterinario ";

    // =========================================================================
    // Métodos abstractos de BaseDAO
    // =========================================================================

    /**
     * Busca una consulta médica por su identificador único.
     *
     * @param id identificador único de la consulta (PK {@code id_consulta})
     * @return consulta completa con relaciones cargadas, o {@code null}
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public ConsultaMedica buscarPorId(int id) throws SQLException {
        String sql = SQL_SELECT_COMPLETO + "WHERE cm.id_consulta = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearConsulta(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna todas las consultas médicas activas.
     *
     * @return lista de consultas activas ordenada por fecha descendente
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public List<ConsultaMedica> buscarTodos() throws SQLException {
        String sql = SQL_SELECT_COMPLETO
                   + "WHERE cm.estado = 'Activa' ORDER BY cm.fecha DESC";
        List<ConsultaMedica> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearConsulta(rs));
            }
        }
        return lista;
    }

    /**
     * Inserta una consulta directamente con PreparedStatement.
     * Para uso normal, preferir {@link #registrarConsultaConTurno} o
     * {@link #registrarConsultaUrgencia}.
     *
     * @param cm consulta a insertar
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void insertar(ConsultaMedica cm) throws SQLException {
        String sql = "INSERT INTO consulta_medica "
                   + "(id_turno, id_mascota, id_veterinario, sintomas, diagnostico, estado) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (cm.getTurno() != null) {
                ps.setInt(1, cm.getTurno().getIdTurno());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setInt(2, cm.getMascota().getIdMascota());
            ps.setInt(3, cm.getVeterinario().getId());
            ps.setString(4, cm.getSintomas());
            ps.setString(5, cm.getDiagnostico());
            ps.setString(6, cm.getEstado().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    cm.setIdConsulta(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Actualiza los datos de una consulta existente.
     *
     * @param cm consulta con los datos actualizados
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void actualizar(ConsultaMedica cm) throws SQLException {
        String sql = "UPDATE consulta_medica SET sintomas = ?, diagnostico = ?, "
                   + "id_veterinario_modif = ? WHERE id_consulta = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cm.getSintomas());
            ps.setString(2, cm.getDiagnostico());
            if (cm.getVeterinarioModif() != null) {
                ps.setInt(3, cm.getVeterinarioModif().getId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setInt(4, cm.getIdConsulta());
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // Métodos específicos — SPs y vistas
    // =========================================================================

    /**
     * Registra una consulta médica para un turno existente mediante el SP
     * {@code sp_registrar_consulta_turno}. También marca el turno como Atendido (RN-12).
     *
     * <p>El SP tiene 6 parámetros:
     * (IN p_id_turno, IN p_id_mascota, IN p_id_veterinario,
     *  IN p_sintomas, IN p_diagnostico, OUT p_id_consulta).
     * El OUT se registra en la posición 6.</p>
     *
     * @param idTurno        identificador del turno
     * @param idMascota      identificador de la mascota
     * @param idVeterinario  identificador del veterinario actuante
     * @param sintomas       síntomas reportados
     * @param diagnostico    diagnóstico del veterinario
     * @return identificador de la consulta generada por la BD
     * @throws SQLException si ocurre un error durante el registro
     */
    public int registrarConsultaConTurno(int idTurno, int idMascota, int idVeterinario,
                                          String sintomas, String diagnostico)
            throws SQLException {
        try (Connection conn = getConnection();
             CallableStatement cs = conn.prepareCall(
                     "{CALL sp_registrar_consulta_turno(?, ?, ?, ?, ?, ?)}")) {
            cs.setInt(1, idTurno);
            cs.setInt(2, idMascota);
            cs.setInt(3, idVeterinario);
            cs.setString(4, sintomas);
            cs.setString(5, diagnostico);
            cs.registerOutParameter(6, Types.INTEGER); // OUT p_id_consulta
            cs.execute();
            return cs.getInt(6);
        }
    }

    /**
     * Registra una consulta de urgencia (sin turno previo) mediante el SP
     * {@code sp_registrar_consulta_urgencia} (RN-01).
     *
     * <p>El SP tiene 5 parámetros:
     * (IN p_id_mascota, IN p_id_veterinario, IN p_sintomas, IN p_diagnostico,
     *  OUT p_id_consulta).
     * El OUT se registra en la posición 5.</p>
     *
     * @param idMascota     identificador de la mascota
     * @param idVeterinario identificador del veterinario actuante
     * @param sintomas      síntomas reportados
     * @param diagnostico   diagnóstico del veterinario
     * @return identificador de la consulta generada
     * @throws SQLException si ocurre un error durante el registro
     */
    public int registrarConsultaUrgencia(int idMascota, int idVeterinario,
                                          String sintomas, String diagnostico)
            throws SQLException {
        try (Connection conn = getConnection();
             CallableStatement cs = conn.prepareCall(
                     "{CALL sp_registrar_consulta_urgencia(?, ?, ?, ?, ?)}")) {
            cs.setInt(1, idMascota);
            cs.setInt(2, idVeterinario);
            cs.setString(3, sintomas);
            cs.setString(4, diagnostico);
            cs.registerOutParameter(5, Types.INTEGER); // OUT p_id_consulta
            cs.execute();
            return cs.getInt(5);
        }
    }

    /**
     * Aplica baja lógica a una consulta médica mediante el SP {@code sp_baja_logica_consulta}.
     *
     * <p>Cambia el estado de la consulta a {@link EstadoConsulta#Inactiva} sin eliminarla.
     * Conforme a RN-07, NUNCA se debe llamar a un DELETE sobre consultas médicas.</p>
     *
     * @param idConsulta          identificador de la consulta a dar de baja
     * @param idVeterinarioModif  identificador del veterinario que realiza la baja
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public void bajaLogica(int idConsulta, int idVeterinarioModif) throws SQLException {
        try (Connection conn = getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_baja_logica_consulta(?, ?)}")) {
            cs.setInt(1, idConsulta);
            cs.setInt(2, idVeterinarioModif);
            cs.execute();
        }
    }

    /**
     * Retorna todas las consultas activas de una mascota específica.
     *
     * @param idMascota identificador de la mascota
     * @return lista de consultas activas de la mascota, ordenada por fecha descendente
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<ConsultaMedica> buscarPorMascota(int idMascota) throws SQLException {
        String sql = SQL_SELECT_COMPLETO
                   + "WHERE cm.id_mascota = ? AND cm.estado = 'Activa' ORDER BY cm.fecha DESC";
        List<ConsultaMedica> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMascota);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearConsulta(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Retorna la consulta médica asociada a un turno específico.
     *
     * @param idTurno identificador del turno
     * @return consulta del turno, o {@code null} si no existe
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public ConsultaMedica buscarPorTurno(int idTurno) throws SQLException {
        String sql = SQL_SELECT_COMPLETO + "WHERE cm.id_turno = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idTurno);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearConsulta(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna las consultas realizadas por un veterinario en una fecha específica.
     *
     * @param idVeterinario identificador del veterinario
     * @param fecha         fecha de las consultas a buscar
     * @return lista de consultas del veterinario en esa fecha
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<ConsultaMedica> buscarPorVeterinario(int idVeterinario, LocalDate fecha)
            throws SQLException {
        String sql = SQL_SELECT_COMPLETO
                   + "WHERE cm.id_veterinario = ? AND DATE(cm.fecha) = ? "
                   + "  AND cm.estado = 'Activa' ORDER BY cm.fecha ASC";
        List<ConsultaMedica> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVeterinario);
            ps.setDate(2, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearConsulta(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Consulta el historial clínico de una mascota usando la vista {@code vw_historial_clinico}.
     *
     * <p>La vista ya filtra consultas con estado {@code Activa} y hace los JOINs
     * necesarios con medicamentos y recetas. Retorna un DTO plano por cada
     * fila (puede haber múltiples filas por consulta si tiene varios medicamentos).</p>
     *
     * @param idMascota identificador de la mascota
     * @return lista de {@link HistorialClinicoDTO}, uno por fila de la vista
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<HistorialClinicoDTO> buscarHistorialClinico(int idMascota) throws SQLException {
        String sql = "SELECT hc.id_consulta, hc.fecha_consulta, hc.sintomas, hc.diagnostico, "
                   + "       hc.nombre_mascota, hc.especie, hc.raza, "
                   + "       hc.apellido_dueno, hc.nombre_dueno, "
                   + "       hc.apellido_vet, hc.nombre_vet, "
                   + "       hc.nombre_comercial, hc.dosis, hc.frecuencia, hc.duracion, "
                   + "       hc.numero_lote, hc.vencimiento_lote "
                   + "FROM vw_historial_clinico hc "
                   + "WHERE hc.id_mascota = ? "
                   + "ORDER BY hc.fecha_consulta DESC";
        // Nota: la vista vw_historial_clinico no filtra por id_mascota directamente,
        // se hace el filtro con un JOIN o subquery. Usamos la tabla base para filtrar.
        String sqlFiltrado = "SELECT cm.id_consulta, cm.fecha AS fecha_consulta, "
                           + "cm.sintomas, cm.diagnostico, "
                           + "m.nombre AS nombre_mascota, e.nombre AS especie, r.nombre AS raza, "
                           + "d.apellido AS apellido_dueno, d.nombre AS nombre_dueno, "
                           + "v.apellido AS apellido_vet, v.nombre AS nombre_vet, "
                           + "med.nombre_comercial, ir.dosis, ir.frecuencia, ir.duracion, "
                           + "s.numero_lote, s.fecha_vencimiento AS vencimiento_lote "
                           + "FROM consulta_medica cm "
                           + "JOIN mascota m ON m.id_mascota = cm.id_mascota "
                           + "JOIN dueno d ON d.id_dueno = m.id_dueno "
                           + "JOIN especie e ON e.id_especie = m.id_especie "
                           + "JOIN raza r ON r.id_raza = m.id_raza "
                           + "JOIN veterinario v ON v.id_veterinario = cm.id_veterinario "
                           + "LEFT JOIN item_receta ir ON ir.id_consulta = cm.id_consulta "
                           + "LEFT JOIN stock s ON s.id_stock = ir.id_stock "
                           + "LEFT JOIN medicamento med ON med.id_medicamento = s.id_medicamento "
                           + "WHERE cm.id_mascota = ? AND cm.estado = 'Activa' "
                           + "ORDER BY cm.fecha DESC";
        List<HistorialClinicoDTO> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlFiltrado)) {
            ps.setInt(1, idMascota);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HistorialClinicoDTO dto = new HistorialClinicoDTO();
                    dto.setIdConsulta(rs.getInt("id_consulta"));
                    Timestamp ts = rs.getTimestamp("fecha_consulta");
                    dto.setFechaConsulta(ts != null ? ts.toLocalDateTime() : null);
                    dto.setSintomas(rs.getString("sintomas"));
                    dto.setDiagnostico(rs.getString("diagnostico"));
                    dto.setNombreMascota(rs.getString("nombre_mascota"));
                    dto.setEspecieRaza(rs.getString("especie") + " " + rs.getString("raza"));
                    dto.setNombreDueno(rs.getString("apellido_dueno") + ", "
                                    + rs.getString("nombre_dueno"));
                    dto.setNombreVeterinario(rs.getString("apellido_vet") + ", "
                                           + rs.getString("nombre_vet"));
                    dto.setMedicamento(rs.getString("nombre_comercial"));
                    dto.setDosis(rs.getString("dosis"));
                    dto.setFrecuencia(rs.getString("frecuencia"));
                    dto.setDuracion(rs.getString("duracion"));
                    dto.setLote(rs.getString("numero_lote"));
                    Date fv = rs.getDate("vencimiento_lote");
                    dto.setFechaVencimiento(fv != null ? fv.toLocalDate() : null);
                    lista.add(dto);
                }
            }
        }
        return lista;
    }

    // =========================================================================
    // Método privado de mapeo
    // =========================================================================

    /**
     * Mapea una fila del {@link ResultSet} a un objeto {@link ConsultaMedica}.
     *
     * <p>El turno se deja como {@code null} si {@code id_turno} es NULL en la BD
     * (caso de urgencia, RN-01).</p>
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto {@link ConsultaMedica} con sus relaciones cargadas
     * @throws SQLException si ocurre un error al leer el ResultSet
     */
    private ConsultaMedica mapearConsulta(ResultSet rs) throws SQLException {
        // Veterinario
        EstadoRegistro estVet = EstadoRegistro.valueOf(rs.getString("estado_vet"));
        Veterinario vet = new Veterinario(
            rs.getInt("id_veterinario"),
            rs.getString("nombre_vet"),
            rs.getString("apellido_vet"),
            rs.getString("matricula"),
            rs.getString("telefono_vet"),
            rs.getString("email_vet"),
            estVet
        );
        // Especie + Raza + Dueno + Mascota
        Especie especie = new Especie(rs.getInt("id_especie"), rs.getString("nombre_especie"));
        Raza raza = new Raza(rs.getInt("id_raza"), especie, rs.getString("nombre_raza"));
        EstadoRegistro estDueno = EstadoRegistro.valueOf(rs.getString("estado_dueno"));
        Dueno dueno = new Dueno(
            rs.getInt("id_dueno"),
            rs.getString("dni"),
            rs.getString("nombre_dueno"),
            rs.getString("apellido_dueno"),
            rs.getString("telefono_dueno"),
            rs.getString("direccion"),
            rs.getString("email_dueno"),
            estDueno
        );
        Date fnac = rs.getDate("fecha_nacimiento");
        LocalDate fechaNac = (fnac != null) ? fnac.toLocalDate() : null;
        EstadoRegistro estMascota = EstadoRegistro.valueOf(rs.getString("estado_mascota"));
        Mascota mascota = new Mascota(
            rs.getInt("id_mascota"), dueno,
            rs.getString("nombre_mascota"),
            especie, raza, fechaNac,
            rs.getString("sexo"),
            rs.getString("color"),
            rs.getString("senas_particulares"),
            estMascota
        );
        // Turno (nullable para urgencias)
        int idTurnoRaw = rs.getInt("id_turno");
        Turno turno = null; // rs.wasNull() comprueba si el último getInt devolvió NULL
        if (!rs.wasNull()) {
            turno = new Turno();
            turno.setIdTurno(idTurnoRaw);
        }
        // ConsultaMedica
        EstadoConsulta estadoConsulta = EstadoConsulta.valueOf(rs.getString("estado_consulta"));
        Timestamp ts = rs.getTimestamp("fecha_consulta");
        LocalDateTime fechaConsulta = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();
        Date fmod = rs.getDate("fecha_modificacion");
        LocalDate fechaMod = (fmod != null) ? fmod.toLocalDate() : null;
        return new ConsultaMedica(
            rs.getInt("id_consulta"),
            turno,
            mascota,
            vet,
            fechaConsulta,
            rs.getString("sintomas"),
            rs.getString("diagnostico"),
            estadoConsulta,
            fechaMod,
            null // veterinarioModif: no cargado para simplificar (se carga bajo demanda)
        );
    }
}
