package com.sigvet.service;

import com.sigvet.dao.AlertaStockDAO;
import com.sigvet.dao.MedicamentoDAO;
import com.sigvet.model.AlertaStock;
import com.sigvet.model.enums.EstadoAlerta;
import com.sigvet.model.enums.TipoAlerta;

import java.sql.SQLException;
import java.util.List;

/**
 * Servicio de lógica de negocio para el caso de uso CU-05: Gestionar Alertas de Stock.
 *
 * <p>Las alertas de stock son generadas automáticamente por los triggers de la BD:
 * <ul>
 *   <li>{@code trg_alerta_stock_bajo_insert} / {@code trg_alerta_stock_bajo_update}:
 *       cuando el stock disponible cae por debajo del mínimo (RN-06).</li>
 *   <li>{@code trg_alerta_vencimiento_insert}: al ingresar lotes próximos a vencer.</li>
 * </ul>
 *
 * <p>Este servicio SOLO consulta y actualiza el estado de las alertas generadas.
 * NO crea alertas manualmente.</p>
 *
 * <p><strong>Transiciones de estado válidas (RN):</strong></p>
 * <ul>
 *   <li>{@link EstadoAlerta#Pendiente} → {@link EstadoAlerta#En_Gestion}</li>
 *   <li>{@link EstadoAlerta#Pendiente} → {@link EstadoAlerta#Resuelta}</li>
 *   <li>{@link EstadoAlerta#En_Gestion} → {@link EstadoAlerta#Resuelta}</li>
 * </ul>
 *
 * <p><strong>Nota de mapeo BD:</strong> el valor {@code 'En Gestion'} en MySQL
 * corresponde al enum {@link EstadoAlerta#En_Gestion} en Java.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see AlertaStockDAO
 */
public class AlertaService {

    /** DAO para la gestión de alertas de stock. */
    private final AlertaStockDAO alertaDAO;

    /** DAO para información adicional de medicamentos (para mensajes de error). */
    private final MedicamentoDAO medicamentoDAO;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor que instancia los DAOs necesarios para la gestión de alertas.
     */
    public AlertaService() {
        this.alertaDAO      = new AlertaStockDAO();
        this.medicamentoDAO = new MedicamentoDAO();
    }

    // =========================================================================
    // Métodos de negocio
    // =========================================================================

    /**
     * Retorna todas las alertas activas (con estado {@code Pendiente} o {@code En Gestion}).
     *
     * <p>Utilizado para el panel de alertas del dashboard y la pantalla de gestión
     * de alertas (CU-05).</p>
     *
     * @return lista de alertas activas ordenada por fecha de generación descendente
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<AlertaStock> obtenerAlertasActivas() throws SQLException {
        return alertaDAO.buscarActivas();
    }

    /**
     * Retorna las alertas de un tipo específico ({@code STOCK_BAJO} o {@code VENCIMIENTO_PROXIMO}).
     *
     * @param tipo tipo de alerta a filtrar
     * @return lista de alertas del tipo especificado
     * @throws IllegalArgumentException si el tipo es nulo
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public List<AlertaStock> obtenerAlertasPorTipo(TipoAlerta tipo)
            throws IllegalArgumentException, SQLException {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de alerta no puede ser nulo.");
        }
        return alertaDAO.buscarPorTipo(tipo);
    }

    /**
     * Cambia el estado de una alerta con validación de transición.
     *
     * <p><strong>Transiciones válidas:</strong></p>
     * <ul>
     *   <li>{@code Pendiente → En_Gestion} (tomar la alerta para gestión)</li>
     *   <li>{@code Pendiente → Resuelta} (resolver directamente)</li>
     *   <li>{@code En_Gestion → Resuelta} (marcar como resuelta tras gestión)</li>
     * </ul>
     * <p>La transición inversa (ej.: {@code Resuelta → Pendiente}) no está permitida.</p>
     *
     * @param idAlerta    identificador de la alerta
     * @param nuevoEstado nuevo estado a asignar
     * @throws IllegalArgumentException si el estado es nulo o la transición es inválida
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public void cambiarEstadoAlerta(int idAlerta, EstadoAlerta nuevoEstado)
            throws IllegalArgumentException, SQLException {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado no puede ser nulo.");
        }
        if (idAlerta <= 0) {
            throw new IllegalArgumentException("ID de alerta inválido.");
        }

        // Verificar transición válida consultando el estado actual
        AlertaStock alertaActual = alertaDAO.buscarPorId(idAlerta);
        if (alertaActual == null) {
            throw new IllegalArgumentException("No existe una alerta con ID " + idAlerta + ".");
        }

        EstadoAlerta estadoActual = alertaActual.getEstado();

        // Validar transiciones de estado con switch
        boolean transicionValida;
        switch (estadoActual) {
            case Pendiente:
                transicionValida = (nuevoEstado == EstadoAlerta.En_Gestion
                                 || nuevoEstado == EstadoAlerta.Resuelta);
                break;
            case En_Gestion:
                transicionValida = (nuevoEstado == EstadoAlerta.Resuelta);
                break;
            case Resuelta:
                transicionValida = false; // Una alerta resuelta no puede cambiar de estado
                break;
            default:
                transicionValida = false;
        }

        if (!transicionValida) {
            throw new IllegalArgumentException(
                "Transición de estado inválida: '"
                + estadoActual.toDbValue() + "' → '" + nuevoEstado.toDbValue() + "'.");
        }

        alertaDAO.cambiarEstado(idAlerta, nuevoEstado);
    }

    /**
     * Marca una alerta como resuelta, registrando la fecha de resolución automáticamente.
     *
     * <p>Delega a {@link AlertaStockDAO#resolverAlerta(int)} que ejecuta
     * {@code UPDATE alerta_stock SET estado = 'Resuelta', fecha_resolucion = NOW()}.</p>
     *
     * @param idAlerta identificador de la alerta a resolver
     * @throws IllegalArgumentException si el ID es inválido
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public void resolverAlerta(int idAlerta)
            throws IllegalArgumentException, SQLException {
        if (idAlerta <= 0) {
            throw new IllegalArgumentException("ID de alerta inválido.");
        }
        alertaDAO.resolverAlerta(idAlerta);
    }

    /**
     * Cuenta el total de alertas con estado {@code Pendiente} o {@code En Gestion}.
     *
     * <p>Utilizado para mostrar el badge con el número de alertas activas en el
     * dashboard del sistema (indicador visual).</p>
     *
     * @return cantidad de alertas activas (pendientes + en gestión)
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public int contarAlertasPendientes() throws SQLException {
        List<AlertaStock> activas = alertaDAO.buscarActivas();
        int contador = 0;
        // Contar solo las pendientes o en gestión usando un for-each
        for (AlertaStock alerta : activas) {
            if (alerta.getEstado() == EstadoAlerta.Pendiente
                    || alerta.getEstado() == EstadoAlerta.En_Gestion) {
                contador++;
            }
        }
        return contador;
    }

    /**
     * Retorna una alerta específica por su ID.
     *
     * @param idAlerta identificador de la alerta
     * @return alerta encontrada, o {@code null} si no existe
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public AlertaStock buscarAlerta(int idAlerta) throws SQLException {
        return alertaDAO.buscarPorId(idAlerta);
    }

    /**
     * Retorna todas las alertas desde la vista {@code vw_alertas_activas}.
     *
     * @return lista de alertas activas enriquecidas con datos del medicamento
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<AlertaStock> obtenerAlertasActivasVista() throws SQLException {
        return alertaDAO.buscarAlertasActivas();
    }
}
