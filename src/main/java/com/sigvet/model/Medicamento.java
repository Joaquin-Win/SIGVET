package com.sigvet.model;

import com.sigvet.model.enums.EstadoRegistro;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Representa un medicamento del catálogo farmacéutico del sistema SIGVET.
 *
 * <p>Un medicamento describe la presentación comercial de un fármaco. El stock físico
 * se gestiona mediante lotes ({@link Stock}), donde cada lote tiene su propia
 * cantidad, número de lote y fecha de vencimiento.</p>
 *
 * <p><strong>Constantes de dominio:</strong></p>
 * <ul>
 *   <li>{@link #STOCK_MINIMO_DEFAULT}: umbral de alerta de stock bajo por defecto.</li>
 *   <li>{@link #DIAS_ALERTA_VENCIMIENTO}: días de anticipación para alertas de vencimiento.</li>
 * </ul>
 *
 * <p><strong>Reglas de negocio:</strong></p>
 * <ul>
 *   <li>RN-06: Los triggers {@code trg_alerta_stock_bajo_insert/update} y
 *       {@code trg_alerta_vencimiento_insert} generan alertas automáticas.</li>
 * </ul>
 *
 * <p><strong>Mapeo BD:</strong> tabla {@code medicamento}, PK {@code id_medicamento}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Stock
 * @see AlertaStock
 */
public class Medicamento {

    /**
     * Umbral de stock mínimo por defecto, usado si no se configura uno específico.
     * Valor: {@value} unidades.
     */
    public static final int STOCK_MINIMO_DEFAULT = 5;

    /**
     * Días de anticipación para generar alerta de vencimiento próximo.
     * Valor: {@value} días.
     */
    public static final int DIAS_ALERTA_VENCIMIENTO = 30;

    /** Identificador único del medicamento (PK en BD). */
    private int idMedicamento;

    /** Nombre genérico (principio activo) del medicamento. */
    private String nombreGenerico;

    /** Nombre comercial o de marca del medicamento. */
    private String nombreComercial;

    /** Descripción de la presentación y dosis (ej.: "Comprimidos 500mg"). */
    private String dosisPresentacion;

    /** Precio de venta unitario. */
    private double precioVenta;

    /**
     * Umbral de stock mínimo para generar alerta.
     * Por defecto: {@link #STOCK_MINIMO_DEFAULT}.
     */
    private int stockMinimoAlerta;

    /** Estado lógico del medicamento en el catálogo. */
    private EstadoRegistro estado;

    /** Fecha de la última actualización de precio (nullable). */
    private LocalDate fechaActualizacionPrecio;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío. Inicializa con valores por defecto:
     * {@link #stockMinimoAlerta} = {@link #STOCK_MINIMO_DEFAULT},
     * {@link #estado} = {@link EstadoRegistro#Activo}.
     */
    public Medicamento() {
        this.stockMinimoAlerta = STOCK_MINIMO_DEFAULT;
        this.estado = EstadoRegistro.Activo;
    }

    /**
     * Constructor completo con todos los atributos del medicamento.
     *
     * @param idMedicamento          identificador único (PK en BD)
     * @param nombreGenerico         nombre genérico / principio activo
     * @param nombreComercial        nombre comercial de marca
     * @param dosisPresentacion      descripción de la presentación
     * @param precioVenta            precio de venta (> 0)
     * @param stockMinimoAlerta      umbral de alerta de stock bajo
     * @param estado                 estado lógico del medicamento
     * @param fechaActualizacionPrecio fecha de última actualización de precio (nullable)
     */
    public Medicamento(int idMedicamento, String nombreGenerico, String nombreComercial,
                       String dosisPresentacion, double precioVenta, int stockMinimoAlerta,
                       EstadoRegistro estado, LocalDate fechaActualizacionPrecio) {
        this.idMedicamento = idMedicamento;
        setNombreGenerico(nombreGenerico);
        setNombreComercial(nombreComercial);
        this.dosisPresentacion = (dosisPresentacion != null) ? dosisPresentacion.trim() : "";
        setPrecioVenta(precioVenta);
        setStockMinimoAlerta(stockMinimoAlerta);
        this.estado = (estado != null) ? estado : EstadoRegistro.Activo;
        this.fechaActualizacionPrecio = fechaActualizacionPrecio;
    }

    /**
     * Constructor parcial sin fecha de actualización de precio.
     *
     * @param idMedicamento     identificador único
     * @param nombreGenerico    nombre genérico / principio activo
     * @param nombreComercial   nombre comercial de marca
     * @param dosisPresentacion descripción de la presentación
     * @param precioVenta       precio de venta
     * @param stockMinimoAlerta umbral de alerta
     * @param estado            estado lógico
     */
    public Medicamento(int idMedicamento, String nombreGenerico, String nombreComercial,
                       String dosisPresentacion, double precioVenta,
                       int stockMinimoAlerta, EstadoRegistro estado) {
        this(idMedicamento, nombreGenerico, nombreComercial, dosisPresentacion,
             precioVenta, stockMinimoAlerta, estado, null);
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /**
     * Retorna el identificador único del medicamento.
     *
     * @return ID del medicamento
     */
    public int getIdMedicamento() {
        return idMedicamento;
    }

    /**
     * Establece el identificador único del medicamento.
     *
     * @param idMedicamento ID; debe ser >= 0
     */
    public void setIdMedicamento(int idMedicamento) {
        if (idMedicamento < 0) {
            throw new IllegalArgumentException("El ID de medicamento no puede ser negativo.");
        }
        this.idMedicamento = idMedicamento;
    }

    /**
     * Retorna el nombre genérico del medicamento.
     *
     * @return nombre genérico (principio activo)
     */
    public String getNombreGenerico() {
        return nombreGenerico;
    }

    /**
     * Establece el nombre genérico. No puede ser nulo ni vacío.
     *
     * @param nombreGenerico nombre genérico
     * @throws IllegalArgumentException si es nulo o vacío
     */
    public void setNombreGenerico(String nombreGenerico) {
        if (nombreGenerico == null || nombreGenerico.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre genérico no puede ser nulo ni vacío.");
        }
        this.nombreGenerico = nombreGenerico.trim();
    }

    /**
     * Retorna el nombre comercial del medicamento.
     *
     * @return nombre comercial de marca
     */
    public String getNombreComercial() {
        return nombreComercial;
    }

    /**
     * Establece el nombre comercial. No puede ser nulo ni vacío.
     *
     * @param nombreComercial nombre comercial
     * @throws IllegalArgumentException si es nulo o vacío
     */
    public void setNombreComercial(String nombreComercial) {
        if (nombreComercial == null || nombreComercial.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre comercial no puede ser nulo ni vacío.");
        }
        this.nombreComercial = nombreComercial.trim();
    }

    /**
     * Retorna la descripción de la presentación y dosis.
     *
     * @return descripción de la presentación
     */
    public String getDosisPresentacion() {
        return dosisPresentacion;
    }

    /**
     * Establece la descripción de la presentación.
     *
     * @param dosisPresentacion descripción (puede ser vacío)
     */
    public void setDosisPresentacion(String dosisPresentacion) {
        this.dosisPresentacion = (dosisPresentacion != null) ? dosisPresentacion.trim() : "";
    }

    /**
     * Retorna el precio de venta unitario.
     *
     * @return precio de venta
     */
    public double getPrecioVenta() {
        return precioVenta;
    }

    /**
     * Establece el precio de venta. Debe ser mayor a 0.
     *
     * @param precioVenta precio de venta
     * @throws IllegalArgumentException si es <= 0
     */
    public void setPrecioVenta(double precioVenta) {
        if (precioVenta <= 0) {
            throw new IllegalArgumentException("El precio de venta debe ser mayor a 0.");
        }
        this.precioVenta = precioVenta;
    }

    /**
     * Retorna el umbral de stock mínimo para alertas.
     *
     * @return umbral de stock mínimo
     */
    public int getStockMinimoAlerta() {
        return stockMinimoAlerta;
    }

    /**
     * Establece el umbral de stock mínimo para alertas. Debe ser >= 0.
     *
     * @param stockMinimoAlerta umbral de stock
     * @throws IllegalArgumentException si es negativo
     */
    public void setStockMinimoAlerta(int stockMinimoAlerta) {
        if (stockMinimoAlerta < 0) {
            throw new IllegalArgumentException(
                "El stock mínimo de alerta no puede ser negativo.");
        }
        this.stockMinimoAlerta = stockMinimoAlerta;
    }

    /**
     * Retorna el estado lógico del medicamento.
     *
     * @return estado del medicamento
     */
    public EstadoRegistro getEstado() {
        return estado;
    }

    /**
     * Establece el estado lógico del medicamento.
     *
     * @param estado estado del medicamento; no puede ser nulo
     * @throws IllegalArgumentException si es nulo
     */
    public void setEstado(EstadoRegistro estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado del medicamento no puede ser nulo.");
        }
        this.estado = estado;
    }

    /**
     * Retorna la fecha de la última actualización de precio.
     *
     * @return fecha de actualización de precio (puede ser nula)
     */
    public LocalDate getFechaActualizacionPrecio() {
        return fechaActualizacionPrecio;
    }

    /**
     * Establece la fecha de la última actualización de precio.
     *
     * @param fechaActualizacionPrecio fecha (nullable)
     */
    public void setFechaActualizacionPrecio(LocalDate fechaActualizacionPrecio) {
        this.fechaActualizacionPrecio = fechaActualizacionPrecio;
    }

    // =========================================================================
    // Métodos de Object
    // =========================================================================

    /**
     * Representación en texto del medicamento para la UI.
     *
     * @return nombre comercial y genérico del medicamento
     */
    @Override
    public String toString() {
        return nombreComercial + " (" + nombreGenerico + ") - " + dosisPresentacion;
    }

    /**
     * Compara dos medicamentos por su ID.
     *
     * @param obj objeto a comparar
     * @return {@code true} si tienen el mismo ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Medicamento otro = (Medicamento) obj;
        return this.idMedicamento == otro.idMedicamento;
    }

    /**
     * Hash code basado en el ID del medicamento.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(idMedicamento);
    }
}
