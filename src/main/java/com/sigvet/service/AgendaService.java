package com.sigvet.service;

import com.sigvet.dao.AgendaDisponibilidadDAO;
import com.sigvet.dao.SlotAgendaDAO;
import com.sigvet.dao.VeterinarioDAO;
import com.sigvet.exception.FranjaSuperpuestaException;
import com.sigvet.model.AgendaDisponibilidad;
import com.sigvet.model.SlotAgenda;
import com.sigvet.model.Veterinario;
import com.sigvet.model.enums.EstadoSlot;

import com.sigvet.util.OrdenamientoBusquedaUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;


/**
 * Servicio de lógica de negocio para el caso de uso CU-01: Configurar Agenda Veterinaria.
 *
 * <p>Orquesta la configuración de franjas horarias semanales y la generación de slots
 * individuales de atención para los veterinarios. Actúa como intermediario entre la
 * capa de presentación (Swing) y los DAOs {@link AgendaDisponibilidadDAO} y
 * {@link SlotAgendaDAO}.</p>
 *
 * <p><strong>Responsabilidades:</strong></p>
 * <ul>
 *   <li>Validar la coherencia de las franjas horarias antes de persistirlas.</li>
 *   <li>Delegar la validación de superposición a la BD (trigger {@code trg_validar_franjas_no_superpuestas}).</li>
 *   <li>Convertir las {@link SQLException} con mensaje {@code "superpuesta"} en
 *       {@link FranjaSuperpuestaException} para feedback amigable al usuario (RN-11).</li>
 * </ul>
 *
 * @author SIGVET
 * @version 1.0
 * @see AgendaDisponibilidadDAO
 * @see SlotAgendaDAO
 */
public class AgendaService {

    /** DAO para la gestión de franjas horarias de disponibilidad. */
    private final AgendaDisponibilidadDAO agendaDAO;

    /** DAO para la gestión de slots individuales generados a partir de las franjas. */
    private final SlotAgendaDAO slotDAO;

    /** DAO para la consulta de veterinarios. */
    private final VeterinarioDAO veterinarioDAO;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor que instancia los DAOs necesarios para el servicio de agenda.
     */
    public AgendaService() {
        this.agendaDAO    = new AgendaDisponibilidadDAO();
        this.slotDAO      = new SlotAgendaDAO();
        this.veterinarioDAO = new VeterinarioDAO();
    }

    // =========================================================================
    // Métodos de negocio
    // =========================================================================

