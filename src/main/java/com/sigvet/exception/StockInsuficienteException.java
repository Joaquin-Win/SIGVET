package com.sigvet.exception;

/**
 * Excepción de dominio que se lanza cuando el stock disponible de un medicamento
 * no es suficiente para completar la operación de dispensación solicitada.
 *
 * <p>Esta excepción se origina principalmente en el SP {@code sp_descontar_stock_fifo}
 * cuando la suma de unidades en todos los lotes no vencidos es menor a la cantidad
 * solicitada (RN-02). El SP realiza un ROLLBACK automático en este caso.</p>
 *
 * <p>En la capa Java, se lanza al capturar una {@link java.sql.SQLException} cuyo
 * mensaje contiene las palabras clave {@code "stock"} o {@code "insuficiente"}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see com.sigvet.dao.InventarioDAO
 * @see com.sigvet.service.InventarioService
 */
public class StockInsuficienteException extends Exception {

    /**
     * Constructor con mensaje de error por defecto.
     * Se usa cuando no se necesita contextualizar el error con datos específicos.
     */
    public StockInsuficienteException() {
        super("Stock insuficiente para completar la operación de dispensación.");
    }

    /**
     * Constructor con mensaje de error personalizado.
     * Permite proporcionar información específica sobre el medicamento y la cantidad.
     *
     * @param mensaje mensaje descriptivo del error (ej.: "Stock insuficiente de Amoxicilina: se necesitan 5, hay 2")
     */
    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
