package com.sigvet.model.enums;

/**
 * Indica el estado de actividad de un registro en el sistema SIGVET.
 *
 * <p>Se aplica a las entidades principales: {@code Veterinario}, {@code Dueno},
 * {@code Mascota} y {@code Medicamento}. Permite implementar baja lógica
 * (soft delete) sin eliminar datos del sistema, preservando la integridad
 * referencial y el historial de operaciones.</p>
 *
 * @author SIGVET
 * @version 1.0
 */
public enum EstadoRegistro {

    /** El registro está activo y operativo; aparece en listados y puede ser seleccionado. */
    Activo,

    /**
     * El registro fue dado de baja lógicamente; no aparece en los listados normales
     * pero se conserva en la BD para preservar el historial.
     */
    Inactivo
}
