package com.sigvet.service;

import com.sigvet.dao.TurnoDAO;
import com.sigvet.model.dto.TurnoDelDiaDTO;
import com.sigvet.model.enums.EstadoTurno;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de lógica de negocio para el caso de uso CU-08: Consultar Agenda del Día.
 *
 * <p>Provee la agenda de turnos del día actual consultando la vista
 * {@code vw_turnos_del_dia} mediante {@link TurnoDAO#buscarTurnosDelDia()}.
 * La vista ya filtra por {@code CURDATE()} y ordena por hora.</p>
 *
 * <p>Este servicio es intencionalmente simple (solo lectura):
 * <ul>
 *   <li>Delega la consulta principal a la vista de la BD.</li>
 *   <li>Ofrece filtrado en memoria por veterinario o estado para uso en la UI.</li>
 * </ul>
 *
 * @author SIGVET
 * @version 1.0
 * @see TurnoDAO
 * @see TurnoDelDiaDTO
 */
public class AgendaDiaService {

    /** DAO para la consulta de turnos y la vista {@code vw_turnos_del_dia}. */
    private final TurnoDAO turnoDAO;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor que instancia el DAO necesario para la agenda del día.
     */
    public AgendaDiaService() {
        this.turnoDAO = new TurnoDAO();
    }

    // =========================================================================
    // Métodos de negocio
    // =========================================================================

    /**
     * Retorna todos los turnos del día actual desde la vista {@code vw_turnos_del_dia}.
     *
     * <p>La vista de BD ya filtra por {@code CURDATE()} y ordena por hora ascendente.
     * Incluye turnos de todos los veterinarios y todos los estados.</p>
     *
     * @return lista de {@link TurnoDelDiaDTO} con los turnos de hoy
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<TurnoDelDiaDTO> obtenerTurnosDelDia() throws SQLException {
        return turnoDAO.buscarTurnosDelDia();
    }

    /**
     * Retorna los turnos del día filtrados por veterinario.
     *
     * <p>Se obtienen todos los turnos del día y luego se filtra en memoria
     * comparando el nombre del veterinario en el DTO. Si {@code idVeterinario}
     * es 0, se retornan todos los turnos sin filtrar.</p>
     *
     * @param nombreVeterinario nombre completo del veterinario (formato "Apellido, Nombre")
     *                          para filtrar; {@code null} o vacío retorna todos
     * @return lista de {@link TurnoDelDiaDTO} del veterinario especificado
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<TurnoDelDiaDTO> obtenerTurnosDelDiaPorVeterinario(String nombreVeterinario)
            throws SQLException {
        List<TurnoDelDiaDTO> todos = turnoDAO.buscarTurnosDelDia();

        // Sin filtro → retornar todo
        if (nombreVeterinario == null || nombreVeterinario.trim().isEmpty()) {
            return todos;
        }

        // Filtrar en memoria usando for-each
        List<TurnoDelDiaDTO> filtrados = new ArrayList<>();
        String filtro = nombreVeterinario.trim().toLowerCase();
        for (TurnoDelDiaDTO dto : todos) {
            String nomVet = dto.getNombreVeterinario() != null
                ? dto.getNombreVeterinario().toLowerCase() : "";
            if (nomVet.contains(filtro)) {
                filtrados.add(dto);
            }
        }
        return filtrados;
    }

    /**
     * Retorna los turnos del día con estado {@code Pendiente}.
     *
     * <p>Útil para mostrar la lista de pacientes que aún no fueron atendidos.</p>
     *
     * @return lista de turnos pendientes de hoy
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<TurnoDelDiaDTO> obtenerTurnosPendientes() throws SQLException {
        List<TurnoDelDiaDTO> todos = turnoDAO.buscarTurnosDelDia();
        List<TurnoDelDiaDTO> pendientes = new ArrayList<>();
        for (TurnoDelDiaDTO dto : todos) {
            if (dto.getEstado() == EstadoTurno.Pendiente) {
                pendientes.add(dto);
            }
        }
        return pendientes;
    }

    /**
     * Retorna los turnos del día con estado {@code Atendido}.
     *
     * <p>Útil para el resumen de atenciones del día.</p>
     *
     * @return lista de turnos atendidos hoy
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<TurnoDelDiaDTO> obtenerTurnosAtendidos() throws SQLException {
        List<TurnoDelDiaDTO> todos = turnoDAO.buscarTurnosDelDia();
        List<TurnoDelDiaDTO> atendidos = new ArrayList<>();
        for (TurnoDelDiaDTO dto : todos) {
            if (dto.getEstado() == EstadoTurno.Atendido) {
                atendidos.add(dto);
            }
        }
        return atendidos;
    }

    /**
     * Cuenta el número de turnos pendientes del día actual.
     *
     * <p>Utilizado para el indicador numérico en el dashboard.</p>
     *
     * @return cantidad de turnos pendientes hoy
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public int contarTurnosPendientesHoy() throws SQLException {
        List<TurnoDelDiaDTO> pendientes = obtenerTurnosPendientes();
        return pendientes.size();
    }

    /**
     * Cuenta el número de turnos atendidos del día actual.
     *
     * <p>Utilizado para el indicador de productividad en el dashboard.</p>
     *
     * @return cantidad de turnos atendidos hoy
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public int contarTurnosAtendidosHoy() throws SQLException {
        List<TurnoDelDiaDTO> atendidos = obtenerTurnosAtendidos();
        return atendidos.size();
    }
}
