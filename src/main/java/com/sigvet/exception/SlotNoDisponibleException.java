package com.sigvet.exception;

/**
 * Excepción de dominio que se lanza cuando se intenta reservar un slot de agenda
 * que no se encuentra en estado {@code Disponible}.
 *
 * <p>Un slot puede estar en estado {@code Reservado} por un turno activo existente,
 * lo que impide su reutilización (RN-08). Esta excepción complementa a
 * {@link TurnoOcupadoException}: mientras {@code TurnoOcupadoException} representa
 * el conflicto de concurrencia, esta excepción representa el caso en que el slot
 * ya estaba reservado al momento de la selección.</p>
 *
 * <p>Se lanza al capturar una {@link java.sql.SQLException} cuyo mensaje contiene
 * las palabras clave {@code "disponible"}, {@code "slot"} o {@code "reservado"}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see com.sigvet.dao.TurnoDAO
 * @see com.sigvet.service.TurnoService
 * @see TurnoOcupadoException
 */
public class SlotNoDisponibleException extends Exception {

    /**
     * Constructor con mensaje de error por defecto.
     */
    public SlotNoDisponibleException() {
        super("El slot seleccionado no está disponible. Puede haber sido reservado recientemente.");
    }

    /**
     * Constructor con mensaje de error personalizado.
     *
     * @param mensaje mensaje descriptivo del estado del slot
     */
    public SlotNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
