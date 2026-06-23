package com.sigvet.model;

import com.sigvet.model.enums.EstadoAlerta;
import com.sigvet.model.enums.TipoAlerta;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa una alerta de stock generada automáticamente por los triggers de la BD.
 *
 * <p>Las alertas se generan en dos escenarios (RN-06):</p>
 * <ol>
 *   <li><strong>Stock bajo:</strong> cuando el stock disponible (no vencido) de un
 *       medicamento cae por debajo de {@code medicamento.stock_minimo_alerta}.
 *       Triggers: {@code trg_alerta_stock_bajo_insert}, {@code trg_alerta_stock_bajo_update}.</li>
 *   <li><strong>Vencimiento próximo:</strong> cuando un lote vence dentro de los próximos
 *       {@link com.sigvet.model.Medicamento#DIAS_ALERTA_VENCIMIENTO} días.
 *       Trigger: {@code trg_alerta_vencimiento_insert}.</li>
 * </ol>
 *
 * <p><strong>Importante — Mapeo del estado:</strong> la columna {@code estado} en MySQL
 * usa el valor {@code 'En Gestion'} (con espacio). En Java se representa como
 * {@link EstadoAlerta#En_Gestion}. Usar siempre {@link EstadoAlerta#toDbValue()} y
 * {@link EstadoAlerta#fromDbValue(String)} para la conversión.</p>
 *
 * <p><strong>Mapeo BD:</strong> tabla {@code alerta_stock}, PK {@code id_alerta},
 * FK {@code id_medicamento → medicamento}. Vista: {@code vw_alertas_activas}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Medicamento
 * @see EstadoAlerta
 * @see TipoAlerta
 */
public class AlertaStock {

    /** Identificador único de la alerta (PK en BD). */
    private int idAlerta;

    /** Medicamento al que se refiere la alerta (FK {@code id_medicamento → medicamento}). */
    private Medicamento medicamento;

    /** Tipo de alerta: {@link TipoAlerta#STOCK_BAJO} o {@link TipoAlerta#VENCIMIENTO_PROXIMO}. */
    private TipoAlerta tipo;

    /** Mensaje descriptivo de la alerta (generado por el trigger). */
    private String mensaje;

    /** Estado de gestión de la alerta. */
    private EstadoAlerta estado;

    /** Timestamp en que se generó la alerta (registrado por el trigger). */
    private LocalDateTime fechaGeneracion;

    /**
     * Timestamp de resolución de la alerta (nullable).
     * Se completa cuando el estado cambia a {@link EstadoAlerta#Resuelta}.
     */
    private LocalDateTime fechaResolucion;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío. Estado por defecto: {@link EstadoAlerta#Pendiente}.
     */
    public AlertaStock() {
        this.estado = EstadoAlerta.Pendiente;
        this.fechaGeneracion = LocalDateTime.now();
    }

    /**
     * Constructor completo con todos los atributos de la alerta.
     *
     * @param idAlerta        identificador único (PK en BD)
     * @param medicamento     medicamento afectado; no puede ser nulo
     * @param tipo            tipo de alerta; no puede ser nulo
     * @param mensaje         mensaje descriptivo; no puede ser nulo ni vacío
     * @param estado          estado de la alerta
     * @param fechaGeneracion timestamp de generación
     * @param fechaResolucion timestamp de resolución (puede ser nulo)
     */
    public AlertaStock(int idAlerta, Medicamento medicamento, TipoAlerta tipo,
                       String mensaje, EstadoAlerta estado,
                       LocalDateTime fechaGeneracion, LocalDateTime fechaResolucion) {
        this.idAlerta = idAlerta;
        setMedicamento(medicamento);
        setTipo(tipo);
        setMensaje(mensaje);
        this.estado = (estado != null) ? estado : EstadoAlerta.Pendiente;
        this.fechaGeneracion = (fechaGeneracion != null) ? fechaGeneracion : LocalDateTime.now();
        this.fechaResolucion = fechaResolucion;
    }

    /**
     * Constructor parcial sin fecha de resolución.
     *
     * @param idAlerta        identificador único
     * @param medicamento     medicamento afectado
     * @param tipo            tipo de alerta
     * @param mensaje         mensaje descriptivo
     * @param estado          estado de la alerta
     * @param fechaGeneracion timestamp de generación
     */
    public AlertaStock(int idAlerta, Medicamento medicamento, TipoAlerta tipo,
                       String mensaje, EstadoAlerta estado, LocalDateTime fechaGeneracion) {
        this(idAlerta, medicamento, tipo, mensaje, estado, fechaGeneracion, null);
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /**
     * Retorna el identificador único de la alerta.
     *
     * @return ID de la alerta
     */
    public int getIdAlerta() {
        return idAlerta;
    }

    /**
     * Establece el identificador único de la alerta.
     *
     * @param idAlerta ID; debe ser >= 0
     */
    public void setIdAlerta(int idAlerta) {
        if (idAlerta < 0) {
            throw new IllegalArgumentException("El ID de alerta no puede ser negativo.");
        }
        this.idAlerta = idAlerta;
    }

    /**
     * Retorna el medicamento afectado por la alerta.
     *
     * @return medicamento
     */
    public Medicamento getMedicamento() {
        return medicamento;
    }

    /**
     * Establece el medicamento afectado. No puede ser nulo.
     *
     * @param medicamento medicamento afectado
     * @throws IllegalArgumentException si es nulo
     */
    public void setMedicamento(Medicamento medicamento) {
        if (medicamento == null) {
            throw new IllegalArgumentException("El medicamento de la alerta no puede ser nulo.");
        }
        this.medicamento = medicamento;
    }

    /**
     * Retorna el tipo de alerta.
     *
     * @return tipo de alerta
     */
    public TipoAlerta getTipo() {
        return tipo;
    }

    /**
     * Establece el tipo de alerta. No puede ser nulo.
     *
     * @param tipo tipo de alerta
     * @throws IllegalArgumentException si es nulo
     */
    public void setTipo(TipoAlerta tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de alerta no puede ser nulo.");
        }
        this.tipo = tipo;
    }

    /**
     * Retorna el mensaje descriptivo de la alerta.
     *
     * @return mensaje de la alerta
     */
    public String getMensaje() {
        return mensaje;
    }

    /**
     * Establece el mensaje descriptivo. No puede ser nulo ni vacío.
     *
     * @param mensaje mensaje de la alerta
     * @throws IllegalArgumentException si es nulo o vacío
     */
    public void setMensaje(String mensaje) {
        if (mensaje == null || mensaje.trim().isEmpty()) {
            throw new IllegalArgumentException("El mensaje de la alerta no puede ser nulo ni vacío.");
        }
        this.mensaje = mensaje.trim();
    }

    /**
     * Retorna el estado de gestión de la alerta.
     *
     * @return estado de la alerta
     */
    public EstadoAlerta getEstado() {
        return estado;
    }

    /**
     * Establece el estado de gestión. No puede ser nulo.
     *
     * @param estado estado de la alerta
     * @throws IllegalArgumentException si es nulo
     */
    public void setEstado(EstadoAlerta estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado de la alerta no puede ser nulo.");
        }
        this.estado = estado;
    }

    /**
     * Retorna el timestamp de generación de la alerta.
     *
     * @return timestamp de generación
     */
    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    /**
     * Establece el timestamp de generación.
     *
     * @param fechaGeneracion timestamp de generación
     */
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    /**
     * Retorna el timestamp de resolución de la alerta.
     *
     * @return timestamp de resolución (puede ser nulo)
     */
    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    /**
     * Establece el timestamp de resolución.
     *
     * @param fechaResolucion timestamp de resolución (nullable)
     */
    public void setFechaResolucion(LocalDateTime fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    // =========================================================================
    // Métodos de Object
    // =========================================================================

    /**
     * Representación en texto de la alerta para la UI.
     *
     * @return descripción con tipo, medicamento y estado
     */
    @Override
    public String toString() {
        String nombreMed = (medicamento != null) ? medicamento.getNombreComercial() : "Sin medicamento";
        return "[" + tipo + "] " + nombreMed + " - " + estado.toDbValue()
            + " | " + fechaGeneracion.toLocalDate();
    }

    /**
     * Compara dos alertas por su ID.
     *
     * @param obj objeto a comparar
     * @return {@code true} si tienen el mismo ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AlertaStock otra = (AlertaStock) obj;
        return this.idAlerta == otra.idAlerta;
    }

    /**
     * Hash code basado en el ID de la alerta.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(idAlerta);
    }
}
