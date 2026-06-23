package com.sigvet.exception;

/**
 * Excepción de dominio que se lanza cuando se intenta reservar un slot que ya fue
 * ocupado concurrentemente por otro proceso o usuario.
 *
 * <p>El SP {@code sp_reservar_turno} usa {@code SELECT FOR UPDATE} para bloquear el
 * slot durante la transacción (RN-05). Si el slot ya fue tomado por otra transacción
 * concurrente entre la verificación visual y el momento de la llamada al SP, MySQL
 * lanza un error que se convierte en esta excepción.</p>
 *
 * <p>También aplica cuando el trigger {@code trg_verificar_slot_disponible} detecta
 * que el slot ya tiene un turno activo al intentar insertar (RN-08).</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see com.sigvet.dao.TurnoDAO
 * @see com.sigvet.service.TurnoService
 */
public class TurnoOcupadoException extends Exception {

    /**
     * Constructor con mensaje de error por defecto.
     */
    public TurnoOcupadoException() {
        super("El horario seleccionado ya fue reservado por otro usuario. Por favor, elija otro horario.");
    }

    /**
     * Constructor con mensaje de error personalizado.
     *
     * @param mensaje mensaje descriptivo del conflicto de concurrencia
     */
    public TurnoOcupadoException(String mensaje) {
        super(mensaje);
    }
}
