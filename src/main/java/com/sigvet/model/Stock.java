package com.sigvet.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Representa un lote de stock de un {@link Medicamento} en el sistema SIGVET.
 *
 * <p>Cada registro de stock corresponde a un lote físico con número de lote único,
 * fecha de vencimiento y cantidad disponible. El sistema gestiona múltiples lotes
 * por medicamento (relación 1:N de {@link Medicamento} a {@link Stock}).</p>
 *
 * <p><strong>Reglas de negocio:</strong></p>
 * <ul>
 *   <li>RN-09: La fecha de vencimiento debe ser estrictamente mayor a la fecha de hoy.
 *       El trigger {@code trg_validar_vencimiento_stock} lanza error si no se cumple.</li>
 *   <li>RN-10: El descuento de stock se realiza en orden FIFO por fecha de vencimiento
 *       mediante {@code sp_descontar_stock_fifo}. Java NO reimplementa este orden.</li>
 *   <li>RN-06: Los triggers {@code trg_alerta_stock_bajo_insert/update} y
 *       {@code trg_alerta_vencimiento_insert} generan alertas automáticamente.</li>
 * </ul>
 *
 * <p><strong>Mapeo BD:</strong> tabla {@code stock}, PK {@code id_stock},
 * FK {@code id_medicamento → medicamento}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Medicamento
 * @see ItemReceta
 */
public class Stock {

    /** Identificador único del lote de stock (PK en BD). */
    private int idStock;

    /** Medicamento al que pertenece este lote (FK {@code id_medicamento → medicamento}). */
    private Medicamento medicamento;

    /** Cantidad de unidades disponibles en este lote. */
    private int cantidad;

    /** Número de lote del proveedor (identificador físico del lote). */
    private String numeroLote;

    /** Fecha de vencimiento del lote. Debe ser > hoy (RN-09). */
    private LocalDate fechaVencimiento;

    /** Fecha de ingreso del lote al sistema. */
    private LocalDate fechaIngreso;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío. La fecha de ingreso se inicializa con la fecha actual.
     */
    public Stock() {
        this.fechaIngreso = LocalDate.now();
    }

    /**
     * Constructor completo con todos los atributos del lote de stock.
     *
     * @param idStock          identificador único del lote (PK en BD)
     * @param medicamento      medicamento al que pertenece; no puede ser nulo
     * @param cantidad         cantidad de unidades; debe ser > 0
     * @param numeroLote       número de lote del proveedor; no puede ser nulo ni vacío
     * @param fechaVencimiento fecha de vencimiento del lote; debe ser > hoy (RN-09)
     * @param fechaIngreso     fecha de ingreso al sistema
     */
    public Stock(int idStock, Medicamento medicamento, int cantidad,
                 String numeroLote, LocalDate fechaVencimiento, LocalDate fechaIngreso) {
        this.idStock = idStock;
        setMedicamento(medicamento);
        setCantidad(cantidad);
        setNumeroLote(numeroLote);
        this.fechaVencimiento = fechaVencimiento; // validación hecha en ValidadorUtil o trigger
        this.fechaIngreso = (fechaIngreso != null) ? fechaIngreso : LocalDate.now();
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /**
     * Retorna el identificador único del lote de stock.
     *
     * @return ID del lote
     */
    public int getIdStock() {
        return idStock;
    }

    /**
     * Establece el identificador único del lote.
     *
     * @param idStock ID del lote; debe ser >= 0
     */
    public void setIdStock(int idStock) {
        if (idStock < 0) {
            throw new IllegalArgumentException("El ID de stock no puede ser negativo.");
        }
        this.idStock = idStock;
    }

    /**
     * Retorna el medicamento al que pertenece este lote.
     *
     * @return medicamento
     */
    public Medicamento getMedicamento() {
        return medicamento;
    }

    /**
     * Establece el medicamento del lote. No puede ser nulo.
     *
     * @param medicamento medicamento propietario del lote
     * @throws IllegalArgumentException si es nulo
     */
    public void setMedicamento(Medicamento medicamento) {
        if (medicamento == null) {
            throw new IllegalArgumentException("El medicamento del stock no puede ser nulo.");
        }
        this.medicamento = medicamento;
    }

    /**
     * Retorna la cantidad de unidades disponibles en este lote.
     *
     * @return cantidad de unidades
     */
    public int getCantidad() {
        return cantidad;
    }

    /**
     * Establece la cantidad de unidades. Debe ser >= 0 (puede llegar a 0 tras descuentos).
     *
     * @param cantidad cantidad de unidades
     * @throws IllegalArgumentException si es negativa
     */
    public void setCantidad(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad de stock no puede ser negativa.");
        }
        this.cantidad = cantidad;
    }

    /**
     * Retorna el número de lote del proveedor.
     *
     * @return número de lote
     */
    public String getNumeroLote() {
        return numeroLote;
    }

    /**
     * Establece el número de lote. No puede ser nulo ni vacío.
     *
     * @param numeroLote número de lote del proveedor
     * @throws IllegalArgumentException si es nulo o vacío
     */
    public void setNumeroLote(String numeroLote) {
        if (numeroLote == null || numeroLote.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de lote no puede ser nulo ni vacío.");
        }
        this.numeroLote = numeroLote.trim();
    }

    /**
     * Retorna la fecha de vencimiento del lote.
     *
     * @return fecha de vencimiento
     */
    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    /**
     * Establece la fecha de vencimiento del lote.
     * La validación de que sea &gt; hoy se delega a {@link com.sigvet.util.ValidadorUtil}
     * y al trigger {@code trg_validar_vencimiento_stock} en la BD (RN-09).
     *
     * @param fechaVencimiento fecha de vencimiento
     */
    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    /**
     * Retorna la fecha de ingreso del lote al sistema.
     *
     * @return fecha de ingreso
     */
    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    /**
     * Establece la fecha de ingreso del lote.
     *
     * @param fechaIngreso fecha de ingreso (no puede ser nula)
     */
    public void setFechaIngreso(LocalDate fechaIngreso) {
        if (fechaIngreso == null) {
            throw new IllegalArgumentException("La fecha de ingreso no puede ser nula.");
        }
        this.fechaIngreso = fechaIngreso;
    }

    // =========================================================================
    // Métodos de Object
    // =========================================================================

    /**
     * Representación en texto del lote de stock para la UI.
     *
     * @return descripción con lote, vencimiento y cantidad
     */
    @Override
    public String toString() {
        String nombreMed = (medicamento != null) ? medicamento.getNombreComercial() : "Sin medicamento";
        return nombreMed + " | Lote: " + numeroLote
            + " | Vence: " + fechaVencimiento
            + " | Cantidad: " + cantidad;
    }

    /**
     * Compara dos lotes de stock por su ID.
     *
     * @param obj objeto a comparar
     * @return {@code true} si tienen el mismo ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Stock otro = (Stock) obj;
        return this.idStock == otro.idStock;
    }

    /**
     * Hash code basado en el ID del lote.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(idStock);
    }
}
