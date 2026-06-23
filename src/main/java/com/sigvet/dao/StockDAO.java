package com.sigvet.dao;

import com.sigvet.exception.StockInsuficienteException;
import com.sigvet.exception.VencimientoInvalidoException;
import com.sigvet.model.Medicamento;
import com.sigvet.model.Stock;
import com.sigvet.model.enums.EstadoRegistro;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad {@link Stock}.
 *
 * <p>Gestiona los lotes de stock de medicamentos. Las operaciones críticas se
 * delegan a los SPs:
 * <ul>
 *   <li>{@code sp_ingresar_stock} – ingresa un lote y genera alertas via trigger</li>
 *   <li>{@code sp_descontar_stock_fifo} – descuenta en orden FIFO (RN-10)</li>
 *   <li>{@code fn_stock_total_medicamento} – función escalar de stock total</li>
 * </ul>
 *
 * @author SIGVET
 * @version 1.0
 * @see Stock
 * @see BaseDAO
 */
public class StockDAO extends BaseDAO<Stock> {

    /** SQL base con JOIN para construir objetos Stock completos. */
    private static final String SQL_SELECT_COMPLETO =
        "SELECT s.id_stock, s.cantidad, s.numero_lote, s.fecha_vencimiento, s.fecha_ingreso, "
      + "       m.id_medicamento, m.nombre_generico, m.nombre_comercial, "
      + "       m.dosis_presentacion, m.precio_venta, m.stock_minimo_alerta, "
      + "       m.estado AS estado_med, m.fecha_actualizacion_precio "
      + "FROM stock s "
      + "JOIN medicamento m ON m.id_medicamento = s.id_medicamento ";

    // =========================================================================
    // Métodos abstractos de BaseDAO
    // =========================================================================

    /**
     * Busca un lote de stock por su identificador único.
     *
     * @param id identificador único del lote (PK {@code id_stock})
     * @return lote con medicamento completo, o {@code null} si no existe
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public Stock buscarPorId(int id) throws SQLException {
        String sql = SQL_SELECT_COMPLETO + "WHERE s.id_stock = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearStock(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna todos los lotes de stock, ordenados por medicamento y vencimiento.
     *
     * @return lista de lotes
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public List<Stock> buscarTodos() throws SQLException {
        String sql = SQL_SELECT_COMPLETO
                   + "ORDER BY m.nombre_comercial ASC, s.fecha_vencimiento ASC";
        List<Stock> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearStock(rs));
            }
        }
        return lista;
    }

    /**
     * Inserta un lote de stock directamente. Para uso normal, preferir
     * {@link #ingresarStock} que usa el SP con validación de vencimiento y alertas.
     *
     * <p>Si el trigger {@code trg_validar_vencimiento_stock} rechaza la fecha de
     * vencimiento (RN-09), la {@link SQLException} resultante tendrá un mensaje
     * que contiene las palabras clave {@code "vencimiento"} o {@code "vencido"}.</p>
     *
     * @param s lote a insertar; el medicamento debe tener su ID asignado
     * @throws SQLException si la fecha de vencimiento es inválida (RN-09) u otro error de BD
     */
    @Override
    public void insertar(Stock s) throws SQLException {
        String sql = "INSERT INTO stock (id_medicamento, cantidad, numero_lote, "
                   + "fecha_vencimiento, fecha_ingreso) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getMedicamento().getIdMedicamento());
            ps.setInt(2, s.getCantidad());
            ps.setString(3, s.getNumeroLote());
            ps.setDate(4, Date.valueOf(s.getFechaVencimiento()));
            ps.setDate(5, Date.valueOf(s.getFechaIngreso()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    s.setIdStock(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            // Preservar el error original; la capa Service puede inspeccionar el mensaje
            // para convertirlo en VencimientoInvalidoException si es necesario.
            throw e;
        }
    }

    /**
     * Actualiza los datos de un lote de stock.
     *
     * @param s lote con los datos actualizados
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void actualizar(Stock s) throws SQLException {
        String sql = "UPDATE stock SET cantidad = ?, numero_lote = ?, "
                   + "fecha_vencimiento = ? WHERE id_stock = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, s.getCantidad());
            ps.setString(2, s.getNumeroLote());
            ps.setDate(3, Date.valueOf(s.getFechaVencimiento()));
            ps.setInt(4, s.getIdStock());
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // Métodos específicos — SPs y funciones
    // =========================================================================

    /**
     * Ingresa un nuevo lote de stock llamando al SP {@code sp_ingresar_stock}.
     *
     * <p>El SP valida la fecha de vencimiento y activa los triggers de alerta.
     * Tiene 5 parámetros:
     * (IN p_id_medicamento, IN p_cantidad, IN p_numero_lote,
     *  IN p_fecha_vencimiento, OUT p_id_stock).
     * El OUT se registra en la posición 5.</p>
     *
     * @param idMedicamento    identificador del medicamento
     * @param cantidad         cantidad de unidades del lote
     * @param numeroLote       número de lote del proveedor
     * @param fechaVencimiento fecha de vencimiento del lote (debe ser &gt; hoy, RN-09)
     * @return identificador del lote generado
     * @throws SQLException si ocurre un error de BD o el vencimiento es inválido
     * @throws VencimientoInvalidoException si el trigger rechaza la fecha (RN-09)
     */
    public int ingresarStock(int idMedicamento, int cantidad, String numeroLote,
                              LocalDate fechaVencimiento)
            throws SQLException, VencimientoInvalidoException {
        try (Connection conn = getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_ingresar_stock(?, ?, ?, ?, ?)}")) {
            cs.setInt(1, idMedicamento);
            cs.setInt(2, cantidad);
            cs.setString(3, numeroLote);
            cs.setDate(4, Date.valueOf(fechaVencimiento));
            cs.registerOutParameter(5, Types.INTEGER); // OUT p_id_stock
            cs.execute();
            return cs.getInt(5);
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("vencimiento") || msg.contains("vencido")) {
                throw new VencimientoInvalidoException(
                    "Fecha de vencimiento inválida: debe ser posterior a hoy (RN-09). "
                    + "Fecha proporcionada: " + fechaVencimiento);
            }
            throw e;
        }
    }