    /**
     * Retorna la lista de veterinarios activos disponibles para configurar en la agenda.
     *
     * @return lista de veterinarios con estado {@code Activo}
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Veterinario> obtenerVeterinariosActivos() throws SQLException {
        List<Veterinario> lista = veterinarioDAO.buscarActivos();
        OrdenamientoBusquedaUtil.ordenarPorNombre(lista, v -> v.getApellido());
        return lista;
    }

    /**
     * Retorna todas las franjas horarias configuradas para un veterinario específico.
     *
     * @param idVeterinario identificador del veterinario
     * @return lista de franjas de disponibilidad, ordenada por día y hora de inicio
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<AgendaDisponibilidad> obtenerFranjasPorVeterinario(int idVeterinario)
            throws SQLException {
        return agendaDAO.buscarPorVeterinario(idVeterinario);
    }

    /**
     * Agrega una nueva franja horaria a la agenda de un veterinario.
     *
     * <p><strong>Validaciones previas (feedback rápido):</strong></p>
     * <ol>
     *   <li>Los parámetros {@code horaInicio} y {@code horaFin} no pueden ser nulos.</li>
     *   <li>{@code horaInicio} debe ser estrictamente menor a {@code horaFin}.</li>
     * </ol>
     *
     * <p><strong>Validación en BD:</strong> el trigger {@code trg_validar_franjas_no_superpuestas}
     * verifica la superposición con franjas existentes (RN-11).</p>
     *
     * @param idVeterinario identificador del veterinario
     * @param diaSemana     día de la semana (ej.: {@code "Lunes"})
     * @param horaInicio    hora de inicio de la franja
     * @param horaFin       hora de fin de la franja
     * @throws IllegalArgumentException   si las horas son nulas o incoherentes
     * @throws FranjaSuperpuestaException si la franja se superpone con una existente (RN-11)
     * @throws SQLException               si ocurre otro error de BD
     */
    public void agregarFranja(int idVeterinario, String diaSemana,
                               LocalTime horaInicio, LocalTime horaFin)
            throws IllegalArgumentException, FranjaSuperpuestaException, SQLException {
        // --- Validaciones de entrada ---
        if (horaInicio == null || horaFin == null) {
            throw new IllegalArgumentException(
                "Los horarios de inicio y fin son obligatorios.");
        }
        if (!horaInicio.isBefore(horaFin)) {
            throw new IllegalArgumentException(
                "La hora de inicio (" + horaInicio + ") debe ser anterior "
                + "a la hora de fin (" + horaFin + ").");
        }
        if (diaSemana == null || diaSemana.trim().isEmpty()) {
            throw new IllegalArgumentException("El día de la semana es obligatorio.");
        }
        if (idVeterinario <= 0) {
            throw new IllegalArgumentException("Debe seleccionar un veterinario válido.");
        }

        // --- Construir entidad y persistir ---
        Veterinario vet = new Veterinario();
        vet.setId(idVeterinario);
        AgendaDisponibilidad franja = new AgendaDisponibilidad(
            0, vet, diaSemana.trim(), horaInicio, horaFin
        );

        try {
            agendaDAO.insertar(franja);
        } catch (SQLException e) {
            // Convertir error de BD de superposición en excepción de dominio amigable
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("superpuesta") || msg.contains("superpone")
                    || msg.contains("overlap") || e.getErrorCode() == 1644) {
                throw new FranjaSuperpuestaException(
                    "La franja " + horaInicio + "–" + horaFin
                    + " se superpone con una franja existente del veterinario (RN-11).");
            }
            throw e;
        }
    }

    /**
     * Genera los slots de 30 minutos para los próximos 14 días de un veterinario,
     * a partir de las franjas horarias ya configuradas.
     *
     * <p>Los slots se insertan con {@code INSERT IGNORE} para evitar duplicados
     * si ya fueron generados previamente.</p>
     *
     * @param idVeterinario identificador del veterinario
     * @throws IllegalArgumentException si el ID es inválido
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public void generarSlotsSemana(int idVeterinario)
            throws IllegalArgumentException, SQLException {
        if (idVeterinario <= 0) {
            throw new IllegalArgumentException("Debe seleccionar un veterinario válido.");
        }
        slotDAO.generarSlotsSemana(idVeterinario);
    }

    /**
     * Retorna los slots disponibles de un veterinario para una fecha específica.
     *
     * @param idVeterinario identificador del veterinario
     * @param fecha         fecha para la que se buscan slots
     * @return lista de {@link SlotAgenda} con estado {@code Disponible}
     * @throws IllegalArgumentException si la fecha es nula o pasada
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public List<SlotAgenda> obtenerSlotsDisponibles(int idVeterinario, LocalDate fecha)
            throws IllegalArgumentException, SQLException {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula.");
        }
        if (fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                "No se pueden consultar slots de fechas pasadas.");
        }
        return slotDAO.buscarDisponiblesPorFecha(idVeterinario, fecha);
    }

    /**
     * Elimina una franja horaria de la agenda, manejando correctamente el
     * FK constraint con {@code slot_agenda}.
     *
     * <p><strong>Flujo:</strong></p>
     * <ol>
     *   <li>Verifica si hay turnos Pendiente/Confirmado en slots de esta franja.
     *       Si los hay, lanza {@link SQLException} con mensaje amigable (no se puede eliminar).</li>
     *   <li>Elimina los slots en estado {@code Disponible} de la franja.</li>
     *   <li>Elimina la franja.</li>
     * </ol>
     *
     * @param idAgenda identificador de la franja a eliminar
     * @throws IllegalArgumentException si el ID es inválido
     * @throws SQLException             si hay turnos reservados o error de BD
     */
    public void eliminarFranja(int idAgenda)
            throws IllegalArgumentException, SQLException {
        if (idAgenda <= 0) {
            throw new IllegalArgumentException("ID de franja inválido.");
        }
        // 1. Verificar si hay turnos pendientes/confirmados
        if (slotDAO.tieneTurnosReservados(idAgenda)) {
            throw new SQLException(
                "No se puede eliminar la franja porque tiene TURNOS RESERVADOS vigentes.\n"
                + "Cancele los turnos desde 'Gestionar Turnos' antes de eliminar la franja.");
        }
        // 2. Eliminar slots disponibles (resuelve FK constraint)
        int slotsEliminados = slotDAO.eliminarSlotsDisponibles(idAgenda);
        System.out.println("[AgendaService] Eliminados " + slotsEliminados
            + " slots disponibles de la franja " + idAgenda);
        // 3. Eliminar la franja
        agendaDAO.eliminar(idAgenda);
    }

    /**
     * Genera automáticamente slots de 30 minutos para los próximos {@code dias} días,
     * basándose en todas las franjas de agenda configuradas.
     *
     * <p>Solo genera slots para fechas que aún no tengan slots en la franja correspondiente
     * (usa {@link com.sigvet.dao.SlotAgendaDAO#existenSlotsParaFecha} para evitar duplicados).
     * Diseñado para ejecutarse automáticamente al iniciar la aplicación.</p>
     *
     * @param dias número de días hacia adelante para generar slots (incluyendo hoy)
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public void generarSlotsProximosDias(int dias) throws SQLException {
        // DIAS_VALIDOS del modelo: {"Lunes","Martes","Miercoles","Jueves","Viernes","Sabado","Domingo"}
        // DayOfWeek.getValue(): Monday=1 ... Sunday=7  → index = getValue()-1
        String[] DIAS = {"Lunes","Martes","Miercoles","Jueves","Viernes","Sabado","Domingo"};

        LocalDate hoy   = LocalDate.now();
        LocalDate hasta = hoy.plusDays(dias);

        List<AgendaDisponibilidad> franjas = agendaDAO.buscarTodos();
        int slotsCreados = 0;

        for (AgendaDisponibilidad franja : franjas) {
            String diaSemana = franja.getDiaSemana();

            for (LocalDate fecha = hoy; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
                int dow = fecha.getDayOfWeek().getValue() - 1; // Mon=0 ... Sun=6
                if (!DIAS[dow].equals(diaSemana)) continue;
                if (slotDAO.existenSlotsParaFecha(franja.getIdAgenda(), fecha)) continue;

                // Generar slots de 30 minutos
                LocalTime hora = franja.getHoraInicio();
                while (hora.isBefore(franja.getHoraFin())) {
                    SlotAgenda slot = new SlotAgenda();
                    slot.setAgenda(franja);
                    slot.setFecha(fecha);
                    slot.setHora(hora);
                    slot.setEstado(EstadoSlot.Disponible);
                    slotDAO.insertar(slot);
                    hora = hora.plusMinutes(30);
                    slotsCreados++;
                }
            }
        }
        if (slotsCreados > 0) {
            System.out.println("[AgendaService] Generados " + slotsCreados
                + " slots para los próximos " + dias + " días.");
        }
    }
}
