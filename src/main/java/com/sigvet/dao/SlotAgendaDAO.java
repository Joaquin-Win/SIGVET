package com.sigvet.dao;

import com.sigvet.model.AgendaDisponibilidad;
import com.sigvet.model.SlotAgenda;
import com.sigvet.model.Veterinario;
import com.sigvet.model.enums.EstadoRegistro;
import com.sigvet.model.enums.EstadoSlot;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad {@link SlotAgenda}.
 *
 * <p>Gestiona los slots individuales generados a partir de las franjas de
 * {@link AgendaDisponibilidad}. Cada slot tiene duración fija de
 * {@link AgendaDisponibilidad#DURACION_SLOT_MINUTOS} minutos.</p>
 *
 * <p>El estado de los slots es administrado automáticamente por los triggers:
 * {@code trg_actualizar_slot_al_reservar} y {@code trg_liberar_slot_al_cancelar}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see SlotAgenda
 * @see BaseDAO
 */
public class SlotAgendaDAO extends BaseDAO<SlotAgenda> {

    /** SQL base con JOIN a agenda y veterinario para objeto completo. */
    private static final String SQL_SELECT_COMPLETO =
        "SELECT s.id_slot, s.fecha, s.hora, s.estado AS estado_slot, "
      + "       a.id_agenda, a.dia_semana, a.hora_inicio, a.hora_fin, "
      + "       v.id_veterinario, v.nombre AS nombre_vet, v.apellido AS apellido_vet, "
      + "       v.matricula, v.telefono AS telefono_vet, v.email AS email_vet, "
      + "       v.estado AS estado_vet "
      + "FROM slot_agenda s "
      + "JOIN agenda_disponibilidad a ON a.id_agenda = s.id_agenda "
      + "JOIN veterinario v ON v.id_veterinario = a.id_veterinario ";

    // =========================================================================
    // Métodos abstractos de BaseDAO
    // =========================================================================

    /**
     * Busca un slot por su identificador único.
     *
     * @param id identificador único del slot (PK {@code id_slot})
     * @return slot con agenda y veterinario completos, o {@code null}
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public SlotAgenda buscarPorId(int id) throws SQLException {
        String sql = SQL_SELECT_COMPLETO + "WHERE s.id_slot = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearSlot(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna todos los slots de todas las agendas.
     *
     * @return lista de slots ordenada por fecha y hora
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public List<SlotAgenda> buscarTodos() throws SQLException {
        String sql = SQL_SELECT_COMPLETO + "ORDER BY s.fecha ASC, s.hora ASC";
        List<SlotAgenda> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearSlot(rs));
            }
        }
        return lista;
    }

    /**
     * Inserta un nuevo slot en la agenda.
     *
     * @param s slot a insertar; la agenda debe tener su ID asignado
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void insertar(SlotAgenda s) throws SQLException {
        String sql = "INSERT INTO slot_agenda (id_agenda, fecha, hora, estado) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getAgenda().getIdAgenda());
            ps.setDate(2, Date.valueOf(s.getFecha()));
            ps.setTime(3, Time.valueOf(s.getHora()));
            ps.setString(4, s.getEstado().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    s.setIdSlot(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Actualiza el estado de un slot (normalmente gestionado por triggers).
     *
     * @param s slot con el estado actualizado
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void actualizar(SlotAgenda s) throws SQLException {
        String sql = "UPDATE slot_agenda SET estado = ? WHERE id_slot = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getEstado().name());
            ps.setInt(2, s.getIdSlot());
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // Métodos específicos
    // =========================================================================

    /**
     * Retorna los slots disponibles de un veterinario para una fecha específica.
     *
     * <p>Utilizado al reservar un turno: muestra solo los slots libres del
     * veterinario seleccionado para la fecha elegida.</p>
     *
     * @param idVeterinario identificador del veterinario
     * @param fecha         fecha para la que se buscan slots disponibles
     * @return lista de slots disponibles ordenada por hora
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<SlotAgenda> buscarDisponiblesPorFecha(int idVeterinario, LocalDate fecha)
            throws SQLException {
        String sql;
        if (idVeterinario <= 0) {
            // Sin filtro de veterinario → devuelve todos los vets
            sql = SQL_SELECT_COMPLETO
                + "WHERE s.fecha = ? AND s.estado = 'Disponible' "
                + "ORDER BY v.apellido ASC, s.hora ASC";
        } else {
            sql = SQL_SELECT_COMPLETO
                + "WHERE s.fecha = ? AND s.estado = 'Disponible' "
                + "  AND a.id_veterinario = ? "
                + "ORDER BY s.hora ASC";
        }
        List<SlotAgenda> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            if (idVeterinario > 0) ps.setInt(2, idVeterinario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearSlot(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Retorna todos los slots de una fecha específica (de todos los veterinarios).
     *
     * @param fecha fecha para la que se buscan slots
     * @return lista de slots de la fecha, ordenada por veterinario y hora
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<SlotAgenda> buscarPorFecha(LocalDate fecha) throws SQLException {
        String sql = SQL_SELECT_COMPLETO
                   + "WHERE s.fecha = ? ORDER BY v.apellido ASC, s.hora ASC";
        List<SlotAgenda> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearSlot(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Genera y persiste los slots de 30 minutos para la semana a partir de las franjas
     * configuradas en la agenda de un veterinario.
     *
     * <p>Para cada franja de la agenda del veterinario, genera slots desde
     * {@code horaInicio} hasta {@code horaFin} con pasos de
     * {@link AgendaDisponibilidad#DURACION_SLOT_MINUTOS} minutos, para los próximos
     * 7 días a partir de hoy que coincidan con el {@code diaSemana} de la franja.</p>
     *
     * <p>Usa la constante {@link AgendaDisponibilidad#DURACION_SLOT_MINUTOS} = 30 para
     * determinar la duración de cada slot. Solo genera slots para fechas futuras.</p>
     *
     * @param idVeterinario identificador del veterinario para el que se generan slots
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public void generarSlotsSemana(int idVeterinario) throws SQLException {
        // 1. Obtener las franjas del veterinario
        String sqlFranjas = "SELECT id_agenda, dia_semana, hora_inicio, hora_fin "
                          + "FROM agenda_disponibilidad WHERE id_veterinario = ?";
        String sqlInsert = "INSERT IGNORE INTO slot_agenda (id_agenda, fecha, hora, estado) "
                         + "VALUES (?, ?, ?, 'Disponible')";

        // Mapa de día de semana nombre → número ISO (Lunes=1, ..., Domingo=7)
        java.util.Map<String, Integer> diasMap = new java.util.LinkedHashMap<>();
        diasMap.put("Lunes",     1);
        diasMap.put("Martes",    2);
        diasMap.put("Miercoles", 3);
        diasMap.put("Jueves",    4);
        diasMap.put("Viernes",   5);
        diasMap.put("Sabado",    6);
        diasMap.put("Domingo",   7);

        try (Connection conn = getConnection();
             PreparedStatement psFranjas = conn.prepareStatement(sqlFranjas);
             PreparedStatement psInsert  = conn.prepareStatement(sqlInsert)) {

            psFranjas.setInt(1, idVeterinario);

            try (ResultSet rs = psFranjas.executeQuery()) {
                while (rs.next()) {
                    int    idAgenda   = rs.getInt("id_agenda");
                    String diaSemana  = rs.getString("dia_semana");
                    LocalTime inicio  = rs.getTime("hora_inicio").toLocalTime();
                    LocalTime fin     = rs.getTime("hora_fin").toLocalTime();

                    Integer diaNumero = diasMap.get(diaSemana);
                    if (diaNumero == null) continue;

                    // Generar slots para los próximos 14 días
                    LocalDate hoy = LocalDate.now();
                    for (int i = 0; i <= 14; i++) {
                        LocalDate fechaCandidata = hoy.plusDays(i);
                        // getDayOfWeek().getValue(): Lunes=1 ... Domingo=7
                        if (fechaCandidata.getDayOfWeek().getValue() == diaNumero) {
                            // Generar cada slot de DURACION_SLOT_MINUTOS minutos
                            LocalTime horaSlot = inicio;
                            while (horaSlot.isBefore(fin)) {
                                psInsert.setInt(1, idAgenda);
                                psInsert.setDate(2, Date.valueOf(fechaCandidata));
                                psInsert.setTime(3, Time.valueOf(horaSlot));
                                psInsert.addBatch();
                                horaSlot = horaSlot.plusMinutes(AgendaDisponibilidad.DURACION_SLOT_MINUTOS);
                            }
                        }
                    }
                }
            }
            psInsert.executeBatch();
        }
    }

    // =========================================================================
    // Métodos para manejo de integridad referencial y generación automática
    // =========================================================================

    /**
     * Verifica si algún slot de la franja indicada tiene un turno PENDIENTE asociado.
     * Utilizado por {@link com.sigvet.service.AgendaService#eliminarFranja} para
     * impedir la eliminación cuando hay reservas vigentes (RN-13).
     *
     * @param idAgenda identificador de la franja de disponibilidad
     * @return {@code true} si existen turnos Pendiente/Confirmado para esta franja
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public boolean tieneTurnosReservados(int idAgenda) throws SQLException {
        String sql = "SELECT COUNT(*) FROM slot_agenda s "
                   + "JOIN turno t ON t.id_slot = s.id_slot "
                   + "WHERE s.id_agenda = ? AND t.estado IN ('Pendiente', 'Confirmado')";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAgenda);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    /**
     * Elimina todos los slots en estado {@code Disponible} de la franja indicada.
     * Invocado justo antes de eliminar la franja para resolver el FK constraint.
     *
     * @param idAgenda identificador de la franja cuyos slots se eliminarán
     * @return cantidad de slots eliminados
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public int eliminarSlotsDisponibles(int idAgenda) throws SQLException {
        String sql = "DELETE FROM slot_agenda WHERE id_agenda = ? AND estado = 'Disponible'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAgenda);
            return ps.executeUpdate();
        }
    }

    /**
     * Verifica si ya existen slots generados para una franja y fecha determinadas.
     * Utilizado por {@link com.sigvet.service.AgendaService#generarSlotsProximosDias}
     * para evitar duplicados al generar slots automáticamente.
     *
     * @param idAgenda identificador de la franja
     * @param fecha    fecha a verificar
     * @return {@code true} si ya hay al menos un slot para esa franja y fecha
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public boolean existenSlotsParaFecha(int idAgenda, LocalDate fecha) throws SQLException {
        String sql = "SELECT COUNT(*) FROM slot_agenda WHERE id_agenda = ? AND fecha = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAgenda);
            ps.setDate(2, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    // =========================================================================
    // Método privado de mapeo
    // =========================================================================

    /**
     * Mapea una fila del {@link ResultSet} a un objeto {@link SlotAgenda}.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto {@link SlotAgenda} con agenda y veterinario completos
     * @throws SQLException si ocurre un error al leer el ResultSet
     */
    private SlotAgenda mapearSlot(ResultSet rs) throws SQLException {
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
        AgendaDisponibilidad agenda = new AgendaDisponibilidad(
            rs.getInt("id_agenda"),
            vet,
            rs.getString("dia_semana"),
            rs.getTime("hora_inicio").toLocalTime(),
            rs.getTime("hora_fin").toLocalTime()
        );
        EstadoSlot estado = EstadoSlot.valueOf(rs.getString("estado_slot"));
        LocalDate  fecha  = rs.getDate("fecha").toLocalDate();
        LocalTime  hora   = rs.getTime("hora").toLocalTime();
        return new SlotAgenda(rs.getInt("id_slot"), agenda, fecha, hora, estado);
    }
}
