package com.sigvet.service;

import com.sigvet.dao.MascotaDAO;
import com.sigvet.dao.SlotAgendaDAO;
import com.sigvet.dao.TurnoDAO;
import com.sigvet.dao.VeterinarioDAO;
import com.sigvet.exception.MascotaNoRegistradaException;
import com.sigvet.exception.SlotNoDisponibleException;
import com.sigvet.exception.TurnoOcupadoException;
import com.sigvet.model.Mascota;
import com.sigvet.model.SlotAgenda;
import com.sigvet.model.Turno;
import com.sigvet.model.Veterinario;
import com.sigvet.model.enums.EstadoTurno;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de lógica de negocio para el caso de uso CU-02: Gestionar Turnos.
 *
 * <p>Orquesta la reserva, cancelación y consulta de turnos veterinarios.
 * Las operaciones críticas de reserva delegan al SP {@code sp_reservar_turno}
 * mediante {@link TurnoDAO}, que usa {@code SELECT FOR UPDATE} para garantizar
 * la integridad concurrente (RN-05/08).</p>
 *
 * <p><strong>Responsabilidades:</strong></p>
 * <ul>
 *   <li>Validar que la mascota exista antes de reservar (RN-04).</li>
 *   <li>Convertir errores de BD en excepciones de dominio con mensajes amigables.</li>
 *   <li>Delegar la lógica de reserva atómica al SP de BD.</li>
 * </ul>
 *
 * @author SIGVET
 * @version 1.0
 * @see TurnoDAO
 * @see MascotaDAO
 */
public class TurnoService {

    /** DAO para la gestión de turnos. */
    private final TurnoDAO turnoDAO;

    /** DAO para verificar la existencia de mascotas. */
    private final MascotaDAO mascotaDAO;

    /** DAO para consultar slots disponibles. */
    private final SlotAgendaDAO slotDAO;

    /** DAO para consultar veterinarios. */
    private final VeterinarioDAO veterinarioDAO;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor que instancia los DAOs necesarios para la gestión de turnos.
     */
    public TurnoService() {
        this.turnoDAO      = new TurnoDAO();
        this.mascotaDAO    = new MascotaDAO();
        this.slotDAO       = new SlotAgendaDAO();
        this.veterinarioDAO = new VeterinarioDAO();
    }

    // =========================================================================
    // Métodos de negocio
    // =========================================================================

