package com.sigvet.model.enums;

/**
 * Representa el estado de una consulta médica veterinaria.
 *
 * <p>Conforme a la regla de negocio RN-07, las consultas médicas NUNCA se eliminan
 * físicamente de la base de datos. En su lugar, se aplica baja lógica cambiando su
 * estado a {@link #Inactiva} mediante el SP {@code sp_baja_logica_consulta}.</p>
 *
 * @author SIGVET
 * @version 1.0
 */
public enum EstadoConsulta {

    /** Consulta vigente, visible en el historial clínico y en reportes. */
    Activa,

    /** Consulta dada de baja lógicamente; no se elimina físicamente de la BD. */
    Inactiva
}
