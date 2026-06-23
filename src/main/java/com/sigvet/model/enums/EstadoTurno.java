package com.sigvet.model.enums;

/**
 * Representa los posibles estados de un turno médico veterinario.
 *
 * <p>Los estados siguen el ciclo de vida de un turno desde su creación hasta su resolución:
 * <ul>
 *   <li>{@link #Pendiente} – Turno reservado, aún no atendido.</li>
 *   <li>{@link #Atendido} – El paciente fue atendido; se generó una consulta médica.</li>
 *   <li>{@link #Cancelado} – El turno fue cancelado por el sistema o el usuario.</li>
 *   <li>{@link #Inasistencia} – El paciente no se presentó al turno.</li>
 * </ul>
 * </p>
 *
 * @author SIGVET
 * @version 1.0
 */
public enum EstadoTurno {

    /** Turno reservado y pendiente de atención. */
    Pendiente,

    /** Turno completado: el paciente fue atendido y se registró la consulta médica. */
    Atendido,

    /** Turno cancelado antes de la fecha/hora del slot. */
    Cancelado,

    /** El paciente no se presentó al turno asignado. */
    Inasistencia
}