    /**
     * Retorna los slots disponibles de un veterinario para una fecha específica.
     *
     * @param idVeterinario identificador del veterinario
     * @param fecha         fecha para la que se buscan slots disponibles
     * @return lista de slots con estado {@code Disponible}
     * @throws IllegalArgumentException si la fecha es nula o pasada
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public List<SlotAgenda> buscarSlotsDisponibles(int idVeterinario, LocalDate fecha)
            throws IllegalArgumentException, SQLException {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria.");
        }
        if (fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                "No se pueden reservar turnos para fechas pasadas.");
        }
        return slotDAO.buscarDisponiblesPorFecha(idVeterinario, fecha);
    }

    /**
     * Reserva un turno llamando al SP {@code sp_reservar_turno} que garantiza
     * atomicidad y control de concurrencia (RN-05/08).
     *
     * <p><strong>Validaciones previas (feedback rápido):</strong></p>
     * <ol>
     *   <li>{@code idMascota} debe ser &gt; 0 y la mascota debe existir y estar activa (RN-04).</li>
     *   <li>{@code idSlot} debe ser &gt; 0.</li>
     * </ol>
     *
     * @param idMascota identificador de la mascota
     * @param idSlot    identificador del slot a reservar
     * @param motivo    motivo del turno (puede ser nulo o vacío)
     * @return identificador del turno generado por la BD
     * @throws MascotaNoRegistradaException si la mascota no existe o está inactiva (RN-04)
     * @throws SlotNoDisponibleException    si el slot no está disponible (RN-08)
     * @throws TurnoOcupadoException        si hubo concurrencia y el slot fue tomado (RN-05)
     * @throws IllegalArgumentException     si los parámetros son inválidos
     * @throws SQLException                 si ocurre otro error de BD
     */
    public int reservarTurno(int idMascota, int idSlot, String motivo)
            throws MascotaNoRegistradaException, SlotNoDisponibleException,
                   TurnoOcupadoException, IllegalArgumentException, SQLException {
        // --- Validaciones de entrada ---
        if (idMascota <= 0) {
            throw new MascotaNoRegistradaException(
                "Debe seleccionar una mascota válida (RN-04).");
        }
        if (idSlot <= 0) {
            throw new IllegalArgumentException("Debe seleccionar un horario válido.");
        }

        // --- Verificar que la mascota existe y está activa (RN-04) ---
        Mascota mascota = mascotaDAO.buscarPorId(idMascota);
        if (mascota == null) {
            throw new MascotaNoRegistradaException(
                "La mascota con ID " + idMascota + " no existe en el sistema (RN-04).");
        }
        if (mascota.getEstado().name().equals("Inactivo")) {
            throw new MascotaNoRegistradaException(
                "La mascota '" + mascota.getNombre()
                + "' está inactiva y no puede reservar turnos (RN-04).");
        }

        // --- Delegar al DAO que llama al SP sp_reservar_turno ---
        try {
            return turnoDAO.reservarTurno(idMascota, idSlot, motivo);
        } catch (SlotNoDisponibleException | TurnoOcupadoException e) {
            throw e; // Re-propagar excepciones de dominio sin modificar
        } catch (SQLException e) {
            // Convertir otros errores de BD relacionados a excepciones de dominio
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("disponible") || msg.contains("slot")) {
                throw new SlotNoDisponibleException(
                    "El horario seleccionado ya no está disponible.");
            }
            if (msg.contains("reservado") || msg.contains("ocupado")) {
                throw new TurnoOcupadoException(
                    "El horario fue tomado por otro usuario. Seleccione otro horario.");
            }
            throw e;
        }
    }

    /**
     * Cancela un turno o registra inasistencia mediante el SP {@code sp_cancelar_turno}.
     *
     * <p>El SP libera el slot (trigger {@code trg_liberar_slot_al_cancelar}).
     * El valor de {@code estadoNuevo} debe ser {@code "Cancelado"} o {@code "Inasistencia"}.</p>
     *
     * @param idTurno     identificador del turno
     * @param estadoNuevo nuevo estado: {@code "Cancelado"} o {@code "Inasistencia"}
     * @throws IllegalArgumentException si el estado es inválido
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public void cancelarTurno(int idTurno, String estadoNuevo)
            throws IllegalArgumentException, SQLException {
        if (idTurno <= 0) {
            throw new IllegalArgumentException("ID de turno inválido.");
        }
        // Validar que el estado sea uno de los permitidos
        if (!EstadoTurno.Cancelado.name().equals(estadoNuevo)
                && !EstadoTurno.Inasistencia.name().equals(estadoNuevo)) {
            throw new IllegalArgumentException(
                "Estado inválido para cancelación: '" + estadoNuevo
                + "'. Use 'Cancelado' o 'Inasistencia'.");
        }
        turnoDAO.cancelarTurno(idTurno, estadoNuevo);
    }

    /**
     * Registra la inasistencia de un paciente a su turno.
     *
     * @param idTurno identificador del turno
     * @throws IllegalArgumentException si el ID es inválido
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public void marcarInasistencia(int idTurno)
            throws IllegalArgumentException, SQLException {
        if (idTurno <= 0) {
            throw new IllegalArgumentException("ID de turno inválido.");
        }
        turnoDAO.marcarInasistencia(idTurno);
    }

    /**
     * Retorna los turnos con estado {@code Pendiente} de una fecha específica.
     *
     * @param fecha fecha para la que se consultan los turnos pendientes
     * @return lista de turnos pendientes ordenada por hora
     * @throws IllegalArgumentException si la fecha es nula
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public List<Turno> buscarTurnosPendientes(LocalDate fecha)
            throws IllegalArgumentException, SQLException {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria.");
        }
        return turnoDAO.buscarPendientesPorFecha(fecha);
    }

    /**
     * Busca una mascota por su ID para verificar que existe antes de reservar.
     *
     * @param idMascota identificador de la mascota
     * @return mascota encontrada, o {@code null} si no existe
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public Mascota buscarMascota(int idMascota) throws SQLException {
        return mascotaDAO.buscarPorId(idMascota);
    }

    /**
     * Retorna todas las mascotas activas de un dueño específico.
     *
     * <p>Utilizado para poblar el combo de mascotas cuando el operador selecciona un dueño.</p>
     *
     * @param idDueno identificador del dueño
     * @return lista de mascotas activas del dueño
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Mascota> buscarMascotasPorDueno(int idDueno) throws SQLException {
        return mascotaDAO.buscarPorDueno(idDueno);
    }

    /**
     * Retorna el turno asociado a un slot específico.
     *
     * @param idSlot identificador del slot
     * @return turno del slot, o {@code null} si no tiene turno activo
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public Turno buscarTurnoPorSlot(int idSlot) throws SQLException {
        return turnoDAO.buscarPorSlot(idSlot);
    }

    /**
     * Retorna el historial de turnos de una mascota.
     *
     * @param idMascota identificador de la mascota
     * @return lista de todos los turnos de la mascota, ordenada por fecha descendente
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Turno> buscarTurnosPorMascota(int idMascota) throws SQLException {
        return turnoDAO.buscarPorMascota(idMascota);
    }

    /**
     * Retorna todos los veterinarios con estado {@code Activo}.
     * Utilizado por {@link com.sigvet.view.GestionarTurnosDialog} para poblar
     * el combo de filtro de veterinarios al abrir el diálogo.
     *
     * @return lista de veterinarios activos, ordenada por apellido
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Veterinario> buscarVeterinariosActivos() throws SQLException {
        return veterinarioDAO.buscarActivos();
    }
}
