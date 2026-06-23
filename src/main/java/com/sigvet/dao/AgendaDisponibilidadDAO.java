package com.sigvet.dao;

import com.sigvet.exception.FranjaSuperpuestaException;
import com.sigvet.model.AgendaDisponibilidad;
import com.sigvet.model.Veterinario;
import com.sigvet.model.enums.EstadoRegistro;

import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad {@link AgendaDisponibilidad}.
 *
 * <p>Gestiona la configuración de franjas horarias semanales de los veterinarios.
 * Al insertar, el trigger {@code trg_validar_franjas_no_superpuestas} valida que
 * no haya superposición (RN-11); si la hay, la {@link SQLException} se convierte
 * en {@link FranjaSuperpuestaException}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see AgendaDisponibilidad
 * @see BaseDAO
 */
public class AgendaDisponibilidadDAO extends BaseDAO<AgendaDisponibilidad> {

    /** SQL base con JOIN para construir el objeto completo con veterinario. */
    private static final String SQL_SELECT_COMPLETO =
        "SELECT a.id_agenda, a.dia_semana, a.hora_inicio, a.hora_fin, "
      + "       v.id_veterinario, v.nombre AS nombre_vet, v.apellido AS apellido_vet, "
      + "       v.matricula, v.telefono AS telefono_vet, v.email AS email_vet, "
      + "       v.estado AS estado_vet "
      + "FROM agenda_disponibilidad a "
      + "JOIN veterinario v ON v.id_veterinario = a.id_veterinario ";

    // =========================================================================
    // Métodos abstractos de BaseDAO
    // =========================================================================

    /**
     * Busca una agenda de disponibilidad por su identificador único.
     *
     * @param id identificador único (PK {@code id_agenda})
     * @return agenda encontrada con veterinario completo, o {@code null}
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public AgendaDisponibilidad buscarPorId(int id) throws SQLException {
        String sql = SQL_SELECT_COMPLETO + "WHERE a.id_agenda = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearAgenda(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna todas las franjas horarias de todos los veterinarios.
     *
     * @return lista de agendas ordenada por veterinario y día
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public List<AgendaDisponibilidad> buscarTodos() throws SQLException {
        String sql = SQL_SELECT_COMPLETO + "ORDER BY v.apellido ASC, a.dia_semana ASC, a.hora_inicio ASC";
        List<AgendaDisponibilidad> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearAgenda(rs));
            }
        }
        return lista;
    }

    /**
     * Inserta una nueva franja horaria en la agenda de un veterinario.
     *
     * <p>El trigger {@code trg_validar_franjas_no_superpuestas} valida que no haya
     * superposición (RN-11). Si la hay, la {@link java.sql.SQLException} resultante
     * contiene la palabra clave {@code "superpuesta"} en su mensaje; la capa Service
     * la convierte en {@code FranjaSuperpuestaException}.</p>
     *
     * @param a franja horaria a insertar
     * @throws SQLException si la franja se superpone (RN-11) u otro error de BD
     */
    @Override
    public void insertar(AgendaDisponibilidad a) throws SQLException {
        String sql = "INSERT INTO agenda_disponibilidad (id_veterinario, dia_semana, hora_inicio, hora_fin) "
                   + "VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getVeterinario().getId());
            ps.setString(2, a.getDiaSemana());
            ps.setTime(3, Time.valueOf(a.getHoraInicio()));
            ps.setTime(4, Time.valueOf(a.getHoraFin()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    a.setIdAgenda(keys.getInt(1));
                }
            }
        }
        // Si el trigger rechaza, la SQLException se propaga con su mensaje original
    }

    /**
     * Actualiza una franja horaria existente.
     *
     * <p>El trigger {@code trg_validar_franjas_no_superpuestas_update} valida la
     * superposición al actualizar. Si la hay, la {@link java.sql.SQLException}
     * contiene la palabra clave {@code "superpuesta"} en su mensaje.</p>
     *
     * @param a franja con los datos actualizados; debe tener {@code idAgenda} asignado
     * @throws SQLException si ocurre un error de BD
     */
    @Override
    public void actualizar(AgendaDisponibilidad a) throws SQLException {
        String sql = "UPDATE agenda_disponibilidad SET id_veterinario = ?, dia_semana = ?, "
                   + "hora_inicio = ?, hora_fin = ? WHERE id_agenda = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, a.getVeterinario().getId());
            ps.setString(2, a.getDiaSemana());
            ps.setTime(3, Time.valueOf(a.getHoraInicio()));
            ps.setTime(4, Time.valueOf(a.getHoraFin()));
            ps.setInt(5, a.getIdAgenda());
            ps.executeUpdate();
        }
        // Si el trigger rechaza, la SQLException se propaga con su mensaje original
    }

    // =========================================================================
    // Métodos específicos
    // =========================================================================

    /**
     * Retorna todas las franjas horarias configuradas para un veterinario.
     *
     * @param idVeterinario identificador del veterinario
     * @return lista de franjas del veterinario, ordenada por día y hora de inicio
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<AgendaDisponibilidad> buscarPorVeterinario(int idVeterinario) throws SQLException {
        String sql = SQL_SELECT_COMPLETO
                   + "WHERE a.id_veterinario = ? ORDER BY a.dia_semana ASC, a.hora_inicio ASC";
        List<AgendaDisponibilidad> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVeterinario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearAgenda(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Elimina físicamente una franja de agenda.
     *
     * <p>Las franjas de agenda sí pueden eliminarse físicamente ya que no tienen
     * datos históricos sensibles. Los slots ya generados no se eliminan.</p>
     *
     * @param id identificador de la franja a eliminar
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM agenda_disponibilidad WHERE id_agenda = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // Método privado de mapeo
    // =========================================================================

    /**
     * Mapea una fila del {@link ResultSet} a un objeto {@link AgendaDisponibilidad}.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto {@link AgendaDisponibilidad} con veterinario completo
     * @throws SQLException si ocurre un error al leer el ResultSet
     */
    private AgendaDisponibilidad mapearAgenda(ResultSet rs) throws SQLException {
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
        LocalTime horaInicio = rs.getTime("hora_inicio").toLocalTime();
        LocalTime horaFin    = rs.getTime("hora_fin").toLocalTime();
        return new AgendaDisponibilidad(
            rs.getInt("id_agenda"),
            vet,
            rs.getString("dia_semana"),
            horaInicio,
            horaFin
        );
    }
}
