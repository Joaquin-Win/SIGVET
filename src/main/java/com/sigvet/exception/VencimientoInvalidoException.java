package com.sigvet.exception;

/**
 * Excepción de dominio que se lanza cuando la fecha de vencimiento de un lote de
 * stock es inválida, es decir, es igual o anterior a la fecha actual.
 *
 * <p>La regla de negocio RN-09 exige que todos los lotes ingresados tengan una
 * fecha de vencimiento estrictamente mayor a la fecha de hoy. Esta validación
 * se realiza en dos niveles:</p>
 * <ol>
 *   <li><strong>Java (feedback rápido):</strong> {@link com.sigvet.util.ValidadorUtil#validarFechaVencimiento(java.time.LocalDate)}
 *       valida antes de enviar el dato a la BD.</li>
 *   <li><strong>BD (barrera final):</strong> los triggers {@code trg_validar_vencimiento_stock}
 *       y {@code trg_validar_vencimiento_stock_update} impiden la persistencia.</li>
 * </ol>
 *
 * @author SIGVET
 * @version 1.0
 * @see com.sigvet.util.ValidadorUtil
 * @see com.sigvet.service.InventarioService
 */
public class VencimientoInvalidoException extends Exception {

    /**
     * Constructor con mensaje de error por defecto.
     */
    public VencimientoInvalidoException() {
        super("La fecha de vencimiento del lote es inválida: debe ser estrictamente mayor a la fecha actual (RN-09).");
    }

    /**
     * Constructor con mensaje de error personalizado.
     *
     * @param mensaje mensaje descriptivo con la fecha inválida proporcionada
     */
    public VencimientoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
