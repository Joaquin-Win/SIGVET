package com.sigvet.model;

import java.util.Objects;

/**
 * Representa un ítem de receta asociado a una {@link ConsultaMedica}.
 *
 * <p>Cada ítem de receta describe un medicamento prescripto en una consulta,
 * incluyendo la cantidad recetada, la pauta posológica (dosis, frecuencia, duración)
 * y si el medicamento fue dispensado al momento de la consulta.</p>
 *
 * <p><strong>Flujo de CU-03 (Registrar Consulta):</strong></p>
 * <ol>
 *   <li>Se crea la consulta mediante {@code sp_registrar_consulta_turno} o
 *       {@code sp_registrar_consulta_urgencia}.</li>
 *   <li>Por cada medicamento, se llama a {@code sp_descontar_stock_fifo} para
 *       descontar el stock en orden FIFO.</li>
 *   <li>Se inserta el {@code ItemReceta} directamente con {@code PreparedStatement}.</li>
 * </ol>
 *
 * <p><strong>Nota:</strong> el atributo {@code stock} referencia el lote específico
 * ({@link Stock}) del que se descontó la cantidad (FK {@code id_stock → stock}).
 * En la inserción real, el lote concreto lo determina {@code sp_descontar_stock_fifo}.</p>
 *
 * <p><strong>Mapeo BD:</strong> tabla {@code item_receta}, PK {@code id_item_receta}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see ConsultaMedica
 * @see Stock
 */
public class ItemReceta {

    /** Identificador único del ítem de receta (PK en BD). */
    private int idItemReceta;

    /** Consulta médica a la que pertenece este ítem (FK {@code id_consulta → consulta_medica}). */
    private ConsultaMedica consulta;

    /** Lote de stock del que se descontó el medicamento (FK {@code id_stock → stock}). */
    private Stock stock;

    /** Cantidad de unidades recetadas y descontadas del stock. */
    private int cantidad;

    /** Descripción de la dosis del medicamento (ej.: "1 comprimido", "5ml"). */
    private String dosis;

    /** Frecuencia de administración (ej.: "Cada 8 horas", nullable). */
    private String frecuencia;

    /** Duración del tratamiento (ej.: "7 días", nullable). */
    private String duracion;

    /**
     * Indica si el medicamento fue dispensado al paciente en el momento de la consulta.
     * Por defecto: {@code true}.
     */
    private boolean dispensado;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío. {@code dispensado} por defecto: {@code true}.
     */
    public ItemReceta() {
        this.dispensado = true;
    }

    /**
     * Constructor completo con todos los atributos del ítem de receta.
     *
     * @param idItemReceta identificador único (PK en BD)
     * @param consulta     consulta médica propietaria; no puede ser nula
     * @param stock        lote de stock descontado; no puede ser nulo
     * @param cantidad     cantidad recetada; debe ser > 0
     * @param dosis        descripción de la dosis; no puede ser nula ni vacía
     * @param frecuencia   frecuencia de administración (nullable)
     * @param duracion     duración del tratamiento (nullable)
     * @param dispensado   si fue dispensado en el momento
     */
    public ItemReceta(int idItemReceta, ConsultaMedica consulta, Stock stock,
                      int cantidad, String dosis, String frecuencia,
                      String duracion, boolean dispensado) {
        this.idItemReceta = idItemReceta;
        setConsulta(consulta);
        setStock(stock);
        setCantidad(cantidad);
        setDosis(dosis);
        this.frecuencia = frecuencia;
        this.duracion = duracion;
        this.dispensado = dispensado;
    }

