package com.sigvet.model.enums;

/**
 * Clasifica el tipo de alerta generada automáticamente por los triggers de la base de datos.
 *
 * <p>Los triggers {@code trg_alerta_stock_bajo_insert}, {@code trg_alerta_stock_bajo_update},
 * {@code trg_alerta_vencimiento_insert} y {@code trg_alerta_vencimiento_update} generan
 * registros en {@code alerta_stock} usando estos valores como discriminador de tipo.</p>
 *
 * @author SIGVET
 * @version 1.0
 */
public enum TipoAlerta {

    /**
     * Alerta de stock bajo: el stock disponible (no vencido) cayó por debajo del umbral
     * configurado en {@code medicamento.stock_minimo_alerta}.
     */
    STOCK_BAJO,

    /**
     * Alerta de vencimiento próximo: existe un lote cuya {@code fecha_vencimiento} cae dentro
     * de los próximos {@code DIAS_ALERTA_VENCIMIENTO} días (constante definida en la BD como 30 días).
     */
    VENCIMIENTO_PROXIMO
}
