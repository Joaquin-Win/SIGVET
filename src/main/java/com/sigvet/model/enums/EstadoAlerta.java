package com.sigvet.model.enums;

/**
 * Representa el estado de una alerta de stock en el sistema SIGVET.
 *
 * <p><strong>IMPORTANTE — Mapeo con la base de datos:</strong><br>
 * La columna {@code estado} de la tabla {@code alerta_stock} en MySQL usa el valor literal
 * {@code 'En Gestion'} (con espacio). Este enum usa {@link #En_Gestion} (con guion bajo)
 * para cumplir las convenciones de nomenclatura Java. La conversión se realiza mediante los
 * métodos {@link #toDbValue()} y {@link #fromDbValue(String)}.</p>
 *
 * <p>Ciclo de vida de una alerta:</p>
 * <ol>
 *   <li>{@link #Pendiente} – Generada automáticamente por triggers, aún no revisada.</li>
 *   <li>{@link #En_Gestion} – Un operador tomó conocimiento y está gestionando la situación.</li>
 *   <li>{@link #Resuelta} – El problema fue resuelto (stock repuesto o lote descartado).</li>
 * </ol>
 *
 * @author SIGVET
 * @version 1.0
 */
public enum EstadoAlerta {

    /** Alerta generada por trigger, pendiente de revisión por el personal. */
    Pendiente,

    /**
     * Alerta en gestión activa. Se almacena en la BD como {@code 'En Gestion'} (con espacio).
     * En Java se representa con guion bajo para respetar las convenciones de nomenclatura.
     */
    En_Gestion,

    /** Alerta resuelta; el stock fue repuesto o el lote vencido fue gestionado. */
    Resuelta;

    /**
     * Convierte el valor del enum al literal exacto esperado por la base de datos MySQL.
     *
     * <p>Ejemplo: {@code En_Gestion.toDbValue()} retorna {@code "En Gestion"}.</p>
     *
     * @return cadena con el valor en formato BD (guion bajo reemplazado por espacio)
     */
    public String toDbValue() {
        return this.name().replace("_", " ");
    }

    /**
     * Convierte un valor literal proveniente de la base de datos al enum correspondiente.
     *
     * <p>Ejemplo: {@code EstadoAlerta.fromDbValue("En Gestion")} retorna {@link #En_Gestion}.</p>
     *
     * @param dbValue valor leído del ResultSet (columna {@code estado} de {@code alerta_stock})
     * @return el enum {@link EstadoAlerta} correspondiente
     * @throws IllegalArgumentException si el valor no corresponde a ningún estado conocido
     */
    public static EstadoAlerta fromDbValue(String dbValue) {
        if (dbValue == null) {
            throw new IllegalArgumentException("El valor de estado de alerta no puede ser nulo.");
        }
        return EstadoAlerta.valueOf(dbValue.replace(" ", "_"));
    }
}
