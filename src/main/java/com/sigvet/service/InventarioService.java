package com.sigvet.service;

import com.sigvet.dao.MedicamentoDAO;
import com.sigvet.dao.StockDAO;
import com.sigvet.exception.VencimientoInvalidoException;
import com.sigvet.model.Medicamento;
import com.sigvet.model.Stock;
import com.sigvet.util.ValidadorUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de lógica de negocio para el caso de uso CU-04: Gestionar Inventario y Stock.
 *
 * <p>Orquesta las operaciones de alta, modificación y baja de medicamentos y lotes
 * de stock. El ingreso de stock delega al SP {@code sp_ingresar_stock} que valida
 * el vencimiento (trigger RN-09) y genera alertas automáticamente (trigger RN-06).</p>
 *
 * <p><strong>Responsabilidades:</strong></p>
 * <ul>
 *   <li>Validar campos obligatorios antes de ir a BD (feedback rápido).</li>
 *   <li>Delegar la lógica transaccional a los DAOs y SPs de BD.</li>
 *   <li>Convertir {@link SQLException} con mensajes de vencimiento en
 *       {@link VencimientoInvalidoException} amigable para el usuario.</li>
 * </ul>
 *
 * @author SIGVET
 * @version 1.0
 * @see MedicamentoDAO
 * @see StockDAO
 */
public class InventarioService {

    /** DAO para la gestión del catálogo de medicamentos. */
    private final MedicamentoDAO medicamentoDAO;

    /** DAO para la gestión de lotes de stock. */
    private final StockDAO stockDAO;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor que instancia los DAOs necesarios para la gestión de inventario.
     */
    public InventarioService() {
        this.medicamentoDAO = new MedicamentoDAO();
        this.stockDAO       = new StockDAO();
    }

    // =========================================================================
    // Métodos de negocio — Medicamentos
    // =========================================================================

    /**
     * Retorna todos los medicamentos activos del catálogo.
     *
     * @return lista de medicamentos con estado {@code Activo}
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Medicamento> obtenerMedicamentos() throws SQLException {
        return medicamentoDAO.buscarActivos();
    }

    /**
     * Retorna todos los medicamentos (activos e inactivos) del catálogo.
     *
     * @return lista completa de medicamentos
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Medicamento> obtenerTodosMedicamentos() throws SQLException {
        return medicamentoDAO.buscarTodos();
    }

    /**
     * Da de alta un nuevo medicamento en el catálogo del sistema.
     *
     * <p><strong>Validaciones previas:</strong></p>
     * <ul>
     *   <li>Nombre genérico, nombre comercial y dosis de presentación son obligatorios.</li>
     *   <li>El precio de venta debe ser mayor a 0.</li>
     *   <li>El stock mínimo de alerta debe ser mayor o igual a 0.</li>
     * </ul>
     *
     * @param m medicamento con los datos a registrar
     * @throws IllegalArgumentException si los datos son inválidos
     * @throws SQLException             si ocurre un error de BD (ej.: nombre duplicado)
     */
    public void altaMedicamento(Medicamento m)
            throws IllegalArgumentException, SQLException {
        // --- Validaciones de campos obligatorios ---
        ValidadorUtil.validarNoVacio(m.getNombreGenerico(),    "Nombre genérico");
        ValidadorUtil.validarNoVacio(m.getNombreComercial(),   "Nombre comercial");
        ValidadorUtil.validarNoVacio(m.getDosisPresentacion(), "Dosis/Presentación");
        ValidadorUtil.validarPrecioPositivo(m.getPrecioVenta());
        ValidadorUtil.validarMinimo(m.getStockMinimoAlerta(), 0, "Stock mínimo de alerta");

        medicamentoDAO.insertar(m);
    }

