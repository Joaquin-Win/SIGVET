package com.sigvet.exception;

/**
 * Excepción de dominio que se lanza cuando se intenta reservar un turno para una
 * mascota que no está registrada en el sistema o cuyo registro está inactivo.
 *
 * <p>Aplica la regla de negocio RN-04: todo turno debe estar asociado a una
 * mascota con registro activo en la BD. Esta excepción se lanza en la capa de
 * servicio antes de llamar al SP {@code sp_reservar_turno}, como validación
 * previa que proporciona feedback rápido al usuario.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see com.sigvet.service.TurnoService
 */
public class MascotaNoRegistradaException extends Exception {

    /**
     * Constructor con mensaje de error por defecto.
     */
    public MascotaNoRegistradaException() {
        super("No se puede reservar el turno: la mascota no está registrada en el sistema o su registro está inactivo.");
    }

    /**
     * Constructor con mensaje de error personalizado.
     *
     * @param mensaje mensaje descriptivo del error
     */
    public MascotaNoRegistradaException(String mensaje) {
        super(mensaje);
    }
}
