package com.sigvet.dao;

import com.sigvet.exception.SlotNoDisponibleException;
import com.sigvet.exception.TurnoOcupadoException;
import com.sigvet.model.*;
import com.sigvet.model.dto.TurnoDelDiaDTO;
import com.sigvet.model.enums.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad {@link Turno}.
 *
 * <p>Gestiona la reserva, cancelación y consulta de turnos. Las operaciones críticas
 * de reserva y cancelación se delegan a los SPs {@code sp_reservar_turno} y
 * {@code sp_cancelar_turno} que garantizan la integridad concurrente (RN-05/08).</p>
 *
 * <p>También expone {@link #buscarTurnosDelDia()} que consulta la vista
 * {@code vw_turnos_del_dia} para el caso de uso CU-08 (Agenda del Día).</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Turno
 * @see BaseDAO
 */
public class TurnoDAO extends BaseDAO<Turno> {

    /** SQL base con JOINs para construir el objeto Turno completo. */
    private static final String SQL_SELECT_COMPLETO =
        "SELECT t.id_turno, t.motivo, t.estado AS estado_turno, t.fecha_registro, "
      + "       s.id_slot, s.fecha AS fecha_slot, s.hora AS hora_slot, "
      + "       s.estado AS estado_slot, "
      + "       a.id_agenda, a.dia_semana, a.hora_inicio, a.hora_fin, "
      + "       v.id_veterinario, v.nombre AS nombre_vet, v.apellido AS apellido_vet, "
      + "       v.matricula, v.telefono AS telefono_vet, v.email AS email_vet, "
      + "       v.estado AS estado_vet, "
      + "       m.id_mascota, m.nombre AS nombre_mascota, m.fecha_nacimiento, "
      + "       m.sexo, m.color, m.senas_particulares, m.estado AS estado_mascota, "
      + "       d.id_dueno, d.dni, d.nombre AS nombre_dueno, d.apellido AS apellido_dueno, "
      + "       d.telefono AS telefono_dueno, d.direccion, d.email AS email_dueno, "
      + "       d.estado AS estado_dueno, "
      + "       e.id_especie, e.nombre AS nombre_especie, "
      + "       r.id_raza, r.nombre AS nombre_raza "
      + "FROM turno t "
      + "JOIN slot_agenda s ON s.id_slot = t.id_slot "
      + "JOIN agenda_disponibilidad a ON a.id_agenda = s.id_agenda "
      + "JOIN veterinario v ON v.id_veterinario = a.id_veterinario "
      + "JOIN mascota m ON m.id_mascota = t.id_mascota "
      + "JOIN dueno d ON d.id_dueno = m.id_dueno "
      + "JOIN especie e ON e.id_especie = m.id_especie "
      + "JOIN raza r ON r.id_raza = m.id_raza ";

    // =========================================================================
    // Métodos abstractos de BaseDAO
    // =========================================================================

    /**
     * Busca un turno por su identificador único, cargando todos los objetos relacionados.
     *
     * @param id identificador único del turno (PK {@code id_turno})
     * @return turno completo o {@code null} si no existe
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public Turno buscarPorId(int id) throws SQLException {
        String sql = SQL_SELECT_COMPLETO + "WHERE t.id_turno = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearTurno(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna todos los turnos registrados.
     *
     * @return lista de turnos ordenada por fecha y hora del slot
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public List<Turno> buscarTodos() throws SQLException {
        String sql = SQL_SELECT_COMPLETO + "ORDER BY s.fecha DESC, s.hora ASC";
        List<Turno> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearTurno(rs));
            }
        }
        return lista;
    }

    /**
     * Inserta un turno directamente. Para uso normal, preferir {@link #reservarTurno}.
     *
     * @param t turno a insertar
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void insertar(Turno t) throws SQLException {
        String sql = "INSERT INTO turno (id_mascota, id_slot, motivo, estado) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, t.getMascota().getIdMascota());
            ps.setInt(2, t.getSlot().getIdSlot());
            ps.setString(3, t.getMotivo());
            ps.setString(4, t.getEstado().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    t.setIdTurno(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Actualiza el estado de un turno. Para cambios de estado, preferir los SPs específicos.
     *
     * @param t turno con estado actualizado
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void actualizar(Turno t) throws SQLException {
        String sql = "UPDATE turno SET estado = ?, motivo = ? WHERE id_turno = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getEstado().name());
            ps.setString(2, t.getMotivo());
            ps.setInt(3, t.getIdTurno());
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // Métodos específicos — SPs y consultas
    // =========================================================================

    /**
     * Reserva un turno llamando al SP {@code sp_reservar_turno} que usa
     * {@code SELECT FOR UPDATE} para evitar condiciones de carrera (RN-05).
     *
     * <p>El SP tiene 4 parámetros: (IN p_id_mascota, IN p_id_slot, IN p_motivo, OUT p_id_turno).
     * El parámetro OUT se registra en la posición 4 con {@link Types#INTEGER}.</p>
     *
     * @param idMascota identificador de la mascota
     * @param idSlot    identificador del slot a reservar
     * @param motivo    motivo del turno (puede ser nulo)
     * @return identificador del turno generado por la BD
     * @throws SQLException              si ocurre un error de BD no relacionado con disponibilidad
     * @throws SlotNoDisponibleException si el slot no está en estado Disponible (RN-08)
     * @throws TurnoOcupadoException     si el slot fue tomado concurrentemente (RN-05)
     */
    public int reservarTurno(int idMascota, int idSlot, String motivo)
            throws SQLException, SlotNoDisponibleException, TurnoOcupadoException {
        try (Connection conn = getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_reservar_turno(?, ?, ?, ?)}")) {
            cs.setInt(1, idMascota);
            cs.setInt(2, idSlot);
            cs.setString(3, motivo != null ? motivo : "");
            cs.registerOutParameter(4, Types.INTEGER); // OUT p_id_turno
            cs.execute();
            return cs.getInt(4);
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("disponible") || msg.contains("slot")) {
                throw new SlotNoDisponibleException(
                    "El horario seleccionado ya no está disponible.");
            }
            if (msg.contains("turno") && msg.contains("activo") || msg.contains("reservado")) {
                throw new TurnoOcupadoException(
                    "El horario ya fue reservado por otro usuario.");
            }
            throw e;
        }
    }

    /**
     * Cancela un turno o registra inasistencia llamando al SP {@code sp_cancelar_turno}.
     *
     * <p>El SP libera el slot asociado (trigger {@code trg_liberar_slot_al_cancelar}).
     * Tiene 2 parámetros: (IN p_id_turno, IN p_estado_nuevo).</p>
     *
     * @param idTurno      identificador del turno a cancelar
     * @param estadoNuevo  nuevo estado: {@code "Cancelado"} o {@code "Inasistencia"}
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public void cancelarTurno(int idTurno, String estadoNuevo) throws SQLException {
        try (Connection conn = getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_cancelar_turno(?, ?)}")) {
            cs.setInt(1, idTurno);
            cs.setString(2, estadoNuevo);
            cs.execute();
        }
    }

    /**
     * Registra la inasistencia de un paciente al turno.
     *
     * <p>Delega a {@link #cancelarTurno} con estado {@code "Inasistencia"}.</p>
     *
     * @param idTurno identificador del turno
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public void marcarInasistencia(int idTurno) throws SQLException {
        cancelarTurno(idTurno, EstadoTurno.Inasistencia.name());
    }

    /**
     * Retorna el turno asociado a un slot específico.
     *
     * @param idSlot identificador del slot
     * @return turno del slot, o {@code null} si no existe turno activo
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public Turno buscarPorSlot(int idSlot) throws SQLException {
        String sql = SQL_SELECT_COMPLETO + "WHERE t.id_slot = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSlot);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearTurno(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna el historial de turnos de una mascota específica.
     *
     * @param idMascota identificador de la mascota
     * @return lista de turnos de la mascota, ordenada por fecha descendente
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Turno> buscarPorMascota(int idMascota) throws SQLException {
        String sql = SQL_SELECT_COMPLETO
                   + "WHERE t.id_mascota = ? ORDER BY s.fecha DESC, s.hora DESC";
        List<Turno> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMascota);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearTurno(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Retorna los turnos pendientes de una fecha específica.
     *
     * @param fecha fecha para la que se buscan turnos pendientes
     * @return lista de turnos pendientes ordenada por hora
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Turno> buscarPendientesPorFecha(LocalDate fecha) throws SQLException {
        String sql = SQL_SELECT_COMPLETO
                   + "WHERE s.fecha = ? AND t.estado = 'Pendiente' ORDER BY s.hora ASC";
        List<Turno> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearTurno(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Consulta la vista {@code vw_turnos_del_dia} y retorna los turnos del día actual.
     *
     * <p>La vista ya filtra por {@code CURDATE()} y ordena por hora.
     * Se usa para el caso de uso CU-08 (Agenda del Día).</p>
     *
     * @return lista de {@link TurnoDelDiaDTO} con los turnos de hoy
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<TurnoDelDiaDTO> buscarTurnosDelDia() throws SQLException {
        String sql = "SELECT id_turno, hora_turno, nombre_mascota, especie, raza, "
                   + "       apellido_dueno, nombre_dueno, telefono_dueno, "
                   + "       apellido_vet, nombre_vet, estado_turno, motivo "
                   + "FROM vw_turnos_del_dia";
        List<TurnoDelDiaDTO> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TurnoDelDiaDTO dto = new TurnoDelDiaDTO();
                dto.setIdTurno(rs.getInt("id_turno"));
                dto.setHora(rs.getTime("hora_turno").toLocalTime());
                dto.setNombreMascota(rs.getString("nombre_mascota"));
                dto.setEspecie(rs.getString("especie"));
                dto.setRaza(rs.getString("raza"));
                String nombreDueno = rs.getString("apellido_dueno") + ", "
                                   + rs.getString("nombre_dueno");
                dto.setNombreDueno(nombreDueno);
                dto.setTelefonoDueno(rs.getString("telefono_dueno"));
                String nombreVet = rs.getString("apellido_vet") + ", "
                                 + rs.getString("nombre_vet");
                dto.setNombreVeterinario(nombreVet);
                dto.setEstado(EstadoTurno.valueOf(rs.getString("estado_turno")));
                dto.setMotivo(rs.getString("motivo"));
                lista.add(dto);
            }
        }
        return lista;
    }

    // =========================================================================
    // Método privado de mapeo
    // =========================================================================

    /**
     * Mapea una fila del {@link ResultSet} a un objeto {@link Turno} completo.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto {@link Turno} con todos sus objetos relacionados
     * @throws SQLException si ocurre un error al leer el ResultSet
     */
    private Turno mapearTurno(ResultSet rs) throws SQLException {
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
        // AgendaDisponibilidad
        AgendaDisponibilidad agenda = new AgendaDisponibilidad(
            rs.getInt("id_agenda"),
            vet,
            rs.getString("dia_semana"),
            rs.getTime("hora_inicio").toLocalTime(),
            rs.getTime("hora_fin").toLocalTime()
        );
        // SlotAgenda
        SlotAgenda slot = new SlotAgenda(
            rs.getInt("id_slot"),
            agenda,
            rs.getDate("fecha_slot").toLocalDate(),
            rs.getTime("hora_slot").toLocalTime(),
            EstadoSlot.valueOf(rs.getString("estado_slot"))
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
        // Turno
        EstadoTurno estadoTurno = EstadoTurno.valueOf(rs.getString("estado_turno"));
        Timestamp tsReg = rs.getTimestamp("fecha_registro");
        LocalDateTime fechaReg = (tsReg != null) ? tsReg.toLocalDateTime() : LocalDateTime.now();
        return new Turno(
            rs.getInt("id_turno"),
            mascota,
            slot,
            rs.getString("motivo"),
            estadoTurno,
            fechaReg
        );
    }
}
