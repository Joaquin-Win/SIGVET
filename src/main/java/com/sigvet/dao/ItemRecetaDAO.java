package com.sigvet.dao;

import com.sigvet.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad {@link ItemReceta}.
 *
 * <p>Gestiona los ítems de receta asociados a consultas médicas.
 * Las inserciones se realizan con {@code PreparedStatement} directo
 * ya que no existe un SP para esta operación.</p>
 *
 * <p>Flujo de uso en CU-03 (Registrar Consulta):
 * <ol>
 *   <li>Crear consulta via {@code ConsultaMedicaDAO.registrarConsultaConTurno()}</li>
 *   <li>Por cada medicamento: llamar {@code StockDAO.descontarStockFifo()}</li>
 *   <li>Insertar el ítem con {@link #insertarItemReceta}</li>
 * </ol>
 * </p>
 *
 * @author SIGVET
 * @version 1.0
 * @see ItemReceta
 * @see BaseDAO
 */
public class ItemRecetaDAO extends BaseDAO<ItemReceta> {

    // =========================================================================
    // Métodos abstractos de BaseDAO
    // =========================================================================

    /**
     * Busca un ítem de receta por su identificador único.
     *
     * @param id identificador único del ítem (PK {@code id_item_receta})
     * @return ítem encontrado con consulta y stock completos, o {@code null}
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public ItemReceta buscarPorId(int id) throws SQLException {
        String sql = "SELECT ir.id_item_receta, ir.cantidad, ir.dosis, ir.frecuencia, "
                   + "       ir.duracion, ir.dispensado, "
                   + "       ir.id_consulta, ir.id_stock, "
                   + "       s.numero_lote, s.fecha_vencimiento, s.fecha_ingreso, s.cantidad AS cant_lote, "
                   + "       m.id_medicamento, m.nombre_generico, m.nombre_comercial, "
                   + "       m.dosis_presentacion, m.precio_venta, m.stock_minimo_alerta, "
                   + "       m.estado AS estado_med, m.fecha_actualizacion_precio "
                   + "FROM item_receta ir "
                   + "JOIN stock s ON s.id_stock = ir.id_stock "
                   + "JOIN medicamento m ON m.id_medicamento = s.id_medicamento "
                   + "WHERE ir.id_item_receta = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearItemReceta(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna todos los ítems de receta (de todas las consultas).
     *
     * @return lista de ítems
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public List<ItemReceta> buscarTodos() throws SQLException {
        String sql = "SELECT ir.id_item_receta, ir.cantidad, ir.dosis, ir.frecuencia, "
                   + "       ir.duracion, ir.dispensado, "
                   + "       ir.id_consulta, ir.id_stock, "
                   + "       s.numero_lote, s.fecha_vencimiento, s.fecha_ingreso, s.cantidad AS cant_lote, "
                   + "       m.id_medicamento, m.nombre_generico, m.nombre_comercial, "
                   + "       m.dosis_presentacion, m.precio_venta, m.stock_minimo_alerta, "
                   + "       m.estado AS estado_med, m.fecha_actualizacion_precio "
                   + "FROM item_receta ir "
                   + "JOIN stock s ON s.id_stock = ir.id_stock "
                   + "JOIN medicamento m ON m.id_medicamento = s.id_medicamento "
                   + "ORDER BY ir.id_consulta DESC, ir.id_item_receta ASC";
        List<ItemReceta> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearItemReceta(rs));
            }
        }
        return lista;
    }

    /**
     * Inserta un ítem de receta. Para el flujo de CU-03, preferir
     * {@link #insertarItemReceta(int, int, int, String, String, String)}.
     *
     * @param item ítem de receta a insertar
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void insertar(ItemReceta item) throws SQLException {
        insertarItemReceta(
            item.getConsulta().getIdConsulta(),
            item.getStock().getIdStock(),
            item.getCantidad(),
            item.getDosis(),
            item.getFrecuencia(),
            item.getDuracion()
        );
    }

    /**
     * Actualiza un ítem de receta (por ejemplo, marcar como dispensado).
     *
     * @param item ítem con los datos actualizados
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void actualizar(ItemReceta item) throws SQLException {
        String sql = "UPDATE item_receta SET cantidad = ?, dosis = ?, frecuencia = ?, "
                   + "duracion = ?, dispensado = ? WHERE id_item_receta = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getCantidad());
            ps.setString(2, item.getDosis());
            ps.setString(3, item.getFrecuencia());
            ps.setString(4, item.getDuracion());
            ps.setBoolean(5, item.isDispensado());
            ps.setInt(6, item.getIdItemReceta());
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // Métodos específicos
    // =========================================================================

    /**
     * Retorna todos los ítems de receta de una consulta específica.
     *
     * @param idConsulta identificador de la consulta médica
     * @return lista de ítems de la consulta
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<ItemReceta> buscarPorConsulta(int idConsulta) throws SQLException {
        String sql = "SELECT ir.id_item_receta, ir.cantidad, ir.dosis, ir.frecuencia, "
                   + "       ir.duracion, ir.dispensado, "
                   + "       ir.id_consulta, ir.id_stock, "
                   + "       s.numero_lote, s.fecha_vencimiento, s.fecha_ingreso, s.cantidad AS cant_lote, "
                   + "       m.id_medicamento, m.nombre_generico, m.nombre_comercial, "
                   + "       m.dosis_presentacion, m.precio_venta, m.stock_minimo_alerta, "
                   + "       m.estado AS estado_med, m.fecha_actualizacion_precio "
                   + "FROM item_receta ir "
                   + "JOIN stock s ON s.id_stock = ir.id_stock "
                   + "JOIN medicamento m ON m.id_medicamento = s.id_medicamento "
                   + "WHERE ir.id_consulta = ? ORDER BY ir.id_item_receta ASC";
        List<ItemReceta> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idConsulta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearItemReceta(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Inserta un ítem de receta directamente con {@code PreparedStatement}.
     *
     * <p>Este método implementa la inserción directa requerida por el flujo de CU-03.
     * NO existe un SP para {@code item_receta}; se inserta con SQL directo.</p>
     *
     * @param idConsulta identificador de la consulta médica (FK)
     * @param idStock    identificador del lote de stock descontado (FK)
     * @param cantidad   cantidad recetada
     * @param dosis      descripción de la dosis
     * @param frecuencia frecuencia de administración (puede ser nulo)
     * @param duracion   duración del tratamiento (puede ser nulo)
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public void insertarItemReceta(int idConsulta, int idStock, int cantidad,
                                    String dosis, String frecuencia, String duracion)
            throws SQLException {
        String sql = "INSERT INTO item_receta "
                   + "(id_consulta, id_stock, cantidad, dosis, frecuencia, duracion, dispensado) "
                   + "VALUES (?, ?, ?, ?, ?, ?, true)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idConsulta);
            ps.setInt(2, idStock);
            ps.setInt(3, cantidad);
            ps.setString(4, dosis);
            if (frecuencia != null && !frecuencia.trim().isEmpty()) {
                ps.setString(5, frecuencia);
            } else {
                ps.setNull(5, Types.VARCHAR);
            }
            if (duracion != null && !duracion.trim().isEmpty()) {
                ps.setString(6, duracion);
            } else {
                ps.setNull(6, Types.VARCHAR);
            }
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // Método privado de mapeo
    // =========================================================================

    /**
     * Mapea una fila del {@link ResultSet} a un objeto {@link ItemReceta}.
     *
     * <p>La {@link com.sigvet.model.ConsultaMedica} se construye mínimamente
     * (solo con ID) para evitar la carga recursiva completa del grafo de objetos.</p>
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto {@link ItemReceta} con stock y medicamento completos
     * @throws SQLException si ocurre un error al leer el ResultSet
     */
    private ItemReceta mapearItemReceta(ResultSet rs) throws SQLException {
        // Medicamento
        com.sigvet.model.enums.EstadoRegistro estMed =
            com.sigvet.model.enums.EstadoRegistro.valueOf(rs.getString("estado_med"));
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
        // Stock
        Stock stock = new Stock(
            rs.getInt("id_stock"),
            med,
            rs.getInt("cant_lote"),
            rs.getString("numero_lote"),
            rs.getDate("fecha_vencimiento").toLocalDate(),
            rs.getDate("fecha_ingreso").toLocalDate()
        );
        // ConsultaMedica (solo ID — carga mínima)
        ConsultaMedica consultaMin = new ConsultaMedica();
        consultaMin.setIdConsulta(rs.getInt("id_consulta"));
        // ItemReceta
        return new ItemReceta(
            rs.getInt("id_item_receta"),
            consultaMin,
            stock,
            rs.getInt("cantidad"),
            rs.getString("dosis"),
            rs.getString("frecuencia"),
            rs.getString("duracion"),
            rs.getBoolean("dispensado")
        );
    }
}