    /**
     * Constructor parcial sin frecuencia, duración ni dispensado (usa valores por defecto).
     *
     * @param idItemReceta identificador único
     * @param consulta     consulta médica propietaria
     * @param stock        lote de stock
     * @param cantidad     cantidad recetada
     * @param dosis        descripción de la dosis
     */
    public ItemReceta(int idItemReceta, ConsultaMedica consulta, Stock stock,
                      int cantidad, String dosis) {
        this(idItemReceta, consulta, stock, cantidad, dosis, null, null, true);
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /**
     * Retorna el identificador único del ítem de receta.
     *
     * @return ID del ítem
     */
    public int getIdItemReceta() {
        return idItemReceta;
    }

    /**
     * Establece el identificador único del ítem.
     *
     * @param idItemReceta ID; debe ser >= 0
     */
    public void setIdItemReceta(int idItemReceta) {
        if (idItemReceta < 0) {
            throw new IllegalArgumentException("El ID de ítem de receta no puede ser negativo.");
        }
        this.idItemReceta = idItemReceta;
    }

    /**
     * Retorna la consulta médica propietaria de este ítem.
     *
     * @return consulta médica
     */
    public ConsultaMedica getConsulta() {
        return consulta;
    }

    /**
     * Establece la consulta médica propietaria. No puede ser nula.
     *
     * @param consulta consulta médica propietaria
     * @throws IllegalArgumentException si es nula
     */
    public void setConsulta(ConsultaMedica consulta) {
        if (consulta == null) {
            throw new IllegalArgumentException("La consulta del ítem de receta no puede ser nula.");
        }
        this.consulta = consulta;
    }

    /**
     * Retorna el lote de stock descontado.
     *
     * @return lote de stock
     */
    public Stock getStock() {
        return stock;
    }

    /**
     * Establece el lote de stock. No puede ser nulo.
     *
     * @param stock lote de stock descontado
     * @throws IllegalArgumentException si es nulo
     */
    public void setStock(Stock stock) {
        if (stock == null) {
            throw new IllegalArgumentException("El stock del ítem de receta no puede ser nulo.");
        }
        this.stock = stock;
    }

    /**
     * Retorna la cantidad de unidades recetadas.
     *
     * @return cantidad recetada
     */
    public int getCantidad() {
        return cantidad;
    }

    /**
     * Establece la cantidad recetada. Debe ser mayor a 0.
     *
     * @param cantidad cantidad de unidades
     * @throws IllegalArgumentException si es <= 0
     */
    public void setCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad del ítem de receta debe ser mayor a 0.");
        }
        this.cantidad = cantidad;
    }

    /**
     * Retorna la descripción de la dosis.
     *
     * @return descripción de la dosis
     */
    public String getDosis() {
        return dosis;
    }

    /**
     * Establece la descripción de la dosis. No puede ser nula ni vacía.
     *
     * @param dosis descripción de la dosis
     * @throws IllegalArgumentException si es nula o vacía
     */
    public void setDosis(String dosis) {
        if (dosis == null || dosis.trim().isEmpty()) {
            throw new IllegalArgumentException("La dosis del ítem de receta no puede ser nula ni vacía.");
        }
        this.dosis = dosis.trim();
    }

    /**
     * Retorna la frecuencia de administración.
     *
     * @return frecuencia (puede ser nula)
     */
    public String getFrecuencia() {
        return frecuencia;
    }

    /**
     * Establece la frecuencia de administración.
     *
     * @param frecuencia frecuencia (puede ser nula)
     */
    public void setFrecuencia(String frecuencia) {
        this.frecuencia = frecuencia;
    }

    /**
     * Retorna la duración del tratamiento.
     *
     * @return duración (puede ser nula)
     */
    public String getDuracion() {
        return duracion;
    }

    /**
     * Establece la duración del tratamiento.
     *
     * @param duracion duración (puede ser nula)
     */
    public void setDuracion(String duracion) {
        this.duracion = duracion;
    }

    /**
     * Indica si el medicamento fue dispensado al paciente.
     *
     * @return {@code true} si fue dispensado, {@code false} en caso contrario
     */
    public boolean isDispensado() {
        return dispensado;
    }

    /**
     * Establece si el medicamento fue dispensado.
     *
     * @param dispensado {@code true} si fue dispensado
     */
    public void setDispensado(boolean dispensado) {
        this.dispensado = dispensado;
    }

    // =========================================================================
    // Métodos de Object
    // =========================================================================

    /**
     * Representación en texto del ítem de receta.
     *
     * @return descripción con medicamento, dosis y cantidad
     */
    @Override
    public String toString() {
        String nombreMed = (stock != null && stock.getMedicamento() != null)
            ? stock.getMedicamento().getNombreComercial() : "Sin medicamento";
        return nombreMed + " | Dosis: " + dosis + " | Cantidad: " + cantidad
            + (frecuencia != null ? " | " + frecuencia : "");
    }

    /**
     * Compara dos ítems por su ID.
     *
     * @param obj objeto a comparar
     * @return {@code true} si tienen el mismo ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ItemReceta otro = (ItemReceta) obj;
        return this.idItemReceta == otro.idItemReceta;
    }

    /**
     * Hash code basado en el ID del ítem.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(idItemReceta);
    }
}
