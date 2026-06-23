package com.sigvet.model.enums;

/**
 * Representa el estado de disponibilidad de un slot (franja horaria) de la agenda.
 *
 * <p>Un slot nace como {@link #Disponible} y pasa a {@link #Reservado} cuando se asocia
 * a un turno activo. Cuando el turno se cancela o el paciente no asiste, el slot vuelve
 * a {@link #Disponible} (gestionado por el trigger {@code trg_liberar_slot_al_cancelar}).</p>
 *
 * @author SIGVET
 * @version 1.0
 */
public enum EstadoSlot {

    /** El slot está libre y puede ser reservado para un nuevo turno. */
    Disponible,

    /** El slot ya tiene un turno activo asociado; no puede ser reservado nuevamente. */
    Reservado
}
