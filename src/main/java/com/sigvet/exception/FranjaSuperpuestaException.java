package com.sigvet.exception;

/**
 * Excepción de dominio que se lanza cuando se intenta configurar dos franjas horarias
 * que se superponen en la agenda de un veterinario.
 *
 * <p>La regla de negocio RN-11 prohíbe que un veterinario tenga franjas horarias
 * superpuestas en el mismo día de la semana. Esta validación ocurre en dos niveles:</p>
 * <ol>
 *   <li><strong>Java (feedback rápido):</strong> validación visual antes de enviar a la BD.</li>
 *   <li><strong>BD (barrera final):</strong> los triggers
 *       {@code trg_validar_franjas_no_superpuestas} (INSERT) y
 *       {@code trg_validar_franjas_no_superpuestas_update} (UPDATE) impiden la persistencia.</li>
 * </ol>
 *
 * <p>Se lanza al capturar una {@link java.sql.SQLException} cuyo mensaje contiene
 * las palabras clave {@code "superpuesta"} o {@code "superpone"}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see com.sigvet.dao.AgendaDAO
 * @see com.sigvet.service.AgendaService
 */
public class FranjaSuperpuestaException extends Exception {

    /**
     * Constructor con mensaje de error por defecto.
     */
    public FranjaSuperpuestaException() {
        super("Las franjas horarias se superponen. El veterinario ya tiene una franja configurada que se solapa con la nueva (RN-11).");
    }

    /**
     * Constructor con mensaje de error personalizado.
     *
     * @param mensaje mensaje descriptivo con el detalle de la superposición
     */
    public FranjaSuperpuestaException(String mensaje) {
        super(mensaje);
    }
}