    /**
     * Modifica los datos de un medicamento existente.
     *
     * <p>Aplica las mismas validaciones que {@link #altaMedicamento}.</p>
     *
     * @param m medicamento con los datos actualizados; debe tener {@code idMedicamento} asignado
     * @throws IllegalArgumentException si los datos son inválidos
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public void modificarMedicamento(Medicamento m)
            throws IllegalArgumentException, SQLException {
        if (m.getIdMedicamento() <= 0) {
            throw new IllegalArgumentException("ID de medicamento inválido.");
        }
        ValidadorUtil.validarNoVacio(m.getNombreGenerico(),    "Nombre genérico");
        ValidadorUtil.validarNoVacio(m.getNombreComercial(),   "Nombre comercial");
        ValidadorUtil.validarNoVacio(m.getDosisPresentacion(), "Dosis/Presentación");
        ValidadorUtil.validarPrecioPositivo(m.getPrecioVenta());

        medicamentoDAO.actualizar(m);
    }

    /**
     * Aplica baja lógica a un medicamento cambiando su estado a {@code Inactivo}.
     *
     * <p>No elimina el registro para preservar la integridad de las recetas históricas.</p>
     *
     * @param id identificador del medicamento
     * @throws IllegalArgumentException si el ID es inválido
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public void bajaLogicaMedicamento(int id)
            throws IllegalArgumentException, SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de medicamento inválido.");
        }
        medicamentoDAO.bajaLogica(id);
    }

    /**
     * Busca medicamentos por nombre genérico o comercial (búsqueda parcial).
     *
     * @param nombre texto a buscar
     * @return lista de medicamentos que coinciden
     * @throws IllegalArgumentException si el texto de búsqueda está vacío
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public List<Medicamento> buscarMedicamentosPorNombre(String nombre)
            throws IllegalArgumentException, SQLException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("Ingrese al menos un carácter para buscar.");
        }
        return medicamentoDAO.buscarPorNombre(nombre.trim());
    }

    // =========================================================================
    // Métodos de negocio — Stock
    // =========================================================================

    /**
     * Retorna todos los lotes de stock de un medicamento específico.
     *
     * @param idMedicamento identificador del medicamento
     * @return lista de lotes ordenada por fecha de vencimiento
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Stock> obtenerStockPorMedicamento(int idMedicamento) throws SQLException {
        return stockDAO.buscarPorMedicamento(idMedicamento);
    }

    /**
     * Ingresa un nuevo lote de stock llamando al SP {@code sp_ingresar_stock}
     * (5 parámetros: 4 IN + 1 OUT {@code p_id_stock}).
     *
     * <p>El SP valida la fecha de vencimiento y activa los triggers que generan
     * alertas automáticas de stock bajo (RN-06) y vencimiento próximo.</p>
     *
     * <p><strong>Validaciones previas:</strong></p>
     * <ul>
     *   <li>Cantidad debe ser mayor a 0.</li>
     *   <li>Número de lote no puede ser vacío.</li>
     *   <li>Fecha de vencimiento debe ser estrictamente posterior a hoy (RN-09).</li>
     * </ul>
     *
     * @param idMedicamento    identificador del medicamento
     * @param cantidad         cantidad de unidades del lote
     * @param numeroLote       número de lote del proveedor
     * @param fechaVencimiento fecha de vencimiento (debe ser &gt; hoy)
     * @return identificador del lote generado por la BD
     * @throws IllegalArgumentException     si los datos son inválidos
     * @throws VencimientoInvalidoException si la fecha de vencimiento no es válida (RN-09)
     * @throws SQLException                 si ocurre otro error de BD
     */
    public int ingresarStock(int idMedicamento, int cantidad,
                              String numeroLote, LocalDate fechaVencimiento)
            throws IllegalArgumentException, VencimientoInvalidoException, SQLException {
        // --- Validaciones en Java (feedback rápido) ---
        if (idMedicamento <= 0) {
            throw new IllegalArgumentException("Debe seleccionar un medicamento válido.");
        }
        ValidadorUtil.validarCantidadPositiva(cantidad, "Cantidad");
        ValidadorUtil.validarNoVacio(numeroLote, "Número de lote");
        ValidadorUtil.validarFechaVencimiento(fechaVencimiento); // lanza VencimientoInvalidoException

        // --- Delegar al DAO que llama al SP sp_ingresar_stock ---
        try {
            return stockDAO.ingresarStock(idMedicamento, cantidad, numeroLote.trim(), fechaVencimiento);
        } catch (VencimientoInvalidoException e) {
            throw e; // Re-propagar excepción de dominio sin modificar
        } catch (SQLException e) {
            // Convertir error de trigger de vencimiento en excepción de dominio
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("vencimiento") || msg.contains("vencido")) {
                throw new VencimientoInvalidoException(
                    "La fecha de vencimiento ingresada no es válida (RN-09): " + fechaVencimiento);
            }
            throw e;
        }
    }

    /**
     * Retorna el stock total disponible (lotes no vencidos) de un medicamento
     * mediante la función escalar {@code fn_stock_total_medicamento}.
     *
     * @param idMedicamento identificador del medicamento
     * @return cantidad total disponible en unidades
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public int obtenerStockTotal(int idMedicamento) throws SQLException {
        return stockDAO.stockTotal(idMedicamento);
    }

    /**
     * Retorna el resumen de stock por medicamento desde la vista {@code vw_stock_medicamentos}.
     *
     * <p>Cada elemento del resultado es un {@code Object[]} con las columnas de la vista,
     * listo para cargar en la {@code JTable} del panel de inventario (CU-04).</p>
     *
     * @return lista de filas de la vista
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Object[]> obtenerResumenStock() throws SQLException {
        return stockDAO.buscarStockMedicamentos();
    }

    /**
     * Retorna los lotes no vencidos de un medicamento en orden FIFO
     * (los que vencen antes, primero).
     *
     * @param idMedicamento identificador del medicamento
     * @return lista de lotes disponibles en orden FIFO
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Stock> obtenerLotesNoVencidos(int idMedicamento) throws SQLException {
        return stockDAO.buscarNoVencidosPorMedicamento(idMedicamento);
    }
}
