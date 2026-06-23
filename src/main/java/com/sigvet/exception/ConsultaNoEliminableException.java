package com.sigvet.exception;

/**
 * Excepción de dominio que se lanza cuando se intenta eliminar físicamente una
 * consulta médica del sistema.
 *
 * <p>La regla de negocio RN-07 prohíbe estrictamente la eliminación física de
 * consultas médicas para preservar el historial clínico completo. El trigger
 * {@code trg_prevenir_eliminar_consulta} en la BD también impide esta operación
 * a nivel de base de datos.</p>
 *
 * <p>La operación correcta es la baja lógica mediante el SP
 * {@code sp_baja_logica_consulta}, que cambia el estado a {@code Inactiva}
 * sin eliminar el registro.</p>
 *
 * <p>El código Java de la capa DAO y Service NUNCA debe emitir un {@code DELETE}
 * sobre la tabla {@code consulta_medica}. Esta excepción se incluye como barrera
 * adicional si se detecta un intento erróneo en la capa Java.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see com.sigvet.dao.ConsultaDAO
 * @see com.sigvet.service.ConsultaMedicaService
 */
public class ConsultaNoEliminableException extends Exception {

    /**
     * Constructor con mensaje de error por defecto.
     */
    public ConsultaNoEliminableException() {
        super("No se puede eliminar una consulta médica (RN-07). Use la baja lógica para desactivarla.");
    }

    /**
     * Constructor con mensaje de error personalizado.
     *
     * @param mensaje mensaje descriptivo del intento de eliminación
     */
    public ConsultaNoEliminableException(String mensaje) {
        super(mensaje);
    }
}