    /**
     * Descuenta stock en orden FIFO llamando al SP {@code sp_descontar_stock_fifo}.
     *
     * <p>El SP ordena los lotes por {@code fecha_vencimiento ASC} y descuenta de los
     * que vencen primero (RN-10). Si el stock total es insuficiente, el SP hace
     * ROLLBACK (RN-02).</p>
     *
     * <p>El SP tiene exactamente 2 parámetros:
     * (IN p_id_medicamento INT, IN p_cantidad INT).</p>
     *
     * @param idMedicamento identificador del medicamento
     * @param cantidad      cantidad a descontar
     * @throws SQLException              si ocurre un error de BD no relacionado con stock
     * @throws StockInsuficienteException si el stock disponible es insuficiente (RN-02)
     */
    public void descontarStockFifo(int idMedicamento, int cantidad)
            throws SQLException, StockInsuficienteException {
        try (Connection conn = getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_descontar_stock_fifo(?, ?)}")) {
            cs.setInt(1, idMedicamento);
            cs.setInt(2, cantidad);
            cs.execute();
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("stock") || msg.contains("insuficiente")) {
                throw new StockInsuficienteException(
                    "Stock insuficiente para descontar " + cantidad + " unidades (RN-02).");
            }
            throw e;
        }
    }

    /**
     * Obtiene el stock total (suma de todos los lotes no vencidos) de un medicamento
     * mediante la función escalar {@code fn_stock_total_medicamento}.
     *
     * <p>Se llama usando el patrón {@code {? = CALL fn_stock_total_medicamento(?)}}
     * registrando el primer parámetro como OUT con {@link Types#INTEGER}.</p>
     *
     * @param idMedicamento identificador del medicamento
     * @return stock total disponible en unidades
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public int stockTotal(int idMedicamento) throws SQLException {
        try (Connection conn = getConnection();
             CallableStatement cs = conn.prepareCall(
                     "{? = CALL fn_stock_total_medicamento(?)}")) {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setInt(2, idMedicamento);
            cs.execute();
            return cs.getInt(1);
        }
    }

    /**
     * Retorna todos los lotes de un medicamento específico, ordenados por vencimiento.
     *
     * @param idMedicamento identificador del medicamento
     * @return lista de lotes del medicamento
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Stock> buscarPorMedicamento(int idMedicamento) throws SQLException {
        String sql = SQL_SELECT_COMPLETO
                   + "WHERE s.id_medicamento = ? ORDER BY s.fecha_vencimiento ASC";
        List<Stock> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMedicamento);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearStock(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Retorna los lotes no vencidos y con stock disponible de un medicamento,
     * ordenados por fecha de vencimiento ascendente (orden FIFO).
     *
     * @param idMedicamento identificador del medicamento
     * @return lista de lotes disponibles en orden FIFO
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Stock> buscarNoVencidosPorMedicamento(int idMedicamento) throws SQLException {
        String sql = SQL_SELECT_COMPLETO
                   + "WHERE s.id_medicamento = ? "
                   + "  AND s.fecha_vencimiento > CURDATE() "
                   + "  AND s.cantidad > 0 "
                   + "ORDER BY s.fecha_vencimiento ASC";
        List<Stock> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMedicamento);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearStock(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Consulta la vista {@code vw_stock_medicamentos} y retorna el resumen de stock
     * por medicamento (total, disponible, vencido, estado de alerta).
     *
     * <p>Cada fila de la vista agrupa todos los lotes de un medicamento y calcula
     * los totales. Se usa para mostrar el panel de inventario (CU-04).</p>
     *
     * @return lista de filas de la vista como arrays de Object para la JTable
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Object[]> buscarStockMedicamentos() throws SQLException {
        String sql = "SELECT id_medicamento, nombre_generico, nombre_comercial, "
                   + "       dosis_presentacion, precio_venta, stock_minimo_alerta, "
                   + "       stock_total, stock_disponible, stock_vencido, "
                   + "       total_lotes, proximo_vencimiento, estado_stock "
                   + "FROM vw_stock_medicamentos";
        List<Object[]> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Date pv = rs.getDate("proximo_vencimiento");
                Object[] fila = {
                    rs.getInt("id_medicamento"),
                    rs.getString("nombre_comercial"),
                    rs.getString("nombre_generico"),
                    rs.getString("dosis_presentacion"),
                    rs.getDouble("precio_venta"),
                    rs.getInt("stock_minimo_alerta"),
                    rs.getInt("stock_total"),
                    rs.getInt("stock_disponible"),
                    rs.getInt("stock_vencido"),
                    rs.getInt("total_lotes"),
                    pv != null ? pv.toLocalDate().toString() : "—",
                    rs.getString("estado_stock")
                };
                lista.add(fila);
            }
        }
        return lista;
    }

    // =========================================================================
    // Método privado de mapeo
    // =========================================================================

    /**
     * Mapea una fila del {@link ResultSet} a un objeto {@link Stock} con medicamento completo.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto {@link Stock} con su medicamento
     * @throws SQLException si ocurre un error al leer el ResultSet
     */
    private Stock mapearStock(ResultSet rs) throws SQLException {
        EstadoRegistro estMed = EstadoRegistro.valueOf(rs.getString("estado_med"));
        Date fap = rs.getDate("fecha_actualizacion_precio");
        LocalDate fechaAct = (fap != null) ? fap.toLocalDate() : null;
        Medicamento med = new Medicamento(
            rs.getInt("id_medicamento"),
            rs.getString("nombre_generico"),
            rs.getString("nombre_comercial"),
            rs.getString("dosis_presentacion"),
            rs.getDouble("precio_venta"),
            rs.getInt("stock_minimo_alerta"),
            estMed,
            fechaAct
        );
        LocalDate fechaVenc = rs.getDate("fecha_vencimiento").toLocalDate();
        LocalDate fechaIng  = rs.getDate("fecha_ingreso").toLocalDate();
        return new Stock(
            rs.getInt("id_stock"),
            med,
            rs.getInt("cantidad"),
            rs.getString("numero_lote"),
            fechaVenc,
            fechaIng
        );
    }
}
